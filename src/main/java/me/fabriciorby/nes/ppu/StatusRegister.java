package me.fabriciorby.nes.ppu;

import static me.fabriciorby.nes.ppu.StatusRegister.Status.*;

public class StatusRegister {

    int statusRegister;

    enum Status {
        UNUSED(0), // 5 bits
        SPRITE_OVERFLOW(1 << 5),
        SPRITE_ZERO_HIT(1 << 6),
        VERTICAL_BLANK(1 << 7);

        final int bit;

        Status(int bit) {
            this.bit = bit;
        }
    }

    public StatusRegister(int statusRegister) {
        this.statusRegister = statusRegister;
    }

    public int get(Status status) {
        return (statusRegister & status.bit) > 0 ? 1: 0;
    }

    public void set(Status status, int value) {
        if (value == 0) {
            this.statusRegister &= ~status.bit;
        } else {
            this.statusRegister |= status.bit;
        }
    }

    public int getSpriteOverflow() {
        return get(SPRITE_OVERFLOW);
    }

    public int getSpriteZeroHit() {
        return get(SPRITE_ZERO_HIT);
    }

    public int getVerticalBlank() {
        return get(VERTICAL_BLANK);
    }

    public void getSpriteOverflow(int value) {
        set(SPRITE_OVERFLOW, value);
    }

    public void getSpriteZeroHit(int value) {
        set(SPRITE_ZERO_HIT, value);
    }

    public void getVerticalBlank(int value) {
        set(VERTICAL_BLANK, value);
    }
}
