package com.lxy.termdate.util;

import androidx.annotation.NonNull;

import com.lxy.termdate.contract.Contract;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

public class CopyingInputStream extends FilterInputStream {
    @NonNull
    private final ByteArrayAppender out;

    public CopyingInputStream(@NonNull InputStream in, int size) {
        super(Contract.requireNonNull(in));
        out = new ByteArrayAppender(size);
    }

    @Override
    public int read() throws IOException {
        var value = super.read();
        out.append(value);
        return value;
    }

    @Override
    public int read(@NonNull byte[] b) throws IOException {
        var result = super.read(b);
        out.append(b);
        return result;
    }

    @Override
    public int read(@NonNull byte[] b, int off, int len) throws IOException {
        var result = super.read(b, off, len);
        out.append(b, off, len);
        return result;
    }

    public int size() {
        return out.size();
    }

    @NonNull
    public byte[] buffer() {
        return out.buffer();
    }
}
