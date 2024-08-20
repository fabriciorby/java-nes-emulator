package me.fabriciorby.nes.ppu;

import javafx.scene.paint.Color;

import java.util.Arrays;

public class Sprite {
    Color[][] pixelArray;
    int width;
    int height;

    public Sprite(int width, int height) {
        this.pixelArray = new Color[width][height];
        for (Color[] row: pixelArray)
            Arrays.fill(row, Color.BLACK);
        this.width = width;
        this.height = height;
    }

    public void setPixel(int x, int y, Color pixel) {
        if(x >= 0 && x < width && y >= 0 && y < height)
            this.pixelArray[x][y] = pixel;
    }

    public Color[][] getPixelArray() {
        return pixelArray;
    }

    public int getWidth() {
        return this.width;
    }

    public int getHeight() {
        return this.height;
    }
}
