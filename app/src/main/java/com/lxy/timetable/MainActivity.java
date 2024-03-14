package com.lxy.timetable;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import androidx.activity.ComponentActivity;
import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.databinding.DataBindingUtil;

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
import com.lxy.timetable.contract.Contract;
import com.lxy.timetable.contract.Operator;
import com.lxy.timetable.contract.Value;
import com.lxy.timetable.data.Cell;
import com.lxy.timetable.data.DeserializeResult;
import com.lxy.timetable.data.MergeStates;
import com.lxy.timetable.data.TimeTableData;
import com.lxy.timetable.databinding.ActivityMainBinding;
import com.lxy.timetable.enumset.OneEnumSet;
import com.lxy.timetable.enumset.TwoEnumSet;
import com.lxy.timetable.util.ArrayView;
import com.lxy.timetable.util.CopyingInputStream;
import com.lxy.timetable.util.DateUtil;
import com.lxy.timetable.util.InstanceFieldAccessor;
import com.lxy.timetable.util.ToastUtil;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
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
import java.time.LocalDate;
import java.util.AbstractList;
import java.util.Set;

import timber.log.Timber;

public class MainActivity extends ComponentActivity {
    private static final int OFFSET = 1;
    private static final int REAL_ROW_COUNT = TimeTableData.ROW_COUNT;
    private static final int REAL_COLUMN_COUNT = TimeTableData.COLUMN_COUNT + OFFSET;
    @SuppressWarnings("unused")
    private static final int REAL_CELL_COUNT = REAL_ROW_COUNT * REAL_COLUMN_COUNT;
    @NonNull
    private static final String TIME_TABLE_DATA_FILE_NAME = "TimeTable.bin";
    @NonNull
    private static final String MIME_ALL = "*/*";
    @NonNull
    private static final String[] FILTER_MIME_ALL = {MIME_ALL};
    @NonNull
    private static final InstanceFieldAccessor<TableMeasurer<Cell>> SmartTable_measurer
            = InstanceFieldAccessor.of(SmartTable.class, "measurer");
    @NonNull
    private static final InstanceFieldAccessor<TableConfig> SmartTable_config
            = InstanceFieldAccessor.of(SmartTable.class, "config");
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
    private static final Set<StandardOpenOption> OPTIONS_READ =
            new OneEnumSet<>(StandardOpenOption.READ);
    @NonNull
    private static final Set<StandardOpenOption> OPTIONS_CREATE_NEW =
            new OneEnumSet<>(StandardOpenOption.CREATE_NEW);
    @NonNull
    private static final Set<StandardOpenOption> OPTIONS_WRITE_AND_CREATE =
            new TwoEnumSet<>(StandardOpenOption.WRITE, StandardOpenOption.CREATE);
    @NonNull
    private static final ArrayView<CellRange> MERGED_CELL_RANGES_VIEW;
    @NonNull
    private static final ArrayTableData<Cell> TIME_TABLE_DATA;
    private static final float DIVIDER_WIDTH;
    @NonNull
    private static final String[] ROW_HEADER;
    @NonNull
    private static final String[] COLUMN_HEADER;
    @NonNull
    private static final String[] ROW_TOOLTIP;
    @NonNull
    private static final Path TIME_TABLE_DATA_PATH;
    @NonNull
    private static final TableConfig DEFAULT_TABLE_CONFIG;

    static {
        final var CELL_RANGE_COUNT = TimeTableData.MAXIMUM_MERGED_ROWS * TimeTableData.COLUMN_COUNT;

        var ranges = new CellRange[CELL_RANGE_COUNT];
        for (int i = 0; i < CELL_RANGE_COUNT; i++) {
            ranges[i] = new CellRange(0, 0, 0, 0);
        }
        MERGED_CELL_RANGES_VIEW = new ArrayView<>(ranges, 0);
    }

    static {
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

        var tableData = ArrayTableData.create(
                (String) null, null,
                TimeTableData.TIME_TABLE_DATA,
                null);
        tableData.getColumns().add(0, column);
        addColumn(tableData, column);
        tableData.setXSequenceFormat(new BaseSequenceFormat() {
            @Override
            public String format(Integer integer) {
                return COLUMN_HEADER[integer - 1];
            }
        });
        tableData.setUserCellRange(MERGED_CELL_RANGES_VIEW);
        TIME_TABLE_DATA = tableData;
    }

