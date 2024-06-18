package me.fabriciorby.nes.cpu;

public class Debugger {

    int programCounter;
    long clockCount;
    Instruction instruction;
    Cpu cpu;

    public Debugger(Cpu cpu) {
        this.programCounter = cpu.programCounter;
        this.clockCount = cpu.clockCount;
        this.cpu = cpu;
        this.instruction = cpu.lookupInstructions[cpu.read(programCounter)];
    }

    public void log() {
        String debug = """
                $%02X: %s %s
                A: $%02X [%s]
                X: $%02X [%s]
                Y: $%02X [%s]
                StackPointer: $%02X
                Flags: %s%s%s%s%s%s%s%s
                Clock count: %d
                """.formatted(
                programCounter, instruction.getName(), getInstructionInfo(instruction),
                cpu.accumulator, cpu.accumulator, cpu.xRegister, cpu.xRegister,
                cpu.yRegister, cpu.yRegister, cpu.stackPointer,
                checkFlag(StatusRegister.NEGATIVE), checkFlag(StatusRegister.OVERFLOW), checkFlag(StatusRegister.UNUSED),
                checkFlag(StatusRegister.BREAK), checkFlag(StatusRegister.DECIMAL), checkFlag(StatusRegister.DISABLE_INTERRUPTS),
                checkFlag(StatusRegister.ZERO), checkFlag(StatusRegister.CARRY), clockCount);
        System.out.println(debug);
    }

    public String getCurrentInstruction() {
        return "$%02X: %s %s".formatted(programCounter, instruction.getName(), getInstructionInfo(instruction));
    }

    public String getInstruction(int address) {
        var instruction = cpu.lookupInstructions[cpu.read(address)];
        return "$%02X: %s %s".formatted(address, instruction.getName(), getInstructionInfo(instruction));
    }

    public String getAccumulator() {
        return "A: $%02X [%s]".formatted(cpu.accumulator, cpu.accumulator);
    }

    public String getXRegister() {
        return "X: $%02X [%s]".formatted(cpu.xRegister, cpu.xRegister);
    }

    public String getYRegister() {
        return "Y: $%02X [%s]".formatted(cpu.yRegister, cpu.yRegister);
    }

    public String getStackPointer() {
        return "StackPointer: $%02X".formatted(cpu.stackPointer);
    }

    public String getFlags() {
        return "Flags: %s%s%s%s%s%s%s%s".formatted(checkFlag(StatusRegister.NEGATIVE), checkFlag(StatusRegister.OVERFLOW), checkFlag(StatusRegister.UNUSED),
                checkFlag(StatusRegister.BREAK), checkFlag(StatusRegister.DECIMAL), checkFlag(StatusRegister.DISABLE_INTERRUPTS),
                checkFlag(StatusRegister.ZERO), checkFlag(StatusRegister.CARRY));
    }

    String getInstructionInfo(Instruction instruction) {

        int address = programCounter + 1;

        return switch (instruction.addressingModeName()) {
            case "IMM", "ZP0", "ZPX", "ZPY", "IZX", "IZY" ->
                    "#$%02X".formatted(cpu.read(address));
            case "ABS", "IND", "ABY", "ABX" ->
                    "$%04X".formatted((cpu.read(address + 1) << 8 | cpu.read(address)));
            case "REL" ->
                    "$%02X [$%04X]".formatted(cpu.read(address), ((byte) cpu.read(address) + address + 1));
            default -> "";
        } + " {" + instruction.addressingModeName() + "}";
    }

    private char checkFlag(StatusRegister statusRegister) {
        return cpu.getFlag(statusRegister) == 1 ? statusRegister.code : '.';
    }
}
