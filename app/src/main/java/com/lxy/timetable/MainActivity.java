package com.lxy.timetable;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;

import androidx.activity.ComponentActivity;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bin.david.form.core.SmartTable;
import com.bin.david.form.core.TableConfig;
import com.bin.david.form.core.TableMeasurer;
import com.bin.david.form.data.CellRange;
import com.bin.david.form.data.column.Column;
import com.bin.david.form.data.format.draw.MultiLineDrawFormat;
import com.bin.david.form.data.format.sequence.BaseSequenceFormat;
import com.bin.david.form.data.style.FontStyle;
import com.bin.david.form.data.style.LineStyle;
import com.bin.david.form.data.table.ArrayTableData;
import com.bin.david.form.utils.DensityUtils;
import com.lxy.timetable.data.Cell;
import com.lxy.timetable.databinding.ActivityMainBinding;
import com.lxy.timetable.data.DeserializeResult;
import com.lxy.timetable.data.MergeStates;
import com.lxy.timetable.data.TimeTableData;
import com.lxy.timetable.util.ByteArrayAppender;
import com.lxy.timetable.util.Contract;
import com.lxy.timetable.util.TimberUtil;
import com.lxy.timetable.util.ToastUtil;
import com.lxy.timetable.util.DateUtil;

import java.io.FileNotFoundException;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.AccessMode;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.spi.FileSystemProvider;
import java.time.LocalDate;
import java.util.AbstractList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

import timber.log.Timber;

public class MainActivity extends ComponentActivity {
    private static class TimeTableFileNotFoundException extends IOException {
        public TimeTableFileNotFoundException(String message) {
            super(message);
        }

        public TimeTableFileNotFoundException(Throwable cause) {
            super(cause);
        }
    }

    private interface OpenTimeTableFileHandler {
        @NonNull
        InputStream openInputStream() throws IOException;

        boolean reallySucceeded();

        void fileNotFound();

        void failed();
    }

    private static class CopyingInputStream extends FilterInputStream {
        @NonNull
        private final ByteArrayAppender out;

        public CopyingInputStream(@NonNull InputStream in, int size) {
            super(Contract.requireNonNull(in));
            out = new ByteArrayAppender(size);
        }

        @Override
        public int read() throws IOException {
            int value = super.read();
            out.append(value);
            return value;
        }

        @Override
        public int read(@NonNull byte[] b) throws IOException {
            int result = super.read(b);
            out.append(b);
            return result;
        }

