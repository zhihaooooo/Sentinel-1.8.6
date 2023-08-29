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
package com.alibaba.csp.sentinel.slots.block.flow.controller;

import java.util.concurrent.atomic.AtomicLong;

import com.alibaba.csp.sentinel.slots.block.flow.TrafficShapingController;

import com.alibaba.csp.sentinel.util.TimeUtil;
import com.alibaba.csp.sentinel.node.Node;

/**
 * <p>漏桶算法</p>
 * <p>1、基于漏桶算法和虚拟队列等待机制实现的匀速限流效果。可理解为请求在虚拟队列中排队通过，每 count/1000毫秒 通过一个请求</p>
 * <p>2、虚拟队列的好处在于队列并非存在，但是在多核CPU并行处理请求时，可能会出现多个请求并排占用同一个位置的现象，实际通过的QPS会超出阈值，但不会超特别多</p>
 * <p>3、使用前提是 限流阈值类型为 QPS，并且阈值小于等于 1000。适合用于请求突发性增长后剧降的场景</p>
 *
 * @author jialiang.linjl
 */
public class RateLimiterController implements TrafficShapingController {

    // 虚拟队列的最大等待时长（默认500毫秒），排队等待时间超过这个值的请求会被拒绝
    private final int maxQueueingTimeMs;

    // 限流阈值（QPS）
    private final double count;

    /**
     * 最近一个请求通过的时间，用于计算下一个请求的预期通过时间，这是虚拟队列的核心实现。在虚拟队列中，将 latestPassedTime 回退一个时间间隔，相当于将虚拟队列中的一个元素移除。
     */
    private final AtomicLong latestPassedTime = new AtomicLong(-1);

    public RateLimiterController(int timeOut, double count) {
        this.maxQueueingTimeMs = timeOut;
        this.count = count;
    }

    @Override
    public boolean canPass(Node node, int acquireCount) {
        return canPass(node, acquireCount, false);
    }

    @Override
    public boolean canPass(Node node, int acquireCount, boolean prioritized) {
        // Pass when acquire count is less or equal than 0.
        if (acquireCount <= 0) {
            return true;
        }
        // Reject when count is less or equal than 0.
        // Otherwise,the costTime will be max of long and waitTime will overflow in some cases.
        if (count <= 0) {
            return false;
        }

        long currentTime = TimeUtil.currentTimeMillis();
        // 计算连续两个请求通过的时间间隔，即通过一个请求的固定速率，QPS为200时，cosTime为 5毫秒，也可以说每5毫秒通过一个请求就是固定速率
        long costTime = Math.round(1.0 * (acquireCount) / count * 1000);

        // 当前请求的期望通过时间
        long expectedTime = costTime + latestPassedTime.get();

        // 当前请求的期望通过时间 < 当前时间，请求可以通过
        if (expectedTime <= currentTime) {
            // Contention may exist here, but it's okay.
            latestPassedTime.set(currentTime);
            return true;
        } 
        // 当前请求的期望通过时间 => 当前时间
        else {
            // 需要休眠等待，等待时间为 期望通过时间 - 当前时间
            long waitTime = costTime + latestPassedTime.get() - TimeUtil.currentTimeMillis();
            // 如果等待时间 > 队列允许最大等待时间，请求被拒绝
            if (waitTime > maxQueueingTimeMs) {
                return false;
            } else {
                long oldTime = latestPassedTime.addAndGet(costTime);
                try {
                    waitTime = oldTime - TimeUtil.currentTimeMillis();
                    if (waitTime > maxQueueingTimeMs) {
                        latestPassedTime.addAndGet(-costTime);
                        return false;
                    }
                    // in race condition waitTime may <= 0
                    if (waitTime > 0) {
                        Thread.sleep(waitTime);
                    }
                    return true;
                } catch (InterruptedException e) {
                }
            }
        }
        return false;
    }

}
