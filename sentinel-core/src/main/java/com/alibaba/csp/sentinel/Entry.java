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
package com.alibaba.csp.sentinel;

import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.alibaba.csp.sentinel.util.TimeUtil;
import com.alibaba.csp.sentinel.util.function.BiConsumer;
import com.alibaba.csp.sentinel.context.ContextUtil;
import com.alibaba.csp.sentinel.node.Node;
import com.alibaba.csp.sentinel.slotchain.ResourceWrapper;
import com.alibaba.csp.sentinel.context.Context;

/**
 * <p>1、每次调用 {@link SphU}#entry() 将生成一个 {@link Entry}。</p>
 *
 * <p>2、此类持有调用过程中的部分信息：
 * <li>{@link Entry#createTimestamp}，这个 entry 的创建时间。</li>
 * <li>{@link Entry#curNode}，DefaultNode 类型，资源在当前 Context 的统计。</li>
 * <li>{@link Entry#originNode}，StatisticNode类型，来源的统计。</li>
 * <li>{@link Entry#resourceWrapper}，资源名称</li>
 * </p>
 *
 *
 * @author qinan.qn
 * @author jialiang.linjl
 * @author leyou(lihao)
 * @author Eric Zhao
 * @see SphU
 * @see Context
 * @see ContextUtil
 */
public abstract class Entry implements AutoCloseable {

    private static final Object[] OBJECTS0 = new Object[0];

    private final long createTimestamp;

    private long completeTimestamp;

    private Node curNode;

    private Node originNode;

    private Throwable error;
    private BlockException blockError;

    protected final ResourceWrapper resourceWrapper;

    public Entry(ResourceWrapper resourceWrapper) {
        this.resourceWrapper = resourceWrapper;
        this.createTimestamp = TimeUtil.currentTimeMillis();
    }

    public ResourceWrapper getResourceWrapper() {
        return resourceWrapper;
    }

    /** 结束对当前资源的访问并且记录到当前 {@link Context} */
    public void exit() throws ErrorEntryFreeException {
        exit(1, OBJECTS0);
    }

    public void exit(int count) throws ErrorEntryFreeException {
        exit(count, OBJECTS0);
    }

    /**
     * Equivalent to {@link #exit()}. Support try-with-resources since JDK 1.7.
     *
     * @since 1.5.0
     */
    @Override
    public void close() {
        exit();
    }

    /**
     * Exit this entry. This method should invoke if and only if once at the end of the resource protection.
     *
     * @param count tokens to release.
     * @param args extra parameters
     * @throws ErrorEntryFreeException, if {@link Context#getCurEntry()} is not this entry.
     */
    public abstract void exit(int count, Object... args) throws ErrorEntryFreeException;

    /**
     * Exit this entry.
     *
     * @param count tokens to release.
     * @param args extra parameters
     * @return next available entry after exit, that is the parent entry.
     * @throws ErrorEntryFreeException, if {@link Context#getCurEntry()} is not this entry.
     */
    protected abstract Entry trueExit(int count, Object... args) throws ErrorEntryFreeException;

    /**
     * Get related {@link Node} of the parent {@link Entry}.
     *
     * @return
     */
    public abstract Node getLastNode();

    public long getCreateTimestamp() {
        return createTimestamp;
    }

    public long getCompleteTimestamp() {
        return completeTimestamp;
    }

    public Entry setCompleteTimestamp(long completeTimestamp) {
        this.completeTimestamp = completeTimestamp;
        return this;
    }

    public Node getCurNode() {
        return curNode;
    }

    public void setCurNode(Node node) {
        this.curNode = node;
    }

    public BlockException getBlockError() {
        return blockError;
    }

    public Entry setBlockError(BlockException blockError) {
        this.blockError = blockError;
        return this;
    }

    public Throwable getError() {
        return error;
    }

    public void setError(Throwable error) {
        this.error = error;
    }

    /**
     * Get origin {@link Node} of the this {@link Entry}.
     *
     * @return origin {@link Node} of the this {@link Entry}, may be null if no origin specified by
     * {@link ContextUtil#enter(String name, String origin)}.
     */
    public Node getOriginNode() {
        return originNode;
    }

    public void setOriginNode(Node originNode) {
        this.originNode = originNode;
    }

    /**
     * Like {@code CompletableFuture} since JDK 8, it guarantees specified handler
     * is invoked when this entry terminated (exited), no matter it's blocked or permitted.
     * Use it when you did some STATEFUL operations on entries.
     * 
     * @param handler handler function on the invocation terminates
     * @since 1.8.0
     */
    public abstract void whenTerminate(BiConsumer<Context, Entry> handler);
    
}
