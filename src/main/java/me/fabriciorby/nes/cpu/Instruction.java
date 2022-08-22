package me.fabriciorby.nes.cpu;

import java.util.function.Supplier;
import java.util.stream.IntStream;

public record Instruction(String operationName, Supplier<Integer> operation, String addressingModeName,
                          Supplier<Integer> addressingMode, int totalCycles) {

    public static Instruction[] getInstructions(Cpu cpu) {
        return new Instruction[] {
                new Instruction("BRK", cpu::BRK, "IMM", cpu::IMM, 7), new Instruction("ORA", cpu::ORA, "IZX", cpu::IZX, 6), new Instruction("???", cpu::XXX, "IMP", cpu::IMP, 2), new Instruction("???", cpu::XXX, "IMP", cpu::IMP, 8), new Instruction("???", cpu::NOP, "IMP", cpu::IMP, 3), new Instruction("ORA", cpu::ORA, "ZP0", cpu::ZP0, 3), new Instruction("ASL", cpu::ASL, "ZP0", cpu::ZP0, 5), new Instruction("???", cpu::XXX, "IMP", cpu::IMP, 5), new Instruction("PHP", cpu::PHP, "IMP", cpu::IMP, 3), new Instruction("ORA", cpu::ORA, "IMM", cpu::IMM, 2), new Instruction("ASL", cpu::ASL, "IMP", cpu::IMP, 2), new Instruction("???", cpu::XXX, "IMP", cpu::IMP, 2), new Instruction("???", cpu::NOP, "IMP", cpu::IMP, 4), new Instruction("ORA", cpu::ORA, "ABS", cpu::ABS, 4), new Instruction("ASL", cpu::ASL, "ABS", cpu::ABS, 6), new Instruction("???", cpu::XXX, "IMP", cpu::IMP, 6),
                new Instruction("BPL", cpu::BPL, "REL", cpu::REL, 2), new Instruction("ORA", cpu::ORA, "IZY", cpu::IZY, 5), new Instruction("???", cpu::XXX, "IMP", cpu::IMP, 2), new Instruction("???", cpu::XXX, "IMP", cpu::IMP, 8), new Instruction("???", cpu::NOP, "IMP", cpu::IMP, 4), new Instruction("ORA", cpu::ORA, "ZPX", cpu::ZPX, 4), new Instruction("ASL", cpu::ASL, "ZPX", cpu::ZPX, 6), new Instruction("???", cpu::XXX, "IMP", cpu::IMP, 6), new Instruction("CLC", cpu::CLC, "IMP", cpu::IMP, 2), new Instruction("ORA", cpu::ORA, "ABY", cpu::ABY, 4), new Instruction("???", cpu::NOP, "IMP", cpu::IMP, 2), new Instruction("???", cpu::XXX, "IMP", cpu::IMP, 7), new Instruction("???", cpu::NOP, "IMP", cpu::IMP, 4), new Instruction("ORA", cpu::ORA, "ABX", cpu::ABX, 4), new Instruction("ASL", cpu::ASL, "ABX", cpu::ABX, 7), new Instruction("???", cpu::XXX, "IMP", cpu::IMP, 7),
                new Instruction("JSR", cpu::JSR, "ABS", cpu::ABS, 6), new Instruction("AND", cpu::AND, "IZX", cpu::IZX, 6), new Instruction("???", cpu::XXX, "IMP", cpu::IMP, 2), new Instruction("???", cpu::XXX, "IMP", cpu::IMP, 8), new Instruction("BIT", cpu::BIT, "ZP0", cpu::ZP0, 3), new Instruction("AND", cpu::AND, "ZP0", cpu::ZP0, 3), new Instruction("ROL", cpu::ROL, "ZP0", cpu::ZP0, 5), new Instruction("???", cpu::XXX, "IMP", cpu::IMP, 5), new Instruction("PLP", cpu::PLP, "IMP", cpu::IMP, 4), new Instruction("AND", cpu::AND, "IMM", cpu::IMM, 2), new Instruction("ROL", cpu::ROL, "IMP", cpu::IMP, 2), new Instruction("???", cpu::XXX, "IMP", cpu::IMP, 2), new Instruction("BIT", cpu::BIT, "ABS", cpu::ABS, 4), new Instruction("AND", cpu::AND, "ABS", cpu::ABS, 4), new Instruction("ROL", cpu::ROL, "ABS", cpu::ABS, 6), new Instruction("???", cpu::XXX, "IMP", cpu::IMP, 6),
                new Instruction("BMI", cpu::BMI, "REL", cpu::REL, 2), new Instruction("AND", cpu::AND, "IZY", cpu::IZY, 5), new Instruction("???", cpu::XXX, "IMP", cpu::IMP, 2), new Instruction("???", cpu::XXX, "IMP", cpu::IMP, 8), new Instruction("???", cpu::NOP, "IMP", cpu::IMP, 4), new Instruction("AND", cpu::AND, "ZPX", cpu::ZPX, 4), new Instruction("ROL", cpu::ROL, "ZPX", cpu::ZPX, 6), new Instruction("???", cpu::XXX, "IMP", cpu::IMP, 6), new Instruction("SEC", cpu::SEC, "IMP", cpu::IMP, 2), new Instruction("AND", cpu::AND, "ABY", cpu::ABY, 4), new Instruction("???", cpu::NOP, "IMP", cpu::IMP, 2), new Instruction("???", cpu::XXX, "IMP", cpu::IMP, 7), new Instruction("???", cpu::NOP, "IMP", cpu::IMP, 4), new Instruction("AND", cpu::AND, "ABX", cpu::ABX, 4), new Instruction("ROL", cpu::ROL, "ABX", cpu::ABX, 7), new Instruction("???", cpu::XXX, "IMP", cpu::IMP, 7),
                new Instruction("RTI", cpu::RTI, "IMP", cpu::IMP, 6), new Instruction("EOR", cpu::EOR, "IZX", cpu::IZX, 6), new Instruction("???", cpu::XXX, "IMP", cpu::IMP, 2), new Instruction("???", cpu::XXX, "IMP", cpu::IMP, 8), new Instruction("???", cpu::NOP, "IMP", cpu::IMP, 3), new Instruction("EOR", cpu::EOR, "ZP0", cpu::ZP0, 3), new Instruction("LSR", cpu::LSR, "ZP0", cpu::ZP0, 5), new Instruction("???", cpu::XXX, "IMP", cpu::IMP, 5), new Instruction("PHA", cpu::PHA, "IMP", cpu::IMP, 3), new Instruction("EOR", cpu::EOR, "IMM", cpu::IMM, 2), new Instruction("LSR", cpu::LSR, "IMP", cpu::IMP, 2), new Instruction("???", cpu::XXX, "IMP", cpu::IMP, 2), new Instruction("JMP", cpu::JMP, "ABS", cpu::ABS, 3), new Instruction("EOR", cpu::EOR, "ABS", cpu::ABS, 4), new Instruction("LSR", cpu::LSR, "ABS", cpu::ABS, 6), new Instruction("???", cpu::XXX, "IMP", cpu::IMP, 6),
                new Instruction("BVC", cpu::BVC, "REL", cpu::REL, 2), new Instruction("EOR", cpu::EOR, "IZY", cpu::IZY, 5), new Instruction("???", cpu::XXX, "IMP", cpu::IMP, 2), new Instruction("???", cpu::XXX, "IMP", cpu::IMP, 8), new Instruction("???", cpu::NOP, "IMP", cpu::IMP, 4), new Instruction("EOR", cpu::EOR, "ZPX", cpu::ZPX, 4), new Instruction("LSR", cpu::LSR, "ZPX", cpu::ZPX, 6), new Instruction("???", cpu::XXX, "IMP", cpu::IMP, 6), new Instruction("CLI", cpu::CLI, "IMP", cpu::IMP, 2), new Instruction("EOR", cpu::EOR, "ABY", cpu::ABY, 4), new Instruction("???", cpu::NOP, "IMP", cpu::IMP, 2), new Instruction("???", cpu::XXX, "IMP", cpu::IMP, 7), new Instruction("???", cpu::NOP, "IMP", cpu::IMP, 4), new Instruction("EOR", cpu::EOR, "ABX", cpu::ABX, 4), new Instruction("LSR", cpu::LSR, "ABX", cpu::ABX, 7), new Instruction("???", cpu::XXX, "IMP", cpu::IMP, 7),
                new Instruction("RTS", cpu::RTS, "IMP", cpu::IMP, 6), new Instruction("ADC", cpu::ADC, "IZX", cpu::IZX, 6), new Instruction("???", cpu::XXX, "IMP", cpu::IMP, 2), new Instruction("???", cpu::XXX, "IMP", cpu::IMP, 8), new Instruction("???", cpu::NOP, "IMP", cpu::IMP, 3), new Instruction("ADC", cpu::ADC, "ZP0", cpu::ZP0, 3), new Instruction("ROR", cpu::ROR, "ZP0", cpu::ZP0, 5), new Instruction("???", cpu::XXX, "IMP", cpu::IMP, 5), new Instruction("PLA", cpu::PLA, "IMP", cpu::IMP, 4), new Instruction("ADC", cpu::ADC, "IMM", cpu::IMM, 2), new Instruction("ROR", cpu::ROR, "IMP", cpu::IMP, 2), new Instruction("???", cpu::XXX, "IMP", cpu::IMP, 2), new Instruction("JMP", cpu::JMP, "IND", cpu::IND, 5), new Instruction("ADC", cpu::ADC, "ABS", cpu::ABS, 4), new Instruction("ROR", cpu::ROR, "ABS", cpu::ABS, 6), new Instruction("???", cpu::XXX, "IMP", cpu::IMP, 6),
                new Instruction("BVS", cpu::BVS, "REL", cpu::REL, 2), new Instruction("ADC", cpu::ADC, "IZY", cpu::IZY, 5), new Instruction("???", cpu::XXX, "IMP", cpu::IMP, 2), new Instruction("???", cpu::XXX, "IMP", cpu::IMP, 8), new Instruction("???", cpu::NOP, "IMP", cpu::IMP, 4), new Instruction("ADC", cpu::ADC, "ZPX", cpu::ZPX, 4), new Instruction("ROR", cpu::ROR, "ZPX", cpu::ZPX, 6), new Instruction("???", cpu::XXX, "IMP", cpu::IMP, 6), new Instruction("SEI", cpu::SEI, "IMP", cpu::IMP, 2), new Instruction("ADC", cpu::ADC, "ABY", cpu::ABY, 4), new Instruction("???", cpu::NOP, "IMP", cpu::IMP, 2), new Instruction("???", cpu::XXX, "IMP", cpu::IMP, 7), new Instruction("???", cpu::NOP, "IMP", cpu::IMP, 4), new Instruction("ADC", cpu::ADC, "ABX", cpu::ABX, 4), new Instruction("ROR", cpu::ROR, "ABX", cpu::ABX, 7), new Instruction("???", cpu::XXX, "IMP", cpu::IMP, 7),
                new Instruction("???", cpu::NOP, "IMP", cpu::IMP, 2), new Instruction("STA", cpu::STA, "IZX", cpu::IZX, 6), new Instruction("???", cpu::NOP, "IMP", cpu::IMP, 2), new Instruction("???", cpu::XXX, "IMP", cpu::IMP, 6), new Instruction("STY", cpu::STY, "ZP0", cpu::ZP0, 3), new Instruction("STA", cpu::STA, "ZP0", cpu::ZP0, 3), new Instruction("STX", cpu::STX, "ZP0", cpu::ZP0, 3), new Instruction("???", cpu::XXX, "IMP", cpu::IMP, 3), new Instruction("DEY", cpu::DEY, "IMP", cpu::IMP, 2), new Instruction("???", cpu::NOP, "IMP", cpu::IMP, 2), new Instruction("TXA", cpu::TXA, "IMP", cpu::IMP, 2), new Instruction("???", cpu::XXX, "IMP", cpu::IMP, 2), new Instruction("STY", cpu::STY, "ABS", cpu::ABS, 4), new Instruction("STA", cpu::STA, "ABS", cpu::ABS, 4), new Instruction("STX", cpu::STX, "ABS", cpu::ABS, 4), new Instruction("???", cpu::XXX, "IMP", cpu::IMP, 4),
                new Instruction("BCC", cpu::BCC, "REL", cpu::REL, 2), new Instruction("STA", cpu::STA, "IZY", cpu::IZY, 6), new Instruction("???", cpu::XXX, "IMP", cpu::IMP, 2), new Instruction("???", cpu::XXX, "IMP", cpu::IMP, 6), new Instruction("STY", cpu::STY, "ZPX", cpu::ZPX, 4), new Instruction("STA", cpu::STA, "ZPX", cpu::ZPX, 4), new Instruction("STX", cpu::STX, "ZPY", cpu::ZPY, 4), new Instruction("???", cpu::XXX, "IMP", cpu::IMP, 4), new Instruction("TYA", cpu::TYA, "IMP", cpu::IMP, 2), new Instruction("STA", cpu::STA, "ABY", cpu::ABY, 5), new Instruction("TXS", cpu::TXS, "IMP", cpu::IMP, 2), new Instruction("???", cpu::XXX, "IMP", cpu::IMP, 5), new Instruction("???", cpu::NOP, "IMP", cpu::IMP, 5), new Instruction("STA", cpu::STA, "ABX", cpu::ABX, 5), new Instruction("???", cpu::XXX, "IMP", cpu::IMP, 5), new Instruction("???", cpu::XXX, "IMP", cpu::IMP, 5),
                new Instruction("LDY", cpu::LDY, "IMM", cpu::IMM, 2), new Instruction("LDA", cpu::LDA, "IZX", cpu::IZX, 6), new Instruction("LDX", cpu::LDX, "IMM", cpu::IMM, 2), new Instruction("???", cpu::XXX, "IMP", cpu::IMP, 6), new Instruction("LDY", cpu::LDY, "ZP0", cpu::ZP0, 3), new Instruction("LDA", cpu::LDA, "ZP0", cpu::ZP0, 3), new Instruction("LDX", cpu::LDX, "ZP0", cpu::ZP0, 3), new Instruction("???", cpu::XXX, "IMP", cpu::IMP, 3), new Instruction("TAY", cpu::TAY, "IMP", cpu::IMP, 2), new Instruction("LDA", cpu::LDA, "IMM", cpu::IMM, 2), new Instruction("TAX", cpu::TAX, "IMP", cpu::IMP, 2), new Instruction("???", cpu::XXX, "IMP", cpu::IMP, 2), new Instruction("LDY", cpu::LDY, "ABS", cpu::ABS, 4), new Instruction("LDA", cpu::LDA, "ABS", cpu::ABS, 4), new Instruction("LDX", cpu::LDX, "ABS", cpu::ABS, 4), new Instruction("???", cpu::XXX, "IMP", cpu::IMP, 4),
                new Instruction("BCS", cpu::BCS, "REL", cpu::REL, 2), new Instruction("LDA", cpu::LDA, "IZY", cpu::IZY, 5), new Instruction("???", cpu::XXX, "IMP", cpu::IMP, 2), new Instruction("???", cpu::XXX, "IMP", cpu::IMP, 5), new Instruction("LDY", cpu::LDY, "ZPX", cpu::ZPX, 4), new Instruction("LDA", cpu::LDA, "ZPX", cpu::ZPX, 4), new Instruction("LDX", cpu::LDX, "ZPY", cpu::ZPY, 4), new Instruction("???", cpu::XXX, "IMP", cpu::IMP, 4), new Instruction("CLV", cpu::CLV, "IMP", cpu::IMP, 2), new Instruction("LDA", cpu::LDA, "ABY", cpu::ABY, 4), new Instruction("TSX", cpu::TSX, "IMP", cpu::IMP, 2), new Instruction("???", cpu::XXX, "IMP", cpu::IMP, 4), new Instruction("LDY", cpu::LDY, "ABX", cpu::ABX, 4), new Instruction("LDA", cpu::LDA, "ABX", cpu::ABX, 4), new Instruction("LDX", cpu::LDX, "ABY", cpu::ABY, 4), new Instruction("???", cpu::XXX, "IMP", cpu::IMP, 4),
                new Instruction("CPY", cpu::CPY, "IMM", cpu::IMM, 2), new Instruction("CMP", cpu::CMP, "IZX", cpu::IZX, 6), new Instruction("???", cpu::NOP, "IMP", cpu::IMP, 2), new Instruction("???", cpu::XXX, "IMP", cpu::IMP, 8), new Instruction("CPY", cpu::CPY, "ZP0", cpu::ZP0, 3), new Instruction("CMP", cpu::CMP, "ZP0", cpu::ZP0, 3), new Instruction("DEC", cpu::DEC, "ZP0", cpu::ZP0, 5), new Instruction("???", cpu::XXX, "IMP", cpu::IMP, 5), new Instruction("INY", cpu::INY, "IMP", cpu::IMP, 2), new Instruction("CMP", cpu::CMP, "IMM", cpu::IMM, 2), new Instruction("DEX", cpu::DEX, "IMP", cpu::IMP, 2), new Instruction("???", cpu::XXX, "IMP", cpu::IMP, 2), new Instruction("CPY", cpu::CPY, "ABS", cpu::ABS, 4), new Instruction("CMP", cpu::CMP, "ABS", cpu::ABS, 4), new Instruction("DEC", cpu::DEC, "ABS", cpu::ABS, 6), new Instruction("???", cpu::XXX, "IMP", cpu::IMP, 6),
                new Instruction("BNE", cpu::BNE, "REL", cpu::REL, 2), new Instruction("CMP", cpu::CMP, "IZY", cpu::IZY, 5), new Instruction("???", cpu::XXX, "IMP", cpu::IMP, 2), new Instruction("???", cpu::XXX, "IMP", cpu::IMP, 8), new Instruction("???", cpu::NOP, "IMP", cpu::IMP, 4), new Instruction("CMP", cpu::CMP, "ZPX", cpu::ZPX, 4), new Instruction("DEC", cpu::DEC, "ZPX", cpu::ZPX, 6), new Instruction("???", cpu::XXX, "IMP", cpu::IMP, 6), new Instruction("CLD", cpu::CLD, "IMP", cpu::IMP, 2), new Instruction("CMP", cpu::CMP, "ABY", cpu::ABY, 4), new Instruction("NOP", cpu::NOP, "IMP", cpu::IMP, 2), new Instruction("???", cpu::XXX, "IMP", cpu::IMP, 7), new Instruction("???", cpu::NOP, "IMP", cpu::IMP, 4), new Instruction("CMP", cpu::CMP, "ABX", cpu::ABX, 4), new Instruction("DEC", cpu::DEC, "ABX", cpu::ABX, 7), new Instruction("???", cpu::XXX, "IMP", cpu::IMP, 7),
                new Instruction("CPX", cpu::CPX, "IMM", cpu::IMM, 2), new Instruction("SBC", cpu::SBC, "IZX", cpu::IZX, 6), new Instruction("???", cpu::NOP, "IMP", cpu::IMP, 2), new Instruction("???", cpu::XXX, "IMP", cpu::IMP, 8), new Instruction("CPX", cpu::CPX, "ZP0", cpu::ZP0, 3), new Instruction("SBC", cpu::SBC, "ZP0", cpu::ZP0, 3), new Instruction("INC", cpu::INC, "ZP0", cpu::ZP0, 5), new Instruction("???", cpu::XXX, "IMP", cpu::IMP, 5), new Instruction("INX", cpu::INX, "IMP", cpu::IMP, 2), new Instruction("SBC", cpu::SBC, "IMM", cpu::IMM, 2), new Instruction("NOP", cpu::NOP, "IMP", cpu::IMP, 2), new Instruction("???", cpu::SBC, "IMP", cpu::IMP, 2), new Instruction("CPX", cpu::CPX, "ABS", cpu::ABS, 4), new Instruction("SBC", cpu::SBC, "ABS", cpu::ABS, 4), new Instruction("INC", cpu::INC, "ABS", cpu::ABS, 6), new Instruction("???", cpu::XXX, "IMP", cpu::IMP, 6),
                new Instruction("BEQ", cpu::BEQ, "REL", cpu::REL, 2), new Instruction("SBC", cpu::SBC, "IZY", cpu::IZY, 5), new Instruction("???", cpu::XXX, "IMP", cpu::IMP, 2), new Instruction("???", cpu::XXX, "IMP", cpu::IMP, 8), new Instruction("???", cpu::NOP, "IMP", cpu::IMP, 4), new Instruction("SBC", cpu::SBC, "ZPX", cpu::ZPX, 4), new Instruction("INC", cpu::INC, "ZPX", cpu::ZPX, 6), new Instruction("???", cpu::XXX, "IMP", cpu::IMP, 6), new Instruction("SED", cpu::SED, "IMP", cpu::IMP, 2), new Instruction("SBC", cpu::SBC, "ABY", cpu::ABY, 4), new Instruction("NOP", cpu::NOP, "IMP", cpu::IMP, 2), new Instruction("???", cpu::XXX, "IMP", cpu::IMP, 7), new Instruction("???", cpu::NOP, "IMP", cpu::IMP, 4), new Instruction("SBC", cpu::SBC, "ABX", cpu::ABX, 4), new Instruction("INC", cpu::INC, "ABX", cpu::ABX, 7), new Instruction("???", cpu::XXX, "IMP", cpu::IMP, 7),
        };
    }

    public int runAndGetCycles() {
        return IntStream.of(this.addressingMode.get(), this.operation.get(), this.totalCycles).sum();
    }

    boolean isIMP() {
        return this.addressingModeName.equals("IMP");
    }

    public String getName() {
        return this.operationName;
    }
}