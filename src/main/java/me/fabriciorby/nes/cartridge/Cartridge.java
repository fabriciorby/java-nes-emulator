package me.fabriciorby.nes.cartridge;

import me.fabriciorby.nes.mapper.Mapper;
import me.fabriciorby.nes.mapper.Mapper000;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Cartridge {

    private final byte[] PRG;
    private final byte[] CHR;

    int mapperId;
    int PRGBanks;
    int CHRBanks;

    Header header;
    Mapper mapper;

    public Cartridge(String filename) {
        try (InputStream inputStream = Files.newInputStream(Paths.get(filename))) {
            this.header = new Header(inputStream.readNBytes(16));
            if ((header.mapper1 & 0x04) != 0) {
                inputStream.skipNBytes(512);
            }
            mapperId = ((header.mapper2 >> 4) << 4) | (header.mapper1 >> 4);
            int fileType = 1;
            PRGBanks = header.PRGRomChunks;
            PRG = inputStream.readNBytes(PRGBanks * 16384);
            CHRBanks = header.CHRRomChunks;
            if (CHRBanks == 0) {
                CHR = inputStream.readNBytes(8192);
            } else {
                CHR = inputStream.readNBytes(CHRBanks * 8192);
            }
            this.mapper = switch (mapperId) {
                case 0 -> new Mapper000(PRGBanks, CHRBanks);
                default -> throw new IllegalStateException("Unexpected value: " + mapperId);
            };
        } catch (IOException e) {
            throw new IllegalArgumentException("Invalid file!");
        }
    }

    public boolean cpuCanWrite(int address) {
        return mapper.cpuMapWrite(address) != -1;
    }

    public boolean cpuCanRead(int address) {
        return mapper.cpuMapRead(address) != -1;
    }

    public void cpuWrite(int address, int data) {
        int mappedAddress = mapper.cpuMapWrite(address);
        PRG[mappedAddress] = (byte) data;
    }

    public int cpuRead(int address) {
        int mappedAddress = mapper.cpuMapRead(address);
        return Byte.toUnsignedInt(PRG[mappedAddress]);
    }

    public boolean ppuCanWrite(int address) {
        return mapper.ppuMapWrite(address) != -1;
    }

    public boolean ppuCanRead(int address) {
        return mapper.ppuMapRead(address) != -1;
    }

    public void ppuWrite(int address, int data) {
        int mappedAddress = mapper.ppuMapWrite(address);
        CHR[mappedAddress] = (byte) data;
    }

    public int ppuRead(int address) {
        int mappedAddress = mapper.ppuMapRead(address);
        return Byte.toUnsignedInt(CHR[mappedAddress]);
    }

}
