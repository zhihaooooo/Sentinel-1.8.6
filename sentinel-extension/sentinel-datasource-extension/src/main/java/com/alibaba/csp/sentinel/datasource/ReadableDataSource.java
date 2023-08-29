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
package com.alibaba.csp.sentinel.datasource;

import com.alibaba.csp.sentinel.property.SentinelProperty;

/**
 * <p>1、可读数据源负责检索配置（只读）</p>
 * <p>2、参数化类型 S 代表用于装载从数据源中读取的配置类型，参数化类型 T 代表对应 sentinel 中的规则类型。
 * 例如，我们可以定义一个 FlowRuleProps 类，用于从数据源读取的限流规则配置，然后将 FlowRuleProps 实例转换为 FlowRule 实例，
 * 所以 S 可以被替换成 FlowRuleProps，T 可以被替换成 FlowRule。</p>
 *
 * @param <S> source data type
 * @param <T> target data type
 * @author leyou
 * @author Eric Zhao
 */
public interface ReadableDataSource<S, T> {

    /**
     * 加载配置并转换成 sentinel 中的规则类型
     */
    T loadConfig() throws Exception;

    /**
     * 从数据源加载数据，数据源可以是yml配置文件、mysql。。。。。具体的数据源由实现类决定。最终的 S 类型也由开发者自定义
     */
    S readSource() throws Exception;

    /**
     * Get {@link SentinelProperty} of the data source.
     */
    SentinelProperty<T> getProperty();

    /**
     * 停止加载规则数据
     */
    void close() throws Exception;
}