        @Override
        public int read(@NonNull byte[] b, int off, int len) throws IOException {
            int result = super.read(b, off, len);
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

    private static class ArrayView<T> extends AbstractList<T> {
        @NonNull
        private final T[] array;
        private int length;

        public ArrayView(@NonNull T[] array) {
            this.array = array;
            this.length = array.length;
        }

        public ArrayView(@NonNull T[] array, int length) {
            this.array = Contract.requireNonNull(array);
            setLength(length);
        }

        @NonNull
        public T[] getArray() {
            return array;
        }

        @Override
        public T get(int index) {
            Contract.validateIndex(index, length);
            return array[index];
        }

        public void setLength(int length) {
            Contract.validateLength(length, array.length);
            this.length = length;
        }

        @Override
        public int size() {
            return length;
        }

        @Override
        public void clear() {
            // com.bin.david.form.core.SmartTable.onDetachedFromWindow ->
            // com.bin.david.form.core.SmartTable.release ->
            // com.bin.david.form.data.table.TableData.clear ->
            // userSetRangeAddress.clear ->
            // com.lxy.timetable.MainActivity$ArrayView.clear
            length = 0;
        }
    }

    private static class InstanceFieldAccessor<T> {
        private final Field field;

        @NonNull
        public static <T> InstanceFieldAccessor<T> of(
                @NonNull Class<?> clazz, @NonNull String name) {
            try {
                return new InstanceFieldAccessor<>(clazz.getDeclaredField(name));
            } catch (NoSuchFieldException e) {
                throw TimberUtil.errorAndThrowException(e,
                        "SmartTable.class.getDeclaredField(\"" + name + "\") failed");
            }
        }

        public InstanceFieldAccessor(@NonNull Field field) {
            if (BuildConfig.DEBUG) {
                Contract.require(!Modifier.isStatic(field.getModifiers()),
                        "Non static field required");
            }
            field.setAccessible(true);
            this.field = field;
        }

        @SuppressWarnings("unchecked")
        @Nullable
        public T get(@NonNull Object o) {
            try {
                return (T) field.get(o);
            } catch (IllegalAccessException e) {
                throw TimberUtil.errorAndThrowException(e, "Field.get failed");
            }
        }

        public void set(@NonNull Object o, @NonNull T value) {
            try {
                field.set(o, value);
            } catch (IllegalAccessException e) {
                throw TimberUtil.errorAndThrowException(e, "Field.set failed");
            }
        }
    }

    private static final int OFFSET = 1;
    private static final int REAL_ROW_COUNT = TimeTableData.ROW_COUNT;
    private static final int REAL_COLUMN_COUNT = TimeTableData.COLUMN_COUNT + OFFSET;
    private static final int REAL_CELL_COUNT = REAL_ROW_COUNT * REAL_COLUMN_COUNT;

    @NonNull
    private static final String TIME_TABLE_DATA_FILE_NAME = "TimeTable.bin";
    @NonNull
    private static final String MIME_ALL = "*/*";
    @NonNull
    private static final String[] FILTER_MIME_ALL = {MIME_ALL};
    @NonNull
    private static final String[] ROW_HEADER =
            GlobalContext.getResource().getStringArray(R.array.row_header);
    @NonNull
    private static final String[] COLUMN_HEADER =
            GlobalContext.getResource().getStringArray(R.array.column_header);
    @NonNull
    private static final String[] ROW_TOOLTIP =
            GlobalContext.getResource().getStringArray(R.array.row_tooltip);
    @NonNull
    private static final Path TIME_TABLE_DATA_PATH = FileSystems.getDefault().getPath(
            GlobalContext.get().getFilesDir().getAbsolutePath(), TIME_TABLE_DATA_FILE_NAME);
    @NonNull
    private static final ArrayView<CellRange> MERGED_CELL_RANGES_VIEW = initialize(() -> {
        final int CELL_RANGE_COUNT = TimeTableData.MAXIMUM_MERGED_ROWS * TimeTableData.COLUMN_COUNT;

        CellRange[] ranges = new CellRange[CELL_RANGE_COUNT];
        for (int i = 0; i < CELL_RANGE_COUNT; i++) {
            ranges[i] = new CellRange(0, 0, 0, 0);
        }
        return new ArrayView<>(ranges, 0);
    });

    @SuppressWarnings({"unchecked", "rawtypes"})
    @NonNull
    private static final ArrayTableData<Cell> TIME_TABLE_DATA = initialize(() -> {
        Column<String> column = new Column<>("", "");
        column.setDatas(new AbstractList<>() {
            @Override
            @NonNull
            public String get(int index) {
                TimeTableData.validateRowIndex(index);
                return ROW_HEADER[index];
            }

            @Override
            public int size() {
                return TimeTableData.ROW_COUNT;
            }
        });
        column.setFast(true);

        ArrayTableData<Cell> tableData = ArrayTableData.create(
                (String) null, null, TimeTableData.TIME_TABLE_DATA, null);
        tableData.getColumns().add(0, column);
        tableData.getArrayColumns().add(0, (Column<Cell>) (Column) column);
        tableData.setXSequenceFormat(new BaseSequenceFormat() {
            @Override
            public String format(Integer integer) {
                return COLUMN_HEADER[integer - 1];
            }
        });
        tableData.setUserCellRange(MERGED_CELL_RANGES_VIEW);
        return tableData;
    });

    @NonNull
    private static final InstanceFieldAccessor<TableMeasurer<Cell>> SmartTable_measurer
            = InstanceFieldAccessor.of(SmartTable.class, "measurer");

    @NonNull
    private static final InstanceFieldAccessor<TableConfig> SmartTable_config
            = InstanceFieldAccessor.of(SmartTable.class, "config");

    private static final float DIVIDER_WIDTH =
            GlobalContext.getResource().getDimension(R.dimen.divider_width);

    @NonNull
    private static final TableConfig DEFAULT_TABLE_CONFIG = initialize(() -> {
        final int TEN_DP = 10;

        Context context = GlobalContext.get();
        Resources resources = context.getResources();

        TableConfig config = new TableConfig();
        config.dp10 = DensityUtils.dp2px(context, TEN_DP);
        int padding = (int) resources.getDimension(R.dimen.padding);
        FontStyle fontStyle = new FontStyle((int) resources.getDimension(R.dimen.font_size),
                context.getColor(R.color.text_color));
        LineStyle lineStyle = new LineStyle(
                DIVIDER_WIDTH, context.getColor(R.color.divider_color));
        // Title
        config.setShowColumnTitle(false).setShowTableTitle(false).setShowXSequence(true).setShowYSequence(false)
                // Fixed
                .setFixedTitle(false).setFixedXSequence(true)
                // Font style
                .setContentStyle(fontStyle).setColumnTitleStyle(fontStyle).setXSequenceStyle(fontStyle)
                // Line style
                .setContentGridStyle(lineStyle).setColumnTitleGridStyle(lineStyle).setSequenceGridStyle(lineStyle)
                // Padding
                .setHorizontalPadding(padding).setVerticalPadding(padding)
                .setColumnTitleHorizontalPadding(padding).setColumnTitleVerticalPadding(padding);
        return config;
    });

    @NonNull
    private static final IntentFilter INTENT_FILTER = new IntentFilter(Intent.ACTION_TIME_CHANGED);
    @NonNull
    private static final LinkOption[] EMPTY_LINK_OPTIONS = new LinkOption[0];
    @NonNull
    private static final AccessMode[] ACCESS_READ = new AccessMode[]{AccessMode.READ};
    @NonNull
    private static final AccessMode[] ACCESS_WRITE = new AccessMode[]{AccessMode.WRITE};
    @NonNull
    private static final FileAttribute<?>[] EMPTY_FILE_ATTRIBUTES = new FileAttribute<?>[0];
    @NonNull
    private static final ActivityResultContracts.OpenDocument OPEN_DOCUMENT =
            new ActivityResultContracts.OpenDocument();
    @NonNull
    private static final ActivityResultContracts.CreateDocument CREATE_DOCUMENT =
            new ActivityResultContracts.CreateDocument(MIME_ALL);

    @NonNull
    private static final Set<StandardOpenOption> OPTIONS_READ = EnumSet.of(StandardOpenOption.READ);

    @NonNull
    private static final Set<StandardOpenOption> OPTIONS_CREATE_NEW = EnumSet.of(StandardOpenOption.CREATE_NEW);

    @NonNull
    private static final Set<StandardOpenOption> OPTIONS_WRITE_AND_CREATE =
            EnumSet.of(StandardOpenOption.WRITE, StandardOpenOption.CREATE);

    @NonNull
    private final ActivityResultLauncher<String[]> importTable =
            registerForActivityResult(OPEN_DOCUMENT, this::openExternalTimeTableFile);
    @NonNull
    private final ActivityResultLauncher<String> exportTable =
            registerForActivityResult(CREATE_DOCUMENT, this::saveExternalTimeTable);

    @NonNull
    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(@NonNull Context context, Intent intent) {
            if (BuildConfig.DEBUG) {
                Timber.d("System date changed, intent: %s", intent);
            }
            refreshTimeAndWeeks();
        }
    };

