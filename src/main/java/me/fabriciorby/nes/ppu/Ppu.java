package me.fabriciorby.nes.ppu;

import javafx.scene.paint.Color;
import me.fabriciorby.nes.cartridge.Cartridge;

import java.util.function.Consumer;

public class Ppu {

    private Cartridge cartridge;

    int[][] tablePattern    = new int[2][4096]; //useless, but it does exist
    public int[][] tableName       = new int[2][1024];
    int[]   tablePalette    = new int[32];

    // https://www.nesdev.org/wiki/PPU_palettes#Palettes
    Color[] paletteColors = Colors.getColors();

    ControlRegister controlRegister = new ControlRegister();
    MaskRegister maskRegister = new MaskRegister();
    StatusRegister statusRegister = new StatusRegister();

    int addressLatch = 0x00;
    int ppuDataBuffer = 0x00;

    LoopyRegister vRamAddress = new LoopyRegister();
    LoopyRegister tRamAddress = new LoopyRegister();
    int fineX;

    public boolean nonMaskableInterrupt;

    public void cpuWrite(int address, int data) {

        switch (address) {
            case 0x0000 -> { // Control
                controlRegister.controlRegister = data;
                tRamAddress.set(LoopyRegister.Loopy.NAMETABLE_X, controlRegister.get(ControlRegister.Control.NAMETABLE_X));
                tRamAddress.set(LoopyRegister.Loopy.NAMETABLE_Y, controlRegister.get(ControlRegister.Control.NAMETABLE_Y));
            }
            case 0x0001 -> maskRegister.maskRegister = data; // Mask
            case 0x0002 -> {} //statusRegister.statusRegister = data; // Status
            case 0x0003 -> {
            } // OAM Address
            case 0x0004 -> {
            } // OAM Data
            case 0x0005 -> { // Scroll
                if (addressLatch == 0) {
                    fineX = data & 0x07;
                    tRamAddress.set(LoopyRegister.Loopy.COARSE_X, data >> 3);
                    addressLatch = 1;
                } else {
                    tRamAddress.set(LoopyRegister.Loopy.FINE_Y, data & 0x07);
                    tRamAddress.set(LoopyRegister.Loopy.COARSE_Y, data >> 3);
                    addressLatch = 0;
                }
            }
            case 0x0006 -> { // PPU Address
                if (addressLatch == 0) {
                    tRamAddress.loopyRegister = ((data & 0x3F) << 8) | (tRamAddress.loopyRegister & 0x00FF);
                    addressLatch = 1;
                } else {
                    tRamAddress.loopyRegister = (tRamAddress.loopyRegister & 0xFF00) | data;
                    vRamAddress = tRamAddress;
                    addressLatch = 0;
                }
            }
            case 0x0007 -> { // PPU Data
                ppuWrite(vRamAddress.loopyRegister, data);
                vRamAddress.loopyRegister += (controlRegister.get(ControlRegister.Control.INCREMENT_MODE) != 0 ? 32 : 1);
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
                    int data = (statusRegister.statusRegister & 0xE0) | (ppuDataBuffer & 0x1F);
                    statusRegister.setVerticalBlank(0);
                    addressLatch = 0;
                    yield data;

                }
                case 0x0003 -> 0; // OAM Address
                case 0x0004 -> 0; // OAM Data
                case 0x0005 -> 0; // Scroll
                case 0x0006 -> 0; // PPU Address
                case 0x0007 -> { // PPU Data
                    int data = ppuDataBuffer;
                    ppuDataBuffer = ppuRead(vRamAddress.loopyRegister);
                    if (vRamAddress.loopyRegister >= 0x3F00) data = ppuDataBuffer;
                    vRamAddress.loopyRegister += (controlRegister.get(ControlRegister.Control.INCREMENT_MODE) != 0 ? 32 : 1);
                   yield data;
                }
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
            address &= 0x0FFF;
            switch (cartridge.mirror) {
                case VERTICAL -> {
                    if (address >= 0x0000 && address <= 0x03FF)
                        tableName[0][address & 0x03FF] = data;
                    if (address >= 0x0400 && address <= 0x07FF)
                        tableName[1][address & 0x03FF] = data;
                    if (address >= 0x0800 && address <= 0x0BFF)
                        tableName[0][address & 0x03FF] = data;
                    if (address >= 0x0C00 && address <= 0x0FFF)
                        tableName[1][address & 0x03FF] = data;
                }
                case HORIZONTAL -> {
                    if (address >= 0x0000 && address <= 0x03FF)
                        tableName[0][address & 0x03FF] = data;
                    if (address >= 0x0400 && address <= 0x07FF)
                        tableName[0][address & 0x03FF] = data;
                    if (address >= 0x0800 && address <= 0x0BFF)
                        tableName[1][address & 0x03FF] = data;
                    if (address >= 0x0C00 && address <= 0x0FFF)
                        tableName[1][address & 0x03FF] = data;
                }
                default -> {}
            }
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
            address &= 0x0FFF;
            switch (cartridge.mirror) {
                case VERTICAL -> {
                    if (address >= 0x0000 && address <= 0x03FF)
                        data = tableName[0][address & 0x03FF];
                    if (address >= 0x0400 && address <= 0x07FF)
                        data = tableName[1][address & 0x03FF];
                    if (address >= 0x0800 && address <= 0x0BFF)
                        data = tableName[0][address & 0x03FF];
                    if (address >= 0x0C00 && address <= 0x0FFF)
                        data = tableName[1][address & 0x03FF];
                }
                case HORIZONTAL -> {
                    if (address >= 0x0000 && address <= 0x03FF)
                        data = tableName[0][address & 0x03FF];
                    if (address >= 0x0400 && address <= 0x07FF)
                        data = tableName[0][address & 0x03FF];
                    if (address >= 0x0800 && address <= 0x0BFF)
                        data = tableName[1][address & 0x03FF];
                    if (address >= 0x0C00 && address <= 0x0FFF)
                        data = tableName[1][address & 0x03FF];
                }
                default -> {}
            }
        } else if (address >= 0x3F00 && address <= 0x3FFF) {
            address &= 0x001F;
            if (address == 0x0010) address = 0x0000;
            if (address == 0x0014) address = 0x0004;
            if (address == 0x0018) address = 0x0008;
            if (address == 0x001C) address = 0x000C;
            data = tablePalette[address] & (maskRegister.get(MaskRegister.Mask.GRAYSCALE) != 0 ? 0x30 : 0x3F);
        }
        return data;
    }

