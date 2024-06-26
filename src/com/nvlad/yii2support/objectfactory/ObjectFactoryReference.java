package com.nvlad.yii2support.objectfactory;

import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReferenceBase;
import com.jetbrains.php.lang.psi.elements.ArrayCreationExpression;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import com.nvlad.yii2support.common.ClassUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Created by oleg on 14.03.2017.
 */
public class ObjectFactoryReference extends PsiReferenceBase<PsiElement> {
    ObjectFactoryReference(@NotNull PsiElement element)
    {
        super(element);
    }

    @Nullable
    @Override
    public PsiElement resolve() {
        PsiElement possibleArrayCreation = myElement.getParent().getParent().getParent();
        if (possibleArrayCreation instanceof ArrayCreationExpression arrayCreation) {
            PsiDirectory dir = myElement.getContainingFile().getContainingDirectory();
            PhpClass phpClass = ObjectFactoryUtils.findClassByArrayCreation(arrayCreation, dir);

            if (phpClass != null) {
                return ClassUtils.findWritableField(phpClass, myElement.getText());
            }

        }
        return null;
    }

    @NotNull
    @Override
    public Object @NotNull [] getVariants() {
        return new Object[0];
    }
}
