package me.fabriciorby.nes.ppu;

import me.fabriciorby.nes.cartridge.Cartridge;

import java.awt.*;

public class Ppu {

    private Cartridge cartridge;

    int[][] tablePattern    = new int[2][4096]; //useless, but it does exist
    int[][] tableName       = new int[2][1024];
    int[]   tablePalette    = new int[32];

    // https://www.nesdev.org/wiki/PPU_palettes#Palettes
    Color[] paletteColors = Colors.getColors();

    public void cpuWrite(int address, int data) {

        int result = switch (address) {
            case 0x0000 -> 0; // Control
            case 0x0001 -> 0; // Mask
            case 0x0002 -> 0; // Status
            case 0x0003 -> 0; // OAM Address
            case 0x0004 -> 0; // OAM Data
            case 0x0005 -> 0; // Scroll
            case 0x0006 -> 0; // PPU Address
            case 0x0007 -> 0; // PPU Data
            default -> 0;
        };

    }

    public int cpuRead(int address, boolean readOnly) {

        if (readOnly) {

            return switch (address) {
                case 0x0000 -> 0; // Control
                case 0x0001 -> 0; // Mask
                case 0x0002 -> 0; // Status
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
                case 0x0002 -> 0; // Status
                case 0x0003 -> 0; // OAM Address
                case 0x0004 -> 0; // OAM Data
                case 0x0005 -> 0; // Scroll
                case 0x0006 -> 0; // PPU Address
                case 0x0007 -> 0; // PPU Data
                default -> 0;
            };

        }


    }

    public void ppuWrite(int address, int data) {
        address &= 0x3FFF;

        cartridge.ppuWrite(address, data);

    }

    public int ppuRead(int address) {
        address &= 0x3FFF;

        cartridge.ppuRead(address);

        return 0;
    }

    public void connect(Cartridge cartridge) {
        this.cartridge = cartridge;
    }

    private int cycle;
    private int scanline;
    private boolean frameComplete;

    public void clock() {
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


    static class Sprite {
        Color[][] pixelArray;
        public Sprite(int width, int height) {
            this.pixelArray = new Color[width][height];
        }

        public void setPixel(int x, int y, Color pixel) {
            this.pixelArray[x][y] = pixel;
        }


    }

    Sprite spriteScreen = new Sprite(256, 240);
    Sprite[] spriteNameTable = { new Sprite(256, 240), new Sprite(256, 240) };
    Sprite[] spritePatternTable = { new Sprite(256, 240), new Sprite(256, 240) };

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
