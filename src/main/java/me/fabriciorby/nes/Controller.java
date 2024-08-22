package me.fabriciorby.nes;

import java.util.EnumMap;

public class Controller {
    public enum Key {
        RIGHT((byte) 0x01),
        LEFT((byte) 0x02),
        DOWN((byte) 0x04),
        UP((byte) 0x08),
        START((byte) 0x10),
        SELECT((byte) 0x20),
        A((byte) 0x40),
        B((byte) 0x80);

        final byte value;

        Key(byte value) {
            this.value = value;
        }
    }

    EnumMap<Key, Boolean> pressedKeys = new EnumMap<>(Key.class);

    public byte getByteCode() {
        return pressedKeys.keySet().stream()
                .map(key -> pressedKeys.get(key) ? key.value : 0x00)
                .reduce((byte) 0x00, (acc, val) -> (byte) (acc | val));
    }

}
