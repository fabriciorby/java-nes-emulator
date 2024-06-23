package me.fabriciorby.nes.ppu;

import javafx.scene.paint.Color;
import me.fabriciorby.nes.cartridge.Cartridge;

import java.util.Arrays;

public class Ppu {

    private Cartridge cartridge;

    int[][] tablePattern    = new int[2][4096]; //useless, but it does exist
    int[][] tableName       = new int[2][1024];
    int[]   tablePalette    = new int[32];

    // https://www.nesdev.org/wiki/PPU_palettes#Palettes
    Color[] paletteColors = Colors.getColors();

    ControlRegister controlRegister = new ControlRegister();
    MaskRegister maskRegister = new MaskRegister();
    StatusRegister statusRegister = new StatusRegister();

    int addressLatch = 0x00;
    int ppuDataBuffer = 0x00;
    int ppuAddress = 0x0000;

    public void cpuWrite(int address, int data) {

        switch (address) {
            case 0x0000 -> controlRegister.controlRegister = data; // Control
            case 0x0001 -> maskRegister.maskRegister = data; // Mask
            case 0x0002 -> {} //statusRegister.statusRegister = data; // Status
            case 0x0003 -> {
            } // OAM Address
            case 0x0004 -> {
            } // OAM Data
            case 0x0005 -> {
            } // Scroll
            case 0x0006 -> { // PPU Address
                if (addressLatch == 0) {
                    ppuAddress = (ppuAddress & 0x00FF) | (data << 8);
                    addressLatch = 1;
                } else {
                    ppuAddress = (ppuAddress & 0xFF00) | data;
                    addressLatch = 0;
                }
            }
            case 0x0007 -> { // PPU Data
                ppuWrite(ppuAddress, data);
                ppuAddress++;
            }
            default -> {
            }
        };

    }

    public int cpuRead(int address, boolean readOnly) {
         readOnly = false;
        if (readOnly) {

            return switch (address) {
                case 0x0000 -> controlRegister.controlRegister; // Control
                case 0x0001 -> maskRegister.maskRegister; // Mask
                case 0x0002 -> statusRegister.statusRegister; // Status
                case 0x0003 -> 0; // OAM Address
                case 0x0004 -> 0; // OAM Data
                case 0x0005 -> 0; // Scroll
                case 0x0006 -> 0; // PPU Address
                case 0x0007 -> 0; // PPU Data
                default -> 0;
            };

        } else {

            return switch (address) {
                case 0x0000 -> 0; // Control
                case 0x0001 -> 0; // Mask
                case 0x0002 -> { // Status
                    statusRegister.setVerticalBlank(1);
                    int data = (statusRegister.statusRegister & 0xE0) | (ppuDataBuffer & 0x1F);
//                    statusRegister.setVerticalBlank(0);
                    addressLatch = 0;
                    yield data;

                }
                case 0x0003 -> 0; // OAM Address
                case 0x0004 -> 0; // OAM Data
                case 0x0005 -> 0; // Scroll
                case 0x0006 -> 0; // PPU Address
                case 0x0007 -> {
                    int data = ppuDataBuffer;
                    ppuDataBuffer = ppuRead(ppuAddress);
                    if (ppuAddress >= 0x3F00) data = ppuDataBuffer;
                   ppuAddress++;
                   yield data;
                } // PPU Data
                default -> 0;
            };

        }


    }

    public void ppuWrite(int address, int data) {

        address &= 0x3FFF;

        if (cartridge.ppuCanWrite(address)) {
            cartridge.ppuWrite(address, data);
        } else if (address >= 0x0000 && address <= 0x1FFF) {
            tablePattern[(address & 0x1000) >> 12][address & 0x0FFF] = data;
        } else if (address >= 0x2000 && address <= 0x3EFF) {

        } else if (address >= 0x3F00 && address <= 0x3FFF) {
            address &= 0x001F;
            if (address == 0x0010) address = 0x0000;
            if (address == 0x0014) address = 0x0004;
            if (address == 0x0018) address = 0x0008;
            if (address == 0x001C) address = 0x000C;
            tablePalette[address] = data;
        }

    }

