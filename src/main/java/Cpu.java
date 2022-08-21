import java.util.function.Supplier;

public class Cpu {

    private Bus bus;

    public void connectBus(Bus bus) {
        this.bus = bus;
    }

    private int read(int address) {
        return bus.read(address);
    }

    private void write(int address, int data) {
        bus.write(address, data);
    }

    public int accumulator = 0x00;
    public int xRegister = 0x00;
    public int yRegister = 0x00;
    public int stackPointer = 0x00;
    public int programCounter = 0x0000;
    public int statusRegister = 0x00;

    private int fetched = 0x00;
    private int addressAbsolute = 0x0000;
    private int addressRelative = 0x00;
    private int operationCode = 0x00;
    private int cycles = 0;

    enum StatusRegister {

        CARRY(1 << 0),
        ZERO(1 << 1),
        INTERRUPTS(1 << 2),
        DECIMAL(1 << 3),
        BREAK(1 << 4),
        UNUSED(1 << 5),
        OVERFLOW(1 << 6),
        NEGATIVE(1 << 7);

        public final int bit;

        StatusRegister(int bit) {
            this.bit = bit;
        }
    }

    private int getFlag(StatusRegister statusRegister) {
        return ((this.statusRegister & statusRegister.bit) > 0) ? 1 : 0;
    }

    private void setFlag(StatusRegister statusRegister, boolean value) {
        if (value) {
            this.statusRegister |= statusRegister.bit;
        } else {
            this.statusRegister &= ~statusRegister.bit;
        }
    }

    public void clockSignal() {
        if (cycles == 0) {
            operationCode = read(programCounter);
            programCounter++;

            Instruction instruction = lookupInstructions[operationCode];
            cycles = instruction.totalCycles;
            int additionalCycle1 = instruction.addressingMode.get();
            int additionalCycle2 = instruction.operation.get();
            cycles += additionalCycle1 & additionalCycle2;
        }
        cycles--;
    }

    public void resetSignal() {

    }

    public void interruptRequestSignal() {

    }

    public void nonMaskableInterruptRequestSignal() {

    }

    //AddressingModes

    Supplier<Integer> ABS = () -> AB(0);
    Supplier<Integer> ABX = () -> AB(xRegister);
    Supplier<Integer> ABY = () -> AB(yRegister);
    private int AB(int register) {
        int low = read(programCounter);
        programCounter++;
        int high = read(programCounter);
        programCounter++;
        addressAbsolute = (high << 8) | low;
        addressAbsolute += register;
        if ((addressAbsolute & 0xFF00) != (high << 8)) {
            return 1;
        } else {
            return 0;
        }
    }

    Supplier<Integer> IMP = () -> {
        fetched = accumulator;
        return 0;
    };
    Supplier<Integer> IMM = () -> {
        addressAbsolute = programCounter++;
        return 0;
    };
    Supplier<Integer> REL = () -> {
        addressRelative = read(programCounter);
        programCounter++;
        if ((addressRelative & 0x80) != 0)
            addressRelative |= 0xFF00;
        return 0;
    };

    Supplier<Integer> IND = () -> {
        int low = read(programCounter);
        programCounter++;
        int high = read(programCounter);
        programCounter++;

        int pointer = (high << 8) | low;
        addressAbsolute = (read(pointer + 1) << 8) | read(pointer);
        return 0;
    };

    Supplier<Integer> IZX = () -> {
        int address = read(programCounter);
        programCounter++;

        int low = read(address + xRegister) & 0x00FF;
        int high = read(address + xRegister + 1) & 0x00FF;

        addressAbsolute = (high << 8) | low;
        return 0;
    };

    Supplier<Integer> IZY = () -> {
        int address = read(programCounter);
        programCounter++;

        int low = read(address & 0x00FF);
        int high = read((address + 1) & 0x00FF);

        addressAbsolute = (high << 8) | low;
        addressAbsolute += yRegister;

        if((addressAbsolute & 0xFF00) != (high << 8)) {
            return 1;
        } else {
            return 0;
        }

    };

    Supplier<Integer> ZP0 = () -> ZP(0);
    Supplier<Integer> ZPX = () -> ZP(xRegister);
    Supplier<Integer> ZPY = () -> ZP(yRegister);