    private ActivityMainBinding binding;

    private TableMeasurer<Cell> tableMeasurer;

    static {
        if (BuildConfig.DEBUG) {
            Contract.require(ROW_HEADER.length == TimeTableData.ROW_COUNT,
                    "Count of row header != ROW_COUNT");
            Contract.require(COLUMN_HEADER.length == REAL_COLUMN_COUNT,
                    "Count of column header != REAL_COLUMN_COUNT");
            Contract.require(ROW_TOOLTIP.length == TimeTableData.ROW_COUNT,
                    "Count of row tooltip != ROW_COUNT");
            Timber.d("\"Table data file: \"%s\"", TIME_TABLE_DATA_PATH);
        }
    }

    @NonNull
    private static <T> T initialize(@NonNull Supplier<T> supplier) {
        return Contract.requireNonNull(Contract.requireNonNull(supplier).get());
    }

    private static boolean checkFileAccess(@NonNull Path path, boolean readOrWrite) {
        try {
            FileSystemProvider provider = path.getFileSystem().provider();
            if (!provider.readAttributes(path, BasicFileAttributes.class, EMPTY_LINK_OPTIONS).isDirectory()) {
                provider.checkAccess(path, readOrWrite ? ACCESS_READ : ACCESS_WRITE);
                return true;
            }
        } catch (IOException ignored) {
        }
        return false;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        initializeTimeTable();
        initializeBinding();
        remeasureTable();
        openInternalTimeTableFile();
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void remeasureTable() {
        if (BuildConfig.DEBUG) {
            Timber.d("Remeasure table");
        }
        SmartTable<Cell> smartTable = getSmartTable();
        smartTable.post(() -> {
            List<Column> columns = TIME_TABLE_DATA.getColumns();
            int tableWidth = smartTable.getMeasuredWidth();
            if (BuildConfig.DEBUG) {
                Timber.d("Table measured width: %d", tableWidth);
            }
            int firstColumnWidth = columns.get(0).getComputeWidth();
            if (BuildConfig.DEBUG) {
                Timber.d("First column width: %d", firstColumnWidth);
            }
            float restWidth = (tableWidth - firstColumnWidth -
                    (TimeTableData.COLUMN_COUNT + 1) * (DIVIDER_WIDTH + 1));
            if (BuildConfig.DEBUG) {
                Timber.d("Rest width: %f", restWidth);
            }
            MultiLineDrawFormat<Cell> format = new MultiLineDrawFormat<>(
                    (int) (restWidth / TimeTableData.COLUMN_COUNT));
            for (int i = 0; i < TimeTableData.COLUMN_COUNT; i++) {
                columns.get(i + OFFSET).setDrawFormat(format);
            }
            smartTable.getConfig().setMinTableWidth(tableWidth);
            remeasure();
        });
    }

    private void initializeBinding() {
        if (BuildConfig.DEBUG) {
            Timber.d("Initialize binding");
        }
        SmartTable<Cell> smartTable = getSmartTable();
        binding.buttonImport.setOnClickListener(v -> importTable.launch(FILTER_MIME_ALL));
        binding.buttonExport.setOnClickListener(v -> {
            if (TimeTableData.isValid() || checkFileAccess(TIME_TABLE_DATA_PATH, true)) {
                exportTable.launch(TIME_TABLE_DATA_FILE_NAME);
            } else {
                promptAndOpenTimeTableFile();
            }
        });
        tableMeasurer = Contract.requireNonNull(SmartTable_measurer.get(smartTable));
        smartTable.setTableData(TIME_TABLE_DATA);
        GlobalContext.registerBroadcastReceiver(receiver, INTENT_FILTER);
    }

    private void tryOpenExternalTableFile() {
        importTable.launch(FILTER_MIME_ALL);
    }

    private void promptAndOpenTimeTableFile() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.prompt_load_time_table_file)
                .setCancelable(true)
                .setPositiveButton(R.string.yes, (dialog, which) -> tryOpenExternalTableFile())
                .setNegativeButton(R.string.no, (dialog, which) -> dialog.dismiss())
                .show();
    }

