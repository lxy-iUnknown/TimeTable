package com.lxy.timetable.enumset;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.lxy.timetable.BuildConfig;
import com.lxy.timetable.contract.Contract;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Iterator;
import java.util.function.Consumer;

public class TwoEnumSet<E extends Enum<E>> extends FixedSizeEnumSet<E> {
    @NonNull
    private final E e1, e2;

    public TwoEnumSet(@NonNull E e1, @NonNull E e2) {
        if (BuildConfig.DEBUG) {
            Contract.require(e1 != e2, "Duplicate item found");
        }
        this.e1 = Contract.requireNonNull(e1);
        this.e2 = Contract.requireNonNull(e2);
    }

    private boolean containsImpl(@Nullable Object o) {
        return o == e1 || o == e2;
    }

    @NonNull
    @Override
    public Iterator<E> iterator() {
        return new SetIterator<>(e1, e2);
    }

    @SuppressWarnings("unchecked")
    @NonNull
    @Override
    public Object[] toArray() {
        var array = (E[]) Array.newInstance(e1.getClass(), 2);
        array[0] = e1;
        array[1] = e2;
        return array;
    }

    @SuppressWarnings({"unchecked", "DataFlowIssue"})
    @NonNull
    @Override
    public <T> T[] toArray(@NonNull T[] a) {
        if (a.length < 2) {
            a = (T[]) Array.newInstance(a.getClass().getComponentType(), 2);
        }
        a[0] = (T) e1;
        a[1] = (T) e2;
        return a;
    }

    @Override
    public boolean containsAll(@NonNull Collection<?> c) {
        return switch (c.size()) {
            case 1 -> containsImpl(c.iterator().next());
            case 2 -> {
                var iterator = c.iterator();
                var e1 = iterator.next();
                var e2 = iterator.next();
                yield (e1 == this.e1 && e2 == this.e2) ||
                        (e1 == this.e2 && e2 == this.e1);
            }
            default -> false;
        };
    }

    @Override
    public int size() {
        return 2;
    }

    @Override
    public boolean contains(@Nullable Object o) {
        return containsImpl(o);
    }

    @Override
    public void forEach(@NonNull Consumer<? super E> action) {
        action.accept(e1);
        action.accept(e2);
    }

    @Override
    public int hashCode() {
        var PRIME = 31;

        return PRIME * PRIME + PRIME * e1.hashCode() + e2.hashCode();
    }

    private static class SetIterator<T> implements Iterator<T> {
        private final T e1, e2;
        private int index = 0;

        private SetIterator(T e1, T e2) {
            this.e1 = e1;
            this.e2 = e2;
        }

        @Override
        public boolean hasNext() {
            return index < 2;
        }

        @Nullable
        @Override
        public T next() {
            return switch (index++) {
                case 0 -> e1;
                case 1 -> e2;
                default -> iteratorOutOfRange();
            };
        }
    }
}
