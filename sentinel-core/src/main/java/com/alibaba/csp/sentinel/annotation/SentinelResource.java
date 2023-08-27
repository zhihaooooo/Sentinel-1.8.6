/*
 * Copyright 1999-2020 Alibaba Group Holding Ltd.
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
package com.alibaba.csp.sentinel.annotation;

import com.alibaba.csp.sentinel.EntryType;
import com.alibaba.csp.sentinel.ResourceTypeConstants;

import java.lang.annotation.*;

/**
 * 定义资源的注解。
 *
 * @author Eric Zhao
 * @author zhaoyuguang
 * @since 0.1.1
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface SentinelResource {

    // 资源名称
    String value() default "";

    // 资源调用类型，入口资源或者出口资源
    EntryType entryType() default EntryType.OUT;

    /** 资源分类，详见 {@link ResourceTypeConstants} */
    int resourceType() default 0;

    // 资源 的请求被 block 时的处理逻辑
    String blockHandler() default "";

    /**
     * The {@code blockHandler} is located in the same class with the original method by default.
     * However, if some methods share the same signature and intend to set the same block handler,
     * then users can set the class where the block handler exists. Note that the block handler method
     * must be static.
     *
     * @return the class where the block handler exists, should not provide more than one classes
     */
    Class<?>[] blockHandlerClass() default {};

    /**
     * @return name of the fallback function, empty by default
     */
    String fallback() default "";

    /**
     * The {@code defaultFallback} is used as the default universal fallback method.
     * It should not accept any parameters, and the return type should be compatible
     * with the original method.
     *
     * @return name of the default fallback method, empty by default
     * @since 1.6.0
     */
    String defaultFallback() default "";

    /**
     * The {@code fallback} is located in the same class with the original method by default.
     * However, if some methods share the same signature and intend to set the same fallback,
     * then users can set the class where the fallback function exists. Note that the shared fallback method
     * must be static.
     *
     * @return the class where the fallback method is located (only single class)
     * @since 1.6.0
     */
    Class<?>[] fallbackClass() default {};

    /**
     * @return the list of exception classes to trace, {@link Throwable} by default
     * @since 1.5.1
     */
    Class<? extends Throwable>[] exceptionsToTrace() default {Throwable.class};
    
    /**
     * Indicates the exceptions to be ignored. Note that {@code exceptionsToTrace} should
     * not appear with {@code exceptionsToIgnore} at the same time, or {@code exceptionsToIgnore}
     * will be of higher precedence.
     *
     * @return the list of exception classes to ignore, empty by default
     * @since 1.6.0
     */
    Class<? extends Throwable>[] exceptionsToIgnore() default {};
}