    private static void handleIOException(@NonNull IOException e) {
        ToastUtil.toast(R.string.open_time_table_file_failed);
        if (BuildConfig.DEBUG) {
            Timber.e(e, "Open timetable data file failed");
        }
    }

    private void openTimeTableFile(@NonNull OpenTimeTableFileHandler handler) {
        Contract.requireNonNull(handler);
        if (BuildConfig.DEBUG) {
            Timber.d("Open timetable file");
        }
        try {
            try (InputStream inputStream = handler.openInputStream()) {
                switch (TimeTableData.deserialize(inputStream)) {
                    case DeserializeResult.OK:
                        if (handler.reallySucceeded()) {
                            ToastUtil.toast(R.string.open_time_table_file_succeeded);
                            openTimeTableSucceed();
                        } else {
                            TimeTableData.clear();
                        }
                        break;
                    case DeserializeResult.INVALID_FILE:
                        ToastUtil.toast(R.string.time_table_file_corrupted);
                        if (BuildConfig.DEBUG) {
                            Timber.w("Open timetable failed.");
                        }
                        break;
                }
            }
        } catch (TimeTableFileNotFoundException e) {
            ToastUtil.toast(R.string.time_table_file_not_found);
            if (BuildConfig.DEBUG) {
                Timber.e(e, "Table data file does not exist");
            }
            handler.fileNotFound();
        } catch (IOException e) {
            handleIOException(e);
            handler.failed();
        }
    }

