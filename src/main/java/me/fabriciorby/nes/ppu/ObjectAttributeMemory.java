package me.fabriciorby.nes.ppu;

public class ObjectAttributeMemory {

    final int MEMORY_ENTRIES = 64;

    public static class MemoryEntry {
        public int y;
        public int id;
        public int attribute;
        public int x;
    }

    final int POSSIBLE_ADDRESSES = MEMORY_ENTRIES*4;

    public MemoryEntry[] memoryEntries = new MemoryEntry[MEMORY_ENTRIES];
    {
        for(int i = 0; i < MEMORY_ENTRIES ; i++)
        {
            memoryEntries[i] = new MemoryEntry();
        }
    }

    private int address = 0x00;

    public void setAddress(int address) {
        if (address > POSSIBLE_ADDRESSES) {
            throw new RuntimeException("Invalid: Address is too high");
        } else {
            this.address = address;
        }
    }

    public MemoryEntry getObject(int index) {
        var copy = new MemoryEntry();
        copy.y = memoryEntries[index].y;
        copy.id = memoryEntries[index].id;
        copy.attribute = memoryEntries[index].attribute;
        copy.x = memoryEntries[index].x;
        return copy;
    }

    public int getData(int address) {
        int index = address / 4;
        return switch (address % 4) {
            case 0 -> memoryEntries[index].y;
            case 1 -> memoryEntries[index].id;
            case 2 -> memoryEntries[index].attribute;
            case 3 -> memoryEntries[index].x;
            default -> 0;
        };
    }

    public int getData() {
        return getData(address);
    }

    public void setData(int data) {
        setData(data, address);
    }

    public void setData(int data, int address) {
        int index = address / 4;
        switch (address % 4) {
            case 0 -> memoryEntries[index].y = data;
            case 1 -> memoryEntries[index].id = data;
            case 2 -> memoryEntries[index].attribute = data;
            case 3 -> memoryEntries[index].x = data;
            default -> {}
        };
    }
}