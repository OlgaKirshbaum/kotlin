/*
 * Copyright 2000-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve

import com.google.common.collect.Sets
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiRecursiveElementWalkingVisitor
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.MacroExpander
import org.jetbrains.kotlin.resolve.BindingContext.PACKAGE_TO_FILES
import org.jetbrains.kotlin.util.slicedMap.WritableSlice

interface FilePreprocessorExtension {
    fun preprocessFile(file: KtFile)
}

class FilePreprocessor(
    private val trace: BindingTrace,
    private val extensions: Iterable<FilePreprocessorExtension>
) {
    fun preprocessFile(file: KtFile, dependencies: Collection<String>) {
        MacroExpander.dependencies = dependencies
        registerFileByPackage(file)

        file.traverse { if (it is KtQuotation) it.initializeHiddenElement() }
        file.traverse { if (it is KtAnnotated && it is KtReplaceable) it.initializeHiddenElement() }

        for (extension in extensions) {
            extension.preprocessFile(file)
        }
    }

    private fun registerFileByPackage(file: KtFile) {
        // Register files corresponding to this package
        // The trace currently does not support bi-di multimaps that would handle this task nicer
        trace.addElementToSlice(PACKAGE_TO_FILES, file.packageFqName, file)
    }

    private fun KtFile.traverse(action: (PsiElement) -> Unit) =
        accept(object : PsiRecursiveElementWalkingVisitor() {
            override fun visitElement(element: PsiElement?) {
                super.visitElement(element)
                if (element != null) action(element)
            }
        })
}

fun <K, T> BindingTrace.addElementToSlice(
    slice: WritableSlice<K, MutableCollection<T>>, key: K, element: T
) {
    val elements = get(slice, key) ?: Sets.newIdentityHashSet()
    elements.add(element)
    record(slice, key, elements)
}
