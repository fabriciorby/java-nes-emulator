public class Bus {

    public final Cpu cpu = new Cpu();
    public final int[] fakeRam = new int[Memory.RAM_SIZE];

    public Bus() {
        cpu.connectBus(this);
    }

    public void write(int address, int data) {
        if (address >= 0x0000 && address <= 0xFFFF) {
            fakeRam[address] = data;
        }
    }

    public int read(int address, boolean readOnly) {
        if (address >= 0x0000 && address <= 0xFFFF) {
            return fakeRam[address];
        }
        return 0x00;
    }

    public int read(int address) {
        return this.read(address, false);
    }

}
