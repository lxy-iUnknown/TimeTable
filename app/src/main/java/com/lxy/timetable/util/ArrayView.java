package com.lxy.timetable.util;

import androidx.annotation.NonNull;

import com.lxy.timetable.contract.Contract;

import java.util.AbstractList;

public class ArrayView<T> extends AbstractList<T> {
    @NonNull
    private final T[] array;
    private int length;

    @SuppressWarnings("unused")
    public ArrayView(@NonNull T[] array) {
        this.array = array;
        this.length = array.length;
    }

    public ArrayView(@NonNull T[] array, int length) {
        this.array = Contract.requireNonNull(array);
        setLength(length);
    }

    @NonNull
    public T[] getArray() {
        return array;
    }

    @Override
    public T get(int index) {
        Contract.requireValidIndex(index, length);
        return array[index];
    }

    public void setLength(int length) {
        Contract.requireValidIndex(length, array.length);
        this.length = length;
    }

    @Override
    public int size() {
        return length;
    }

    @Override
    public void clear() {
        // com.bin.david.form.core.SmartTable.onDetachedFromWindow ->
        // com.bin.david.form.core.SmartTable.release ->
        // com.bin.david.form.data.table.TableData.clear ->
        // userSetRangeAddress.clear ->
        // com.lxy.timetable.MainActivity$ArrayView.clear
        length = 0;
    }
}
