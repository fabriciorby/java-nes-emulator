import java.util.HexFormat;
import java.util.stream.IntStream;

public class Main {
    // Load Program (assembled at https://www.masswerk.at/6502/assembler.html)
		/*
			*=$8000
			LDX #10
			STX $0000
			LDX #3
			STX $0001
			LDY $0000
			LDA #0
			CLC
			loop
			ADC $0001
			DEY
			BNE loop
			STA $0002
			NOP
			NOP
			NOP
		*/

    // Convert hex string into bytes for RAM
    static byte[] program = HexFormat.ofDelimiter(" ").parseHex("A2 0A 8E 00 00 A2 03 8E 01 00 AC 00 00 A9 00 18 6D 01 00 88 D0 FA 8D 02 00 EA EA EA");
    static Bus nes = new Bus();

    public static void main(String... args) {
        int nOffset = 0x8000;

        for (byte b : program) {
            nes.fakeRam[nOffset++] = Byte.toUnsignedInt(b);
        }

        nes.fakeRam[0xFFFC] = 0x00;
        nes.fakeRam[0xFFFD] = 0x80;

        nes.cpu.reset();

        IntStream.range(0, 130)
                .forEach( i -> nes.cpu.clock());
    }

}
