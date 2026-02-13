package com.ryanlin.remapper;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.NumberFormat;

public class AnimatedLabel extends JLabel {
    private Timer timer;

    public AnimatedLabel(String text, int style, int size) {
        super(text);
        setFont(new java.awt.Font("Arial", style, size));
    }

    // 1. THE "NUMBER COUNTER" ANIMATION (0 -> 150,000)
    public void animateCount(long targetValue) {
        setText("0");
        if (timer != null && timer.isRunning()) timer.stop();

        // Duration: 1.5 seconds (1500ms)
        // Frames: 60 fps
        final long startTime = System.currentTimeMillis();
        final long duration = 2000; 

        timer = new Timer(16, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                long now = System.currentTimeMillis();
                float progress = (float)(now - startTime) / duration;

                if (progress >= 1f) {
                    setText(String.format("%,d", targetValue)); // Final formatted value
                    timer.stop();
                } else {
                    // Ease-Out Logic (Starts fast, slows down)
                    // Formula: 1 - (1 - x)^3
                    float ease = 1 - (float)Math.pow(1 - progress, 3);
                    long current = (long)(targetValue * ease);
                    setText(String.format("%,d", current));
                }
            }
        });
        timer.start();
    }

    // 2. THE "TYPEWRITER" ANIMATION (T...y...p...i...n...g)
    public void animateTypewriter(String fullText) {
        setText("");
        if (timer != null && timer.isRunning()) timer.stop();

        timer = new Timer(50, new ActionListener() {
            int index = 0;
            @Override
            public void actionPerformed(ActionEvent e) {
                if (index < fullText.length()) {
                    setText(getText() + fullText.charAt(index));
                    index++;
                } else {
                    timer.stop();
                }
            }
        });
        timer.start();
    }
}