package me.fabriciorby.nes.cpu;

import me.fabriciorby.nes.Bus;

public class Cpu {

    private Bus bus;

    public void connectBus(Bus bus) {
        this.bus = bus;
    }

    int read(int address) {
        return bus.read(address);
    }

    void write(int address, int data) {
        bus.write(address, data);
    }

    public int accumulator = 0x00;
    public int xRegister = 0x00;
    public int yRegister = 0x00;
    public int stackPointer = 0x00;
    public int programCounter = 0x0000;
    public int statusRegister = 0x00;
    public long clockCount = 0L;

    public int operationCode = 0x00;
    private int fetched = 0x00;
    private int addressAbsolute = 0x0000;
    private int addressRelative = 0x00;
    private int cycles = 0;

    private final Instruction[] lookupInstructions = Instruction.getInstructions(this);

    public int getFlag(StatusRegister statusRegister) {
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
            cycles += lookupInstructions[operationCode].runAndGetCycles();

            setFlag(StatusRegister.UNUSED, true);
            debugger.log(lookupInstructions[operationCode]);
        }
        cycles--;
        clockCount++;
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

    int ABS() {
        return AB(0);
    }

    int ABX() {
        return AB(xRegister);
    }

    int ABY() {
        return AB(yRegister);
    }

    int AB(int register) {
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

    int IMP() {
        fetched = accumulator;
        return 0;
    }

    int IMM() {
        addressAbsolute = programCounter++;
        return 0;
    }

    int REL() {
        addressRelative = (byte) read(programCounter);
        programCounter++;
        if ((addressRelative & 0x80) != 0)
            addressRelative |= 0xFF00;
        return 0;
    }

    int IND() {
        int low = read(programCounter);
        programCounter++;
        int high = read(programCounter);
        programCounter++;

        int pointer = (high << 8) | low;
        addressAbsolute = (read(pointer + 1) << 8) | read(pointer);
        return 0;
    }

    int IZX() {
        int address = read(programCounter);
        programCounter++;

        int low = read(address + xRegister) & 0x00FF;
        int high = read(address + xRegister + 1) & 0x00FF;

        addressAbsolute = (high << 8) | low;
        return 0;
    }

    int IZY() {
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
    }

    int ZP0() {
        return ZP(0);
    }

    int ZPX() {
        return ZP(xRegister);
    }

    int ZPY() {
        return ZP(yRegister);
    }

    int ZP(int register) {
        addressAbsolute = read(programCounter) + register;
        programCounter++;
        addressAbsolute &= 0x00FF;
        return 0;
    }

    //OperationCodes

    public int fetch() {
        if (!lookupInstructions[operationCode].isIMP()) {
            fetched = read(addressAbsolute);
        }
        return fetched;
    }

    int ADC() { // Add Memory to Accumulator with Carry
        fetch();
        int temp = accumulator + fetched + getFlag(StatusRegister.CARRY);
        setFlag(StatusRegister.CARRY, temp > 255);
        setFlag(StatusRegister.ZERO, (temp & 0x00FF) == 0);
        setFlag(StatusRegister.NEGATIVE, (temp & 0x80) != 0);
        setFlag(StatusRegister.OVERFLOW, ((~(accumulator ^ fetched) & (accumulator ^ temp)) & 0x0080) != 0);
        accumulator = temp & 0x00FF;
        return 1;
    }

    int AND() {
        fetch();
        accumulator = accumulator & fetched;
        setFlag(StatusRegister.ZERO, accumulator == 0x00);
        setFlag(StatusRegister.NEGATIVE, (accumulator & 0x80) != 0);
        return 1;
    } // "AND" Memory with Accumulator

    int ASL() {
        return 0;
    } // Shift Left One Bit (Memory or Accumulator)

    int BCC() {
        return 0;
    } // Branch on Carry Clear

    int BCS() {
        if (getFlag(StatusRegister.CARRY) == 1) {
            cycles++;
            addressAbsolute = programCounter + addressRelative;
            if ((addressAbsolute & 0xFF00) != (programCounter & 0xFF00)) {
                cycles++;
            }
            programCounter = addressAbsolute;
        }
        return 0;
    } // Branch on Carry Set

    int BEQ() {
        return 0;
    } // Branch on Result Zero

    int BIT() {
        return 0;
    } // Test Bits in Memory with Accumulator

    int BMI() {
        return 0;
    } // Branch on Result Minus

    int BNE() {
        if (getFlag(StatusRegister.ZERO) == 0) {
            cycles++;
            addressAbsolute = programCounter + addressRelative;
            if ((addressAbsolute & 0xFF00) != (programCounter & 0xFF00)) {
                cycles++;
            }
            programCounter = addressAbsolute;
        }
        return 0;
    } // Branch on Result not Zero

    int BPL() {
        return 0;
    } // Branch on Result Plus

    int BRK() {
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
    } // Force Break

    int BVC() {
        return 0;
    } // Branch on Overflow Clear

    int BVS() {
        return 0;
    } // Branch on Overflow Set

    int CLC() {
        setFlag(StatusRegister.CARRY, false);
        return 0;
    } // Clear Carry Flag

    int CLD() {
        return 0;
    } // Clear Decimal Mode

    int CLI() {
        return 0;
    } // Clear interrupt Disable Bit

    int CLV() {
        return 0;
    } // Clear Overflow Flag

    int CMP() {
        return 0;
    } // Compare Memory and Accumulator

    int CPX() {
        return 0;
    } // Compare Memory and Index X

    int CPY() {
        return 0;
    } // Compare Memory and Index Y

    int DEC() {
        return 0;
    } // Decrement Memory by One

    int DEX() {
        xRegister--;
        return DE(xRegister);
    } // Decrement Index X by One

    int DEY() {
        yRegister--;
        return DE(yRegister);
    } // Decrement Index Y by One

    private int DE(int register) {
        setFlag(StatusRegister.ZERO, register == 0x00);
        setFlag(StatusRegister.NEGATIVE, (register & 0x80) != 0);
        return 0;
    }

    int EOR() {
        return 0;
    } // "Exclusive-Or" Memory with Accumulator

    int INC() {
        return 0;
    } // Increment Memory by One

    int INX() {
        return 0;
    } // Increment Index X by One

    int INY() {
        return 0;
    } // Increment Index Y by One

    int JMP() {
        return 0;
    } // Jump to New Location

    int JSR() {
        return 0;
    } // Jump to New Location Saving Return Address

    int LDA() {
        fetch();
        accumulator = fetched;
        return LD(accumulator);
    } // Load Accumulator Memory

    int LDX() {
        fetch();
        xRegister = fetched;
        return LD(xRegister);
    } // Load Index X with Memory

    int LDY() {
        fetch();
        yRegister = fetched;
        return LD(yRegister);
    } // Load Index Y with Memory

    private int LD(int register) {
        setFlag(StatusRegister.ZERO, register == 0x00);
        setFlag(StatusRegister.NEGATIVE, (register & 0x80) != 0);
        return 1;
    }

    int LSR() {
        return 0;
    } // Shift Right One Bit (Memory or Accumulator)

    int NOP() {
        return switch (operationCode) {
            case 0x1C, 0x3C, 0x5C, 0x7C, 0xDC, 0xFC -> 1;
            default -> 0;
        };
    }// No Operation

    int ORA() {
        return 0;
    } // "OR" Memory with Accumulator

    int PHA() {
        write(0x0100 + stackPointer, accumulator);
        stackPointer--;
        return 0;
    } // Push Accumulator on Stack

    int PHP() {
        return 0;
    } // Push Processor Status on Stack

    int PLA() {
        return 0;
    } // Pull Accumulator from Stack

    int PLP() {
        return 0;
    } // Pull Processor Status from Stack

    int ROL() {
        return 0;
    } // Rotate One Bit Left (Memory or Accumulator)

    int ROR() {
        return 0;
    } // Rotate One Bit Right (Memory or Accumulator)

    int RTI() {
        stackPointer++;
        statusRegister = read(0x0100 + stackPointer);
        statusRegister &= ~StatusRegister.BREAK.bit;
        statusRegister &= ~StatusRegister.UNUSED.bit;
        stackPointer++;
        programCounter = read(0x0100 + stackPointer);
        stackPointer++;
        programCounter |= read(0x0100 + stackPointer) << 8;
        return 0;
    } // Return from Interrupt

    int RTS() {
        return 0;
    } // Return from Subroutine

    int SBC() {
        fetch();
        int value = fetched ^ 0x00FF;
        int temp = accumulator + value + getFlag(StatusRegister.CARRY);
        setFlag(StatusRegister.CARRY, (temp & 0xFF00) != 0);
        setFlag(StatusRegister.ZERO, (temp & 0x00FF) == 0);
        setFlag(StatusRegister.OVERFLOW, ((temp ^ accumulator) & (temp ^ value) & 0x0080) != 0);
        setFlag(StatusRegister.NEGATIVE, (temp & 0x0080) != 0);
        accumulator = temp & 0x00FF;
        return 1;
    } // Subtract Memory from Accumulator with Borrow

    int SEC() {
        return 0;
    } // Set Carry Flag

    int SED() {
        return 0;
    } // Set Decimal Mode

    int SEI() {
        return 0;
    } // Set Interrupt Disable Status

    int STA() {
        return ST(accumulator);
    } // Store Accumulator in Memory

    int STX() {
        return ST(xRegister);
    } // Store Index X in Memory

    int STY() {
        return ST(yRegister);
    } // Store Index Y in Memory

    int ST(int register) {
        write(addressAbsolute, register);
        return 0;
    }

    int TAX() {
        return 0;
    } // Transfer Accumulator to Index X

    int TAY() {
        return 0;
    } // Transfer Accumulator to Index Y

    int TSX() {
        return 0;
    } // Transfer Stack Pointer to Index X

    int TXA() {
        return 0;
    } // Transfer Index X to Accumulator

    int TXS() {
        return 0;
    } // Transfer Index X to Stack Pointer

    int TYA() {
        return 0;
    } // Transfer Index Y to Accumulator

    int XXX() {
        return 0;
    } // Illegal OperationCode

}
