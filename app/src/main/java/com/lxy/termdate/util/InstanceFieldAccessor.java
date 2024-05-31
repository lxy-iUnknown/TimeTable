package com.lxy.termdate.util;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.lxy.termdate.BuildConfig;
import com.lxy.termdate.contract.Contract;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

public class InstanceFieldAccessor<T> {
    @NonNull
    private final Field field;

    private InstanceFieldAccessor(@NonNull Field field) {
        if (BuildConfig.DEBUG) {
            Contract.require(!Modifier.isStatic(field.getModifiers()),
                    "Non static field required");
        }
        field.setAccessible(true);
        this.field = field;
    }

    @NonNull
    public static <T> InstanceFieldAccessor<T> of(
            @NonNull Class<?> clazz, @NonNull String name) {
        try {
            return new InstanceFieldAccessor<>(clazz.getDeclaredField(name));
        } catch (NoSuchFieldException e) {
            return Contract.fail("SmartTable.class.getDeclaredField(\"%s\") failed", e, name);
        }
    }

    @SuppressWarnings("unchecked")
    @Nullable
    public T get(@NonNull Object o) {
        try {
            return (T) field.get(o);
        } catch (IllegalAccessException e) {
            return Contract.fail("Field.get failed", e);
        }
    }

    public void set(@NonNull Object o, @NonNull T value) {
        try {
            field.set(o, value);
        } catch (IllegalAccessException e) {
            Contract.fail("Field.set failed", e);
        }
    }
}