    static {
        final int TEN_DP = 10;

        var context = GlobalContext.get();
        var resources = context.getResources();
        DIVIDER_WIDTH = resources.getDimension(R.dimen.divider_width);
        ROW_HEADER = resources.getStringArray(R.array.row_header);
        COLUMN_HEADER = resources.getStringArray(R.array.column_header);
        ROW_TOOLTIP = resources.getStringArray(R.array.row_tooltip);
        TIME_TABLE_DATA_PATH = FileSystems.getDefault()
                .getPath(context.getFilesDir().getAbsolutePath(), TIME_TABLE_DATA_FILE_NAME);

        var config = new TableConfig();
        config.dp10 = DensityUtils.dp2px(context, TEN_DP);
        var padding = (int) resources.getDimension(R.dimen.padding);
        var lineStyle = new LineStyle(
                DIVIDER_WIDTH, context.getColor(R.color.divider_color));
        // Title
        config.setShowColumnTitle(false).setShowTableTitle(false).setShowXSequence(true).setShowYSequence(false)
                // Fixed
                .setFixedTitle(false).setFixedXSequence(true)
                // Line style
                .setContentGridStyle(lineStyle).setColumnTitleGridStyle(lineStyle).setSequenceGridStyle(lineStyle)
                // Padding
                .setHorizontalPadding(padding).setVerticalPadding(padding)
                .setColumnTitleHorizontalPadding(padding).setColumnTitleVerticalPadding(padding);
        DEFAULT_TABLE_CONFIG = config;
    }

    static {
        if (BuildConfig.DEBUG) {
            Contract.requireOperation(
                    new Value<>("rowHeaderLength", ROW_HEADER.length),
                    new Value<>("rowCount", REAL_ROW_COUNT),
                    Operator.EQ
            );
            Contract.requireOperation(
                    new Value<>("columnHeaderLength", COLUMN_HEADER.length),
                    new Value<>("columnCount", REAL_COLUMN_COUNT),
                    Operator.EQ
            );
            Contract.requireOperation(
                    new Value<>("rowToolTipLength", ROW_TOOLTIP.length),
                    new Value<>("rowCount", REAL_ROW_COUNT),
                    Operator.EQ
            );
            Timber.d("Table data file: \"%s\"", TIME_TABLE_DATA_PATH);
        }
    }

    @NonNull
    private final ActivityResultLauncher<String[]> importTable =
            registerForActivityResult(OPEN_DOCUMENT, this::openExternalTimeTableFile);
    @NonNull
    private final ActivityResultLauncher<String> exportTable =
            registerForActivityResult(CREATE_DOCUMENT, this::saveExternalTimeTable);
    private ActivityMainBinding binding;
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
    private TableMeasurer<Cell> tableMeasurer;

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void addColumn(@NonNull ArrayTableData<Cell> tableData, @NonNull Column<String> column) {
        tableData.getArrayColumns().add(0, (Column<Cell>) (Column) column);
    }

    private static boolean checkFileAccess(@NonNull Path path, boolean readOrWrite) {
        try {
            var provider = path.getFileSystem().provider();
            if (!provider.readAttributes(path, BasicFileAttributes.class, EMPTY_LINK_OPTIONS).isDirectory()) {
                provider.checkAccess(path, readOrWrite ? ACCESS_READ : ACCESS_WRITE);
                return true;
            }
        } catch (IOException ignored) {
        }
        return false;
    }

