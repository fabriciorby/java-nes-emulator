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
    public long clockCount = 0L;

    private int fetched = 0x00;
    private int addressAbsolute = 0x0000;
    private int addressRelative = 0x00;
    private int operationCode = 0x00;
    private int cycles = 0;

    enum StatusRegister {

        CARRY(1 << 0, 'C'),
        ZERO(1 << 1, 'Z'),
        DISABLE_INTERRUPTS(1 << 2, 'I'),
        DECIMAL(1 << 3, 'D'),
        BREAK(1 << 4, 'B'),
        UNUSED(1 << 5, 'U'),
        OVERFLOW(1 << 6, 'V'),
        NEGATIVE(1 << 7, 'N');

        public final int bit;
        public final char code;

        StatusRegister(int bit, char code) {
            this.bit = bit;
            this.code = code;
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

    public void clock() {
        if (cycles == 0) {
            Debugger debugger = new Debugger(this);
            operationCode = read(programCounter);
            programCounter++;
            setFlag(StatusRegister.UNUSED, true);

            Instruction instruction = lookupInstructions[operationCode];
            cycles = instruction.totalCycles;
            int additionalCycle1 = instruction.addressingMode.get();
            int additionalCycle2 = instruction.operation.get();
            cycles += additionalCycle1 & additionalCycle2;
            setFlag(StatusRegister.UNUSED, true);
            debugger.log();
        }
        cycles--;
        clockCount++;
    }

    private char checkFlag(StatusRegister statusRegister) {
        return getFlag(statusRegister) == 1 ? statusRegister.code : '.';
    }

    public boolean complete() {
        return cycles == 0;
    }

    public void reset() {
        addressAbsolute = 0xFFFC;
        int low = read(addressAbsolute);
        int high = read(addressAbsolute + 1);

        programCounter = (high << 8) | low;

        accumulator = 0;
        xRegister = 0;
        yRegister = 0;
        stackPointer = 0xFD;
        statusRegister = StatusRegister.UNUSED.bit;

        addressRelative = 0x0000;
        addressAbsolute = 0x0000;
        fetched = 0x00;

        cycles = 8;
    }

    public void interruptRequestSignal() {
        if (getFlag(StatusRegister.DISABLE_INTERRUPTS) == 0) {
            interrupt(0xFFFE);
            cycles = 7;
        }
    }

    public void nonMaskableInterruptRequestSignal() {
        interrupt(0xFFFA);
        cycles = 8;
    }

    private void interrupt(int addressAbsolute) {
        write(0x0100 + stackPointer, (programCounter >> 8) & 0x00FF);
        stackPointer--;
        write(0x0100 + stackPointer, programCounter & 0x00FF);
        stackPointer--;

        setFlag(StatusRegister.BREAK, false);
        setFlag(StatusRegister.UNUSED, true);
        setFlag(StatusRegister.DISABLE_INTERRUPTS, true);
        write(0x0100 + stackPointer, statusRegister);
        stackPointer--;

        this.addressAbsolute = addressAbsolute;
        int low = read(this.addressAbsolute);
        int high = read(this.addressAbsolute + 1);
        programCounter = (high << 8) | low;
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
        addressRelative = (byte) read(programCounter);
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

        if ((addressAbsolute & 0xFF00) != (high << 8)) {
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
        if (lookupInstructions[operationCode].addressingMode != IMP) {
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
    Supplier<Integer> ASL = () -> {
        return 0;
    }; // Shift Left One Bit (Memory or Accumulator)  |
    Supplier<Integer> BCC = () -> {
        return 0;
    }; // Branch on Carry Clear
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
    Supplier<Integer> BEQ = () -> {
        return 0;
    }; // Branch on Result Zero
    Supplier<Integer> BIT = () -> {
        return 0;
    }; // Test Bits in Memory with Accumulator
    Supplier<Integer> BMI = () -> {
        return 0;
    }; // Branch on Result Minus
    Supplier<Integer> BNE = () -> {
        if (getFlag(StatusRegister.ZERO) == 0) {
            cycles++;
            addressAbsolute = programCounter + addressRelative;
            if ((addressAbsolute & 0xFF00) != (programCounter & 0xFF00)) {
                cycles++;
            }
            programCounter = addressAbsolute;
        }
        return 0;
    }; // Branch on Result not Zero
    Supplier<Integer> BPL = () -> {
        return 0;
    }; // Branch on Result Plus
    Supplier<Integer> BRK = () -> {
        programCounter++;
        setFlag(StatusRegister.DISABLE_INTERRUPTS, true);
        write(0x0100 + stackPointer, (programCounter >> 8) & 0x00FF);
        stackPointer--;
        write(0x0100 + stackPointer, programCounter & 0x00FF);
        stackPointer--;

        setFlag(StatusRegister.BREAK, true);
        write(0x0100 + stackPointer, statusRegister);
        setFlag(StatusRegister.BREAK, false);

        programCounter = read(0xFFFE) | (read(0xFFFF) << 8);
        return 0;
    }; // Force Break
    Supplier<Integer> BVC = () -> {
        return 0;
    }; // Branch on Overflow Clear
    Supplier<Integer> BVS = () -> {
        return 0;
    }; // Branch on Overflow Set
    Supplier<Integer> CLC = () -> {
        setFlag(StatusRegister.CARRY, false);
        return 0;
    }; // Clear Carry Flag
    Supplier<Integer> CLD = () -> {
        return 0;
    }; // Clear Decimal Mode
    Supplier<Integer> CLI = () -> {
        return 0;
    }; // Clear interrupt Disable Bit
    Supplier<Integer> CLV = () -> {
        return 0;
    }; // Clear Overflow Flag
    Supplier<Integer> CMP = () -> {
        return 0;
    }; // Compare Memory and Accumulator
    Supplier<Integer> CPX = () -> {
        return 0;
    }; // Compare Memory and Index X
    Supplier<Integer> CPY = () -> {
        return 0;
    }; // Compare Memory and Index Y
    Supplier<Integer> DEC = () -> {
        return 0;
    }; // Decrement Memory by One
    Supplier<Integer> DEX = this::DEX; // Decrement Index X by One
    Supplier<Integer> DEY = this::DEY; // Decrement Index Y by One

    private int DEX() {
        xRegister--;
        return DE(xRegister);
    }

    private int DEY() {
        yRegister--;
        return DE(yRegister);
    }

    private int DE(int register) {
        setFlag(StatusRegister.ZERO, register == 0x00);
        setFlag(StatusRegister.NEGATIVE, (register & 0x80) != 0);
        return 0;
    }

    Supplier<Integer> EOR = () -> {
        return 0;
    }; // "Exclusive-Or" Memory with Accumulator
    Supplier<Integer> INC = () -> {
        return 0;
    }; // Increment Memory by One
    Supplier<Integer> INX = () -> {
        return 0;
    }; // Increment Index X by One
    Supplier<Integer> INY = () -> {
        return 0;
    }; // Increment Index Y by One
    Supplier<Integer> JMP = () -> {
        return 0;
    }; // Jump to New Location
    Supplier<Integer> JSR = () -> {
        return 0;
    }; // Jump to New Location Saving Return Address
    Supplier<Integer> LDA = this::LDA; // Load Accumulator with Memory
    Supplier<Integer> LDX = this::LDX; // Load Index X with Memory
    Supplier<Integer> LDY = this::LDY; // Load Index Y with Memory

    private int LDA() {
        fetch();
        accumulator = fetched;
        return LD(accumulator);
    }
    private int LDX() {
        fetch();
        xRegister = fetched;
        return LD(xRegister);
    }
    private int LDY() {
        fetch();
        yRegister = fetched;
        return LD(yRegister);
    }
    private int LD(int register) {
        setFlag(StatusRegister.ZERO, register == 0x00);
        setFlag(StatusRegister.NEGATIVE, (register & 0x80) != 0);
        return 1;
    }

    Supplier<Integer> LSR = () -> {
        return 0;
    }; // Shift Right One Bit (Memory or Accumulator)
    Supplier<Integer> NOP = () ->
            switch (operationCode) {
                case 0x1C, 0x3C, 0x5C, 0x7C, 0xDC, 0xFC -> 1;
                default -> 0;
            }; // No Operation
    Supplier<Integer> ORA = () -> {
        return 0;
    }; // "OR" Memory with Accumulator
    Supplier<Integer> PHA = () -> {
        write(0x0100 + stackPointer, accumulator);
        stackPointer--;
        return 0;
    }; // Push Accumulator on Stack
    Supplier<Integer> PHP = () -> {
        return 0;
    }; // Push Processor Status on Stack
    Supplier<Integer> PLA = () -> {
        return 0;
    }; // Pull Accumulator from Stack
    Supplier<Integer> PLP = () -> {
        return 0;
    }; // Pull Processor Status from Stack
    Supplier<Integer> ROL = () -> {
        return 0;
    }; // Rotate One Bit Left (Memory or Accumulator)
    Supplier<Integer> ROR = () -> {
        return 0;
    }; // Rotate One Bit Right (Memory or Accumulator)
    Supplier<Integer> RTI = () -> {
        stackPointer++;
        statusRegister = read(0x0100 + stackPointer);
        statusRegister &= ~StatusRegister.BREAK.bit;
        statusRegister &= ~StatusRegister.UNUSED.bit;
        stackPointer++;
        programCounter = read(0x0100 + stackPointer);
        stackPointer++;
        programCounter |= read(0x0100 + stackPointer) << 8;
        return 0;
    }; // Return from Interrupt
    Supplier<Integer> RTS = () -> {
        return 0;
    }; // Return from Subroutine
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
    Supplier<Integer> SEC = () -> {
        return 0;
    }; // Set Carry Flag
    Supplier<Integer> SED = () -> {
        return 0;
    }; // Set Decimal Mode
    Supplier<Integer> SEI = () -> {
        return 0;
    }; // Set Interrupt Disable Status
    Supplier<Integer> STA = () -> ST(accumulator); // Store Accumulator in Memory
    Supplier<Integer> STX = () -> ST(xRegister); // Store Index X in Memory
    Supplier<Integer> STY = () -> ST(yRegister); // Store Index Y in Memory

    private int ST(int register) {
        write(addressAbsolute, register);
        return 0;
    }

    Supplier<Integer> TAX = () -> {
        return 0;
    }; // Transfer Accumulator to Index X
    Supplier<Integer> TAY = () -> {
        return 0;
    }; // Transfer Accumulator to Index Y
    Supplier<Integer> TSX = () -> {
        return 0;
    }; // Transfer Stack Pointer to Index X
    Supplier<Integer> TXA = () -> {
        return 0;
    }; // Transfer Index X to Accumulator
    Supplier<Integer> TXS = () -> {
        return 0;
    }; // Transfer Index X to Stack Pointer
    Supplier<Integer> TYA = () -> {
        return 0;
    }; // Transfer Index Y to Accumulator
    Supplier<Integer> XXX = () -> {
        return 0;
    }; // Illegal OperationCode

    Instruction[] lookupInstructions =
            {
                    new Instruction("BRK IMM", BRK, IMM, 7), new Instruction("ORA IZX", ORA, IZX, 6), new Instruction("??? IMP", XXX, IMP, 2), new Instruction("??? IMP", XXX, IMP, 8), new Instruction("??? IMP", NOP, IMP, 3), new Instruction("ORA ZP0", ORA, ZP0, 3), new Instruction("ASL ZP0", ASL, ZP0, 5), new Instruction("??? IMP", XXX, IMP, 5), new Instruction("PHP IMP", PHP, IMP, 3), new Instruction("ORA IMM", ORA, IMM, 2), new Instruction("ASL IMP", ASL, IMP, 2), new Instruction("??? IMP", XXX, IMP, 2), new Instruction("??? IMP", NOP, IMP, 4), new Instruction("ORA ABS", ORA, ABS, 4), new Instruction("ASL ABS", ASL, ABS, 6), new Instruction("??? IMP", XXX, IMP, 6),
                    new Instruction("BPL REL", BPL, REL, 2), new Instruction("ORA IZY", ORA, IZY, 5), new Instruction("??? IMP", XXX, IMP, 2), new Instruction("??? IMP", XXX, IMP, 8), new Instruction("??? IMP", NOP, IMP, 4), new Instruction("ORA ZPX", ORA, ZPX, 4), new Instruction("ASL ZPX", ASL, ZPX, 6), new Instruction("??? IMP", XXX, IMP, 6), new Instruction("CLC IMP", CLC, IMP, 2), new Instruction("ORA ABY", ORA, ABY, 4), new Instruction("??? IMP", NOP, IMP, 2), new Instruction("??? IMP", XXX, IMP, 7), new Instruction("??? IMP", NOP, IMP, 4), new Instruction("ORA ABX", ORA, ABX, 4), new Instruction("ASL ABX", ASL, ABX, 7), new Instruction("??? IMP", XXX, IMP, 7),
                    new Instruction("JSR ABS", JSR, ABS, 6), new Instruction("AND IZX", AND, IZX, 6), new Instruction("??? IMP", XXX, IMP, 2), new Instruction("??? IMP", XXX, IMP, 8), new Instruction("BIT ZP0", BIT, ZP0, 3), new Instruction("AND ZP0", AND, ZP0, 3), new Instruction("ROL ZP0", ROL, ZP0, 5), new Instruction("??? IMP", XXX, IMP, 5), new Instruction("PLP IMP", PLP, IMP, 4), new Instruction("AND IMM", AND, IMM, 2), new Instruction("ROL IMP", ROL, IMP, 2), new Instruction("??? IMP", XXX, IMP, 2), new Instruction("BIT ABS", BIT, ABS, 4), new Instruction("AND ABS", AND, ABS, 4), new Instruction("ROL ABS", ROL, ABS, 6), new Instruction("??? IMP", XXX, IMP, 6),
                    new Instruction("BMI REL", BMI, REL, 2), new Instruction("AND IZY", AND, IZY, 5), new Instruction("??? IMP", XXX, IMP, 2), new Instruction("??? IMP", XXX, IMP, 8), new Instruction("??? IMP", NOP, IMP, 4), new Instruction("AND ZPX", AND, ZPX, 4), new Instruction("ROL ZPX", ROL, ZPX, 6), new Instruction("??? IMP", XXX, IMP, 6), new Instruction("SEC IMP", SEC, IMP, 2), new Instruction("AND ABY", AND, ABY, 4), new Instruction("??? IMP", NOP, IMP, 2), new Instruction("??? IMP", XXX, IMP, 7), new Instruction("??? IMP", NOP, IMP, 4), new Instruction("AND ABX", AND, ABX, 4), new Instruction("ROL ABX", ROL, ABX, 7), new Instruction("??? IMP", XXX, IMP, 7),
                    new Instruction("RTI IMP", RTI, IMP, 6), new Instruction("EOR IZX", EOR, IZX, 6), new Instruction("??? IMP", XXX, IMP, 2), new Instruction("??? IMP", XXX, IMP, 8), new Instruction("??? IMP", NOP, IMP, 3), new Instruction("EOR ZP0", EOR, ZP0, 3), new Instruction("LSR ZP0", LSR, ZP0, 5), new Instruction("??? IMP", XXX, IMP, 5), new Instruction("PHA IMP", PHA, IMP, 3), new Instruction("EOR IMM", EOR, IMM, 2), new Instruction("LSR IMP", LSR, IMP, 2), new Instruction("??? IMP", XXX, IMP, 2), new Instruction("JMP ABS", JMP, ABS, 3), new Instruction("EOR ABS", EOR, ABS, 4), new Instruction("LSR ABS", LSR, ABS, 6), new Instruction("??? IMP", XXX, IMP, 6),
                    new Instruction("BVC REL", BVC, REL, 2), new Instruction("EOR IZY", EOR, IZY, 5), new Instruction("??? IMP", XXX, IMP, 2), new Instruction("??? IMP", XXX, IMP, 8), new Instruction("??? IMP", NOP, IMP, 4), new Instruction("EOR ZPX", EOR, ZPX, 4), new Instruction("LSR ZPX", LSR, ZPX, 6), new Instruction("??? IMP", XXX, IMP, 6), new Instruction("CLI IMP", CLI, IMP, 2), new Instruction("EOR ABY", EOR, ABY, 4), new Instruction("??? IMP", NOP, IMP, 2), new Instruction("??? IMP", XXX, IMP, 7), new Instruction("??? IMP", NOP, IMP, 4), new Instruction("EOR ABX", EOR, ABX, 4), new Instruction("LSR ABX", LSR, ABX, 7), new Instruction("??? IMP", XXX, IMP, 7),
                    new Instruction("RTS IMP", RTS, IMP, 6), new Instruction("ADC IZX", ADC, IZX, 6), new Instruction("??? IMP", XXX, IMP, 2), new Instruction("??? IMP", XXX, IMP, 8), new Instruction("??? IMP", NOP, IMP, 3), new Instruction("ADC ZP0", ADC, ZP0, 3), new Instruction("ROR ZP0", ROR, ZP0, 5), new Instruction("??? IMP", XXX, IMP, 5), new Instruction("PLA IMP", PLA, IMP, 4), new Instruction("ADC IMM", ADC, IMM, 2), new Instruction("ROR IMP", ROR, IMP, 2), new Instruction("??? IMP", XXX, IMP, 2), new Instruction("JMP IND", JMP, IND, 5), new Instruction("ADC ABS", ADC, ABS, 4), new Instruction("ROR ABS", ROR, ABS, 6), new Instruction("??? IMP", XXX, IMP, 6),
                    new Instruction("BVS REL", BVS, REL, 2), new Instruction("ADC IZY", ADC, IZY, 5), new Instruction("??? IMP", XXX, IMP, 2), new Instruction("??? IMP", XXX, IMP, 8), new Instruction("??? IMP", NOP, IMP, 4), new Instruction("ADC ZPX", ADC, ZPX, 4), new Instruction("ROR ZPX", ROR, ZPX, 6), new Instruction("??? IMP", XXX, IMP, 6), new Instruction("SEI IMP", SEI, IMP, 2), new Instruction("ADC ABY", ADC, ABY, 4), new Instruction("??? IMP", NOP, IMP, 2), new Instruction("??? IMP", XXX, IMP, 7), new Instruction("??? IMP", NOP, IMP, 4), new Instruction("ADC ABX", ADC, ABX, 4), new Instruction("ROR ABX", ROR, ABX, 7), new Instruction("??? IMP", XXX, IMP, 7),
                    new Instruction("??? IMP", NOP, IMP, 2), new Instruction("STA IZX", STA, IZX, 6), new Instruction("??? IMP", NOP, IMP, 2), new Instruction("??? IMP", XXX, IMP, 6), new Instruction("STY ZP0", STY, ZP0, 3), new Instruction("STA ZP0", STA, ZP0, 3), new Instruction("STX ZP0", STX, ZP0, 3), new Instruction("??? IMP", XXX, IMP, 3), new Instruction("DEY IMP", DEY, IMP, 2), new Instruction("??? IMP", NOP, IMP, 2), new Instruction("TXA IMP", TXA, IMP, 2), new Instruction("??? IMP", XXX, IMP, 2), new Instruction("STY ABS", STY, ABS, 4), new Instruction("STA ABS", STA, ABS, 4), new Instruction("STX ABS", STX, ABS, 4), new Instruction("??? IMP", XXX, IMP, 4),
                    new Instruction("BCC REL", BCC, REL, 2), new Instruction("STA IZY", STA, IZY, 6), new Instruction("??? IMP", XXX, IMP, 2), new Instruction("??? IMP", XXX, IMP, 6), new Instruction("STY ZPX", STY, ZPX, 4), new Instruction("STA ZPX", STA, ZPX, 4), new Instruction("STX ZPY", STX, ZPY, 4), new Instruction("??? IMP", XXX, IMP, 4), new Instruction("TYA IMP", TYA, IMP, 2), new Instruction("STA ABY", STA, ABY, 5), new Instruction("TXS IMP", TXS, IMP, 2), new Instruction("??? IMP", XXX, IMP, 5), new Instruction("??? IMP", NOP, IMP, 5), new Instruction("STA ABX", STA, ABX, 5), new Instruction("??? IMP", XXX, IMP, 5), new Instruction("??? IMP", XXX, IMP, 5),
                    new Instruction("LDY IMM", LDY, IMM, 2), new Instruction("LDA IZX", LDA, IZX, 6), new Instruction("LDX IMM", LDX, IMM, 2), new Instruction("??? IMP", XXX, IMP, 6), new Instruction("LDY ZP0", LDY, ZP0, 3), new Instruction("LDA ZP0", LDA, ZP0, 3), new Instruction("LDX ZP0", LDX, ZP0, 3), new Instruction("??? IMP", XXX, IMP, 3), new Instruction("TAY IMP", TAY, IMP, 2), new Instruction("LDA IMM", LDA, IMM, 2), new Instruction("TAX IMP", TAX, IMP, 2), new Instruction("??? IMP", XXX, IMP, 2), new Instruction("LDY ABS", LDY, ABS, 4), new Instruction("LDA ABS", LDA, ABS, 4), new Instruction("LDX ABS", LDX, ABS, 4), new Instruction("??? IMP", XXX, IMP, 4),
                    new Instruction("BCS REL", BCS, REL, 2), new Instruction("LDA IZY", LDA, IZY, 5), new Instruction("??? IMP", XXX, IMP, 2), new Instruction("??? IMP", XXX, IMP, 5), new Instruction("LDY ZPX", LDY, ZPX, 4), new Instruction("LDA ZPX", LDA, ZPX, 4), new Instruction("LDX ZPY", LDX, ZPY, 4), new Instruction("??? IMP", XXX, IMP, 4), new Instruction("CLV IMP", CLV, IMP, 2), new Instruction("LDA ABY", LDA, ABY, 4), new Instruction("TSX IMP", TSX, IMP, 2), new Instruction("??? IMP", XXX, IMP, 4), new Instruction("LDY ABX", LDY, ABX, 4), new Instruction("LDA ABX", LDA, ABX, 4), new Instruction("LDX ABY", LDX, ABY, 4), new Instruction("??? IMP", XXX, IMP, 4),
                    new Instruction("CPY IMM", CPY, IMM, 2), new Instruction("CMP IZX", CMP, IZX, 6), new Instruction("??? IMP", NOP, IMP, 2), new Instruction("??? IMP", XXX, IMP, 8), new Instruction("CPY ZP0", CPY, ZP0, 3), new Instruction("CMP ZP0", CMP, ZP0, 3), new Instruction("DEC ZP0", DEC, ZP0, 5), new Instruction("??? IMP", XXX, IMP, 5), new Instruction("INY IMP", INY, IMP, 2), new Instruction("CMP IMM", CMP, IMM, 2), new Instruction("DEX IMP", DEX, IMP, 2), new Instruction("??? IMP", XXX, IMP, 2), new Instruction("CPY ABS", CPY, ABS, 4), new Instruction("CMP ABS", CMP, ABS, 4), new Instruction("DEC ABS", DEC, ABS, 6), new Instruction("??? IMP", XXX, IMP, 6),
                    new Instruction("BNE REL", BNE, REL, 2), new Instruction("CMP IZY", CMP, IZY, 5), new Instruction("??? IMP", XXX, IMP, 2), new Instruction("??? IMP", XXX, IMP, 8), new Instruction("??? IMP", NOP, IMP, 4), new Instruction("CMP ZPX", CMP, ZPX, 4), new Instruction("DEC ZPX", DEC, ZPX, 6), new Instruction("??? IMP", XXX, IMP, 6), new Instruction("CLD IMP", CLD, IMP, 2), new Instruction("CMP ABY", CMP, ABY, 4), new Instruction("NOP IMP", NOP, IMP, 2), new Instruction("??? IMP", XXX, IMP, 7), new Instruction("??? IMP", NOP, IMP, 4), new Instruction("CMP ABX", CMP, ABX, 4), new Instruction("DEC ABX", DEC, ABX, 7), new Instruction("??? IMP", XXX, IMP, 7),
                    new Instruction("CPX IMM", CPX, IMM, 2), new Instruction("SBC IZX", SBC, IZX, 6), new Instruction("??? IMP", NOP, IMP, 2), new Instruction("??? IMP", XXX, IMP, 8), new Instruction("CPX ZP0", CPX, ZP0, 3), new Instruction("SBC ZP0", SBC, ZP0, 3), new Instruction("INC ZP0", INC, ZP0, 5), new Instruction("??? IMP", XXX, IMP, 5), new Instruction("INX IMP", INX, IMP, 2), new Instruction("SBC IMM", SBC, IMM, 2), new Instruction("NOP IMP", NOP, IMP, 2), new Instruction("??? IMP", SBC, IMP, 2), new Instruction("CPX ABS", CPX, ABS, 4), new Instruction("SBC ABS", SBC, ABS, 4), new Instruction("INC ABS", INC, ABS, 6), new Instruction("??? IMP", XXX, IMP, 6),
                    new Instruction("BEQ REL", BEQ, REL, 2), new Instruction("SBC IZY", SBC, IZY, 5), new Instruction("??? IMP", XXX, IMP, 2), new Instruction("??? IMP", XXX, IMP, 8), new Instruction("??? IMP", NOP, IMP, 4), new Instruction("SBC ZPX", SBC, ZPX, 4), new Instruction("INC ZPX", INC, ZPX, 6), new Instruction("??? IMP", XXX, IMP, 6), new Instruction("SED IMP", SED, IMP, 2), new Instruction("SBC ABY", SBC, ABY, 4), new Instruction("NOP IMP", NOP, IMP, 2), new Instruction("??? IMP", XXX, IMP, 7), new Instruction("??? IMP", NOP, IMP, 4), new Instruction("SBC ABX", SBC, ABX, 4), new Instruction("INC ABX", INC, ABX, 7), new Instruction("??? IMP", XXX, IMP, 7),
            };

    record Instruction(String name, Supplier<Integer> operation, Supplier<Integer> addressingMode, int totalCycles) { }

    private class Debugger {
        int programCounter;
        long clockCount;

        Debugger(Cpu cpu) {
            this.programCounter = cpu.programCounter;
            this.clockCount = cpu.clockCount;
        }

        void log() {
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
                    lookupInstructions[operationCode].name(),
                    programCounter, accumulator, xRegister, yRegister, stackPointer,
                    checkFlag(StatusRegister.NEGATIVE), checkFlag(StatusRegister.OVERFLOW), checkFlag(StatusRegister.UNUSED),
                    checkFlag(StatusRegister.BREAK), checkFlag(StatusRegister.DECIMAL), checkFlag(StatusRegister.DISABLE_INTERRUPTS),
                    checkFlag(StatusRegister.ZERO), checkFlag(StatusRegister.CARRY), clockCount);
            System.out.println(debug);
        }
    }

}
