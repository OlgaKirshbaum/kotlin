/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import java.lang.StringBuilder

abstract class KtQuotation(node: ASTNode, private val saveIndents: Boolean = true) : KtExpressionImpl(node), KtReplaceable {
    companion object {
        private const val INSERTION_PLACEHOLDER = "x"
    }

    override val replaceableTools = KtReplaceableTools(node)
    override var hiddenPsi: KtDotQualifiedExpression? = null

    override fun <R, D> accept(visitor: KtVisitor<R, D>, data: D): R = visitor.visitQuotation(this, data)

    fun getEntries(): List<PsiElement> =
        children.filter { it is KtSimpleNameStringTemplateEntry || it is KtBlockStringTemplateEntry }.toList()

    override fun createRealPsiContent(): String {
        val text = StringBuilder()
        var offset = firstChild.textLength
        val insertionsInfo = mutableMapOf<Int, String>()
        if (!saveIndents) {
            val firstChildWithContentText = firstChild.nextSibling.text
            offset += (firstChildWithContentText.length - firstChildWithContentText.trimStart().length)
        }

        for (child in children) {
            val content = getEntryContent(child)
            val childText = child.text
            if (child is KtSimpleNameStringTemplateEntry || child is KtBlockStringTemplateEntry) {
                insertionsInfo[child.startOffsetInParent - offset] = content
                text.append(INSERTION_PLACEHOLDER)
                offset += childText.length - INSERTION_PLACEHOLDER.length
            } else {
                text.append(childText)
            }
        }
        replaceableTools.converter.insertionsInfo = insertionsInfo
        return (if (saveIndents) text else text.trim()).toString()
    }

    private fun getEntryContent(entry: PsiElement): String = when (entry) {
        is KtSimpleNameStringTemplateEntry -> entry.firstChild.nextSibling.text
        is KtBlockStringTemplateEntry -> entry.text.removePrefix(entry.firstChild.text).removeSuffix(entry.lastChild.text)
        else -> entry.text
    }
}