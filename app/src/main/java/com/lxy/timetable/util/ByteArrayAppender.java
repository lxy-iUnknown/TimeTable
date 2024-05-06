package com.lxy.timetable.util;

import androidx.annotation.NonNull;

import com.lxy.timetable.contract.Contract;
import com.lxy.timetable.contract.Operator;
import com.lxy.timetable.contract.Value;

// Simplified ByteArrayOutputStream, all methods are not synchronized
public class ByteArrayAppender {
    private byte[] buffer;
    private int count;

    public ByteArrayAppender(int size) {
        Contract.requireOperation(new Value<>("size", size), Value.ZERO_I, Operator.GE);
        buffer = new byte[size];
    }

    private void ensureCapacity(int minCapacity) {
        var oldCapacity = buffer.length;
        if (minCapacity > oldCapacity) {
            var newBuffer = new byte[Math.max(oldCapacity * 2, minCapacity)];
            System.arraycopy(buffer, 0, newBuffer, 0, oldCapacity);
            buffer = newBuffer;
        }
    }

    public void append(int value) {
        ensureCapacity(count + 1);
        buffer[count++] = (byte) value;
    }

    public void append(@NonNull byte[] buffer) {
        append(buffer, buffer.length);
    }

    public void append(@NonNull byte[] buffer, int size) {
        append(buffer, 0, size);
    }

    public void append(@NonNull byte[] buffer, int offset, int size) {
        if (size == 0) {
            return;
        }
        Contract.requireValidIndex(offset, buffer.length);
        // This will fail when size == 0
        Contract.requireValidIndex(offset + size - 1, buffer.length);
        ensureCapacity(count + size);
        System.arraycopy(Contract.requireNonNull(buffer), offset, this.buffer, count, size);
        count += size;
    }

    @NonNull
    public byte[] buffer() {
        return buffer;
    }

    public int size() {
        return count;
    }
}
