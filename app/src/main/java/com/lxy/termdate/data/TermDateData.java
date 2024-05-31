package com.lxy.termdate.data;

import androidx.annotation.NonNull;

import com.lxy.termdate.BuildConfig;
import com.lxy.termdate.contract.Contract;
import com.lxy.termdate.contract.Operator;
import com.lxy.termdate.contract.Value;
import com.lxy.termdate.util.ByteArrayAppender;
import com.lxy.termdate.util.DateUtil;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.CharBuffer;
import java.util.Arrays;

import timber.log.Timber;

public class TermDateData {
    public static final int ROW_COUNT = 12;
    public static final int COLUMN_COUNT = 5;
    public static final int CELL_COUNT = ROW_COUNT * COLUMN_COUNT;
    public static final int MIN_STRING_LENGTH = 0;
    public static final int MAX_STRING_LENGTH = (1 << Byte.SIZE) - 1;

    public static final int MAXIMUM_MERGED_ROW = TermDateData.ROW_COUNT / 2;
    @SuppressWarnings("PointlessArithmeticExpression")
    public static final int MIN_FILE_SIZE = Integer.SIZE /* gregorian date */ +
            CELL_COUNT * 2 /* odd even */ * (Byte.BYTES /* length */ + MIN_STRING_LENGTH * Character.BYTES) +
            MAXIMUM_MERGED_ROW * Character.BYTES /* merge_state */;
    public static final int ESTIMATED_FILE_SIZE = MIN_FILE_SIZE + 100;
    @SuppressWarnings("unused")
    public static final int MAX_FILE_SIZE = Integer.SIZE /* gregorian date */ +
            CELL_COUNT * 2 /* odd even */ * (Byte.BYTES /* length */ + MAX_STRING_LENGTH * Character.BYTES) +
            MAXIMUM_MERGED_ROW * Character.BYTES /* merge_state */;
    public static final int MAXIMUM_MERGED_ROWS = (1 << (TermDateData.ROW_COUNT - 1)) - 1;
    @NonNull
    public static final char[] MERGE_STATES = new char[COLUMN_COUNT];
    @NonNull
    public static final Cell[][] TIME_TABLE_DATA = new Cell[COLUMN_COUNT][ROW_COUNT];

    @NonNull
    private static final byte[] READ_WRITE_BUFFER = new byte[Long.BYTES];
    @NonNull
    private static final byte[] BYTE_BUFFER = new byte[TermDateData.MAX_STRING_LENGTH * Character.BYTES];
    @NonNull
    private static final CharBuffer WRAPPED_CHAR_BUFFER =
            ByteBuffer.wrap(BYTE_BUFFER).order(ByteOrder.LITTLE_ENDIAN).asCharBuffer();
    @NonNull
    private static final char[] CHAR_BUFFER = new char[TermDateData.MAX_STRING_LENGTH];
    private static final int INVALID = -1;

    private static int BEGIN_DATE;

    static {
        int row, column;
        for (row = 0; row < ROW_COUNT; row++) {
            for (column = 0; column < COLUMN_COUNT; column++) {
                TIME_TABLE_DATA[column][row] = new Cell();
            }
        }
        clearBeginDate();
    }

    @NonNull
    private static String arrayToString(@NonNull byte[] array, int length) {
        Contract.requireValidIndex(length, array.length);
        var max = length - 1;
        if (max == -1) {
            return "[]";
        }
        var sb = new StringBuilder("[");
        for (var i = 0; ; i++) {
            sb.append(Byte.toUnsignedInt(array[i]));
            if (i == max) {
                return sb.append(']').toString();
            }
            sb.append(", ");
        }
    }

    private static <T> T throwEOFException(int bytesToRead, int bytesRead) throws EOFException {
        throw new EOFException("Cannot read " + bytesToRead +
                " bytes because there are only " + bytesRead +
                " bytes to read");
    }

