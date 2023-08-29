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

import com.alibaba.csp.sentinel.util.TimeUtil;
import com.alibaba.csp.sentinel.node.Node;
import com.alibaba.csp.sentinel.slots.block.flow.TrafficShapingController;

/**
 * <p>令牌桶算法</p>
 *
 * <p>
 * Sentinel的“预热”实现是基于Guava的算法。
 * 然而Guava的实现侧重于调整请求间隔，类似于漏桶。
 * Sentinel更加关注于控制每秒传入请求的计数而不计算其间隔，类似于令牌桶算法
 * </p>
 *
 * <p>
 * 存储桶中剩余的令牌用于测量系统效用。
 * 假设一个系统每秒可以处理b个请求。每秒钟b个令牌将添加到桶中，直到桶装满为止。
 * 当系统处理一个请求，它从桶中获取一个令牌。桶中剩余的令牌越少，系统的利用率越低；当令牌中的令牌数量高于某个阈值，我们称之为“饱和”状态
 * </p>
 *
 * <p>
 * 基于Guava的理论，有一个线性方程，y=m*x+b，其中y（也称为y（x））或qps（q））是我们期望的qps
 * *给定饱和周期（例如3分钟），m是从我们的冷（最小）速率到我们的稳定（最大）速率，x（或q）是已占用令牌。
 * </p>
 *
 * <p>前提是 限流阈值类型必须为 QPS </p>
 *
 * @author jialiang.linjl
 */
public class WarmUpController implements TrafficShapingController {

    // 限流阈值（QPS）
    protected double count;
    // 冷启动系数
    private int coldFactor;
    // 在稳定的令牌生产速率下，令牌桶中存储的令牌数
    protected int warningToken = 0;
    // 令牌桶的最大容量
    private int maxToken;
    // 斜率，每秒放行请求数的增长速率
    protected double slope;
    // 令牌桶当前存储的令牌数
    protected AtomicLong storedTokens = new AtomicLong(0);
    // 上一次生产令牌的时间
    protected AtomicLong lastFilledTime = new AtomicLong(0);

    public WarmUpController(double count, int warmUpPeriodInSec, int coldFactor) {
        construct(count, warmUpPeriodInSec, coldFactor);
    }

    public WarmUpController(double count, int warmUpPeriodInSec) {
        construct(count, warmUpPeriodInSec, 3);
    }

    private void construct(double count, int warmUpPeriodInSec, int coldFactor) {

        if (coldFactor <= 1) {
            throw new IllegalArgumentException("Cold factor should be larger than 1");
        }

        this.count = count;

        this.coldFactor = coldFactor;

        // thresholdPermits = 0.5 * warmupPeriod / stableInterval.
        // warningToken = 100;
        warningToken = (int)(warmUpPeriodInSec * count) / (coldFactor - 1);
        // / maxPermits = thresholdPermits + 2 * warmupPeriod /
        // (stableInterval + coldInterval)
        // maxToken = 200
        maxToken = warningToken + (int)(2 * warmUpPeriodInSec * count / (1.0 + coldFactor));

        // slope
        // slope = (coldIntervalMicros - stableIntervalMicros) / (maxPermits
        // - thresholdPermits);
        slope = (coldFactor - 1.0) / count / (maxToken - warningToken);

    }

    @Override
    public boolean canPass(Node node, int acquireCount) {
        return canPass(node, acquireCount, false);
    }

    @Override
    public boolean canPass(Node node, int acquireCount, boolean prioritized) {
        // 当前时间窗口通过的 QPS
        long passQps = (long) node.passQps();

        // 前一个时间窗口通过的 QPS
        long previousQps = (long) node.previousPassQps();
        syncToken(previousQps);

        // 开始计算它的斜率
        // 如果令牌桶中存放的令牌桶数量超过了警戒线，则进入到冷启动阶段，开始调整 QPS
        long restToken = storedTokens.get();// 当前令牌桶中的令牌数
        if (restToken >= warningToken) {
            long aboveToken = restToken - warningToken;
            // 消耗的速度要比warning快，但是要比慢
            // current interval = restToken*slope+1/count
            double warningQps = Math.nextUp(1.0 / (aboveToken * slope + 1.0 / count));
            // 小于 warningQps 才放行
            if (passQps + acquireCount <= warningQps) {
                return true;
            }
        } else {
            // 未超过警戒线的情况下按正常限流，如果放行当前请求之后会导致通过的 QPS 超过阈值，则拦截当前请求。
            // 否则放行
            if (passQps + acquireCount <= count) {
                return true;
            }
        }

        return false;
    }

    protected void syncToken(long passQps) {
        long currentTime = TimeUtil.currentTimeMillis();
        currentTime = currentTime - currentTime % 1000;
        long oldLastFillTime = lastFilledTime.get();
        // 控制每秒只更新一次
        if (currentTime <= oldLastFillTime) {
            return;
        }

        long oldValue = storedTokens.get();
        // 计算新的令牌桶中的令牌数
        long newValue = coolDownTokens(currentTime, passQps);

        if (storedTokens.compareAndSet(oldValue, newValue)) {
            // storedTokens 扣减上个时间窗口的 QPS
            long currentValue = storedTokens.addAndGet(0 - passQps);
            if (currentValue < 0) {
                storedTokens.set(0L);
            }
            lastFilledTime.set(currentTime);
        }

    }

    private long coolDownTokens(long currentTime, long passQps) {
        long oldValue = storedTokens.get();
        long newValue = oldValue;

        // 添加令牌的判断前提条件:
        // 当令牌的消耗程度远远低于警戒线的时候
        if (oldValue < warningToken) {
            newValue = (long)(oldValue + (currentTime - lastFilledTime.get()) * count / 1000);
        } else if (oldValue > warningToken) {
            if (passQps < (int)count / coldFactor) {
                newValue = (long)(oldValue + (currentTime - lastFilledTime.get()) * count / 1000);
            }
        }
        return Math.min(newValue, maxToken);
    }

}
