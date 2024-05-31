package com.lxy.termdate.data;

import androidx.annotation.NonNull;

import com.lxy.termdate.contract.Contract;

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
        if (TermDateData.isValid()) {
            return TermDateData.isEven() ? getEven() : getOdd();
        }
        return "";
    }
}
