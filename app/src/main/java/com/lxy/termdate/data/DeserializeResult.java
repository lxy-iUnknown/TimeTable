package com.lxy.termdate.data;

import androidx.annotation.IntDef;

@IntDef({DeserializeResult.OK, DeserializeResult.INVALID_FILE})
public @interface DeserializeResult {
    int OK = 0;
    int INVALID_FILE = 1;
}
