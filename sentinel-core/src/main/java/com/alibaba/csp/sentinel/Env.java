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

import com.alibaba.csp.sentinel.init.InitExecutor;
import com.alibaba.csp.sentinel.init.InitFunc;

/**
 * <p>1、此类将触发Sentinel的所有初始化</p>
 *
 * <p>2、NOTE: to prevent deadlocks, other classes' static code block or static field should NEVER refer to this class.</p>
 *
 * @author jialiang.linjl
 */
public class Env {

    public static final Sph sph = new CtSph();

    /**
     * 只会执行一次，通过 SPI 机制 加载执行接口 {@link InitFunc} 的所有实现。 如果执行出现异常，JVM进程将退出
     */
    static {
        InitExecutor.doInit();
    }

}