    private int ZP(int register) {
        addressAbsolute = read(programCounter) + register;
        programCounter++;
        addressAbsolute &= 0x00FF;
        return 0;
    }

    //OperationCodes

    public int fetch() {
        if (!(lookupInstructions[operationCode].addressingMode == IMP)) {
            fetched = read(addressAbsolute);
        }
        return fetched;
    }

    Supplier<Integer> ADC = () -> {
        fetch();
        int temp = accumulator + fetched + getFlag(StatusRegister.CARRY);
        setFlag(StatusRegister.CARRY, temp > 255);
        setFlag(StatusRegister.ZERO, (temp & 0x00FF) == 0);
        setFlag(StatusRegister.NEGATIVE, (temp & 0x80) != 0);
        setFlag(StatusRegister.OVERFLOW, ((~(accumulator ^ fetched) & (accumulator ^ temp)) & 0x0080) != 0);
        accumulator = temp & 0x00FF;
        return 1;
    }; // Add Memory to Accumulator with Carry
    Supplier<Integer> AND = () -> {
        fetch();
        accumulator = accumulator & fetched;
        setFlag(StatusRegister.ZERO, accumulator == 0x00);
        setFlag(StatusRegister.NEGATIVE, (accumulator & 0x80) != 0);
        return 1;
    }; // "AND" Memory with Accumulator
    Supplier<Integer> ASL = () -> {return 0;}; // Shift Left One Bit (Memory or Accumulator)  |
    Supplier<Integer> BCC = () -> {return 0;}; // Branch on Carry Clear
    Supplier<Integer> BCS = () -> {
        if (getFlag(StatusRegister.CARRY) == 1) {
            cycles++;
            addressAbsolute = programCounter + addressRelative;
            if ((addressAbsolute & 0xFF00) != (programCounter & 0xFF00)) {
                cycles++;
            }
            programCounter = addressAbsolute;
        }
        return 0;
    }; // Branch on Carry Set
    Supplier<Integer> BEQ = () -> {return 0;}; // Branch on Result Zero
    Supplier<Integer> BIT = () -> {return 0;}; // Test Bits in Memory with Accumulator
    Supplier<Integer> BMI = () -> {return 0;}; // Branch on Result Minus
    Supplier<Integer> BNE = () -> {return 0;}; // Branch on Result not Zero
    Supplier<Integer> BPL = () -> {return 0;}; // Branch on Result Plus
    Supplier<Integer> BRK = () -> {return 0;}; // Force Break
    Supplier<Integer> BVC = () -> {return 0;}; // Branch on Overflow Clear
    Supplier<Integer> BVS = () -> {return 0;}; // Branch on Overflow Set
    Supplier<Integer> CLC = () -> {
        setFlag(StatusRegister.CARRY, false);
        return 0;
    }; // Clear Carry Flag
    Supplier<Integer> CLD = () -> {return 0;}; // Clear Decimal Mode
    Supplier<Integer> CLI = () -> {return 0;}; // Clear interrupt Disable Bit
    Supplier<Integer> CLV = () -> {return 0;}; // Clear Overflow Flag
    Supplier<Integer> CMP = () -> {return 0;}; // Compare Memory and Accumulator
    Supplier<Integer> CPX = () -> {return 0;}; // Compare Memory and Index X
    Supplier<Integer> CPY = () -> {return 0;}; // Compare Memory and Index Y
    Supplier<Integer> DEC = () -> {return 0;}; // Decrement Memory by One
    Supplier<Integer> DEX = () -> {return 0;}; // Decrement Index X by One
    Supplier<Integer> DEY = () -> {return 0;}; // Decrement Index Y by One
    Supplier<Integer> EOR = () -> {return 0;}; // "Exclusive-Or" Memory with Accumulator
    Supplier<Integer> INC = () -> {return 0;}; // Increment Memory by One
    Supplier<Integer> INX = () -> {return 0;}; // Increment Index X by One
    Supplier<Integer> INY = () -> {return 0;}; // Increment Index Y by One
    Supplier<Integer> JMP = () -> {return 0;}; // Jump to New Location
    Supplier<Integer> JSR = () -> {return 0;}; // Jump to New Location Saving Return Address
    Supplier<Integer> LDA = () -> {return 0;}; // Load Accumulator with Memory
    Supplier<Integer> LDX = () -> {return 0;}; // Load Index X with Memory
    Supplier<Integer> LDY = () -> {return 0;}; // Load Index Y with Memory
    Supplier<Integer> LSR = () -> {return 0;}; // Shift Right One Bit (Memory or Accumulator)
    Supplier<Integer> NOP = () -> {return 0;}; // No Operation
    Supplier<Integer> ORA = () -> {return 0;}; // "OR" Memory with Accumulator
    Supplier<Integer> PHA = () -> {
        write(0x0100 + stackPointer, accumulator);
        stackPointer--;
        return 0;
    }; // Push Accumulator on Stack
    Supplier<Integer> PHP = () -> {return 0;}; // Push Processor Status on Stack
    Supplier<Integer> PLA = () -> {return 0;}; // Pull Accumulator from Stack
    Supplier<Integer> PLP = () -> {return 0;}; // Pull Processor Status from Stack
    Supplier<Integer> ROL = () -> {return 0;}; // Rotate One Bit Left (Memory or Accumulator)
    Supplier<Integer> ROR = () -> {return 0;}; // Rotate One Bit Right (Memory or Accumulator)
    Supplier<Integer> RTI = () -> {return 0;}; // Return from Interrupt
    Supplier<Integer> RTS = () -> {return 0;}; // Return from Subroutine
    Supplier<Integer> SBC = () -> {
        fetch();
        int value = fetched ^ 0x00FF;
        int temp = accumulator + value + getFlag(StatusRegister.CARRY);
        setFlag(StatusRegister.CARRY, (temp & 0xFF00) != 0);
        setFlag(StatusRegister.ZERO, (temp & 0x00FF) == 0);
        setFlag(StatusRegister.OVERFLOW, ((temp ^ accumulator) & (temp ^ value) & 0x0080) != 0);
        setFlag(StatusRegister.NEGATIVE, (temp & 0x0080) != 0);
        accumulator = temp & 0x00FF;
        return 1;
    }; // Subtract Memory from Accumulator with Borrow
    Supplier<Integer> SEC = () -> {return 0;}; // Set Carry Flag
    Supplier<Integer> SED = () -> {return 0;}; // Set Decimal Mode
    Supplier<Integer> SEI = () -> {return 0;}; // Set Interrupt Disable Status
    Supplier<Integer> STA = () -> {return 0;}; // Store Accumulator in Memory
    Supplier<Integer> STX = () -> {return 0;}; // Store Index X in Memory
    Supplier<Integer> STY = () -> {return 0;}; // Store Index Y in Memory
    Supplier<Integer> TAX = () -> {return 0;}; // Transfer Accumulator to Index X
    Supplier<Integer> TAY = () -> {return 0;}; // Transfer Accumulator to Index Y
    Supplier<Integer> TSX = () -> {return 0;}; // Transfer Stack Pointer to Index X
    Supplier<Integer> TXA = () -> {return 0;}; // Transfer Index X to Accumulator
    Supplier<Integer> TXS = () -> {return 0;}; // Transfer Index X to Stack Pointer
    Supplier<Integer> TYA = () -> {return 0;}; // Transfer Index Y to Accumulator
    Supplier<Integer> XXX = () -> {return 0;}; // Illegal OperationCode

