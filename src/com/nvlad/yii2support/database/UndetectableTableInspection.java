package com.nvlad.yii2support.database;


import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.jetbrains.php.PhpIndex;
import com.jetbrains.php.lang.inspections.PhpInspection;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import com.jetbrains.php.lang.psi.visitors.PhpElementVisitor;
import com.nvlad.yii2support.common.ClassUtils;
import com.nvlad.yii2support.common.DatabaseUtils;
import org.jetbrains.annotations.NotNull;

/**
 * Created by oleg on 06.04.2017.
 */
public class UndetectableTableInspection extends PhpInspection {
    @NotNull
    @Override
    public PsiElementVisitor buildVisitor(@NotNull ProblemsHolder problemsHolder, boolean b) {
        return new PhpElementVisitor() {
            @Override
            public void visitPhpClass(PhpClass clazz) {
                PhpIndex index = PhpIndex.getInstance(problemsHolder.getProject());
                if (DatabaseUtils.HasConnections(problemsHolder.getProject()) &&
                        ClassUtils.isClassInheritsOrEqual(clazz, ClassUtils.getClass(index, "\\yii\\db\\ActiveRecord"))) {
                    if (DatabaseUtils.getTableByActiveRecordClass(clazz) == null) {
                        problemsHolder.registerProblem(clazz.getChildren()[0], "Can not detect table for class " + clazz.getFQN(), ProblemHighlightType.WEAK_WARNING);
                    }
                }
                super.visitPhpClass(clazz);
            }
        };
    }
}
