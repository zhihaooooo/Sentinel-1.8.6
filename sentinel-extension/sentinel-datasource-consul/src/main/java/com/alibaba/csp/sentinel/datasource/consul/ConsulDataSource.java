/*
 * Copyright 1999-2018 Alibaba Group Holding Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.csp.sentinel.datasource.consul;

import com.alibaba.csp.sentinel.concurrent.NamedThreadFactory;
import com.alibaba.csp.sentinel.datasource.AbstractDataSource;
import com.alibaba.csp.sentinel.datasource.Converter;
import com.alibaba.csp.sentinel.log.RecordLog;
import com.alibaba.csp.sentinel.util.AssertUtil;

import com.alibaba.csp.sentinel.util.StringUtil;
import com.ecwid.consul.v1.ConsulClient;
import com.ecwid.consul.v1.QueryParams;
import com.ecwid.consul.v1.Response;
import com.ecwid.consul.v1.kv.model.GetValue;

import java.util.concurrent.*;

/**
 * <p>1、数据源的初始数据是从 consul 中获取，然后会开启一个监听器监听 consul 中规则信息的变化，进而更新 内存 中的规则信息。</p>
 * <p>2、consul 没有提供 http API 来监听 KV 值的变化，但是我们可以使用其提供的类似长轮训的方式获取 KV 的变化。</p>
 * <p>3、<a href="https://www.consul.io/api/features/blocking.html">blocking queries</a>如果查询到的 index 比之前的要大，说明规则信息发生了变化。</p>
 *
 *
 * @author wavesZh
 * @author Zhiguo.Chen
 */
public class ConsulDataSource<T> extends AbstractDataSource<String, T> {

    private static final int DEFAULT_PORT = 8500;

    private final String address;
    private final String token;
    private final String ruleKey;

    // 长轮训的等待时间
    private final int watchTimeout;

    // 记录上次的 index 值，用于跟本次查询的 新 index 做比较，如果 新值 大于 旧值，说明规则发生了变化
    private volatile long lastIndex;

    private final ConsulClient client;

    private final ConsulKVWatcher watcher = new ConsulKVWatcher();

    @SuppressWarnings("PMD.ThreadPoolCreationRule")
    private final ExecutorService watcherService = Executors.newSingleThreadExecutor(
        new NamedThreadFactory("sentinel-consul-ds-watcher", true));

    public ConsulDataSource(String host, String ruleKey, int watchTimeoutInSecond, Converter<String, T> parser) {
        this(host, DEFAULT_PORT, ruleKey, watchTimeoutInSecond, parser);
    }

    
    public ConsulDataSource(String host, int port, String ruleKey, int watchTimeout, Converter<String, T> parser) {
        this(host, port, null, ruleKey, watchTimeout, parser);
    }

    /**
     * 构造函数中包括了 初次获取规则配置、线程池循环的执行长轮训任务
     */
    public ConsulDataSource(String host, int port, String token, String ruleKey, int watchTimeout, Converter<String, T> parser) {
        super(parser);
        AssertUtil.notNull(host, "Consul host can not be null");
        AssertUtil.notEmpty(ruleKey, "Consul ruleKey can not be empty");
        AssertUtil.isTrue(watchTimeout >= 0, "watchTimeout should not be negative");
        this.client = new ConsulClient(host, port);
        this.address = host + ":" + port;
        this.token = token;
        this.ruleKey = ruleKey;
        this.watchTimeout = watchTimeout;
        loadInitialConfig();
        startKVWatcher();
    }

    private void startKVWatcher() {
        watcherService.submit(watcher);
    }

    /**
     * 1、{@link AbstractDataSource#loadConfig()}
     * 2、{@link ConsulDataSource#readSource()}
     * 3、{@link AbstractDataSource#loadConfig(java.lang.Object)}
     *
     *
     * 1、{@link AbstractDataSource#getProperty()}
     * 2、{@link SentinelProperty#updateValue(java.lang.Object)}
     * 3、{@link DynamicSentinelProperty#updateValue(java.lang.Object)}
     */
    private void loadInitialConfig() {
        try {
            T newValue = loadConfig();
            if (newValue == null) {
                RecordLog.warn(
                    "[ConsulDataSource] WARN: initial config is null, you may have to check your data source");
            }
            getProperty().updateValue(newValue);
        } catch (Exception ex) {
            RecordLog.warn("[ConsulDataSource] Error when loading initial config", ex);
        }
    }

    /**
     * {@link ReadableDataSource} 定义，从数据源加载数据，数据源可以是yml配置文件、mysql。。。。。具体的数据源由实现类决定。
     * 这里是访问 consul API 获取的配置。
     */
    @Override
    public String readSource() throws Exception {
        if (this.client == null) {
            throw new IllegalStateException("Consul has not been initialized or error occurred");
        }
        Response<GetValue> response = getValueImmediately(ruleKey);
        if (response != null) {
            GetValue value = response.getValue();
            lastIndex = response.getConsulIndex();
            return value != null ? value.getDecodedValue() : null;
        }
        return null;
    }

    @Override
    public void close() throws Exception {
        watcher.stop();
        watcherService.shutdown();
    }

    private class ConsulKVWatcher implements Runnable {
        private volatile boolean running = true;

        @Override
        public void run() {
            while (running) {
                // It will be blocked until watchTimeout(s) if rule data has no update.
                Response<GetValue> response = getValue(ruleKey, lastIndex, watchTimeout);
                if (response == null) {
                    try {
                        TimeUnit.MILLISECONDS.sleep(watchTimeout * 1000);
                    } catch (InterruptedException e) {
                    }
                    continue;
                }
                GetValue getValue = response.getValue();
                Long currentIndex = response.getConsulIndex();
                if (currentIndex == null || currentIndex <= lastIndex) {
                    continue;
                }
                lastIndex = currentIndex;
                if (getValue != null) {
                    String newValue = getValue.getDecodedValue();
                    try {
                        getProperty().updateValue(parser.convert(newValue));
                        RecordLog.info("[ConsulDataSource] New property value received for ({}, {}): {}",
                            address, ruleKey, newValue);
                    } catch (Exception ex) {
                        // In case of parsing error.
                        RecordLog.warn("[ConsulDataSource] Failed to update value for ({}, {}), raw value: {}",
                            address, ruleKey, newValue);
                    }
                }
            }
        }

        private void stop() {
            running = false;
        }
    }

    /**
     * 相比长轮训，这里是期望立即获取到 consul 上的规则配置
     */
    private Response<GetValue> getValueImmediately(String key) {
        return getValue(key, -1, -1);
    }

    /**
     * 阻塞式请求 consul 获取配置
     */
    private Response<GetValue> getValue(String key, long index, long waitTime) {
        try {
            if (StringUtil.isNotBlank(token)) {
                return client.getKVValue(key, token, new QueryParams(waitTime, index));
            } else {
                return client.getKVValue(key, new QueryParams(waitTime, index));
            }
        } catch (Throwable t) {
            RecordLog.warn("[ConsulDataSource] Failed to get value for key: " + key, t);
        }
        return null;
    }

}
