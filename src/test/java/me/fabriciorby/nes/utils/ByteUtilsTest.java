package me.fabriciorby.nes.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ByteUtilsTest {

    @Test
    void flipByte() {

        int a = 0b1010_0000;
        int flipped = 0b0000_0101;

        assertEquals(flipped, ByteUtils.flipByte(a));
    }

}