    private void openInternalTimeTableFile() {
        if (TimeTableData.isValid()) {
            if (BuildConfig.DEBUG) {
                Timber.d("Table is already loaded");
            }
            return;
        }
        openTimeTableFile(new OpenTimeTableFileHandler() {
            @NonNull
            @Override
            public InputStream openInputStream() throws IOException {
                try {
                    Path path = TIME_TABLE_DATA_PATH;
                    if (!checkFileAccess(path, true)) {
                        throw new TimeTableFileNotFoundException("Time table file not found");
                    }
                    return Channels.newInputStream(Files.newByteChannel(
                            path, OPTIONS_READ, EMPTY_FILE_ATTRIBUTES));
                } catch (NoSuchFileException | DirectoryNotEmptyException e) {
                    throw new TimeTableFileNotFoundException(e);
                }
            }

            @Override
            public boolean reallySucceeded() {
                return true;
            }

            @Override
            public void fileNotFound() {
                tryOpenExternalTableFile();
            }

            @Override
            public void failed() {
                try {
                    if (BuildConfig.DEBUG) {
                        Timber.w("Try to delete timetable data file");
                    }
                    Files.delete(TIME_TABLE_DATA_PATH);
                } catch (NoSuchFileException | DirectoryNotEmptyException e) {
                    ToastUtil.toast(R.string.delete_time_table_file_failed);
                    if (BuildConfig.DEBUG) {
                        Timber.e(e, "Delete timetable data file failed");
                    }
                } catch (IOException e) {
                    handleIOException(e);
                }
                tryOpenExternalTableFile();
            }
        });
    }

    private void initializeTimeTable() {
        if (BuildConfig.DEBUG) {
            Timber.d("Initialize timetable");
        }
        TIME_TABLE_DATA.setOnItemClickListener((column, value, o, col, row) -> {
            if (col < OFFSET) {
                ToastUtil.toast(ROW_TOOLTIP[row]);
            } else {
                if (TimeTableData.isValid()) {
                    Cell cell = TimeTableData.TIME_TABLE_DATA[col - OFFSET][row];
                    ToastUtil.toast(GlobalContext.get().getString(
                            R.string.class_info, cell.getOdd(), cell.getEven()));
                } else {
                    promptAndOpenTimeTableFile();
                }
            }
        });
        SmartTable<Cell> tableMain = getSmartTable();
        TableConfig config = DEFAULT_TABLE_CONFIG;
        if (BuildConfig.DEBUG) {
            Timber.d("Setting timetable config");
        }
        config.setPaint(tableMain.getConfig().getPaint());
        SmartTable_config.set(tableMain, config);
    }

    private void remeasure() {
        SmartTable<Cell> smartTable = getSmartTable();
        tableMeasurer.measure(smartTable.getTableData(), smartTable.getConfig());
        smartTable.invalidate();
    }

    @SuppressWarnings("unchecked")
    private SmartTable<Cell> getSmartTable() {
        return binding.tableMain;
    }

    private void openTimeTableSucceed() {
        ArrayView<CellRange> view = MERGED_CELL_RANGES_VIEW;
        ArrayTableData<Cell> tableData = TIME_TABLE_DATA;
        int length = 0;
        CellRange[] ranges = view.getArray();
        for (int column = 0; column < TimeTableData.COLUMN_COUNT; column++) {
            MergeStates mergeStates = new MergeStates(TimeTableData.MERGE_STATES[column]);
            for (int index = 0; index < mergeStates.getCount(); index++) {
                int realColumn = column + OFFSET;
                CellRange range = ranges[length++];
                char mergedRow = mergeStates.get(index);
                range.setFirstRow(MergeStates.getFirstRow(mergedRow));
                range.setLastRow(MergeStates.getLastRow(mergedRow));
                range.setFirstCol(realColumn);
                range.setLastCol(realColumn);
            }
        }
        view.setLength(length);
        // Reset user cell ranges
        tableData.getTableInfo().setColumnSize(REAL_COLUMN_COUNT);
        tableData.clearCellRangeAddresses();
        remeasure();
        refreshTimeAndWeeks();
    }

