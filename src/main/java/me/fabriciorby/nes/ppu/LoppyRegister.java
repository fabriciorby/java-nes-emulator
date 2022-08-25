package me.fabriciorby.nes.ppu;

public class LoppyRegister {

    enum Loppy { //TODO see how to get 5 bits :/
        COARSE_X(5),
        COARSE_Y(5),
        NAMETABLE_X(1 << 10),
        NAMETABLE_Y(1 << 11),
        FINE_Y(3),
        UNUSED(1 << 15);

        final int bit;

        Loppy(int bit) {
            this.bit = bit;
        }
    }

}
