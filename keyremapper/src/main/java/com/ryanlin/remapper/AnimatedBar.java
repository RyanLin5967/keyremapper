package com.ryanlin.remapper;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class AnimatedBar extends JPanel {
    private String label;
    private String valueText;
    private Color barColor;
    private float progress = 0f;
    private Timer timer;

    public AnimatedBar(String label, long value, long maxValue, Color color) {
        this.label = label;
        this.valueText = String.format("%,d", value);
        this.barColor = color;
        
        setOpaque(false);
        setPreferredSize(new Dimension(600, 60)); // Fixed height for each row
        
        // Calculate relative width (so the top key is full width, others are smaller)
        final float targetWidthPercentage = (float) value / (float) maxValue;

        timer = new Timer(15, new ActionListener() {
            long startTime = -1;
            @Override
            public void actionPerformed(ActionEvent e) {
                if (startTime == -1) startTime = System.currentTimeMillis();
                long now = System.currentTimeMillis();
                float duration = 1000f; // 1 second animation
                
                float p = (now - startTime) / duration;
                if (p >= 1f) {
                    progress = targetWidthPercentage;
                    timer.stop();
                } else {
                    // Ease-out
                    progress = targetWidthPercentage * (1 - (float)Math.pow(1 - p, 3));
                }
                repaint();
            }
        });
    }

    public void animate() {
        progress = 0f;
        if (timer != null) timer.start();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int h = getHeight();
        int w = getWidth();
        int barH = 40;
        int y = (h - barH) / 2;

        // Draw Label (Left)
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Arial", Font.BOLD, 20));
        g2.drawString(label, 0, y + 28);

        // Draw Bar Background (Dark Line)
        int startX = 100; // Space for label
        int maxBarW = w - startX - 100; // Space for value
        
        g2.setColor(new Color(50, 50, 60));
        g2.fillRoundRect(startX, y, maxBarW, barH, 10, 10);

        // Draw Animated Bar
        int currentBarW = (int) (maxBarW * progress);
        if (currentBarW > 0) {
            g2.setColor(barColor);
            g2.fillRoundRect(startX, y, currentBarW, barH, 10, 10);
        }

        // Draw Value (Right)
        // Only show value if bar has started growing
        if (progress > 0.01f) {
            g2.setColor(Color.LIGHT_GRAY);
            g2.drawString(valueText, startX + currentBarW + 15, y + 28);
        }
    }
}