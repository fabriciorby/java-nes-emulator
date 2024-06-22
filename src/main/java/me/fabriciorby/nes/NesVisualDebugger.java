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
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.*;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import me.fabriciorby.nes.cartridge.Cartridge;
import me.fabriciorby.nes.cpu.Cpu;
import me.fabriciorby.nes.cpu.Debugger;

import java.util.List;
import java.util.stream.IntStream;

public class NesVisualDebugger extends Application {

    private final Bus nes = new Bus();
    private final Cpu cpu = nes.cpu;

    private final Label flags = new Label();
    private final Label programCounter = new Label();
    private final Label accumulator = new Label();
    private final Label xRegister = new Label();
    private final Label yRegister = new Label();
    private final Label stack = new Label();
    private ListView<String> listView;
    private TableView<int[]> tableView;
    private WritableImage render;
    private final ImageView imageView = new ImageView();
    private final ButtonBar buttonBar = new ButtonBar();

    {
        Cartridge cartridge = new Cartridge("nestest.nes");
        nes.insert(cartridge);
        nes.reset();
    }

    @Override
    public void start(Stage stage) {
        setupInstructionList();
        setupMemoryTable();
        setupButtons();
        setupImageRender();
        setupLayout(stage);
        refresh();
    }

    private void setupLayout(Stage stage) {
        VBox vbox = new VBox();
        HBox hbox = new HBox();
        vbox.getChildren().addAll(flags, programCounter, accumulator, xRegister, yRegister, stack, listView, buttonBar);
        hbox.getChildren().addAll(imageView, tableView, new Separator(), vbox);
        tableView.setVisible(false);
        Scene scene = new Scene(hbox);
        stage.setTitle("NES Debugger");
        stage.setScene(scene);
        stage.setResizable(false);
        stage.setWidth(1024);
        stage.setHeight(880);
        stage.show();
    }

    private void setupImageRender() {
        this.render = new WritableImage(nes.ppu.getScreen().getWidth(), nes.ppu.getScreen().getHeight());
    }

    private void setupButtons() {
        buttonBar.setPadding(new Insets(10));
        Button clockButton = new Button("Clock");
        Button resetButton = new Button("Reset");
        ButtonBar.setButtonData(clockButton, ButtonBar.ButtonData.OK_DONE);
        ButtonBar.setButtonData(resetButton, ButtonBar.ButtonData.CANCEL_CLOSE);
        resetButton.setOnAction(event -> {
            nes.reset();
            refresh();
        });
        clockButton.setOnAction(event -> {
            do {nes.clock();} while (!cpu.complete());
            do {nes.clock();} while (cpu.complete());
            refresh();
        });
        buttonBar.getButtons().addAll(clockButton, resetButton);
    }

    private void setupMemoryTable() {
        this.tableView = new TableView<>();
        tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        tableView.setTableMenuButtonVisible(true);
        tableView.setItems(getHexItemsList());
        tableView.getColumns().add(getAddressColumn());
        tableView.getColumns().addAll(getHexColumns());
        tableView.setFixedCellSize(25);
        tableView.prefHeightProperty().bind(tableView.fixedCellSizeProperty().multiply(Bindings.size(tableView.getItems()).add(1.15)));
        tableView.minHeightProperty().bind(tableView.prefHeightProperty());
        tableView.maxHeightProperty().bind(tableView.prefHeightProperty());
    }

    private void setupInstructionList() {
        this.listView = new ListView<>();
        listView.setPrefHeight(700);
        listView.getSelectionModel().select(12);
        listView.addEventFilter(MouseEvent.ANY, Event::consume);
        listView.addEventFilter(KeyEvent.KEY_PRESSED, Event::consume);
    }

    private void refresh() {
        Debugger debugger = new Debugger(cpu);
        tableView.setItems(getHexItemsList());
        tableView.refresh();
        flags.setText(debugger.getFlags());
        programCounter.setText(debugger.getCurrentInstruction());
        accumulator.setText(debugger.getAccumulator());
        xRegister.setText(debugger.getXRegister());
        yRegister.setText(debugger.getYRegister());
        stack.setText(debugger.getStackPointer());
        listView.setItems(FXCollections.observableArrayList(
                IntStream.range(cpu.programCounter - 13, cpu.programCounter + 16)
                        .mapToObj(debugger::getInstruction).toList()));
        listView.getSelectionModel().select(13);
        listView.refresh();
        render();
    }

    private void render() {
        for (int i = 0; i < render.getWidth(); i++) {
            for (int j = 0; j < render.getHeight(); j++) {
                render.getPixelWriter().setColor(i, j, nes.ppu.getScreen().getPixelArray()[i][j]);
            }
        }
        imageView.setImage(scale(render, 3));
    }

    private Image scale(Image input, int scaleFactor) {
        final int W = (int) input.getWidth();
        final int H = (int) input.getHeight();
        final int S = scaleFactor;

        WritableImage output = new WritableImage(W * S, H * S);
        PixelReader reader = input.getPixelReader();
        PixelWriter writer = output.getPixelWriter();

        for (int y = 0; y < H; y++) {
            for (int x = 0; x < W; x++) {
                final int argb = reader.getArgb(x, y);
                for (int dy = 0; dy < S; dy++) {
                    for (int dx = 0; dx < S; dx++) {
                        writer.setArgb(x * S + dx, y * S + dy, argb);
                    }
                }
            }
        }

        return output;
    }

    private ObservableList<int[]> generateData() {
        return FXCollections.observableArrayList(IntStream.iterate(0, i -> i + 16)
                .limit((0xFFFF / 16) + 1)
                .filter(i -> i <= 0x00F0 || (i >= 0x8000 && i <= 0x80F0))
                .mapToObj(i -> {
                    var chunk = IntStream.range(i, i + 16)
                            .map(j -> nes.cpuRead(j, true))
                            .toArray();
                    var result = new int[chunk.length + 1];
                    result[0] = i;
                    System.arraycopy(chunk, 0, result, 1, chunk.length);
                    return result;
                }) //ugly as fuck, but now we have the memory address field at i[0]
                .toList());
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

    public static void main(String[] args) {
        launch(args);
    }
}
