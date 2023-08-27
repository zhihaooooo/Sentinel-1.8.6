/*
 * Copyright 1999-2019 Alibaba Group Holding Ltd.
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

/**
 * @author Eric Zhao
 * @since 1.5.0
 */
public interface OccupySupport {

    /**
     * 尝试占用下一个时间窗口的令牌，如果占用成功，则返回一个小于 OccupyTimeoutProperty.occupyTimeout 的值
     * Try to occupy latter time windows' tokens. If occupy success, a value less than
     * {@code occupyTimeout} in {@link OccupyTimeoutProperty} will be return.
     *
     * <p>
     * 每次占用下一个时间窗口的令牌，当前的线程都要休眠一段时间
     * Each time we occupy tokens of the future window, current thread should sleep for the
     * corresponding time for smoothing QPS. We can't occupy tokens of the future with unlimited,
     * the sleep time limit is {@code occupyTimeout} in {@link OccupyTimeoutProperty}.
     * </p>
     *
     * @param currentTime  current time millis.
     * @param acquireCount tokens count to acquire.
     * @param threshold    qps threshold.
     * @return time should sleep. Time >= {@code occupyTimeout} in {@link OccupyTimeoutProperty} means
     * occupy fail, in this case, the request should be rejected immediately.
     */
    long tryOccupyNext(long currentTime, int acquireCount, double threshold);

    /**
     * Get current waiting amount. Useful for debug.
     *
     * @return current waiting amount
     */
    long waiting();

    /**
     * Add request that occupied.
     *
     * @param futureTime   future timestamp that the acquireCount should be added on.
     * @param acquireCount tokens count.
     */
    void addWaitingRequest(long futureTime, int acquireCount);

    /**
     * Add occupied pass request, which represents pass requests that borrow the latter windows' token.
     *
     * @param acquireCount tokens count.
     */
    void addOccupiedPass(int acquireCount);

    /**
     * Get current occupied pass QPS.
     *
     * @return current occupied pass QPS
     */
    double occupiedPassQps();
}
