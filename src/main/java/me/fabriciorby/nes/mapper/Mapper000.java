package me.fabriciorby.nes.mapper;

public class Mapper000 extends Mapper {
    public Mapper000(int PRGBanks, int CHRBanks) {
        super(PRGBanks, CHRBanks);
    }

    // some ugly workarounds, I was thinking if I should create an object with a "mapped" flag
    // or if I should just throw one exception instead if not able to do the mapping

    @Override
    public int cpuMapRead(int address) {
        if (address >= 0x8000 && address <= 0xFFFF) {
            return address & (PRGBanks > 1 ? 0x7FFF : 0x3FFF);
        }
        return Integer.MAX_VALUE; //ugly workaround
    }

    @Override
    public int cpuMapWrite(int address) {
        if (address >= 0x8000 && address <= 0xFFFF) {
            return address & (PRGBanks > 1 ? 0x7FFF : 0x3FFF);
        }
        return Integer.MAX_VALUE; //ugly workaround
    }

    @Override
    public int ppuMapRead(int address) {
        if (address >= 0x0000 && address <= 0x1FFF) {
            return address;
        } else {
            return Integer.MAX_VALUE; //ugly workaround
        }
    }

    @Override
    public int ppuMapWrite(int address) {
        if (address >= 0x0000 && address <= 0x1FFF && CHRBanks == 0) {
            return address;
        } else {
            return Integer.MAX_VALUE; //ugly workaround
        }
    }
}