    private static void handleIOException(@NonNull IOException e) {
        ToastUtil.toast(R.string.open_time_table_file_failed);
        if (BuildConfig.DEBUG) {
            Timber.e(e, "Open timetable data file failed");
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = DataBindingUtil.setContentView(
                this, R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.activity_main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        initializeTimeTable();
        initializeBinding();
        remeasureTable();
        openInternalTimeTableFile();
    }

    @SuppressWarnings({"unchecked"})
    private void remeasureTable() {
        if (BuildConfig.DEBUG) {
            Timber.d("Remeasure table");
        }
        var smartTable = getSmartTable();
        smartTable.post(() -> {
            var columns = TIME_TABLE_DATA.getColumns();
            var tableWidth = smartTable.getMeasuredWidth();
            if (BuildConfig.DEBUG) {
                Timber.d("Table measured width: %d", tableWidth);
            }
            var firstColumnWidth = columns.get(0).getComputeWidth();
            if (BuildConfig.DEBUG) {
                Timber.d("First column width: %d", firstColumnWidth);
            }
            var restWidth = (tableWidth - firstColumnWidth -
                    (TimeTableData.COLUMN_COUNT + 1) * (DIVIDER_WIDTH + 1));
            if (BuildConfig.DEBUG) {
                Timber.d("Rest width: %f", restWidth);
            }
            var format = new MultiLineDrawFormat<>(
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
        var smartTable = getSmartTable();
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
        registerBroadcastReceiver();
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    private void registerBroadcastReceiver() {
        if (BuildConfig.DEBUG) {
            Timber.d("Register new broadcast receiver");
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(receiver, INTENT_FILTER, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(receiver, INTENT_FILTER);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(receiver);
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

    private void openTimeTableFile(@NonNull OpenTimeTableFileHandler handler) {
        Contract.requireNonNull(handler);
        if (BuildConfig.DEBUG) {
            Timber.d("Open timetable file");
        }
        try {
            try (var inputStream = handler.openInputStream()) {
                if (inputStream == null) {
                    ToastUtil.toast(R.string.time_table_file_not_found);
                    handler.fileNotFound();
                    return;
                }
                switch (TimeTableData.deserialize(inputStream)) {
                    case DeserializeResult.OK -> {
                        if (handler.reallySucceeded()) {
                            ToastUtil.toast(R.string.open_time_table_file_succeeded);
                            openTimeTableSucceed();
                        } else {
                            TimeTableData.clear();
                        }
                    }
                    case DeserializeResult.INVALID_FILE -> {
                        ToastUtil.toast(R.string.time_table_file_corrupted);
                        if (BuildConfig.DEBUG) {
                            Timber.w("Open timetable failed.");
                        }
                    }
                }
            }
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
            @Nullable
            @Override
            public InputStream openInputStream() throws IOException {
                try {
                    var path = TIME_TABLE_DATA_PATH;
                    if (checkFileAccess(path, true)) {
                        return Channels.newInputStream(Files.newByteChannel(
                                path, OPTIONS_READ, EMPTY_FILE_ATTRIBUTES));
                    } else {
                        if (BuildConfig.DEBUG) {
                            Timber.e("External time table file not found");
                        }
                    }
                } catch (NoSuchFileException | DirectoryNotEmptyException e) {
                    if (BuildConfig.DEBUG) {
                        Timber.e(e, "External time table file not found");
                    }
                }
                return null;
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
        var tableMain = getSmartTable();
        var config = DEFAULT_TABLE_CONFIG;
        if (BuildConfig.DEBUG) {
            Timber.d("Setting timetable config");
        }
        var fontStyle = new FontStyle((int) getResources().
                getDimension(R.dimen.font_size), binding.textViewBeginTime.getCurrentTextColor());
        // Font style
        config.setContentStyle(fontStyle)
                .setColumnTitleStyle(fontStyle)
                .setXSequenceStyle(fontStyle)
                .setPaint(tableMain.getConfig().getPaint());
        SmartTable_config.set(tableMain, config);
    }

    private void remeasure() {
        var smartTable = getSmartTable();
        tableMeasurer.measure(smartTable.getTableData(), smartTable.getConfig());
        smartTable.invalidate();
    }

    @SuppressWarnings("unchecked")
    private SmartTable<Cell> getSmartTable() {
        return binding.tableMain;
    }

    private void openTimeTableSucceed() {
        var view = MERGED_CELL_RANGES_VIEW;
        var tableData = TIME_TABLE_DATA;
        int length = 0;
        var ranges = view.getArray();
        for (int column = 0; column < TimeTableData.COLUMN_COUNT; column++) {
            var mergeStates = new MergeStates(TimeTableData.MERGE_STATES[column]);
            for (int index = 0; index < mergeStates.getCount(); index++) {
                var realColumn = column + OFFSET;
                var range = ranges[length++];
                var mergedRow = mergeStates.get(index);
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
            var now = DateUtil.now();
            var beginDate = TimeTableData.beginDate();
            var weeks = DateUtil.weekCount(beginDate, now);
            if (BuildConfig.DEBUG) {
                Timber.d("Now: %s, Begin date: %s, Weeks: %d",
                        LocalDate.ofEpochDay(now), LocalDate.ofEpochDay(beginDate), weeks);
            }
            var beginDateLocalDate = LocalDate.ofEpochDay(beginDate);
            text = getString(R.string.format_begin_time,
                    beginDateLocalDate.getYear(), beginDateLocalDate.getMonthValue(),
                    beginDateLocalDate.getDayOfMonth(), weeks);
        } else {
            text = getString(R.string.unknown_begin_time);
        }
        binding.textViewBeginTime.setText(text);
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

            @Nullable
            @Override
            public InputStream openInputStream() {
                try {
                    var inputStream = getContentResolver().openInputStream(result);
                    if (inputStream == null) {
                        if (BuildConfig.DEBUG) {
                            Timber.e("openInputStream on uri %s returns null", result);
                        }
                    } else {
                        var stream = new CopyingInputStream(
                                inputStream, TimeTableData.ESTIMATED_FILE_SIZE);
                        this.stream = stream;
                        return stream;
                    }
                } catch (FileNotFoundException e) {
                    if (BuildConfig.DEBUG) {
                        Timber.d(e, "Open external timetable data file not found");
                    }
                }
                return null;
            }

            @Override
            public boolean reallySucceeded() {
                try (var outputChannel = Files.newByteChannel(
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
            var path = TIME_TABLE_DATA_PATH;
            try (var outputStream = getContentResolver().openOutputStream(result)) {
                if (outputStream == null) {
                    ToastUtil.toast(R.string.save_time_table_file_failed);
                    if (BuildConfig.DEBUG) {
                        Timber.e("openOutputStream on uri %s returns null", result);
                    }
                    return;
                }
                try {
                    var serialized = TimeTableData.serialize();
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

    private interface OpenTimeTableFileHandler {
        @Nullable
        InputStream openInputStream() throws IOException;

        boolean reallySucceeded();

        void fileNotFound();

        void failed();
    }
}
