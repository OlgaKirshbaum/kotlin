/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi

import com.intellij.lang.ASTNode
import kotlin.meta.Node

class KtQuotationWithExpression(node: ASTNode) : KtQuotation(node, saveIndents = false) {
    override fun astNodeByContent(content: String): Node {
        val parsed = metaTools.factory.createExpression(content)
        return metaTools.converter.convertExpr(parsed)
    }
}