package me.fabriciorby.nes.ppu;

public class LoopyRegister {

    int loopyRegister = 0x00;

    enum Loopy {
        COARSE_X(0b11111, 0), // 5 bits
        COARSE_Y(0b11111, 5), // 5 bits
        NAMETABLE_X(0b1, 10),
        NAMETABLE_Y(0b1, 11),
        FINE_Y(0b111, 12), // 3 bits
        UNUSED(0b1, 15);

        final int mask;
        final int shift;

        Loopy(int mask, int shift) {
            this.mask = mask;
            this.shift = shift;
        }
    }

    public int get(Loopy loopy) {
        return (loopyRegister >> loopy.shift) & loopy.mask;
    }

    public void set(Loopy loopy, int value) {
        this.loopyRegister &= ~(loopy.mask << loopy.shift); //clear
        this.loopyRegister |= ((value & loopy.mask) << loopy.shift); //apply
    }

}