    private void refreshTimeAndWeeks() {
        String text;
        if (TimeTableData.isValid()) {
            long now = DateUtil.now();
            long beginDate = TimeTableData.beginDate();
            long weeks = DateUtil.weekCount(beginDate, now);
            if (BuildConfig.DEBUG) {
                Timber.d("Now: %s, Begin date: %s, Weeks: %d",
                        LocalDate.ofEpochDay(now), LocalDate.ofEpochDay(beginDate), weeks);
            }
            LocalDate beginDateLocalDate = LocalDate.ofEpochDay(beginDate);
            text = getString(R.string.format_begin_time,
                    beginDateLocalDate.getYear(), beginDateLocalDate.getMonthValue(),
                    beginDateLocalDate.getDayOfMonth(), weeks);
        } else {
            text = getString(R.string.unknown_begin_time);
        }
        binding.labelBeginTime.setText(text);
    }

    private void openExternalTimeTableFile(Uri result) {
        if (result == null) {
            ToastUtil.toast(R.string.request_cancelled);
            if (BuildConfig.DEBUG) {
                Timber.w("User cancelled selection");
            }
            return;
        }
        if (BuildConfig.DEBUG) {
            Timber.d("Timetable file location: \"%s\"", result);
        }
        openTimeTableFile(new OpenTimeTableFileHandler() {
            private CopyingInputStream stream;

            @NonNull
            @Override
            public InputStream openInputStream() throws IOException {
                try {
                    CopyingInputStream stream = new CopyingInputStream(getContentResolver().
                            openInputStream(result), TimeTableData.ESTIMATED_FILE_SIZE);
                    this.stream = stream;
                    return stream;
                } catch (FileNotFoundException e) {
                    throw new TimeTableFileNotFoundException(e);
                }
            }

            @Override
            public boolean reallySucceeded() {
                try (SeekableByteChannel outputChannel = Files.newByteChannel(
                        TIME_TABLE_DATA_PATH, OPTIONS_WRITE_AND_CREATE, EMPTY_FILE_ATTRIBUTES)) {
                    if (BuildConfig.DEBUG) {
                        Timber.d("File size: %d", stream.size());
                    }
                    outputChannel.write(ByteBuffer.wrap(stream.buffer(), 0, stream.size()));
                    return true;
                } catch (IOException e) {
                    if (BuildConfig.DEBUG) {
                        Timber.d(e, "Write internal timetable data file failed");
                    }
                    ToastUtil.toast(R.string.save_time_table_file_to_internal_storage_failed);
                    return false;
                }
            }

            @Override
            public void fileNotFound() {

            }

            @Override
            public void failed() {

            }
        });
    }

    private void saveExternalTimeTable(@Nullable Uri result) {
        if (result == null) {
            ToastUtil.toast(R.string.request_cancelled);
            if (BuildConfig.DEBUG) {
                Timber.w("User cancelled selection");
            }
            return;
        }
        try {
            if (BuildConfig.DEBUG) {
                Timber.d("Save timetable file, location: %s", result);
            }
            Path path = TIME_TABLE_DATA_PATH;
            try (OutputStream outputStream = getContentResolver().openOutputStream(result)) {
                try {
                    ByteArrayAppender serialized = TimeTableData.serialize();
                    if (!checkFileAccess(path, false)) {
                        try (SeekableByteChannel outputChannel = Files.newByteChannel(
                                path, OPTIONS_CREATE_NEW, EMPTY_FILE_ATTRIBUTES)) {
                            outputChannel.write(ByteBuffer.wrap(serialized.buffer(), 0, serialized.size()));
                        }
                    }
                    outputStream.write(serialized.buffer(), 0, serialized.size());
                    ToastUtil.toast(R.string.save_time_table_file_succeeded);
                } catch (IOException e) {
                    ToastUtil.toast(R.string.save_time_table_file_to_internal_storage_failed);
                }
            }
        } catch (IOException e) {
            ToastUtil.toast(R.string.save_time_table_file_failed);
            if (BuildConfig.DEBUG) {
                Timber.e(e, "Save timetable data file failed");
            }
        }
    }
}
