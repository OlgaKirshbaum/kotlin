/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.psi;

import org.jetbrains.annotations.NotNull;

import java.util.List;

// TODO: Extend KtReplaceable if necessary methods implemented in all subclasses
public interface KtAnnotated extends KtElement /* KtReplaceable */ {
    default boolean isMacroAnnotated() {
        return getAnnotationEntries().size() == 1 && getAnnotationEntries().get(0).isMacroInvocation();
    }

    @NotNull
    List<KtAnnotation> getAnnotations();

    @NotNull
    List<KtAnnotationEntry> getAnnotationEntries();
}
