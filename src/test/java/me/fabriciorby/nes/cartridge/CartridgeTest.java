package me.fabriciorby.nes.cartridge;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CartridgeTest {

    @Test
    void cartridgeTest() {
        Cartridge cartridge = new Cartridge("nestest.nes");
        assertEquals("NES\u001A", cartridge.header.name);
        assertEquals(0, cartridge.mapperId);
        System.out.println(cartridge.header);
    }

}