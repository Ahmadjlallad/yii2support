package com.nvlad.yii2support.views.completion;

import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.ProcessingContext;
import com.intellij.util.indexing.FileBasedIndex;
import com.jetbrains.php.lang.psi.elements.MethodReference;
import com.jetbrains.php.lang.psi.elements.ParameterList;
import com.nvlad.yii2support.common.PhpUtil;
import com.nvlad.yii2support.utils.Yii2SupportSettings;
import com.nvlad.yii2support.views.entities.ViewInfo;
import com.nvlad.yii2support.views.entities.ViewResolve;
import com.nvlad.yii2support.views.entities.ViewResolveFrom;
import com.nvlad.yii2support.views.index.ViewFileIndex;
import com.nvlad.yii2support.views.util.ViewUtil;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Created by NVlad on 27.12.2016.
 */
class CompletionProvider extends com.intellij.codeInsight.completion.CompletionProvider<CompletionParameters> {
    @Override
    protected void addCompletions(@NotNull CompletionParameters completionParameters,
                                  @NotNull ProcessingContext processingContext,
                                  @NotNull CompletionResultSet completionResultSet) {

        final PsiElement psiElement = completionParameters.getPosition();
        final MethodReference method = PsiTreeUtil.getParentOfType(psiElement, MethodReference.class);

        if (method == null || method.getParameters().length == 0) {
            return;
        }

        if (!ViewUtil.isValidRenderMethod(method)) {
            return;
        }

        PsiElement viewParameter = psiElement;
        while (viewParameter != null && !(viewParameter.getParent() instanceof ParameterList)) {
            viewParameter = viewParameter.getParent();
        }

        if (viewParameter == null || !viewParameter.equals(method.getParameters()[0])) {
            return;
        }

        final ViewResolve resolve = ViewUtil.resolveView(viewParameter);
        if (resolve == null) {
            return;
        }

        final Project project = psiElement.getProject();
        final GlobalSearchScope scope = GlobalSearchScope.projectScope(project);
        final FileBasedIndex fileBasedIndex = FileBasedIndex.getInstance();

        int prefixLength = resolve.key.length();
        int lastSlashPosition = resolve.key.lastIndexOf('/');
        if (lastSlashPosition != -1 && !resolve.key.endsWith("/")) {
            prefixLength = lastSlashPosition + 1;
        }

        if (!completionParameters.isAutoPopup()) {
            if (completionResultSet.getPrefixMatcher().getPrefix().startsWith("@") && lastSlashPosition == -1) {
                final String prefix = completionResultSet.getPrefixMatcher().getPrefix();
                completionResultSet = completionResultSet.withPrefixMatcher(prefix.substring(1));
                prefixLength = 1;
            } else {
                completionResultSet = completionResultSet.withPrefixMatcher(resolve.key.substring(prefixLength));
            }

        }
        if (completionResultSet.getPrefixMatcher().getPrefix().equals("@")) {
            completionResultSet = completionResultSet.withPrefixMatcher("");
        }

        final String prefixFilter = resolve.key.substring(0, prefixLength);
        final Set<String> keys = new HashSet<>();
        fileBasedIndex.processAllKeys(ViewFileIndex.identity, key -> {
            if (key.startsWith(prefixFilter)) {
                keys.add(key);
            }
            return true;
        }, scope, null);

        final PsiManager psiManager = PsiManager.getInstance(project);
        boolean localViewSearch = false;
        if (resolve.from == ViewResolveFrom.View) {
            final String value = PhpUtil.getValue(viewParameter);
            localViewSearch = !value.startsWith("@") && !value.startsWith("//");
        }

        final String defaultViewExtension = '.' + Yii2SupportSettings.getInstance(psiElement.getProject()).defaultViewExtension;
        for (String key : keys) {
            Collection<ViewInfo> views = fileBasedIndex.getValues(ViewFileIndex.identity, key, scope);
            for (ViewInfo view : views) {
                if (!resolve.application.equals(view.application)) {
                    continue;
                }

                if (localViewSearch && !resolve.theme.equals(view.theme)) {
                    continue;
                }

                PsiFile psiFile = psiManager.findFile(Objects.requireNonNull(view.getVirtualFile()));
                if (psiFile != null) {
                    String insertText = key.substring(prefixLength);
                    if (insertText.endsWith(defaultViewExtension)) {
                        insertText = insertText.substring(0, insertText.length() - defaultViewExtension.length());
                    }
                    completionResultSet.addElement(new ViewLookupElement(psiFile, insertText));
                    break;
                } else {
                    System.out.println(view.fileUrl + " => not exists");
                }
            }
        }
    }
}