    public int ppuRead(int address) {
        int data = 0x00;
        address &= 0x3FFF;

        if (cartridge.ppuCanRead(address)) {
            data = cartridge.ppuRead(address);
        } else if (address >= 0x0000 && address <= 0x1FFF) {
            data = tablePattern[(address & 0x1000) >> 12][address & 0x0FFF];
        } else if (address >= 0x2000 && address <= 0x3EFF) {

        } else if (address >= 0x3F00 && address <= 0x3FFF) {
            address &= 0x001F;
            if (address == 0x0010) address = 0x0000;
            if (address == 0x0014) address = 0x0004;
            if (address == 0x0018) address = 0x0008;
            if (address == 0x001C) address = 0x000C;
            data = tablePalette[address];
        }

        return data;
    }

    public void connect(Cartridge cartridge) {
        this.cartridge = cartridge;
    }

    private int cycle;
    private int scanline;
    public boolean frameComplete;

    public void clock() {

        spriteScreen.setPixel(cycle - 1, scanline, paletteColors[(Math.random() > 0.5) ? 0x3F : 0x30]);

        cycle++;
        if (cycle >= 341)
        {
            cycle = 0;
            scanline++;
            if (scanline >= 261)
            {
                scanline = -1;
                frameComplete = true;
            }
        }
    }


    public static class Sprite {
        Color[][] pixelArray;
        int width;
        int height;

        public Sprite(int width, int height) {
            this.pixelArray = new Color[width][height];
            for (Color[] row: pixelArray)
                Arrays.fill(row, Color.BLACK);
            this.width = width;
            this.height = height;
        }

        public void setPixel(int x, int y, Color pixel) {
            if (x >= width || y >= height || x < 0 || y < 0) return;
            this.pixelArray[x][y] = pixel;
        }

        public Color[][] getPixelArray() {
            return pixelArray;
        }

        public int getWidth() {
            return this.width;
        }

        public int getHeight() {
            return this.height;
        }

    }

    Sprite spriteScreen = new Sprite(256, 240);
    Sprite[] spriteNameTable = { new Sprite(256, 240), new Sprite(256, 240) };
    Sprite[] spritePatternTable = { new Sprite(128, 128), new Sprite(128, 128) };

    public Sprite getScreen() {
        return spriteScreen;
    }

    public Sprite getPatternTable(int index, int palette) {
        for (int nTileY = 0; nTileY < 16; nTileY++)
        {
            for (int nTileX = 0; nTileX < 16; nTileX++)
            {
                int nOffset = nTileY * 256 + nTileX * 16;
                for (int row = 0; row < 8; row++)
                {
                    int tileLow = ppuRead(index * 0x1000 + nOffset + row);
                    int tileHigh = ppuRead(index * 0x1000 + nOffset + row + 0x0008);

                    for (int col = 0; col < 8; col++)
                    {
                        int pixel = (tileLow & 0x01) + (tileHigh & 0x01);
                        tileLow >>= 1; tileHigh >>= 1;

                        spritePatternTable[index].setPixel
                                (
                                        nTileX * 8 + (7 - col),
                                        nTileY * 8 + row,
                                        getColourFromPaletteRam(palette, pixel)
                                );
                    }
                }
            }
        }

        return spritePatternTable[index];
    }

    public Color getColourFromPaletteRam(int palette, int pixel) {
        // This is a convenience function that takes a specified palette and pixel
        // index and returns the appropriate screen colour.
        // "0x3F00"       - Offset into PPU addressable range where palettes are stored
        // "palette << 2" - Each palette is 4 bytes in size
        // "pixel"        - Each pixel index is either 0, 1, 2 or 3
        // "& 0x3F"       - Stops us reading beyond the bounds of the palScreen array
        return paletteColors[ppuRead(0x3F00 + (palette << 2) + pixel) & 0x3F];
    }

    public Sprite getNameTable(int index) {
        return spriteNameTable[index];
    }

}
