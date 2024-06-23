package me.fabriciorby.nes.ppu;

public class ControlRegister {

    int controlRegister;

    public enum Control {
        NAMETABLE_X(1),
        NAMETABLE_Y(1 << 1),
        INCREMENT_MODE(1 << 2),
        PATTERN_SPRITE(1 << 3),
        PATTERN_BACKGROUND(1 << 4),
        SPRITE_SIZE(1 << 5),
        SLAVE_MODE(1 << 6),
        ENABLE_NMI(1 << 7);

        final int bit;

        Control(int bit) {
            this.bit = bit;
        }
    }

    public int get(Control control) {
        return (controlRegister & control.bit) > 0 ? 1: 0;
    }

    public void set(Control control, int value) {
        if (value == 0) {
            this.controlRegister &= ~control.bit;
        } else {
            this.controlRegister |= control.bit;
        }
    }

}
