package io.sniffy.util;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;

public class ReflectionCopier<T> {

    private final ReflectionFieldCopier[] reflectionFieldCopiers;

    public ReflectionCopier(Class<? extends T> clazz) {
        this(clazz, Collections.<String>emptySet());
    }

    public ReflectionCopier(Class<? extends T> clazz, String... ignoreFieldName) {
        this(clazz, new HashSet<String>(Arrays.asList(ignoreFieldName)));
    }

    public ReflectionCopier(Class<? extends T> clazz, Set<String> ignoreFieldNames) {

        List<ReflectionFieldCopier> reflectionFieldCopiers = new ArrayList<ReflectionFieldCopier>();

        for (Class<?> superClass = clazz; null != superClass && !superClass.equals(Object.class); superClass = superClass.getSuperclass()) {
            for (Field declaredField : superClass.getDeclaredFields()) {
                if (null == ignoreFieldNames || !ignoreFieldNames.contains(declaredField.getName())) {
                    if (!declaredField.isSynthetic() && !Modifier.isStatic(declaredField.getModifiers())) {
                        reflectionFieldCopiers.add(new ReflectionFieldCopier(superClass, declaredField.getName()));
                    }
                }
            }
        }

        this.reflectionFieldCopiers = reflectionFieldCopiers.toArray(new ReflectionFieldCopier[0]);

    }

    public void copy(T from, T to) {
        for (ReflectionFieldCopier reflectionFieldCopier : reflectionFieldCopiers) {
            reflectionFieldCopier.copy(from, to);
        }
    }

}
