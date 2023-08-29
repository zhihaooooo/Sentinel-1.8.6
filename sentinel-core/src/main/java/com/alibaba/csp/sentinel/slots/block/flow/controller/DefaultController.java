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

import com.alibaba.csp.sentinel.node.Node;
import com.alibaba.csp.sentinel.node.OccupyTimeoutProperty;
import com.alibaba.csp.sentinel.slots.block.RuleConstant;
import com.alibaba.csp.sentinel.slots.block.flow.PriorityWaitException;
import com.alibaba.csp.sentinel.slots.block.flow.TrafficShapingController;
import com.alibaba.csp.sentinel.util.TimeUtil;

/**
 * <p>计数器算法</p>
 * <p>1、默认的 QPS 限流行为控制器（立即拒绝策略）</p>
 * <p>2、适用于明确知道系统的处理能力的情况，比如通过压测确定阈值</p>
 *
 * @author jialiang.linjl
 * @author Eric Zhao
 */
public class DefaultController implements TrafficShapingController {

    private static final int DEFAULT_AVG_USED_TOKENS = 0;

    private double count;
    private int grade;

    public DefaultController(double count, int grade) {
        this.count = count;
        this.grade = grade;
    }

    @Override
    public boolean canPass(Node node, int acquireCount) {
        return canPass(node, acquireCount, false);
    }

    @Override
    public boolean canPass(Node node, int acquireCount, boolean prioritized) {
        int curCount = avgUsedTokens(node);
        // 超过阈值
        if (curCount + acquireCount > count) {
            // 请求按优先级排序 && 规则限流阈值类型是 QPS，表示具有优先级的请求可以占用未来时间窗口的统计指标
            if (prioritized && grade == RuleConstant.FLOW_GRADE_QPS) {
                long currentTime;
                long waitInMs;
                currentTime = TimeUtil.currentTimeMillis();
                // 当前请求需要等待的时间，单位毫秒（占用未来的时间窗口）
                waitInMs = node.tryOccupyNext(currentTime, acquireCount, count);
                // 如果等待时间在限制可占用的最大时间范围内
                if (waitInMs < OccupyTimeoutProperty.getOccupyTimeout()) {
                    // 等待通过的请求总数 + acquireCount
                    node.addWaitingRequest(currentTime + waitInMs, acquireCount);
                    // 占用未来的 pass 指标数量
                    node.addOccupiedPass(acquireCount);
                    // 休眠等待，当前线程阻塞
                    sleep(waitInMs);

                    // 抛出 PriorityWaitException，表示当前请求是等待了 waitInMs 后通过的。
                    throw new PriorityWaitException(waitInMs);
                }
            }

            // （请求不按优先级排序 && 规则限流阈值类型是 QPS）、（请求按优先级排序 && 规则限流阈值类型是 Threads）
            return false;
        }
        return true;
    }

    /**
     * 如果规则的限流阈值类型是 Threads，返回 node 统计的当前并行占用的线程数
     * 如果规则的限流阈值类型是 QPS，返回 node 统计的当前时间窗口已经放行的请求数
     */
    private int avgUsedTokens(Node node) {
        if (node == null) {
            return DEFAULT_AVG_USED_TOKENS;
        }
        return grade == RuleConstant.FLOW_GRADE_THREAD ? node.curThreadNum() : (int)(node.passQps());
    }

    private void sleep(long timeMillis) {
        try {
            Thread.sleep(timeMillis);
        } catch (InterruptedException e) {
            // Ignore.
        }
    }
}