    private static void checkedRead(@NonNull InputStream stream, byte[] buffer, int bytesToRead) throws IOException {
        var bytesRead = stream.read(buffer, 0, bytesToRead);
        if (bytesRead < bytesToRead) {
            throwEOFException(bytesToRead, bytesRead);
        }
    }

    private static byte[] prepareReadWriteBuffer() {
        var buffer = READ_WRITE_BUFFER;
        if (BuildConfig.DEBUG) {
            Arrays.fill(buffer, (byte) 0);
        }
        return buffer;
    }

    private static char readLittleEndianChar(@NonNull InputStream stream) throws IOException {
        var buffer = prepareReadWriteBuffer();
        checkedRead(stream, buffer, Character.BYTES);
        return (char) ((buffer[1] << 8) | Byte.toUnsignedInt(buffer[0]));
    }

    private static int readLittleEndianInt(@NonNull InputStream stream) throws IOException {
        var buffer = prepareReadWriteBuffer();
        checkedRead(stream, buffer, Integer.BYTES);
        return (buffer[3] << 24) |
                (Byte.toUnsignedInt(buffer[2])) << 16 |
                (Byte.toUnsignedInt(buffer[1])) << 8 |
                (Byte.toUnsignedInt(buffer[0]));
    }

    @NonNull
    private static String readString(@NonNull InputStream stream) throws IOException {
        var length = stream.read();
        if (length == -1) {
            return throwEOFException(1, 0);
        }
        if (BuildConfig.DEBUG) {
            Arrays.fill(BYTE_BUFFER, (byte) 0);
            Arrays.fill(CHAR_BUFFER, (char) 0);
        }
        var charBuffer = CHAR_BUFFER;
        var byteBuffer = BYTE_BUFFER;
        checkedRead(stream, byteBuffer, length * Character.BYTES);
        if (BuildConfig.DEBUG) {
            Timber.d("String length: %d, Raw bytes: %s", length,
                    arrayToString(byteBuffer, length * Character.BYTES));
        }
        // Directly copy to char buffer
        var wrappedCharBuffer = WRAPPED_CHAR_BUFFER;
        wrappedCharBuffer.clear();
        wrappedCharBuffer.get(charBuffer, 0, length);
        return new String(charBuffer, 0, length);
    }

    private static void writeLittleEndianInt(@NonNull ByteArrayAppender appender, int value) {
        var buffer = prepareReadWriteBuffer();
        buffer[0] = (byte) value;
        buffer[1] = (byte) (value >> 8);
        buffer[2] = (byte) (value >> 16);
        buffer[3] = (byte) (value >> 24);
        appender.append(buffer, Integer.BYTES);
    }

    private static void writeString(@NonNull ByteArrayAppender appender, @NonNull String value) {
        Contract.requireNonNull(appender);
        Contract.requireNonNull(value);
        var length = value.length();
        Contract.requireOperation(
                new Value<>("length", length),
                new Value<>(TermDateData.MAX_STRING_LENGTH),
                Operator.LE
        );
        appender.append(length);
        var charBuffer = CHAR_BUFFER;
        value.getChars(0, length, charBuffer, 0);
        writeLittleEndianCharArray(appender, charBuffer, length);
    }

    private static void writeLittleEndianCharArray(@NonNull ByteArrayAppender appender,
                                                   @NonNull char[] array, int length) {
        for (var i = 0; i < length; i++) {
            var ch = array[i];
            appender.append(ch);
            appender.append(ch >> 8);
        }
    }

    private static void serializeCell(ByteArrayAppender appender, @NonNull Cell cell) {
        Contract.requireNonNull(cell);
        writeString(appender, cell.getOdd());
        writeString(appender, cell.getEven());
    }

    @NonNull
    private static Cell deserializeCell(InputStream stream) throws IOException {
        return new Cell(readString(stream), readString(stream));
    }

    private static void ensureValid() {
        if (BuildConfig.DEBUG) {
            Contract.require(isValid(), "Invalid weeks");
        }
    }