    Instruction[] lookupInstructions =
            {
                    new Instruction("BRK", BRK, IMM, 7), new Instruction("ORA", ORA, IZX, 6), new Instruction("???", XXX, IMP, 2), new Instruction("???", XXX, IMP, 8), new Instruction("???", NOP, IMP, 3), new Instruction("ORA", ORA, ZP0, 3), new Instruction("ASL", ASL, ZP0, 5), new Instruction("???", XXX, IMP, 5), new Instruction("PHP", PHP, IMP, 3), new Instruction("ORA", ORA, IMM, 2), new Instruction("ASL", ASL, IMP, 2), new Instruction("???", XXX, IMP, 2), new Instruction("???", NOP, IMP, 4), new Instruction("ORA", ORA, ABS, 4), new Instruction("ASL", ASL, ABS, 6), new Instruction("???", XXX, IMP, 6),
                    new Instruction("BPL", BPL, REL, 2), new Instruction("ORA", ORA, IZY, 5), new Instruction("???", XXX, IMP, 2), new Instruction("???", XXX, IMP, 8), new Instruction("???", NOP, IMP, 4), new Instruction("ORA", ORA, ZPX, 4), new Instruction("ASL", ASL, ZPX, 6), new Instruction("???", XXX, IMP, 6), new Instruction("CLC", CLC, IMP, 2), new Instruction("ORA", ORA, ABY, 4), new Instruction("???", NOP, IMP, 2), new Instruction("???", XXX, IMP, 7), new Instruction("???", NOP, IMP, 4), new Instruction("ORA", ORA, ABX, 4), new Instruction("ASL", ASL, ABX, 7), new Instruction("???", XXX, IMP, 7),
                    new Instruction("JSR", JSR, ABS, 6), new Instruction("AND", AND, IZX, 6), new Instruction("???", XXX, IMP, 2), new Instruction("???", XXX, IMP, 8), new Instruction("BIT", BIT, ZP0, 3), new Instruction("AND", AND, ZP0, 3), new Instruction("ROL", ROL, ZP0, 5), new Instruction("???", XXX, IMP, 5), new Instruction("PLP", PLP, IMP, 4), new Instruction("AND", AND, IMM, 2), new Instruction("ROL", ROL, IMP, 2), new Instruction("???", XXX, IMP, 2), new Instruction("BIT", BIT, ABS, 4), new Instruction("AND", AND, ABS, 4), new Instruction("ROL", ROL, ABS, 6), new Instruction("???", XXX, IMP, 6),
                    new Instruction("BMI", BMI, REL, 2), new Instruction("AND", AND, IZY, 5), new Instruction("???", XXX, IMP, 2), new Instruction("???", XXX, IMP, 8), new Instruction("???", NOP, IMP, 4), new Instruction("AND", AND, ZPX, 4), new Instruction("ROL", ROL, ZPX, 6), new Instruction("???", XXX, IMP, 6), new Instruction("SEC", SEC, IMP, 2), new Instruction("AND", AND, ABY, 4), new Instruction("???", NOP, IMP, 2), new Instruction("???", XXX, IMP, 7), new Instruction("???", NOP, IMP, 4), new Instruction("AND", AND, ABX, 4), new Instruction("ROL", ROL, ABX, 7), new Instruction("???", XXX, IMP, 7),
                    new Instruction("RTI", RTI, IMP, 6), new Instruction("EOR", EOR, IZX, 6), new Instruction("???", XXX, IMP, 2), new Instruction("???", XXX, IMP, 8), new Instruction("???", NOP, IMP, 3), new Instruction("EOR", EOR, ZP0, 3), new Instruction("LSR", LSR, ZP0, 5), new Instruction("???", XXX, IMP, 5), new Instruction("PHA", PHA, IMP, 3), new Instruction("EOR", EOR, IMM, 2), new Instruction("LSR", LSR, IMP, 2), new Instruction("???", XXX, IMP, 2), new Instruction("JMP", JMP, ABS, 3), new Instruction("EOR", EOR, ABS, 4), new Instruction("LSR", LSR, ABS, 6), new Instruction("???", XXX, IMP, 6),
                    new Instruction("BVC", BVC, REL, 2), new Instruction("EOR", EOR, IZY, 5), new Instruction("???", XXX, IMP, 2), new Instruction("???", XXX, IMP, 8), new Instruction("???", NOP, IMP, 4), new Instruction("EOR", EOR, ZPX, 4), new Instruction("LSR", LSR, ZPX, 6), new Instruction("???", XXX, IMP, 6), new Instruction("CLI", CLI, IMP, 2), new Instruction("EOR", EOR, ABY, 4), new Instruction("???", NOP, IMP, 2), new Instruction("???", XXX, IMP, 7), new Instruction("???", NOP, IMP, 4), new Instruction("EOR", EOR, ABX, 4), new Instruction("LSR", LSR, ABX, 7), new Instruction("???", XXX, IMP, 7),
                    new Instruction("RTS", RTS, IMP, 6), new Instruction("ADC", ADC, IZX, 6), new Instruction("???", XXX, IMP, 2), new Instruction("???", XXX, IMP, 8), new Instruction("???", NOP, IMP, 3), new Instruction("ADC", ADC, ZP0, 3), new Instruction("ROR", ROR, ZP0, 5), new Instruction("???", XXX, IMP, 5), new Instruction("PLA", PLA, IMP, 4), new Instruction("ADC", ADC, IMM, 2), new Instruction("ROR", ROR, IMP, 2), new Instruction("???", XXX, IMP, 2), new Instruction("JMP", JMP, IND, 5), new Instruction("ADC", ADC, ABS, 4), new Instruction("ROR", ROR, ABS, 6), new Instruction("???", XXX, IMP, 6),
                    new Instruction("BVS", BVS, REL, 2), new Instruction("ADC", ADC, IZY, 5), new Instruction("???", XXX, IMP, 2), new Instruction("???", XXX, IMP, 8), new Instruction("???", NOP, IMP, 4), new Instruction("ADC", ADC, ZPX, 4), new Instruction("ROR", ROR, ZPX, 6), new Instruction("???", XXX, IMP, 6), new Instruction("SEI", SEI, IMP, 2), new Instruction("ADC", ADC, ABY, 4), new Instruction("???", NOP, IMP, 2), new Instruction("???", XXX, IMP, 7), new Instruction("???", NOP, IMP, 4), new Instruction("ADC", ADC, ABX, 4), new Instruction("ROR", ROR, ABX, 7), new Instruction("???", XXX, IMP, 7),
                    new Instruction("???", NOP, IMP, 2), new Instruction("STA", STA, IZX, 6), new Instruction("???", NOP, IMP, 2), new Instruction("???", XXX, IMP, 6), new Instruction("STY", STY, ZP0, 3), new Instruction("STA", STA, ZP0, 3), new Instruction("STX", STX, ZP0, 3), new Instruction("???", XXX, IMP, 3), new Instruction("DEY", DEY, IMP, 2), new Instruction("???", NOP, IMP, 2), new Instruction("TXA", TXA, IMP, 2), new Instruction("???", XXX, IMP, 2), new Instruction("STY", STY, ABS, 4), new Instruction("STA", STA, ABS, 4), new Instruction("STX", STX, ABS, 4), new Instruction("???", XXX, IMP, 4),
                    new Instruction("BCC", BCC, REL, 2), new Instruction("STA", STA, IZY, 6), new Instruction("???", XXX, IMP, 2), new Instruction("???", XXX, IMP, 6), new Instruction("STY", STY, ZPX, 4), new Instruction("STA", STA, ZPX, 4), new Instruction("STX", STX, ZPY, 4), new Instruction("???", XXX, IMP, 4), new Instruction("TYA", TYA, IMP, 2), new Instruction("STA", STA, ABY, 5), new Instruction("TXS", TXS, IMP, 2), new Instruction("???", XXX, IMP, 5), new Instruction("???", NOP, IMP, 5), new Instruction("STA", STA, ABX, 5), new Instruction("???", XXX, IMP, 5), new Instruction("???", XXX, IMP, 5),
                    new Instruction("LDY", LDY, IMM, 2), new Instruction("LDA", LDA, IZX, 6), new Instruction("LDX", LDX, IMM, 2), new Instruction("???", XXX, IMP, 6), new Instruction("LDY", LDY, ZP0, 3), new Instruction("LDA", LDA, ZP0, 3), new Instruction("LDX", LDX, ZP0, 3), new Instruction("???", XXX, IMP, 3), new Instruction("TAY", TAY, IMP, 2), new Instruction("LDA", LDA, IMM, 2), new Instruction("TAX", TAX, IMP, 2), new Instruction("???", XXX, IMP, 2), new Instruction("LDY", LDY, ABS, 4), new Instruction("LDA", LDA, ABS, 4), new Instruction("LDX", LDX, ABS, 4), new Instruction("???", XXX, IMP, 4),
                    new Instruction("BCS", BCS, REL, 2), new Instruction("LDA", LDA, IZY, 5), new Instruction("???", XXX, IMP, 2), new Instruction("???", XXX, IMP, 5), new Instruction("LDY", LDY, ZPX, 4), new Instruction("LDA", LDA, ZPX, 4), new Instruction("LDX", LDX, ZPY, 4), new Instruction("???", XXX, IMP, 4), new Instruction("CLV", CLV, IMP, 2), new Instruction("LDA", LDA, ABY, 4), new Instruction("TSX", TSX, IMP, 2), new Instruction("???", XXX, IMP, 4), new Instruction("LDY", LDY, ABX, 4), new Instruction("LDA", LDA, ABX, 4), new Instruction("LDX", LDX, ABY, 4), new Instruction("???", XXX, IMP, 4),
                    new Instruction("CPY", CPY, IMM, 2), new Instruction("CMP", CMP, IZX, 6), new Instruction("???", NOP, IMP, 2), new Instruction("???", XXX, IMP, 8), new Instruction("CPY", CPY, ZP0, 3), new Instruction("CMP", CMP, ZP0, 3), new Instruction("DEC", DEC, ZP0, 5), new Instruction("???", XXX, IMP, 5), new Instruction("INY", INY, IMP, 2), new Instruction("CMP", CMP, IMM, 2), new Instruction("DEX", DEX, IMP, 2), new Instruction("???", XXX, IMP, 2), new Instruction("CPY", CPY, ABS, 4), new Instruction("CMP", CMP, ABS, 4), new Instruction("DEC", DEC, ABS, 6), new Instruction("???", XXX, IMP, 6),
                    new Instruction("BNE", BNE, REL, 2), new Instruction("CMP", CMP, IZY, 5), new Instruction("???", XXX, IMP, 2), new Instruction("???", XXX, IMP, 8), new Instruction("???", NOP, IMP, 4), new Instruction("CMP", CMP, ZPX, 4), new Instruction("DEC", DEC, ZPX, 6), new Instruction("???", XXX, IMP, 6), new Instruction("CLD", CLD, IMP, 2), new Instruction("CMP", CMP, ABY, 4), new Instruction("NOP", NOP, IMP, 2), new Instruction("???", XXX, IMP, 7), new Instruction("???", NOP, IMP, 4), new Instruction("CMP", CMP, ABX, 4), new Instruction("DEC", DEC, ABX, 7), new Instruction("???", XXX, IMP, 7),
                    new Instruction("CPX", CPX, IMM, 2), new Instruction("SBC", SBC, IZX, 6), new Instruction("???", NOP, IMP, 2), new Instruction("???", XXX, IMP, 8), new Instruction("CPX", CPX, ZP0, 3), new Instruction("SBC", SBC, ZP0, 3), new Instruction("INC", INC, ZP0, 5), new Instruction("???", XXX, IMP, 5), new Instruction("INX", INX, IMP, 2), new Instruction("SBC", SBC, IMM, 2), new Instruction("NOP", NOP, IMP, 2), new Instruction("???", SBC, IMP, 2), new Instruction("CPX", CPX, ABS, 4), new Instruction("SBC", SBC, ABS, 4), new Instruction("INC", INC, ABS, 6), new Instruction("???", XXX, IMP, 6),
                    new Instruction("BEQ", BEQ, REL, 2), new Instruction("SBC", SBC, IZY, 5), new Instruction("???", XXX, IMP, 2), new Instruction("???", XXX, IMP, 8), new Instruction("???", NOP, IMP, 4), new Instruction("SBC", SBC, ZPX, 4), new Instruction("INC", INC, ZPX, 6), new Instruction("???", XXX, IMP, 6), new Instruction("SED", SED, IMP, 2), new Instruction("SBC", SBC, ABY, 4), new Instruction("NOP", NOP, IMP, 2), new Instruction("???", XXX, IMP, 7), new Instruction("???", NOP, IMP, 4), new Instruction("SBC", SBC, ABX, 4), new Instruction("INC", INC, ABX, 7), new Instruction("???", XXX, IMP, 7),
            };

    static class Instruction {
        private final String name;
        private final Supplier<Integer> operation;
        private final Supplier<Integer> addressingMode;
        private final int totalCycles;

        public Instruction(String name, Supplier<Integer> operation, Supplier<Integer> addressingMode, int totalCycles) {
            this.name = name;
            this.operation = operation;
            this.addressingMode = addressingMode;
            this.totalCycles = totalCycles;
        }
    }

}
