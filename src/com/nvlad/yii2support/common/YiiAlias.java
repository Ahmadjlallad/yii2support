package com.nvlad.yii2support.common;

import com.intellij.openapi.project.Project;
import com.nvlad.yii2support.utils.Yii2SupportSettings;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

public class YiiAlias {
    private static final Map<Project, YiiAlias> yiiProjectAliasMap = new HashMap<>();

    public static YiiAlias getInstance(Project project) {
        if (!yiiProjectAliasMap.containsKey(project)) {
            yiiProjectAliasMap.put(project, new YiiAlias(project));
        }

        return yiiProjectAliasMap.get(project);
    }

    private final Map<String, String> myAliasMap;
    private final Map<String, String> myResolvedAliasCache;

    protected YiiAlias(Project project) {
        myAliasMap = new HashMap<>(new WeakHashMap<>(Yii2SupportSettings.getInstance(project).aliasMap));
        myResolvedAliasCache = new HashMap<>();
    }

    @Nullable
    public String getAlias(@NotNull String alias, boolean console) {
        return aliasFromMap(myAliasMap, alias, console);
    }

    @Nullable
    public String resolveAlias(@NotNull String alias, boolean console) {
        String path = myResolvedAliasCache.get(alias);
        if (path != null) {
            return path;
        }

        path = getAlias(alias, console);
        if (path == null) {
            return null;
        }

        myResolvedAliasCache.put(alias, path.replaceFirst("^/+", ""));

        return myResolvedAliasCache.get(alias);
    }

    @Nullable
    private String aliasFromMap(@NotNull Map<String, String> aliasMap, @NotNull String alias, boolean console) {
        if (!alias.startsWith("@")) {
            return alias;
        }

        if (console && alias.startsWith("@app/")) {
             alias = "@yii2support-console-command-app-root" + alias.substring(4);
        }

        String value = aliasMap.get(alias);
        if (value != null) {
            return aliasFromMap(aliasMap, alias, console);
        }

        String foundAlias = null;
        for (String aliasKey : aliasMap.keySet()) {
            if (alias.startsWith(aliasKey)) {
                if (foundAlias == null || aliasKey.length() > foundAlias.length()) {
                    foundAlias = aliasKey;
                }
            }
        }

        if (foundAlias == null) {
            return null;
        }

        value = alias.replace(foundAlias, aliasMap.get(foundAlias));
        return aliasFromMap(aliasMap, value, console);
    }
}
