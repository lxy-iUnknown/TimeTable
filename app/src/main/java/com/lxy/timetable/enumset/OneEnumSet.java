package com.lxy.timetable.enumset;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.lxy.timetable.contract.Contract;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;
import java.util.function.Consumer;

public class OneEnumSet<E extends Enum<E>> extends FixedSizeEnumSet<E> {
    @NonNull
    private final E e;

    public OneEnumSet(@NonNull E e) {
        this.e = Contract.requireNonNull(e);
    }

    private boolean containsImpl(@Nullable Object o) {
        return e.equals(o);
    }

    private boolean equalsImpl(@NonNull Collection<?> c) {
        return containsImpl(c.iterator().next());
    }

    @NonNull
    @Override
    public Iterator<E> iterator() {
        return new SetIterator<>(e);
    }

    @NonNull
    @Override
    public Object[] toArray() {
        return new Object[]{e};
    }

    @SuppressWarnings({"unchecked", "DataFlowIssue"})
    @NonNull
    @Override
    public <T> T[] toArray(@NonNull T[] a) {
        if (a.length < 1) {
            a = (T[]) Array.newInstance(a.getClass().getComponentType(), 1);
        }
        a[0] = (T) e;
        return a;
    }

    @Override
    public boolean containsAll(@NonNull Collection<?> c) {
        if (c.size() != 1) {
            return false;
        }
        return equalsImpl(c);
    }

    @Override
    public int size() {
        return 1;
    }

    @Override
    public boolean contains(@Nullable Object o) {
        return containsImpl(o);
    }

    @Override
    public void forEach(@NonNull Consumer<? super E> action) {
        action.accept(e);
    }

    @Override
    public int hashCode() {
        return e.hashCode();
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (obj instanceof Set<?> set) {
            return set.size() == 1 && equalsImpl(set);
        }
        return false;
    }

    private static class SetIterator<T> implements Iterator<T> {
        private final T e;
        private int index = 0;

        private SetIterator(T e) {
            this.e = e;
        }

        @Override
        public boolean hasNext() {
            return index < 1;
        }

        @Nullable
        @Override
        public T next() {
            if (index++ == 0) {
                return e;
            }
            return iteratorOutOfRange();
        }
    }
}
