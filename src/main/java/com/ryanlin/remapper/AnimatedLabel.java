package com.ryanlin.remapper;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class AnimatedLabel extends JLabel {
    private Timer timer;

    public AnimatedLabel(String text, int style, int size) {
        super(text);
        setFont(new java.awt.Font("Arial", style, size));
    }

    public void animateCount(long targetValue) {
        setText("0");
        if (timer != null && timer.isRunning()) timer.stop();

        final long startTime = System.currentTimeMillis();
        final long duration = 2000; 

        timer = new Timer(16, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                long now = System.currentTimeMillis();
                float progress = (float)(now - startTime) / duration;

                if (progress >= 1f) {
                    setText(String.format("%,d", targetValue));
                    timer.stop();
                } else {
                    float ease = 1 - (float)Math.pow(1 - progress, 3);
                    long current = (long)(targetValue * ease);
                    setText(String.format("%,d", current));
                }
            }
        });
        timer.start();
    }

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
    public void stopAnimation() {
        if (timer != null && timer.isRunning()) {
            timer.stop();
        }
    }
}