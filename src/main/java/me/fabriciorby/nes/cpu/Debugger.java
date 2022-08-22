package me.fabriciorby.nes.cpu;

public class Debugger {

    int programCounter;
    long clockCount;
    Cpu cpu;

    Debugger(Cpu cpu) {
        this.programCounter = cpu.programCounter;
        this.clockCount = cpu.clockCount;
        this.cpu = cpu;
    }

    void log(Instruction instruction) {
        String debug = """
                Operation: %s
                Address: %02X
                Accumulator: %02X
                X Register: %02X
                Y Register: %02X
                StackPointer: %02X
                Flags: %s%s%s%s%s%s%s%s
                Clock count: %d
                """.formatted(
                instruction.getName(),
                this.programCounter, cpu.accumulator, cpu.xRegister, cpu.yRegister, cpu.stackPointer,
                checkFlag(StatusRegister.NEGATIVE), checkFlag(StatusRegister.OVERFLOW), checkFlag(StatusRegister.UNUSED),
                checkFlag(StatusRegister.BREAK), checkFlag(StatusRegister.DECIMAL), checkFlag(StatusRegister.DISABLE_INTERRUPTS),
                checkFlag(StatusRegister.ZERO), checkFlag(StatusRegister.CARRY), clockCount);
        System.out.println(debug);
    }

    private char checkFlag(StatusRegister statusRegister) {
        return cpu.getFlag(statusRegister) == 1 ? statusRegister.code : '.';
    }
}
