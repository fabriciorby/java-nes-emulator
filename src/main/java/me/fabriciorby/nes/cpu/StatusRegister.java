package me.fabriciorby.nes.cpu;

public enum StatusRegister {
    CARRY(1 << 0, 'C'),
    ZERO(1 << 1, 'Z'),
    DISABLE_INTERRUPTS(1 << 2, 'I'),
    DECIMAL(1 << 3, 'D'),
    BREAK(1 << 4, 'B'),
    UNUSED(1 << 5, 'U'),
    OVERFLOW(1 << 6, 'V'),
    NEGATIVE(1 << 7, 'N');

    public final int bit;
    public final char code;

    StatusRegister(int bit, char code) {
        this.bit = bit;
        this.code = code;
    }
}