    private static void clearBeginDate() {
        BEGIN_DATE = INVALID;
    }

    public static void validateRowIndex(int row) {
        Contract.requireValidIndex(row, ROW_COUNT);
    }

    @SuppressWarnings("unused")
    public static void validateColumnIndex(int column) {
        Contract.requireValidIndex(column, COLUMN_COUNT);
    }

    public static long beginDate() {
        return BEGIN_DATE;
    }

    public static boolean isEven() {
        ensureValid();
        return DateUtil.isEven(BEGIN_DATE, DateUtil.now());
    }

    public static boolean isValid() {
        return BEGIN_DATE >= 0;
    }

    public static void clear() {
        clearBeginDate();
        for (var row = 0; row < ROW_COUNT; row++) {
            for (var column = 0; column < COLUMN_COUNT; column++) {
                TIME_TABLE_DATA[column][row].clear();
            }
        }
        Arrays.fill(MERGE_STATES, 0, COLUMN_COUNT, (char) 0);
    }

    @NonNull
    public static ByteArrayAppender serialize() {
        if (BuildConfig.DEBUG) {
            Timber.d("Serialize termdate data");
        }
        var buffer = new ByteArrayAppender(ESTIMATED_FILE_SIZE);
        writeLittleEndianInt(buffer, BEGIN_DATE);
        for (var row = 0; row < ROW_COUNT; row++) {
            for (var column = 0; column < COLUMN_COUNT; column++) {
                serializeCell(buffer, TIME_TABLE_DATA[column][row]);
            }
        }
        writeLittleEndianCharArray(buffer, MERGE_STATES, COLUMN_COUNT);
        return buffer;
    }

    public static @DeserializeResult int deserialize(@NonNull InputStream stream) throws IOException {
        if (BuildConfig.DEBUG) {
            Timber.d("Deserialize termdate data");
        }
        try {
            var epochDay = Integer.toUnsignedLong(readLittleEndianInt(stream));
            if (epochDay <= BuildConfig.MAX_DATE) {
                if (BuildConfig.DEBUG) {
                    Timber.d("Julian day delta: %d", epochDay);
                }
            } else {
                if (BuildConfig.DEBUG) {
                    Timber.e("Invalid julian day delta %d", epochDay);
                }
                return DeserializeResult.INVALID_FILE;
            }
            var cells = new Cell[CELL_COUNT];
            for (var i = 0; i < CELL_COUNT; i++) {
                var cell = deserializeCell(stream);
                if (BuildConfig.DEBUG) {
                    Timber.d("Deserialized cell %d, odd: \"%s\", even: \"%s\"", i + 1, cell.getOdd(), cell.getEven());
                }
                cells[i] = cell;
            }
            var mergeStates = new char[COLUMN_COUNT];
            for (var i = 0; i < COLUMN_COUNT; i++) {
                var mergeState = readLittleEndianChar(stream);
                if (mergeState > MAXIMUM_MERGED_ROWS) {
                    if (BuildConfig.DEBUG) {
                        Timber.e("Invalid merge state %d", (int) mergeState);
                    }
                    return DeserializeResult.INVALID_FILE;
                }
                mergeStates[i] = mergeState;
            }
            for (var row = 0; row < ROW_COUNT; row++) {
                for (var column = 0; column < COLUMN_COUNT; column++) {
                    TIME_TABLE_DATA[column][row].copyFrom(cells[row * TermDateData.COLUMN_COUNT + column]);
                }
            }
            System.arraycopy(mergeStates, 0, MERGE_STATES, 0, COLUMN_COUNT);
            BEGIN_DATE = (int) epochDay;
            ensureValid();
            return DeserializeResult.OK;
        } catch (EOFException e) {
            if (BuildConfig.DEBUG) {
                Timber.e(e, "End of file occurred because termdate data file is invalid");
            }
            return DeserializeResult.INVALID_FILE;
        }
    }
}
