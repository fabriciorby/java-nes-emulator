package me.fabriciorby.nes.ppu;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LoopyRegisterTest {

    LoopyRegister loopyRegister = new LoopyRegister();

    @Test
    void set() {
        loopyRegister.set(LoopyRegister.Loopy.NAMETABLE_X, 1);
        loopyRegister.set(LoopyRegister.Loopy.NAMETABLE_Y, 1);
        loopyRegister.set(LoopyRegister.Loopy.COARSE_X, 0b00111);
        loopyRegister.set(LoopyRegister.Loopy.COARSE_Y, 0b10101);
        loopyRegister.set(LoopyRegister.Loopy.COARSE_Y, 0b10001);
        loopyRegister.set(LoopyRegister.Loopy.COARSE_X, 0b01100);

        assertEquals(0b0000111000101100, loopyRegister.loopyRegister);
        assertEquals(loopyRegister.get(LoopyRegister.Loopy.COARSE_Y), 0b10001);
        assertEquals(loopyRegister.get(LoopyRegister.Loopy.COARSE_Y), 0b10001);
        assertEquals(loopyRegister.get(LoopyRegister.Loopy.NAMETABLE_Y), 0b1);
        assertEquals(loopyRegister.get(LoopyRegister.Loopy.NAMETABLE_X), 0b1);
        printLoopy();

        loopyRegister.set(LoopyRegister.Loopy.FINE_Y, 0b111);
        loopyRegister.set(LoopyRegister.Loopy.UNUSED, 0b1);
        loopyRegister.set(LoopyRegister.Loopy.COARSE_Y, 0b11111);
        loopyRegister.set(LoopyRegister.Loopy.COARSE_X, 0b11111);

        assertEquals(0b1111111111111111, loopyRegister.loopyRegister);
        printLoopy();

        loopyRegister.set(LoopyRegister.Loopy.NAMETABLE_X, 0b0);
        loopyRegister.set(LoopyRegister.Loopy.NAMETABLE_Y, 0b0);

        assertEquals(0b1111001111111111, loopyRegister.loopyRegister);
        assertEquals(loopyRegister.get(LoopyRegister.Loopy.COARSE_Y), 0b11111);
        assertEquals(loopyRegister.get(LoopyRegister.Loopy.COARSE_Y), 0b11111);
        assertEquals(loopyRegister.get(LoopyRegister.Loopy.NAMETABLE_Y), 0b0);
        assertEquals(loopyRegister.get(LoopyRegister.Loopy.NAMETABLE_X), 0b0);
        assertEquals(loopyRegister.get(LoopyRegister.Loopy.FINE_Y), 0b111);
        assertEquals(loopyRegister.get(LoopyRegister.Loopy.UNUSED), 0b1);
        printLoopy();
    }

    private void printLoopy() {
        System.out.println(String.format("%16s", Integer.toBinaryString( loopyRegister.loopyRegister)).replaceAll(" ", "0"));
    }

}