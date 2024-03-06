package com.lxy.timetable.data;

import androidx.annotation.NonNull;

import com.lxy.timetable.contract.Contract;

public class Cell {
    @NonNull
    private String odd;
    @NonNull
    private String even;

    public Cell() {
        this("", "");
    }

    public Cell(@NonNull String odd, @NonNull String even) {
        this.odd = Contract.requireNonNull(odd);
        this.even = Contract.requireNonNull(even);
    }

    public void clear() {
        this.odd = "";
        this.even = "";
    }

    public void copyFrom(@NonNull Cell cell) {
        Contract.requireNonNull(cell);
        this.odd = cell.odd;
        this.even = cell.even;
    }

    public @NonNull String getOdd() {
        return odd;
    }

    public @NonNull String getEven() {
        return even;
    }

    @NonNull
    @Override
    public String toString() {
        if (TimeTableData.isValid()) {
            return TimeTableData.isEven() ? getEven() : getOdd();
        }
        return "";
    }
}
