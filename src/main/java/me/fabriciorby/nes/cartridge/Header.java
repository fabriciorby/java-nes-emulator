package me.fabriciorby.nes.cartridge;

public class Header {
    public Header(byte[] header) {
        this.name = new String(header, 0, 4);
        this.PRGRomChunks = Byte.toUnsignedInt(header[5]);
        this.CHRRomChunks = Byte.toUnsignedInt(header[6]);
        this.mapper1 = Byte.toUnsignedInt(header[7]);
        this.mapper2 = Byte.toUnsignedInt(header[8]);
        this.PRGRamSize = Byte.toUnsignedInt(header[9]);
        this.tvSystem1 = Byte.toUnsignedInt(header[10]);
        this.tvSystem2 = Byte.toUnsignedInt(header[11]);
        this.unused = new String(header, 11, 5); //to confirm
    }
    String name;
    int PRGRomChunks;
    int CHRRomChunks;
    int mapper1;
    int mapper2;
    int PRGRamSize;
    int tvSystem1;
    int tvSystem2;
    String unused;

    @Override
    public String toString() {
        return  """
                name: %s
                PRGRomChunks: %02X
                CHRRomChunks: %02X
                mapper1: %02X
                mapper2: %02X
                PRGRamSize: %02X
                tvSystem1: %02X
                tvSystem2: %02X
                unused: %s
                """.formatted(this.name, this.PRGRomChunks, this.CHRRomChunks, this.mapper1, this.mapper2,
                this.PRGRamSize, this.tvSystem1, this.tvSystem2, this.unused);
    }
}
