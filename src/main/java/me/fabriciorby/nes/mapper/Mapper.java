package me.fabriciorby.nes.mapper;

public abstract class Mapper {

    int PRGBanks;
    int CHRBanks;

    public Mapper(int PRGBanks, int CHRBanks) {
        this.PRGBanks = PRGBanks;
        this.CHRBanks = CHRBanks;
    }

    public abstract int cpuMapRead(int address);
    public abstract int cpuMapWrite(int address);
    public abstract int ppuMapRead(int address);
    public abstract int ppuMapWrite(int address);

}
