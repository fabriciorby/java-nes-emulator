package me.fabriciorby.nes;

import me.fabriciorby.nes.cartridge.Cartridge;
import me.fabriciorby.nes.cpu.Cpu;
import me.fabriciorby.nes.ppu.Ppu;

public class Bus {

    int clockCounter;

    public Cpu cpu = new Cpu();
    public Ppu ppu = new Ppu();
    public int[] cpuRam = new int[2048];
    private Cartridge cartridge;
    public byte[] controller = new byte[2];
    private byte[] controllerState = new byte[2];

    public Bus() {
        cpu.connectBus(this);
    }

    public void cpuWrite(int address, int data) {
        if (cartridge.cpuCanWrite(address)) {
            cartridge.cpuWrite(address, data);
        } else if (address >= 0x0000 && address <= 0x1FFF) {
            cpuRam[address & 0x07FF] = data;
        } else if (address >= 0x2000 && address <= 0x3FFF) {
            ppu.cpuWrite(address & 0x0007, data);
        } else if (address >= 0x4016 && address <= 0x4017) {
            controllerState[address & 0x0001] = controller[address & 0x0001];
        }
    }

    public int cpuRead(int address, boolean readOnly) {
        if (cartridge.cpuCanRead(address)) {
            return cartridge.cpuRead(address);
        } else if (address >= 0x0000 && address <= 0x1FFF) {
            return cpuRam[address & 0x07FF];
        } else if (address >= 0x2000 && address <= 0x3FFF) {
            return ppu.cpuRead(address & 0x0007, readOnly);
        } else if (address >= 0x4016 && address <= 0x4017) {
            int data = Byte.toUnsignedInt((byte) (controllerState[address & 0x0001] & 0x80)) > 0 ? 1 : 0;
            controllerState[address & 0x0001] <<= 1;
            return data;
        }
        return 0x00;
    }

    public int cpuRead(int address) {
        return this.cpuRead(address, false);
    }

    public void insert(Cartridge cartridge) {
        this.cartridge = cartridge;
        this.ppu.connect(cartridge);
    }

    public void reset() {
//        cartridge.reset();
        cpu.reset();
//        ppu.reset();
        clockCounter = 0;
    }

    public void clock() {
        ppu.clock();
        if (clockCounter % 3 == 0) {
            cpu.clock();
        }

        if (ppu.nonMaskableInterrupt) {
            ppu.nonMaskableInterrupt = false;
            cpu.nonMaskableInterruptRequestSignal();
        }

        clockCounter++;
    }

}
