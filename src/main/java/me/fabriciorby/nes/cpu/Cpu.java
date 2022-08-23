package me.fabriciorby.nes.cpu;

import me.fabriciorby.nes.Bus;

import static me.fabriciorby.nes.cpu.StatusRegister.*;

public class Cpu {

    private Bus bus;

    public void connectBus(Bus bus) {
        this.bus = bus;
    }

    int read(int address) {
        return bus.cpuRead(address);
    }

    void write(int address, int data) {
        bus.cpuWrite(address, data);
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

    final Instruction[] lookupInstructions = Instruction.getInstructions(this);

    public int getFlag(StatusRegister statusRegister) {
        return ((this.statusRegister & statusRegister.bit) > 0) ? 1 : 0;
    }

    public boolean getBooleanFlag(StatusRegister statusRegister) {
        return (this.statusRegister & statusRegister.bit) > 0;
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
            setFlag(UNUSED, true);
            cycles += lookupInstructions[operationCode].runAndGetCycles();

            setFlag(UNUSED, true);
            debugger.log();
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
        statusRegister = UNUSED.bit;

        addressRelative = 0x0000;
        addressAbsolute = 0x0000;
        fetched = 0x00;

        cycles = 8;
    }

    public void interruptRequestSignal() {
        if (getFlag(DISABLE_INTERRUPTS) == 0) {
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

        setFlag(BREAK, false);
        setFlag(UNUSED, true);
        setFlag(DISABLE_INTERRUPTS, true);
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
        int temp = accumulator + fetched + getFlag(CARRY);
        setFlag(CARRY, temp > 255);
        setFlag(ZERO, (temp & 0x00FF) == 0);
        setFlag(NEGATIVE, (temp & 0x80) != 0);
        setFlag(OVERFLOW, ((~(accumulator ^ fetched) & (accumulator ^ temp)) & 0x0080) != 0);
        accumulator = temp & 0x00FF;
        return 1;
    }

    int AND() {
        fetch();
        accumulator = accumulator & fetched;
        setFlag(ZERO, accumulator == 0x00);
        setFlag(NEGATIVE, (accumulator & 0x80) != 0);
        return 1;
    } // "AND" Memory with Accumulator

    int ASL() {
        fetch();
        int temp = fetched << 1;
        setFlag(CARRY, (temp & 0xFF00) > 0);
        setFlag(ZERO, (temp & 0x00FF) == 0x00);
        setFlag(NEGATIVE, (temp & 0x80) != 0);
        if (lookupInstructions[operationCode].isIMP()) {
            accumulator = temp & 0x00FF;
        } else {
            write(addressAbsolute, temp & 0x00FF);
        }
        return 0;
    } // Shift Left One Bit (Memory or Accumulator)

    int BCC() {
        return B(CARRY, false);
    } // Branch on Carry Clear

    int BCS() {
        return B(CARRY, true);
    } // Branch on Carry Set

    private int B(StatusRegister statusRegister, boolean flag ) {
        if (getBooleanFlag(statusRegister) == flag) {
            cycles++;
            addressAbsolute = programCounter + addressRelative;
            if ((addressAbsolute & 0xFF00) != (programCounter & 0xFF00)) {
                cycles++;
            }
            programCounter = addressAbsolute;
        }
        return 0;
    }

    int BEQ() {
        return B(ZERO, true);
    } // Branch on Result Zero

    int BIT() {
        fetch();
        int temp = accumulator & fetched;
        setFlag(ZERO, (temp & 0x00FF) == 0x00);
        setFlag(NEGATIVE, (fetched & (1 << 7)) != 0);
        setFlag(OVERFLOW, (fetched & (1 << 6)) != 0);
        return 0;
    } // Test Bits in Memory with Accumulator

    int BMI() {
        return B(NEGATIVE, true);
    } // Branch on Result Minus

    int BNE() {
        return B(ZERO, false);
    } // Branch on Result not Zero

    int BPL() {
        return B(NEGATIVE, false);
    } // Branch on Result Plus

    int BRK() {
        programCounter++;
        setFlag(DISABLE_INTERRUPTS, true);
        write(0x0100 + stackPointer, (programCounter >> 8) & 0x00FF);
        stackPointer--;
        write(0x0100 + stackPointer, programCounter & 0x00FF);
        stackPointer--;

        setFlag(BREAK, true);
        write(0x0100 + stackPointer, statusRegister);
        setFlag(BREAK, false);

        programCounter = read(0xFFFE) | (read(0xFFFF) << 8);
        return 0;
    } // Force Break

    int BVC() {
        return B(OVERFLOW, false);
    } // Branch on Overflow Clear

    int BVS() {
        return B(OVERFLOW, true);
    } // Branch on Overflow Set

    int CLC() {
        return CL(CARRY);
    } // Clear Carry Flag

    int CLD() {
        return CL(DECIMAL);
    } // Clear Decimal Mode

    int CLI() {
        return CL(DISABLE_INTERRUPTS);
    } // Clear interrupt Disable Bit

    int CLV() {
        return CL(OVERFLOW);
    } // Clear Overflow Flag

    private int CL(StatusRegister statusRegister) {
        setFlag(statusRegister, false);
        return 0;
    }

    int CMP() {
        return CP(accumulator) + 1;
    } // Compare Memory and Accumulator

    int CPX() {
        return CP(xRegister);
    } // Compare Memory and Index X

    int CPY() {
        return CP(yRegister);
    } // Compare Memory and Index Y

    private int CP(int register) {
        fetch();
        int temp = register - fetched;
        setFlag(CARRY, register >= fetched);
        setFlag(ZERO, (temp & 0x00FF) == 0x000);
        setFlag(NEGATIVE, (temp & 0x0080) != 0);
        return 0;
    }

    int DEC() {
        fetch();
        int temp = fetched - 1;
        write(addressAbsolute, temp & 0x00FF);
        setFlag(ZERO, (temp & 0x00FF) == 0x0000);
        setFlag(NEGATIVE, (temp & 0x0080) != 0);
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
        setFlag(ZERO, register == 0x00);
        setFlag(NEGATIVE, (register & 0x80) != 0);
        return 0;
    }

    int EOR() {
        fetch();
        accumulator = accumulator ^ fetched;
        setFlag(ZERO, accumulator == 0x00);
        setFlag(NEGATIVE, (accumulator & 0x80) != 0);
        return 1;
    } // "Exclusive-Or" Memory with Accumulator

    int INC() {
        fetch();
        int temp = fetched + 1;
        write(addressAbsolute, temp & 0x00FF);
        setFlag(ZERO, (temp & 0x00FF) == 0x0000);
        setFlag(NEGATIVE, (temp & 0x0080) != 0);
        return 0;
    } // Increment Memory by One

    int INX() {
        return IN(xRegister);
    } // Increment Index X by One

    int INY() {
        return IN(yRegister);
    } // Increment Index Y by One

    private int IN(int register) {
        setFlag(ZERO, register == 0x00);
        setFlag(NEGATIVE, (register & 0x80) != 0);
        return 0;
    }

    int JMP() {
        programCounter = addressAbsolute;
        return 0;
    } // Jump to New Location

    int JSR() {
        programCounter--;
        write(0x0100 + stackPointer, (programCounter >> 8) & 0x00FF);
        stackPointer--;
        write(0x0100 + stackPointer, programCounter & 0x00FF);
        stackPointer--;
        programCounter = addressAbsolute;
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
        setFlag(ZERO, register == 0x00);
        setFlag(NEGATIVE, (register & 0x80) != 0);
        return 1;
    }

    int LSR() {
        fetch();
        setFlag(CARRY, (fetched & 0x001) != 0);
        int temp = fetched >> 1;
        setFlag(ZERO, (temp & 0x00FF) == 0x0000);
        setFlag(NEGATIVE, (temp & 0x0080) != 0);
        if (lookupInstructions[operationCode].isIMP()) {
            accumulator = temp & 0x00FF;
        } else {
            write(addressAbsolute, temp & 0x00FF);
        }
        return 0;
    } // Shift Right One Bit (Memory or Accumulator)

    int NOP() {
        return switch (operationCode) {
            case 0x1C, 0x3C, 0x5C, 0x7C, 0xDC, 0xFC -> 1;
            default -> 0;
        };
    }// No Operation

    int ORA() {
        fetch();
        accumulator = accumulator | fetched;
        setFlag(ZERO, accumulator == 0x00);
        setFlag(NEGATIVE, (accumulator & 0x80) != 0);
        return 1;
    } // "OR" Memory with Accumulator

    int PHA() {
        write(0x0100 + stackPointer, accumulator);
        stackPointer--;
        return 0;
    } // Push Accumulator on Stack

    int PHP() {
        write(0x0100 + stackPointer, statusRegister | BREAK.bit | UNUSED.bit);
        setFlag(BREAK, false);
        setFlag(UNUSED, false);
        stackPointer--;
        return 0;
    } // Push Processor Status on Stack

    int PLA() {
        stackPointer++;
        accumulator = read(0x0100 + stackPointer);
        setFlag(ZERO, accumulator == 0x00);
        setFlag(NEGATIVE, (accumulator & 0x80) != 0);
        return 0;
    } // Pull Accumulator from Stack

    int PLP() {
        stackPointer++;
        statusRegister = read(0x0100 + stackPointer);
        setFlag(UNUSED, true);
        return 0;
    } // Pull Processor Status from Stack

    int ROL() {
        fetch();
        int temp = (fetched << 1) | getFlag(CARRY);
        setFlag(CARRY, (temp & 0xFF00) != 0);
        setFlag(ZERO, (temp & 0x00FF) == 0x0000);
        setFlag(NEGATIVE, (temp & 0x0080) != 0);
        if (lookupInstructions[operationCode].isIMP())
            accumulator = temp & 0x00FF;
        else
            write(addressAbsolute, temp & 0x00FF);
        return 0;
    } // Rotate One Bit Left (Memory or Accumulator)

    int ROR() {
        fetch();
        int temp = (getFlag(CARRY) << 7) | (fetched >> 1);
        setFlag(CARRY, (fetched & 0x01) != 0);
        setFlag(ZERO, (temp & 0x00FF) == 0x0000);
        setFlag(NEGATIVE, (temp & 0x0080) != 0);
        if (lookupInstructions[operationCode].isIMP())
            accumulator = temp & 0x00FF;
        else
            write(addressAbsolute, temp & 0x00FF);
        return 0;
    } // Rotate One Bit Right (Memory or Accumulator)

    int RTI() {
        stackPointer++;
        statusRegister = read(0x0100 + stackPointer);
        statusRegister &= ~BREAK.bit;
        statusRegister &= ~UNUSED.bit;
        stackPointer++;
        programCounter = read(0x0100 + stackPointer);
        stackPointer++;
        programCounter |= read(0x0100 + stackPointer) << 8;
        return 0;
    } // Return from Interrupt

    int RTS() {
        stackPointer++;
        programCounter = read(0x0100 + stackPointer);
        stackPointer++;
        programCounter |= read(0x0100 + stackPointer) << 8;
        programCounter++;
        return 0;
    } // Return from Subroutine

    int SBC() {
        fetch();
        int value = fetched ^ 0x00FF;
        int temp = accumulator + value + getFlag(CARRY);
        setFlag(CARRY, (temp & 0xFF00) != 0);
        setFlag(ZERO, (temp & 0x00FF) == 0);
        setFlag(OVERFLOW, ((temp ^ accumulator) & (temp ^ value) & 0x0080) != 0);
        setFlag(NEGATIVE, (temp & 0x0080) != 0);
        accumulator = temp & 0x00FF;
        return 1;
    } // Subtract Memory from Accumulator with Borrow

    int SEC() {
        return SE(CARRY);
    } // Set Carry Flag

    int SED() {
        return SE(DECIMAL);
    } // Set Decimal Mode

    int SEI() {
        return SE(DISABLE_INTERRUPTS);
    } // Set Interrupt Disable Status

    private int SE(StatusRegister statusRegister) {
        setFlag(statusRegister, true);
        return 0;
    }

    int STA() {
        return ST(accumulator);
    } // Store Accumulator in Memory

    int STX() {
        return ST(xRegister);
    } // Store Index X in Memory

    int STY() {
        return ST(yRegister);
    } // Store Index Y in Memory

    private int ST(int register) {
        write(addressAbsolute, register);
        return 0;
    }

    int TAX() {
        xRegister = accumulator;
        return TX(xRegister);
    } // Transfer Accumulator to Index X

    int TAY() {
        yRegister = accumulator;
        return TX(yRegister);
    } // Transfer Accumulator to Index Y

    int TSX() {
        xRegister = stackPointer;
        return TX(xRegister);
    } // Transfer Stack Pointer to Index X

    int TXA() {
        accumulator = xRegister;
        return TX(accumulator);
    } // Transfer Index X to Accumulator

    int TXS() {
        stackPointer = xRegister;
        return 0;
    } // Transfer Index X to Stack Pointer

    int TYA() {
        accumulator = yRegister;
        return TX(accumulator);
    } // Transfer Index Y to Accumulator

    private int TX(int register) {
        setFlag(ZERO, register == 0x00);
        setFlag(NEGATIVE, (register & 0x80) != 0);
        return 0;
    }

    int XXX() {
        return 0;
    } // Illegal OperationCode

}
