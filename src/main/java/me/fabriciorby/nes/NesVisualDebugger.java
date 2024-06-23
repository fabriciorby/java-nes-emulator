package me.fabriciorby.nes;

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.event.Event;
import javafx.geometry.Insets;
import javafx.scene.Parent;
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
import me.fabriciorby.nes.debugger.CalculateFps;
import me.fabriciorby.nes.ppu.Ppu;

import java.time.Instant;
import java.util.List;
import java.util.stream.IntStream;

public class NesVisualDebugger extends Application {

    private final Bus nes = new Bus();
    private final Cpu cpu = nes.cpu;
    private boolean emulationRun = false;
    private float fResidualTime = 0.0f;
    private int selectedPalette = 0x00;

    private final Label flags = new Label();
    private final Label programCounter = new Label();
    private final Label accumulator = new Label();
    private final Label xRegister = new Label();
    private final Label yRegister = new Label();
    private final Label stack = new Label();
    private final ListView<String> listView = new ListView<>();;
    private final TableView<int[]> tableView = new TableView<>();
    private WritableImage render;
    private final ImageView imageView = new ImageView();
    private final ButtonBar buttonBar = new ButtonBar();
    private Parent layoutParent;
    private float fElapsedTime;
    private long startTime;
    private long auxTime;
    private final Label fpsLabel = new Label();
    private final CalculateFps realFps = new CalculateFps(fpsLabel);
    private final Label emulatorFpsLabel = new Label();
    private final CalculateFps emulatorFps = new CalculateFps(emulatorFpsLabel);
    private WritableImage renderPalette1;
    private WritableImage renderPalette2;
    private final ImageView imagePalette1 = new ImageView();
    private final ImageView imagePalette2 = new ImageView();

    {
        Cartridge cartridge = new Cartridge("nestest.nes");
        nes.insert(cartridge);
        nes.reset();
    }

    @Override
    public void start(Stage stage) {
        setupInstructionList();
//        setupMemoryTable();
        setupButtons();
        setupImageRender();
        setupSpritePalette();
        setupLayout();
        setupGameLoop();
        refresh();

        Scene scene = new Scene(layoutParent);
        scene.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            switch (e.getCode()) {
                case SPACE -> this.emulationRun = !this.emulationRun;
                case R -> reset();
                case C -> clock();
                case F -> frame();
                case P -> {++selectedPalette; selectedPalette &= 0x7;}
            }
        });

        stage.setTitle("NES Debugger");
        stage.setScene(scene);
        stage.setResizable(false);
        stage.setWidth(1024);
        stage.setHeight(880);
        stage.show();
    }

    private void setupSpritePalette() {
        this.renderPalette1 = new WritableImage(
                nes.ppu.getPatternTable(0, selectedPalette).getWidth(),
                nes.ppu.getPatternTable(0, selectedPalette).getHeight()
        );
        this.renderPalette2 = new WritableImage(
                nes.ppu.getPatternTable(1, selectedPalette).getWidth(),
                nes.ppu.getPatternTable(1, selectedPalette).getHeight()
        );
    }

    private void setupGameLoop() {
        startTime = Instant.now().toEpochMilli();
        AnimationTimer frameRateMeter = new AnimationTimer() {
            @Override
            public void handle(long now) {
                auxTime = Instant.now().toEpochMilli();
                fElapsedTime = auxTime - startTime;
                startTime = auxTime;
                realFps.calculate(now);
                if (emulationRun) {
                    if (fResidualTime > 0.0f) {
                        fResidualTime -= fElapsedTime;
                    } else {
                        emulatorFps.calculate(now);
                        fResidualTime +=  (1000 * (1.0f / 60.0f)) - fElapsedTime;
                        do { nes.clock(); } while (!nes.ppu.frameComplete);
                        nes.ppu.frameComplete = false;
                    }
                }
                refresh();
            }
        };
        frameRateMeter.start();
    }

    private void setupLayout() {
        VBox vbox = new VBox();
        HBox hbox = new HBox();
        VBox imageAndFps = new VBox();
        HBox palettes = new HBox();
        palettes.getChildren().addAll(imagePalette1, new Separator(), imagePalette2);
        vbox.getChildren().addAll(flags, programCounter, accumulator, xRegister, yRegister, stack, listView, buttonBar);
        imageAndFps.getChildren().addAll(imageView, fpsLabel, emulatorFpsLabel, palettes);
        hbox.getChildren().addAll(imageAndFps, tableView, new Separator(), vbox);
        tableView.setVisible(false);
        layoutParent = hbox;
    }

    private void setupImageRender() {
        this.render = new WritableImage(nes.ppu.getScreen().getWidth(), nes.ppu.getScreen().getHeight());
    }

    private void reset() {
        nes.reset();
    }

    private void clock() {
        do {nes.clock();} while (!nes.cpu.complete());
        do {nes.clock();} while (nes.cpu.complete());
    }

    private void frame() {
        do {nes.clock();} while (!nes.ppu.frameComplete);
        do {nes.clock();} while (!nes.cpu.complete());
        nes.ppu.frameComplete = false;
    }

    private void setupButtons() {
        buttonBar.setPadding(new Insets(10));
        Button clockButton = new Button("Clock");
        Button resetButton = new Button("Reset");
        resetButton.setOnAction(event -> reset());
        clockButton.setOnAction(event -> clock());
        resetButton.addEventFilter(KeyEvent.KEY_PRESSED, Event::consume);
        clockButton.addEventFilter(KeyEvent.KEY_PRESSED, Event::consume);
        buttonBar.getButtons().addAll(clockButton, resetButton);
    }

    private void setupMemoryTable() {
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
        listView.setPrefHeight(700);
        listView.getSelectionModel().select(12);
        listView.addEventFilter(MouseEvent.ANY, Event::consume);
        listView.addEventFilter(KeyEvent.KEY_PRESSED, Event::consume);
    }

    private void refresh() {
        Debugger debugger = new Debugger(cpu);
        debugger.setLog(false);
//        tableView.setItems(getHexItemsList());
//        tableView.refresh();
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
        renderPalette(renderPalette1, nes.ppu.getPatternTable(0, selectedPalette), imagePalette1);
        renderPalette(renderPalette2, nes.ppu.getPatternTable(1, selectedPalette), imagePalette2);
    }

    private void renderPalette(WritableImage palette, Ppu.Sprite sprite, ImageView imageView) {
        for (int i = 0; i < palette.getWidth(); i++) {
            for (int j = 0; j < palette.getHeight(); j++) {
                palette.getPixelWriter().setColor(i, j, sprite.getPixelArray()[i][j]);
            }
        }
        imageView.setImage(scale(palette, 1));
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
