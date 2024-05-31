package com.lxy.termdate.util;

import androidx.annotation.Nullable;

import java.io.IOException;
import java.io.InputStream;

public interface TermDateFileHandler {
    @Nullable
    InputStream openInputStream() throws IOException;

    boolean reallySucceeded();

    void fileNotFound();

    void failed();
}
