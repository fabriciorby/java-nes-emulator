package me.fabriciorby.nes.cpu;

import me.fabriciorby.nes.Bus;
import me.fabriciorby.nes.Memory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HexFormat;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

class CpuTest {

    private final int CARTRIDGE_OFFSET = 0x8000;
    private Bus cpuBus;

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

    String multiply3by10 = "A2 0A 8E 00 00 A2 03 8E 01 00 AC 00 00 A9 00 18 6D 01 00 88 D0 FA 8D 02 00 EA EA EA";

    @BeforeEach
    void setup() {
        cpuBus = new Bus() {
            {
                this.cpuRam = new int[Memory.RAM_SIZE];
            }

            @Override
            public void cpuWrite(int address, int data) {
                if (address >= 0x0000 && address <= 0xFFFF) {
                    cpuRam[address] = data;
                }
            }

            @Override
            public int cpuRead(int address, boolean readOnly) {
                if (address >= 0x0000 && address <= 0xFFFF) {
                    return cpuRam[address];
                }
                return 0x00;
            }
        };
        cpuBus.cpuRam[0xFFFC] = 0x00;
        cpuBus.cpuRam[0xFFFD] = 0x80;
        cpuBus.cpu.reset();
    }

    @Test
    @DisplayName("Should multiply 3*10")
    void cpuCalculate() {

        byte[] program = getProgram(multiply3by10);
        for (int i = 0; i < program.length; i++) {
            cpuBus.cpuRam[CARTRIDGE_OFFSET + i] = Byte.toUnsignedInt(program[i]);
        }

        IntStream.range(0, 130).forEach( i -> cpuBus.cpu.clock());
        assertEquals(30, cpuBus.cpu.accumulator);

    }

    private byte[] getProgram(String program) {
        return HexFormat.ofDelimiter(" ").parseHex(program);
    }

}