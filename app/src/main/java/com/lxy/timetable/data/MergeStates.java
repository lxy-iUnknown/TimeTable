package com.lxy.timetable.data;

import androidx.annotation.NonNull;

import com.lxy.timetable.BuildConfig;
import com.lxy.timetable.GlobalContext;
import com.lxy.timetable.R;
import com.lxy.timetable.contract.Contract;
import com.lxy.timetable.contract.Operator;
import com.lxy.timetable.contract.Value;

import java.io.IOException;

public class MergeStates {

    private static final int MERGE_ROWS_STRIDE = TimeTableData.MAXIMUM_MERGED_ROW * (2 * Byte.BYTES) + 1;
    // Merged states now generated compile time as raw resource
    @NonNull
    private static final byte[] MERGE_STATE;

    static {
        final var SIZE = (TimeTableData.MAXIMUM_MERGED_ROWS + 1) * (TimeTableData.MAXIMUM_MERGED_ROW * 2 + 1);

        byte[] state;
        try {
            try (var stream = GlobalContext.get().getResources().openRawResource(R.raw.merged_rows)) {
                state = new byte[SIZE];
                var result = stream.read(state, 0, SIZE);
                if (BuildConfig.DEBUG) {
                    Contract.requireOperation(new Value<>("result", result),
                            new Value<>(SIZE), Operator.EQ);
                }
            }
        } catch (IOException e) {
            state = Contract.fail("Cannot load merged states", e);
        }
        MERGE_STATE = state;
    }

    private final int mergedRowsStartIndex;
    private final byte count;

    public MergeStates(char mergedRowsIndex) {
        Contract.validateIndex(mergedRowsIndex, TimeTableData.MAXIMUM_MERGED_ROWS);
        var mergedRowsStartIndex = mergedRowsIndex * MERGE_ROWS_STRIDE;
        this.count = MERGE_STATE[mergedRowsStartIndex];
        this.mergedRowsStartIndex = mergedRowsIndex;
    }

    public static int getFirstRow(char mergedRow) {
        var firstRow = (byte) (mergedRow >> 8);
        TimeTableData.validateRowIndex(firstRow);
        return firstRow;
    }

    public static int getLastRow(char mergedRow) {
        var lastRow = (byte) (mergedRow);
        TimeTableData.validateRowIndex(lastRow);
        return lastRow;
    }

    public int getCount() {
        return count;
    }

    public char get(int mergedRowIndex) {
        Contract.validateIndex(mergedRowIndex, count);
        var start = computeStartIndex(mergedRowIndex);
        return (char) ((MERGE_STATE[start] << 8) | Byte.toUnsignedInt(MERGE_STATE[start + 1]));
    }

    private int computeStartIndex(int mergedRowIndex) {
        return mergedRowsStartIndex + mergedRowIndex * 2 + 1;
    }

    @NonNull
    @Override
    public String toString() {
        if (!BuildConfig.DEBUG) {
            return super.toString();
        }
        var max = this.count - 1;
        var index = mergedRowsStartIndex / MERGE_ROWS_STRIDE;
        var sb = new StringBuilder();
        sb.append("Index: ").append(index).append(", Rows: [");
        if (max == -1) {
            return sb.append(']').toString();
        }
        var mergeState = MERGE_STATE;
        for (int i = 0; ; i++) {
            var startIndex = computeStartIndex(i);
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