public class Bus {

    private final Cpu cpu = new Cpu();
    private final int[] fakeRam = new int[Memory.RAM_SIZE];

    public Bus() {
        cpu.connectBus(this);
    }

    public void write(int address, int data) {
        if (address >= 0x0000 && address <= 0xFFFF) {
            fakeRam[address] = data;
        }
    }

    public int read(int address, boolean readOnly) {
        return 0;
    }

    public int read(int address) {
        return this.read(address, false);
    }

}
