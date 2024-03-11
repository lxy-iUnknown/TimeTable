package com.lxy.timetable.enumset;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.lxy.timetable.BuildConfig;
import com.lxy.timetable.contract.Contract;

import java.util.Collection;
import java.util.Set;
import java.util.function.Predicate;

public abstract class FixedSizeEnumSet<E extends Enum<E>> implements Set<E> {
    private static void readOnly() {
        if (BuildConfig.DEBUG) {
            Contract.fail("Read-only set");
        }
    }

    @NonNull
    protected static <E> E iteratorOutOfRange() {
        return Contract.fail("Iterator out of range");
    }

    @Override
    public boolean add(E e) {
        readOnly();
        return false;
    }

    @Override
    public boolean remove(@Nullable Object o) {
        readOnly();
        return false;
    }

    @Override
    public boolean addAll(@NonNull Collection<? extends E> c) {
        readOnly();
        return false;
    }

    @Override
    public boolean retainAll(@NonNull Collection<?> c) {
        readOnly();
        return false;
    }

    @Override
    public boolean removeAll(@NonNull Collection<?> c) {
        readOnly();
        return false;
    }

    @Override
    public void clear() {
        readOnly();
    }

    @Override
    public boolean isEmpty() {
        return false;
    }

    @Override
    public boolean removeIf(@NonNull Predicate<? super E> filter) {
        readOnly();
        return false;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (obj instanceof Set<?> set) {
            return containsAll(set);
        }
        return false;
    }

    @NonNull
    @Override
    protected Object clone() throws CloneNotSupportedException {
        return super.clone();
    }
}
