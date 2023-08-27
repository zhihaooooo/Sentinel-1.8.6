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

import java.util.HashSet;
import java.util.Set;

import com.alibaba.csp.sentinel.log.RecordLog;
import com.alibaba.csp.sentinel.SphO;
import com.alibaba.csp.sentinel.SphU;
import com.alibaba.csp.sentinel.context.Context;
import com.alibaba.csp.sentinel.slotchain.ResourceWrapper;
import com.alibaba.csp.sentinel.slots.nodeselector.NodeSelectorSlot;

/**
 * <p>1、调用树上的资源节点类型都是 DefaultNode </p>
 * <p>2、DefaultNode 用来存储 指定资源 在 指定的 context 的指标统计结果，这样做的目的是，实现可按不同调用链入口对资源采取不同的流量控制策略</p>
 * <p>3、在同一个 context 调用多次 {@link SphU}#entry() 或 {@link SphO}@entry() 将会生成多个 DefaultNode，多个子 Node 将被放到 {@link DefaultNode#childList}</p>
 *
 * @author qinan.qn
 * @see NodeSelectorSlot
 */
public class DefaultNode extends StatisticNode {

    // todo 资源 和 DefaultNode 的关系 ？？？ 同一个 context 内 一个资源被访问多次，只会有一个 DefaultNode
    private ResourceWrapper id;

    private volatile Set<Node> childList = new HashSet<>();

    // todo 委托模式：ClusterNode 被 ClusterBuilderSlot 委托给 DefaultNode 统计指标数据
    private ClusterNode clusterNode;

    public DefaultNode(ResourceWrapper id, ClusterNode clusterNode) {
        this.id = id;
        this.clusterNode = clusterNode;
    }



    //
    public void addChild(Node node) {
        if (node == null) {
            RecordLog.warn("Trying to add null child to node <{}>, ignored", id.getName());
            return;
        }
        if (!childList.contains(node)) {
            synchronized (this) {
                if (!childList.contains(node)) {
                    Set<Node> newSet = new HashSet<>(childList.size() + 1);
                    newSet.addAll(childList);
                    newSet.add(node);
                    childList = newSet;
                }
            }
            RecordLog.info("Add child <{}> to node <{}>", ((DefaultNode)node).id.getName(), id.getName());
        }
    }

    /**
     * Reset the child node list.
     */
    public void removeChildList() {
        this.childList = new HashSet<>();
    }


    @Override
    public void increaseBlockQps(int count) {
        super.increaseBlockQps(count);
        this.clusterNode.increaseBlockQps(count);
    }

    @Override
    public void increaseExceptionQps(int count) {
        super.increaseExceptionQps(count);
        this.clusterNode.increaseExceptionQps(count);
    }

    @Override
    public void addRtAndSuccess(long rt, int successCount) {
        super.addRtAndSuccess(rt, successCount);
        this.clusterNode.addRtAndSuccess(rt, successCount);
    }

    @Override
    public void increaseThreadNum() {
        super.increaseThreadNum();
        this.clusterNode.increaseThreadNum();
    }

    @Override
    public void decreaseThreadNum() {
        super.decreaseThreadNum();
        this.clusterNode.decreaseThreadNum();
    }

    @Override
    public void addPassRequest(int count) {
        super.addPassRequest(count);
        this.clusterNode.addPassRequest(count);
    }

    public void printDefaultNode() {
        visitTree(0, this);
    }

    /**
     * 查看调用树
     */
    private void visitTree(int level, DefaultNode node) {
        for (int i = 0; i < level; ++i) {
            System.out.print("-");
        }
        if (!(node instanceof EntranceNode)) {
            System.out.println(
                String.format("%s(thread:%s pq:%s bq:%s tq:%s rt:%s 1mp:%s 1mb:%s 1mt:%s)", node.id.getShowName(),
                    node.curThreadNum(), node.passQps(), node.blockQps(), node.totalQps(), node.avgRt(),
                    node.totalRequest() - node.blockRequest(), node.blockRequest(), node.totalRequest()));
        } else {
            System.out.println(
                String.format("Entry-%s(t:%s pq:%s bq:%s tq:%s rt:%s 1mp:%s 1mb:%s 1mt:%s)", node.id.getShowName(),
                    node.curThreadNum(), node.passQps(), node.blockQps(), node.totalQps(), node.avgRt(),
                    node.totalRequest() - node.blockRequest(), node.blockRequest(), node.totalRequest()));
        }
        for (Node n : node.getChildList()) {
            DefaultNode dn = (DefaultNode)n;
            visitTree(level + 1, dn);
        }
    }



    public ResourceWrapper getId() {
        return id;
    }

    public ClusterNode getClusterNode() {
        return clusterNode;
    }

    // todo DefaultNode 的构造函数中已经定义了 ClusterNode，这里又开了个口子 ？？？
    public void setClusterNode(ClusterNode clusterNode) {
        this.clusterNode = clusterNode;
    }

    public Set<Node> getChildList() {
        return childList;
    }


}
