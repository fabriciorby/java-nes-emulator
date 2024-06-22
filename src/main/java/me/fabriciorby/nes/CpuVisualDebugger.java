package me.fabriciorby.nes;

import javafx.application.Application;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.event.Event;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import me.fabriciorby.nes.cpu.Cpu;
import me.fabriciorby.nes.cpu.Debugger;

import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;
import java.util.stream.IntStream;

public class CpuVisualDebugger extends Application {

    private final int RAM_SIZE = 0xFFFF + 1;
    private final int CARTRIDGE_OFFSET = 0x8000;
    String multiply3by10 = "A2 0A 8E 00 00 A2 03 8E 01 00 AC 00 00 A9 00 18 6D 01 00 88 D0 FA 8D 02 00 EA EA EA";

    Bus cpuBus;
    Cpu cpu;
    int[] cpuRam;

    {

        this.cpuBus = new Bus() {
            {
                this.cpuRam = new int[RAM_SIZE];
            }

            @Override
            public void cpuWrite(int address, int data) {
                if (address >= 0x0000 && address <= 0xFFFF) {
                    cpuRam[address] = data;
                }
            }

            @Override
            public int cpuRead(int address, boolean readOnly) {
                if (address >= 0x0000 && address <= 0xFFFF) {
                    return cpuRam[address];
                }
                return 0x00;
            }
        };
        this.cpu = cpuBus.cpu;
        this.cpu.connectBus(cpuBus);
        /*
         * As per the documentation for the 6502, when the CPU is reset, it fetches the 16-bit address from memory
         * locations 0xFFFC and 0xFFFD and sets the program counter to that address. Since the 6502 operates in little
         * endian format, the low byte is stored first, then the high byte. That means that if your program code starts
         * at 0xC000, you need values 0x00 and 0xC0 in bytes 0xFFFC and 0xFFFD respectively.
         * */
        this.cpuBus.cpuRam[0xFFFC] = 0x00;
        this.cpuBus.cpuRam[0xFFFD] = 0x80;
        this.cpuRam = cpuBus.cpuRam;
        this.cpuBus.cpu.reset();
        byte[] program = getProgram(multiply3by10);
        for (int i = 0; i < program.length; i++) {
            cpuBus.cpuRam[CARTRIDGE_OFFSET + i] = Byte.toUnsignedInt(program[i]);
        }
    }

    private ObservableList<int[]> generateData() {
        return FXCollections.observableArrayList(IntStream.iterate(0, i -> i + 16)
                .limit((RAM_SIZE / 16) + 1)
                .filter(i -> i <= 0x00F0 || (i >= 0x8000 && i <= 0x80F0))
                .mapToObj(i -> {
                    var chunk = Arrays.copyOfRange(cpuRam, i, i + 16);
                    var result = new int[chunk.length + 1];
                    result[0] = i;
                    System.arraycopy(chunk, 0, result, 1, chunk.length);
                    return result;
                }) //ugly as fuck, but now we have the memory address field at i[0]
                .toList());
    }