    public void connect(Cartridge cartridge) {
        this.cartridge = cartridge;
    }

    private int cycle;
    private int scanline;
    public boolean frameComplete;

    int bgNextTileId = 0x00;
    int bgNextTileAttrib = 0x00;
    int bgNextTileLsb = 0x00;
    int bgNextTileMsb = 0x00;

    int bgShifterPatternLo = 0x0000;
    int bgShifterPatternHi = 0x0000;
    int bgShifterAttribLo = 0x0000;
    int bgShifterAttribHi = 0x0000;

    public void clock() {
        //For more information about this shit go to
        // https://github.com/OneLoneCoder/olcNES/blob/master/Part%20%234%20-%20PPU%20Backgrounds/olc2C02.cpp

        Consumer<Ppu> incrementScrollX = ppu -> {
            if (maskRegister.get(MaskRegister.Mask.RENDER_BACKGROUND) != 0 || maskRegister.get(MaskRegister.Mask.RENDER_SPRITES) != 0) {
                if (vRamAddress.get(LoopyRegister.Loopy.COARSE_X) == 31) {
                    vRamAddress.set(LoopyRegister.Loopy.COARSE_X, 0b0);
                    vRamAddress.set(LoopyRegister.Loopy.NAMETABLE_X, ~vRamAddress.get(LoopyRegister.Loopy.NAMETABLE_X));
                } else {
                    vRamAddress.set(LoopyRegister.Loopy.COARSE_X, vRamAddress.get(LoopyRegister.Loopy.COARSE_X) + 1);
                }
            }
        };

        Consumer<Ppu> incrementScrollY = ppu -> {
            if (maskRegister.get(MaskRegister.Mask.RENDER_BACKGROUND) != 0 || maskRegister.get(MaskRegister.Mask.RENDER_SPRITES) != 0) {
                if (vRamAddress.get(LoopyRegister.Loopy.FINE_Y) < 7) {
                    vRamAddress.set(LoopyRegister.Loopy.FINE_Y, vRamAddress.get(LoopyRegister.Loopy.FINE_Y) + 1);
                } else {
                    vRamAddress.set(LoopyRegister.Loopy.FINE_Y, 0b0);
                    if (vRamAddress.get(LoopyRegister.Loopy.COARSE_Y) == 29) {
                        vRamAddress.set(LoopyRegister.Loopy.COARSE_Y, 0b0);
                        vRamAddress.set(LoopyRegister.Loopy.NAMETABLE_Y, ~vRamAddress.get(LoopyRegister.Loopy.NAMETABLE_Y));
                    } else if (vRamAddress.get(LoopyRegister.Loopy.COARSE_Y) == 31) {
                        vRamAddress.set(LoopyRegister.Loopy.COARSE_Y, 0b0);
                    } else {
                        vRamAddress.set(LoopyRegister.Loopy.COARSE_Y, vRamAddress.get(LoopyRegister.Loopy.COARSE_Y) + 1);
                    }
                }
            }
        };

        Consumer<Ppu> transferAddressX = ppu -> {
            if (maskRegister.get(MaskRegister.Mask.RENDER_BACKGROUND) != 0 || maskRegister.get(MaskRegister.Mask.RENDER_SPRITES) != 0) {
                vRamAddress.set(LoopyRegister.Loopy.NAMETABLE_X, tRamAddress.get(LoopyRegister.Loopy.NAMETABLE_X));
                vRamAddress.set(LoopyRegister.Loopy.COARSE_X, tRamAddress.get(LoopyRegister.Loopy.COARSE_X));
            }
        };

        Consumer<Ppu> transferAddressY = ppu -> {
            if (maskRegister.get(MaskRegister.Mask.RENDER_BACKGROUND) != 0 || maskRegister.get(MaskRegister.Mask.RENDER_SPRITES) != 0) {
                vRamAddress.set(LoopyRegister.Loopy.FINE_Y, tRamAddress.get(LoopyRegister.Loopy.FINE_Y));
                vRamAddress.set(LoopyRegister.Loopy.NAMETABLE_Y, tRamAddress.get(LoopyRegister.Loopy.NAMETABLE_Y));
                vRamAddress.set(LoopyRegister.Loopy.COARSE_Y, tRamAddress.get(LoopyRegister.Loopy.COARSE_Y));
            }
        };

        Consumer<Ppu> loadBackgroundShifters = ppu -> {
            bgShifterPatternLo = (bgShifterPatternLo & 0xFF00) | bgNextTileLsb;
            bgShifterPatternHi = (bgShifterPatternHi & 0xFF00) | bgNextTileMsb;
            bgShifterAttribLo = (bgShifterAttribLo & 0xFF00) | ((bgNextTileAttrib & 0b01) != 0 ? 0xFF : 0x00);
            bgShifterAttribHi = (bgShifterAttribHi & 0xFF00) | ((bgNextTileAttrib & 0b10) != 0 ? 0xFF : 0x00);
        };

        Consumer<Ppu> updateShifters = ppu -> {
            if (maskRegister.get(MaskRegister.Mask.RENDER_BACKGROUND) != 0) {
                bgShifterPatternLo <<= 1;
                bgShifterPatternHi <<= 1;
                bgShifterAttribLo <<= 1;
                bgShifterAttribHi <<= 1;
            }
        };


        if (scanline >= -1 && scanline < 240) {
            if (scanline == 0 && cycle == 0) {
                cycle = 1;
            }
            if (scanline == -1 && cycle == 1) {
                statusRegister.setVerticalBlank(0);
            }
            if ((cycle >= 2 && cycle < 258) || (cycle >= 321 && cycle < 338)) {
                updateShifters.accept(this);
                switch ((cycle - 1) % 8) {
                    case 0 -> {
                        loadBackgroundShifters.accept(this);
                        bgNextTileId = ppuRead(0x2000 | (vRamAddress.loopyRegister & 0x0FFF));
                    }
                    case 2 -> {
                        bgNextTileAttrib = ppuRead(0x23C0
                                | (vRamAddress.get(LoopyRegister.Loopy.NAMETABLE_Y) << 11)
                                | (vRamAddress.get(LoopyRegister.Loopy.NAMETABLE_X) << 10)
                                | ((vRamAddress.get(LoopyRegister.Loopy.COARSE_Y) >> 2) << 3)
                                | (vRamAddress.get(LoopyRegister.Loopy.COARSE_X) >> 2));
                        if ((vRamAddress.get(LoopyRegister.Loopy.COARSE_Y) & 0x02) != 0) bgNextTileAttrib >>= 4;
                        if ((vRamAddress.get(LoopyRegister.Loopy.COARSE_X) & 0x02) != 0) bgNextTileAttrib >>= 2;
                        bgNextTileAttrib &= 0x03;
                    }
                    case 4 -> bgNextTileLsb = ppuRead(
                            (controlRegister.get(ControlRegister.Control.PATTERN_BACKGROUND) << 12 )
                                    + (bgNextTileId << 4)
                                    + vRamAddress.get(LoopyRegister.Loopy.FINE_Y));

                    case 6 -> bgNextTileMsb = ppuRead(
                            (controlRegister.get(ControlRegister.Control.PATTERN_BACKGROUND) << 12)
                                    + (bgNextTileId << 4)
                                    + (vRamAddress.get(LoopyRegister.Loopy.FINE_Y) + 8));

                    case 7 -> incrementScrollX.accept(this);
                }
            }
            if (cycle == 256) {
                incrementScrollY.accept(this);
            }
            if (cycle == 257) {
                loadBackgroundShifters.accept(this);
                transferAddressX.accept(this);
            }
            if (cycle == 338 || cycle == 340) {
                bgNextTileId = ppuRead(0x2000 | (vRamAddress.loopyRegister & 0x0FFF));
            }
            if (scanline == -1  && cycle >= 280 && cycle < 305) {
                transferAddressY.accept(this);
            }

        }

        if (scanline == 240) {

        }

        if (scanline >= 241 && scanline < 261) {
            if (scanline == 241 && cycle == 1) {
                statusRegister.setVerticalBlank(1);
                if (controlRegister.get(ControlRegister.Control.ENABLE_NMI) != 0) {
                    nonMaskableInterrupt = true;
                }
            }
        }

        int bgPixel = 0x00;
        int bgPalette = 0x00;

        if (maskRegister.get(MaskRegister.Mask.RENDER_BACKGROUND) != 0) {
            int bitMux = (0x8000 >> fineX);

            int p0Pixel = (bgShifterPatternLo & bitMux) > 0 ? 1 : 0;
            int p1Pixel = (bgShifterPatternHi & bitMux) > 0 ? 1 : 0;
            bgPixel = (p1Pixel << 1) | p0Pixel;

            int p0Pal = (bgShifterAttribLo & bitMux) > 0 ? 1 : 0;
            int p1Pal = (bgShifterAttribHi & bitMux) > 0 ? 1 : 0;
            bgPalette = (p1Pal << 1) | p0Pal;
        }

        spriteScreen.setPixel(cycle - 1, scanline, getColourFromPaletteRam(bgPalette, bgPixel));

//        spriteScreen.setPixel(cycle - 1, scanline, paletteColors[(Math.random() > 0.5) ? 0x3F : 0x30]);

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

    Sprite spriteScreen = new Sprite(256, 240);
    Sprite[] spriteNameTable = { new Sprite(256, 240), new Sprite(256, 240) };
    Sprite[] spritePatternTable = { new Sprite(128, 128), new Sprite(128, 128) };

    public Sprite getScreen() {
        return spriteScreen;
    }

    public Sprite getPatternTable(int index, int palette) {
        for (int nTileY = 0; nTileY < 16; nTileY++) {
            for (int nTileX = 0; nTileX < 16; nTileX++) {
                int nOffset = nTileY * 256 + nTileX * 16;
                for (int row = 0; row < 8; row++) {
                    int tileLow = ppuRead(index * 0x1000 + nOffset + row);
                    int tileHigh = ppuRead(index * 0x1000 + nOffset + row + 0x0008);
                    for (int col = 0; col < 8; col++) {
                        int pixel = (tileLow & 0x01) + (tileHigh & 0x01);
                        tileLow >>= 1; tileHigh >>= 1;
                        spritePatternTable[index].setPixel(
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
