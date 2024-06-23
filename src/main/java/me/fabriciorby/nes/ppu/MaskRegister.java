package me.fabriciorby.nes.ppu;

public class MaskRegister {

    int maskRegister;

    public enum Mask {
        GRAYSCALE(1),
        RENDER_BACKGROUND_LEFT(1 << 1),
        RENDER_SPRITES_LEFT(1 << 2),
        RENDER_BACKGROUND(1 << 3),
        RENDER_SPRITES(1 << 4),
        ENHANCE_RED(1 << 5),
        ENHANCE_GREEN(1 << 6),
        ENHANCE_BLUE(1 << 7);

        final int bit;

        Mask(int bit) {
            this.bit = bit;
        }
    }

    public int get(Mask mask) {
        return (maskRegister & mask.bit) > 0 ? 1: 0;
    }

    public void set(Mask mask, int value) {
        if (value == 0) {
            this.maskRegister &= ~mask.bit;
        } else {
            this.maskRegister |= mask.bit;
        }
    }

}