    @Override
    public void start(Stage stage) {

        VBox vbox = new VBox();
        HBox hbox = new HBox();
        Debugger debugger = new Debugger(cpu);
        Label flags = new Label(debugger.getFlags());
        Label programCounter = new Label(debugger.getCurrentInstruction());
        Label accumulator = new Label(debugger.getAccumulator());
        Label xRegister = new Label(debugger.getXRegister());
        Label yRegister = new Label(debugger.getYRegister());
        Label stack = new Label(debugger.getStackPointer());

        ListView<String> listView = new ListView<>();
        listView.setItems(FXCollections.observableArrayList(
                IntStream.range(cpu.programCounter - 13, cpu.programCounter + 16)
                .mapToObj(debugger::getInstruction).toList()));
        listView.setPrefHeight(700);
        listView.getSelectionModel().select(13);
        listView.addEventFilter(MouseEvent.ANY, Event::consume);
        listView.addEventFilter(KeyEvent.KEY_PRESSED, Event::consume);

        TableView<int[]> tableView = new TableView<>();
        tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        tableView.setTableMenuButtonVisible(true);
        tableView.setItems(getHexItemsList());
        tableView.getColumns().add(getAddressColumn());
        tableView.getColumns().addAll(getHexColumns());
        tableView.setFixedCellSize(25);
        tableView.prefHeightProperty().bind(tableView.fixedCellSizeProperty().multiply(Bindings.size(tableView.getItems()).add(1.15)));
        tableView.minHeightProperty().bind(tableView.prefHeightProperty());
        tableView.maxHeightProperty().bind(tableView.prefHeightProperty());

        ButtonBar buttonBar = new ButtonBar();
        buttonBar.setPadding(new Insets(10));
        Button clockButton = new Button("Clock");
        Button cancelButton = new Button("Reset");
        ButtonBar.setButtonData(clockButton, ButtonBar.ButtonData.OK_DONE);
        ButtonBar.setButtonData(cancelButton, ButtonBar.ButtonData.CANCEL_CLOSE);
        cancelButton.setOnAction(event -> {
            cpuBus.reset();
            Debugger bug = new Debugger(cpu);
            tableView.setItems(generateData());
            tableView.refresh();
            flags.setText(bug.getFlags());
            programCounter.setText(bug.getCurrentInstruction());
            accumulator.setText(bug.getAccumulator());
            xRegister.setText(bug.getXRegister());
            yRegister.setText(bug.getYRegister());
            stack.setText(bug.getStackPointer());
            listView.setItems(FXCollections.observableArrayList(
                    IntStream.range(cpu.programCounter - 13, cpu.programCounter + 16)
                            .mapToObj(debugger::getInstruction).toList()));
            listView.getSelectionModel().select(13);
            listView.refresh();
        });
        clockButton.setOnAction(event -> {
            do {cpuBus.clock();} while (!cpu.complete());
            do {cpuBus.clock();} while (cpu.complete());
            Debugger bug = new Debugger(cpu);
            tableView.setItems(generateData());
            tableView.refresh();
            flags.setText(bug.getFlags());
            programCounter.setText(bug.getCurrentInstruction());
            accumulator.setText(bug.getAccumulator());
            xRegister.setText(bug.getXRegister());
            yRegister.setText(bug.getYRegister());
            stack.setText(bug.getStackPointer());
            listView.setItems(FXCollections.observableArrayList(
                    IntStream.range(cpu.programCounter - 13, cpu.programCounter + 16)
                            .mapToObj(debugger::getInstruction).toList()));
            listView.getSelectionModel().select(13);
            listView.refresh();
        });
        buttonBar.getButtons().addAll(clockButton, cancelButton);

        VBox.setVgrow(tableView, Priority.ALWAYS);
        HBox.setHgrow(tableView, Priority.ALWAYS);
        vbox.getChildren().addAll(flags, programCounter, accumulator, xRegister, yRegister, stack, listView, buttonBar);
        hbox.getChildren().addAll(tableView, new Separator(), vbox);

        Scene scene = new Scene(hbox);
        stage.setTitle("6502 Debugger");
        stage.setScene(scene);
        stage.setResizable(false);
        stage.setWidth(1024);
        stage.setHeight(880);
        stage.show();
    }

    private FilteredList<int[]> getHexItemsList() {
        return new FilteredList<>(generateData(), i -> i[0] <= 0x00F0 || (i[0] >= 0x8000 && i[0] <= 0x80F0));
    }

    private static List<TableColumn<int[], String>> getHexColumns() {
        return IntStream.range(1, 17).mapToObj(value -> {
            TableColumn<int[], String> tableColumn = new TableColumn<>(String.valueOf(value));
            tableColumn.setCellValueFactory(cellData -> new ReadOnlyObjectWrapper<>("%02X".formatted(cellData.getValue()[value])));
            return tableColumn;
        }).toList();
    }

    private static TableColumn<int[], String> getAddressColumn() {
        TableColumn<int[], String> column = new TableColumn<>("Addr");
        column.setCellValueFactory(cellData -> new ReadOnlyObjectWrapper<>("$%04X".formatted(cellData.getValue()[0])));
        return column;
    }

    private byte[] getProgram(String program) {
        return HexFormat.ofDelimiter(" ").parseHex(program);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
