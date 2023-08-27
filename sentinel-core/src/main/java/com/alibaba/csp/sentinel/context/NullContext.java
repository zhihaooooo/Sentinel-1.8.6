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
package com.alibaba.csp.sentinel.context;

import com.alibaba.csp.sentinel.Constants;

/**
 * 如果 context 数量达到 {@link Constants#MAX_CONTEXT_NAME_SIZE}，再次创建 context 时将会返回一个 NullContext。也就意味着之后的资源请求并不会被规则校验。
 *
 * @author qinan.qn
 */
public class NullContext extends Context {

    public NullContext() {
        super(null, "null_context_internal");
    }
}
