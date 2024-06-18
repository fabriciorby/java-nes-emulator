package me.fabriciorby.nes.cpu;

import me.fabriciorby.nes.Bus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HexFormat;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

class CpuTest {

    private final int RAM_SIZE = 0xFFFF + 1;
    private final int CARTRIDGE_OFFSET = 0x8000;
    private Bus cpuBus;

    // Load Program (assembled at https://www.masswerk.at/6502/assembler.html)
    // For more information about the OP Codes go to https://www.nesdev.org/6502.txt
    /*
        *=$8000
        LDX #10     ;Store 10 in X Register
        STX $0000   ;Store whatever is in X into $0000
        LDX #3      ;Store 3 in X Register
        STX $0001   ;Store whatever is in X into $0001
        LDY $0000   ;Load whatever is in $0000 into Y Register (it knows because uses 4 bits input)
        LDA #0      ;Store 0 in Accumulator
        CLC         ;Clear Carry just in case
        loop        ;Start the loop by counting how many bytes of memory it takes to get to the end of the loop
        ADC $0001   ;Add whatever is in $0001 to the Accumulator
        DEY         ;Decrease Y by 1 (Y--)
        BNE loop    ;Branch not zero, thus loops a number of instructions back until 'loop' while flag zero is true
        STA $0002   ;Stores whatever is in Accumulator into $0002
        NOP
        NOP
        NOP
    */

    String multiply3by10 = "A2 0A 8E 00 00 A2 03 8E 01 00 AC 00 00 A9 00 18 6D 01 00 88 D0 FA 8D 02 00 EA EA EA";

    @BeforeEach
    void setup() {
        cpuBus = new Bus() {
            {
                this.cpuRam = new int[RAM_SIZE];
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
        /*
        * As per the documentation for the 6502, when the CPU is reset, it fetches the 16-bit address from memory
        * locations 0xFFFC and 0xFFFD and sets the program counter to that address. Since the 6502 operates in little
        * endian format, the low byte is stored first, then the high byte. That means that if your program code starts
        * at 0xC000, you need values 0x00 and 0xC0 in bytes 0xFFFC and 0xFFFD respectively.
        * */
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