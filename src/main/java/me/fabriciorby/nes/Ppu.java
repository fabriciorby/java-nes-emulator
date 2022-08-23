package me.fabriciorby.nes;

import me.fabriciorby.nes.cartridge.Cartridge;

public class Ppu {

    private Cartridge cartridge;

    int[][] tablePattern = new int[2][4096]; //exists but it is useless
    int[][] tableName = new int[2][1024];
    int[] tablePalette = new int[32];

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

    private int cycle;
    private int scanline;
    private boolean frameComplete;

}
