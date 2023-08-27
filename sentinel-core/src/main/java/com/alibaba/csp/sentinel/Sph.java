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

import java.lang.reflect.Method;

import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.alibaba.csp.sentinel.slots.system.SystemRule;

/**
 * 记录统计值和规则校验的基本接口
 *
 * @author qinan.qn
 * @author jialiang.linjl
 * @author leyou
 * @author Eric Zhao
 */
public interface Sph extends SphResourceTypeSupport {

    Entry entry(String name) throws BlockException;

    Entry entry(String name, int batchCount) throws BlockException;

    Entry entry(String name, EntryType trafficType) throws BlockException;

    Entry entry(String name, EntryType trafficType, int batchCount) throws BlockException;

    Entry entry(String name, EntryType trafficType, int batchCount, Object... args) throws BlockException;







    Entry entry(Method method) throws BlockException;

    Entry entry(Method method, int batchCount) throws BlockException;

    Entry entry(Method method, EntryType trafficType) throws BlockException;

    Entry entry(Method method, EntryType trafficType, int batchCount) throws BlockException;

    Entry entry(Method method, EntryType trafficType, int batchCount, Object... args) throws BlockException;








    AsyncEntry asyncEntry(String name, EntryType trafficType, int batchCount, Object... args) throws BlockException;

    Entry entryWithPriority(String name, EntryType trafficType, int batchCount, boolean prioritized) throws BlockException;

    Entry entryWithPriority(String name, EntryType trafficType, int batchCount, boolean prioritized, Object... args) throws BlockException;
}
