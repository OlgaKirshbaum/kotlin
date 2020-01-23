/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi

import org.jetbrains.kotlin.psi.macros.MacroExpander
import org.jetbrains.kotlin.psi.macros.MetaTools
import org.jetbrains.kotlin.psi.psiUtil.forEachDescendantOfTypeVisitor
import kotlin.meta.Node

interface KtReplaceable : KtElement {
    var replacedElement: KtElement
    var hiddenElement: KtElement
    var metaTools: MetaTools

    val hasHiddenElementInitialized: Boolean
    var isHidden: Boolean
    var isRoot: Boolean

    fun convertToNode(): Node

    fun initializeHiddenElement(macroExpander: MacroExpander)

    fun createHiddenElementFromContent(content: String): KtElement
}

fun KtReplaceable.markHiddenRoot(replaced: KtReplaceable) {
    markHidden(replaced)
    isRoot = true
    containingKtFile.analysisContext = replaced.containingKtFile
}

fun KtReplaceable.markHidden(replaced: KtReplaceable) = accept(forEachDescendantOfTypeVisitor<KtReplaceable> {
    it.isHidden = true
    it.replacedElement = replaced
})