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

import com.alibaba.csp.sentinel.context.Context;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.alibaba.csp.sentinel.slots.block.Rule;
import com.alibaba.csp.sentinel.slots.system.SystemRule;

/**
 *
 * <p>1、基本的 Sentinel API</p>
 * <p>2、概念上来说，一个资源将被一个 Entry 包装起来</p>
 * <p>3、demo，{@code "abc"} represent a unique name for the protected resource:</p>
 *
 * <pre>
 *  public void foo() {
 *     Entry entry = null;
 *     try {
 *        entry = SphU.entry("abc");
 *        // resource that need protection
 *     } catch (BlockException blockException) {
 *         // when goes there, it is blocked
 *         // add blocked handle logic here
 *     } catch (Throwable bizException) {
 *         // business exception
 *         Tracer.trace(bizException);
 *     } finally {
 *         // ensure finally be executed
 *         if (entry != null){
 *             entry.exit();
 *         }
 *     }
 *  }
 * </pre>
 *
 * <p>4、请确保调用 {@code SphU.entry()} 和 {@link Entry#exit()} 应该是同一个线程内的操作,否则将会抛出 {@link ErrorEntryFreeException}</p>
 * <P>5、在同一个 {@link Context} 中调用多次 SphU#entry() 将会产生一个调用树</P>
 *
 * @author jialiang.linjl
 * @author Eric Zhao
 * @see SphO
 */
public class SphU {

    private static final Object[] OBJECTS0 = new Object[0];

    private SphU() {}





    public static Entry entry(String name) throws BlockException {
        return Env.sph.entry(name, EntryType.OUT, 1, OBJECTS0);
    }

    public static Entry entry(String name, int batchCount) throws BlockException {
        return Env.sph.entry(name, EntryType.OUT, batchCount, OBJECTS0);
    }

    public static Entry entry(String name, EntryType trafficType) throws BlockException {
        return Env.sph.entry(name, trafficType, 1, OBJECTS0);
    }

    public static Entry entry(String name, EntryType trafficType, int batchCount) throws BlockException {
        return Env.sph.entry(name, trafficType, batchCount, OBJECTS0);
    }

    public static Entry entry(String name, EntryType trafficType, int batchCount, Object... args)
            throws BlockException {
        return Env.sph.entry(name, trafficType, batchCount, args);
    }






    public static Entry entry(Method method) throws BlockException {
        return Env.sph.entry(method, EntryType.OUT, 1, OBJECTS0);
    }

    public static Entry entry(Method method, int batchCount) throws BlockException {
        return Env.sph.entry(method, EntryType.OUT, batchCount, OBJECTS0);
    }

    public static Entry entry(Method method, EntryType trafficType) throws BlockException {
        return Env.sph.entry(method, trafficType, 1, OBJECTS0);
    }

    public static Entry entry(Method method, EntryType trafficType, int batchCount) throws BlockException {
        return Env.sph.entry(method, trafficType, batchCount, OBJECTS0);
    }

    public static Entry entry(Method method, EntryType trafficType, int batchCount, Object... args)
        throws BlockException {
        return Env.sph.entry(method, trafficType, batchCount, args);
    }






    public static AsyncEntry asyncEntry(String name) throws BlockException {
        return Env.sph.asyncEntry(name, EntryType.OUT, 1, OBJECTS0);
    }

    public static AsyncEntry asyncEntry(String name, EntryType trafficType) throws BlockException {
        return Env.sph.asyncEntry(name, trafficType, 1, OBJECTS0);
    }

    public static AsyncEntry asyncEntry(String name, EntryType trafficType, int batchCount, Object... args)
        throws BlockException {
        return Env.sph.asyncEntry(name, trafficType, batchCount, args);
    }






    public static Entry entryWithPriority(String name) throws BlockException {
        return Env.sph.entryWithPriority(name, EntryType.OUT, 1, true);
    }

    public static Entry entryWithPriority(String name, EntryType trafficType) throws BlockException {
        return Env.sph.entryWithPriority(name, trafficType, 1, true);
    }







    public static Entry entry(String name, int resourceType, EntryType trafficType) throws BlockException {
        return Env.sph.entryWithType(name, resourceType, trafficType, 1, OBJECTS0);
    }

    public static Entry entry(String name, int resourceType, EntryType trafficType, Object[] args)
        throws BlockException {
        return Env.sph.entryWithType(name, resourceType, trafficType, 1, args);
    }







    public static AsyncEntry asyncEntry(String name, int resourceType, EntryType trafficType)
        throws BlockException {
        return Env.sph.asyncEntryWithType(name, resourceType, trafficType, 1, false, OBJECTS0);
    }

    public static AsyncEntry asyncEntry(String name, int resourceType, EntryType trafficType, Object[] args)
        throws BlockException {
        return Env.sph.asyncEntryWithType(name, resourceType, trafficType, 1, false, args);
    }

    public static AsyncEntry asyncEntry(String name, int resourceType, EntryType trafficType, int batchCount,
                                        Object[] args) throws BlockException {
        return Env.sph.asyncEntryWithType(name, resourceType, trafficType, batchCount, false, args);
    }
}
