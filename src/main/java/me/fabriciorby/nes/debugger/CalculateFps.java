package me.fabriciorby.nes.debugger;

import javafx.scene.control.Label;

public class CalculateFps {
    private final long[] frameTimes = new long[100];
    private final Label label;
    private int frameTimeIndex = 0 ;
    private boolean arrayFilled = false ;

    public CalculateFps(Label label) {
        this.label = label;
    }

    public void calculate(long now) {
        long oldFrameTime = frameTimes[frameTimeIndex] ;
        frameTimes[frameTimeIndex] = now ;
        frameTimeIndex = (frameTimeIndex + 1) % frameTimes.length ;
        if (frameTimeIndex == 0) {
            arrayFilled = true ;
        }
        if (arrayFilled) {
            long elapsedNanos = now - oldFrameTime ;
            long elapsedNanosPerFrame = elapsedNanos / frameTimes.length ;
            double frameRate = 1_000_000_000.0 / elapsedNanosPerFrame ;
            label.setText(String.format("FPS: %.3f", frameRate));
        }
    }
}
