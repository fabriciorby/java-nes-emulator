import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HexFormat;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

class CpuTest {

    // Load Program (assembled at https://www.masswerk.at/6502/assembler.html)
    /*
        *=$8000
        LDX #10
        STX $0000
        LDX #3
        STX $0001
        LDY $0000
        LDA #0
        CLC
        loop
        ADC $0001
        DEY
        BNE loop
        STA $0002
        NOP
        NOP
        NOP
    */

    String multiply3for10 = "A2 0A 8E 00 00 A2 03 8E 01 00 AC 00 00 A9 00 18 6D 01 00 88 D0 FA 8D 02 00 EA EA EA";

    private byte[] getProgram(String program) {
        return HexFormat.ofDelimiter(" ").parseHex(program);
    }

    final int CARTRIDGE_OFFSET = 0x8000;

    Bus nes = new Bus();

    @BeforeEach
    void setup() {
        nes.fakeRam[0xFFFC] = 0x00;
        nes.fakeRam[0xFFFD] = 0x80;
        nes.cpu.reset();
    }

    @Test
    @DisplayName("Should multiply 3*10")
    void cpuCalculate() {

        byte[] program = getProgram(multiply3for10);
        for (int i = 0; i < program.length; i++) {
            nes.fakeRam[CARTRIDGE_OFFSET + i] = Byte.toUnsignedInt(program[i]);
        }

        IntStream.range(0, 130).forEach( i -> nes.cpu.clock());
        assertEquals(nes.cpu.accumulator, 30);

    }

}