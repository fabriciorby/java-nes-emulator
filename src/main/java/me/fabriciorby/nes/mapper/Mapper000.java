package me.fabriciorby.nes.mapper;

public class Mapper000 extends Mapper {
    public Mapper000(int PRGBanks, int CHRBanks) {
        super(PRGBanks, CHRBanks);
    }

    @Override
    public int cpuMapRead(int address) {
        if (address >= 0x8000 && address <= 0xFFFF) {
            return address & (PRGBanks > 1 ? 0x7FFF : 0x3FFF);
        }
        return address;
    }

    @Override
    public int cpuMapWrite(int address) {
        if (address >= 0x8000 && address <= 0xFFFF) {
            return address & (PRGBanks > 1 ? 0x7FFF : 0x3FFF);
        }
        return address;
    }

    @Override
    public int ppuMapRead(int address) {
        return address;
    }

    @Override
    public int ppuMapWrite(int address) {
        return address;
    }
}
