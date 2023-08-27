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
import java.util.List;

import com.alibaba.csp.sentinel.context.ContextUtil;
import com.alibaba.csp.sentinel.log.RecordLog;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.alibaba.csp.sentinel.slots.block.Rule;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeRuleManager;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRuleManager;
import com.alibaba.csp.sentinel.slots.system.SystemRule;
import com.alibaba.csp.sentinel.slots.system.SystemRuleManager;

/**
 *
 * <p>1、demo，{@code "abc"} represent a unique name for the
 * protected resource:
 * </p>
 *
 * <pre>
 * public void foo() {
 *    if (SphO.entry("abc")) {
 *        try {
 *            // business logic
 *        } finally {
 *            SphO.exit(); // must exit()
 *        }
 *    } else {
 *        // failed to enter the protected resource.
 *    }
 * }
 * </pre>
 *
 *
 * @author jialiang.linjl
 * @author leyou
 * @author Eric Zhao
 * @see SphU
 */
public class SphO {

    private static final Object[] OBJECTS0 = new Object[0];





    public static boolean entry(String name) {
        return entry(name, EntryType.OUT, 1, OBJECTS0);
    }

    public static boolean entry(String name, int batchCount) {
        return entry(name, EntryType.OUT, batchCount, OBJECTS0);
    }

    public static boolean entry(String name, EntryType type) {
        return entry(name, type, 1, OBJECTS0);
    }

    public static boolean entry(String name, EntryType type, int count) {
        return entry(name, type, count, OBJECTS0);
    }

    public static boolean entry(String name, EntryType trafficType, int batchCount, Object... args) {
        try {
            Env.sph.entry(name, trafficType, batchCount, args);
        } catch (BlockException e) {
            return false;
        } catch (Throwable e) {
            RecordLog.warn("SphO fatal error", e);
            return true;
        }
        return true;
    }







    public static boolean entry(Method method) {
        return entry(method, EntryType.OUT, 1, OBJECTS0);
    }

    public static boolean entry(Method method, int batchCount) {
        return entry(method, EntryType.OUT, batchCount, OBJECTS0);
    }

    public static boolean entry(Method method, EntryType type) {
        return entry(method, type, 1, OBJECTS0);
    }

    public static boolean entry(Method method, EntryType type, int count) {
        return entry(method, type, count, OBJECTS0);
    }

    public static boolean entry(Method method, EntryType trafficType, int batchCount, Object... args) {
        try {
            Env.sph.entry(method, trafficType, batchCount, args);
        } catch (BlockException e) {
            return false;
        } catch (Throwable e) {
            RecordLog.warn("SphO fatal error", e);
            return true;
        }
        return true;
    }





    public static void exit(int count, Object... args) {
        ContextUtil.getContext().getCurEntry().exit(count, args);
    }

    public static void exit(int count) {
        ContextUtil.getContext().getCurEntry().exit(count, OBJECTS0);
    }

    public static void exit() {
        exit(1, OBJECTS0);
    }
}
