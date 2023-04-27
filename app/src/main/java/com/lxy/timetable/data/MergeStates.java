package com.lxy.timetable.data;

import androidx.annotation.NonNull;

import com.lxy.timetable.BuildConfig;
import com.lxy.timetable.GlobalContext;
import com.lxy.timetable.R;
import com.lxy.timetable.util.Contract;
import com.lxy.timetable.util.TimberUtil;

import java.io.IOException;
import java.io.InputStream;

import timber.log.Timber;

public class MergeStates {

    private static final int MERGE_ROWS_STRIDE = TimeTableData.MAXIMUM_MERGED_ROW * (2 * Byte.BYTES) + 1;
    // Merged states now generated compile time as raw resource
    @NonNull
    private static final byte[] MERGE_STATE;

    static {
        final int SIZE = (TimeTableData.MAXIMUM_MERGED_ROWS + 1) * (TimeTableData.MAXIMUM_MERGED_ROW * 2 + 1);

        try {
            try (InputStream stream = GlobalContext.get().getResources().openRawResource(R.raw.merged_rows)) {
                byte[] state = new byte[SIZE];
                int result = stream.read(state, 0, SIZE);
                if (BuildConfig.DEBUG) {
                    Contract.require(result == SIZE, "Cannot read more bytes");
                }
                MERGE_STATE = state;
            }
        } catch (IOException e) {
            throw TimberUtil.errorAndThrowException(e, "Cannot load merged states");
        }
    }

    private final int mergedRowsStartIndex;
    private final byte count;

    public MergeStates(char mergedRowsIndex) {
        Contract.validateIndex(mergedRowsIndex, TimeTableData.MAXIMUM_MERGED_ROWS);
        this.mergedRowsStartIndex = mergedRowsIndex * MERGE_ROWS_STRIDE;
        count = MERGE_STATE[mergedRowsStartIndex];
        if (BuildConfig.DEBUG) {
            Timber.d(debugToString());
        }
    }

    public static int getFirstRow(char mergedRow) {
        int firstRow = (byte) (mergedRow >> 8);
        TimeTableData.validateRowIndex(firstRow);
        return firstRow;
    }

    public static int getLastRow(char mergedRow) {
        int lastRow = (byte) (mergedRow);
        TimeTableData.validateRowIndex(lastRow);
        return lastRow;
    }

    public int getCount() {
        return count;
    }

    public char get(int mergedRowIndex) {
        Contract.validateIndex(mergedRowIndex, count);
        int start = computeStartIndex(mergedRowIndex);
        return (char) ((MERGE_STATE[start] << 8) | Byte.toUnsignedInt(MERGE_STATE[start + 1]));
    }

    private int computeStartIndex(int mergedRowIndex) {
        return mergedRowsStartIndex + mergedRowIndex * 2 + 1;
    }

    @NonNull
    public String debugToString() {
        int max = this.count - 1;
        int index = mergedRowsStartIndex / MERGE_ROWS_STRIDE;
        StringBuilder sb = new StringBuilder(256);
        sb.append("Index: ").append(index).append(", Rows: [");
        if (max == -1) {
            return sb.append(']').toString();
        }
        byte[] mergeState = MERGE_STATE;
        for (int i = 0; ; i++) {
            int startIndex = computeStartIndex(i);
            sb.append('{')
                    .append(mergeState[startIndex])
                    .append(", ")
                    .append(mergeState[startIndex + 1])
                    .append('}');
            if (i == max) {
                return sb.append(']').toString();
            }
            sb.append(", ");
        }
    }
}