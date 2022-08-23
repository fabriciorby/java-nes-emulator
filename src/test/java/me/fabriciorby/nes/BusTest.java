package me.fabriciorby.nes;

import me.fabriciorby.nes.cartridge.Cartridge;
import org.junit.jupiter.api.Test;

import java.util.stream.IntStream;

class BusTest {

    @Test
    void nesTest() {

        Bus nes = new Bus();
        Cartridge cartridge = new Cartridge("nestest.nes");
        nes.insert(cartridge);
        nes.reset();

        IntStream.range(0, 1000).forEach(i -> nes.clock());

    }

}