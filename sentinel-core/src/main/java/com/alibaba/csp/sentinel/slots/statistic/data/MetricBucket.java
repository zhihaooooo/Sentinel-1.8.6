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
package com.alibaba.csp.sentinel.slots.statistic.data;

import com.alibaba.csp.sentinel.config.SentinelConfig;
import com.alibaba.csp.sentinel.slots.statistic.MetricEvent;
import java.util.concurrent.atomic.LongAdder;

/**
 * 表示一段时间内的指标数据
 *
 * @author jialiang.linjl
 * @author Eric Zhao
 */
public class MetricBucket {

    /**
     * 存储各项指标的计数，包括 被放行的请求数、被拒绝的请求数、请求异常总数、请求成功总数、总耗时、未来会被放行的请求数
     * 枚举类 MetricEvent 的每个指标对应的 ordinal 作为数组的下标，存储的对应的元素
     */
    private final LongAdder[] counters;

    /**
     * 记录最小耗时（最小响应时间）
     */
    private volatile long minRt;

    public MetricBucket() {
        MetricEvent[] events = MetricEvent.values();
        this.counters = new LongAdder[events.length];
        for (MetricEvent event : events) {
            counters[event.ordinal()] = new LongAdder();
        }
        initMinRt();
    }

    /**
     * 创建一个新的 MetricBucket，将旧的 MetricBucket 的统计结果填充到新的 MetricBucket 中
     */
    public MetricBucket reset(MetricBucket bucket) {
        for (MetricEvent event : MetricEvent.values()) {
            counters[event.ordinal()].reset();
            counters[event.ordinal()].add(bucket.get(event));
        }
        initMinRt();
        return this;
    }

    private void initMinRt() {
        this.minRt = SentinelConfig.statisticMaxRt();
    }

    /**
     * Reset the adders.
     *
     * @return new metric bucket in initial state
     */
    public MetricBucket reset() {
        for (MetricEvent event : MetricEvent.values()) {
            counters[event.ordinal()].reset();
        }
        initMinRt();
        return this;
    }

    public long get(MetricEvent event) {
        return counters[event.ordinal()].sum();
    }

    // MetricEvent 的 ordinal 值 对应 longAdder 数组下标的元素
    public MetricBucket add(MetricEvent event, long n) {
        counters[event.ordinal()].add(n);
        return this;
    }

    public long pass() {
        return get(MetricEvent.PASS);
    }

    public long occupiedPass() {
        return get(MetricEvent.OCCUPIED_PASS);
    }

    public long block() {
        return get(MetricEvent.BLOCK);
    }

    public long exception() {
        return get(MetricEvent.EXCEPTION);
    }

    public long rt() {
        return get(MetricEvent.RT);
    }

    public long minRt() {
        return minRt;
    }

    public long success() {
        return get(MetricEvent.SUCCESS);
    }

    public void addPass(int n) {
        add(MetricEvent.PASS, n);
    }

    public void addOccupiedPass(int n) {
        add(MetricEvent.OCCUPIED_PASS, n);
    }

    public void addException(int n) {
        add(MetricEvent.EXCEPTION, n);
    }

    public void addBlock(int n) {
        add(MetricEvent.BLOCK, n);
    }

    public void addSuccess(int n) {
        add(MetricEvent.SUCCESS, n);
    }

    public void addRT(long rt) {
        add(MetricEvent.RT, rt);

        // Not thread-safe, but it's okay.
        if (rt < minRt) {
            minRt = rt;
        }
    }

    @Override
    public String toString() {
        return "p: " + pass() + ", b: " + block() + ", w: " + occupiedPass();
    }
}
