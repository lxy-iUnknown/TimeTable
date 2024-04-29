package com.lxy.timetable.util;

import androidx.annotation.Nullable;

import java.io.IOException;
import java.io.InputStream;

public interface TimeTableFileHandler {
    @Nullable
    InputStream openInputStream() throws IOException;

    boolean reallySucceeded();

    void fileNotFound();

    void failed();
}
