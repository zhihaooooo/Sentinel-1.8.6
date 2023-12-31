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
package com.alibaba.csp.sentinel.node;

import com.alibaba.csp.sentinel.log.RecordLog;
import com.alibaba.csp.sentinel.property.SentinelProperty;
import com.alibaba.csp.sentinel.property.SimplePropertyListener;
import com.alibaba.csp.sentinel.slots.clusterbuilder.ClusterBuilderSlot;

/**
 * Holds statistic buckets count per second.
 *
 * @author jialiang.linjl
 * @author CarpenterLee
 */
public class SampleCountProperty {

    /**
     * <p>
     * 统计每秒的存储 bucket 的数量。这个变量决定了 QPS 计算的灵敏度。
     * 请通过 {@link #updateSampleCount(int)} 修改此值，否则不生效。
     * 注意此值一定要可以被 1000 整除
     * Statistic buckets count per second. This variable determines sensitivity of the QPS calculation.
     * DO NOT MODIFY this value directly, use {@link #updateSampleCount(int)}, otherwise the modification will not
     * take effect.
     * </p>
     * Node that this value must be divisor of 1000.
     */
    public static volatile int SAMPLE_COUNT = 2;

    public static void register2Property(SentinelProperty<Integer> property) {
        property.addListener(new SimplePropertyListener<Integer>() {
            @Override
            public void configUpdate(Integer value) {
                if (value != null) {
                    updateSampleCount(value);
                }
            }
        });
    }

    /**
     * 更新了 {@link #SAMPLE_COUNT}，如果新旧值不一致，所有的 {@link ClusterNode} 将被重构，反之不会，
     */
    public static void updateSampleCount(int newSampleCount) {
        if (newSampleCount != SAMPLE_COUNT) {
            SAMPLE_COUNT = newSampleCount;
            ClusterBuilderSlot.resetClusterNodes();
        }
        RecordLog.info("SAMPLE_COUNT updated to: {}", SAMPLE_COUNT);
    }
}
