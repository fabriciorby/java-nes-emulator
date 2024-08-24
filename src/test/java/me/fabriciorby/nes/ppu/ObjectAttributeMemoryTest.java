package me.fabriciorby.nes.ppu;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ObjectAttributeMemoryTest {

    @Test
    void get() {
        var OAM = new ObjectAttributeMemory();
        OAM.setAddress(0);
        assertEquals(0, OAM.getData());

        OAM.memoryEntries[1].id = 1;
        OAM.setAddress(4*1 + 1);
        assertEquals(1, OAM.getData());

        OAM.memoryEntries[2].attribute = 2;
        OAM.setAddress(4*2 + 2);
        assertEquals(2, OAM.getData());

        OAM.memoryEntries[2].x = 3;
        OAM.setAddress(4*2 + 3);
        assertEquals(3, OAM.getData());

        OAM.memoryEntries[63].x = 5;
        OAM.setAddress(255);
        assertEquals(5, OAM.getData());
    }
}