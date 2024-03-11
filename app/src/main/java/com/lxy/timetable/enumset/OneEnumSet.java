package com.lxy.timetable.enumset;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.lxy.timetable.contract.Contract;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Iterator;
import java.util.function.Consumer;

public class OneEnumSet<E extends Enum<E>> extends FixedSizeEnumSet<E> {
    @NonNull
    private final E e;

    public OneEnumSet(@NonNull E e) {
        this.e = Contract.requireNonNull(e);
    }

    private boolean containsImpl(@Nullable Object o) {
        return o == e;
    }

    @NonNull
    @Override
    public Iterator<E> iterator() {
        return new SetIterator<>(e);
    }

    @SuppressWarnings("unchecked")
    @NonNull
    @Override
    public Object[] toArray() {
        var array = (E[]) Array.newInstance(e.getClass(), 1);
        array[0] = e;
        return array;
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
        return containsImpl(c.iterator().next());
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
