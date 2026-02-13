package com.ryanlin.remapper;

import javax.swing.*;
import java.awt.Color;
import java.awt.event.MouseEvent;

public class KeyButton extends JButton {
    private int keyCode;
    private boolean isSelectedSource = false;
    private boolean isSelectedDest = false;

    public KeyButton(String text, int keyCode, int width, int height) {
        super(text);
        this.keyCode = keyCode;
        
        // 1. Initialize empty to register with ToolTipManager
        this.setToolTipText(""); 

        this.setPreferredSize(new java.awt.Dimension(width, height));
        this.setMargin(new java.awt.Insets(0, 0, 0, 0));
        this.setFocusable(false);
        
        // Default styling
        this.setBackground(null);
        this.setOpaque(true);
        this.setContentAreaFilled(true);
        this.setBorderPainted(true);
    }

    // 2. The Dynamic Text Logic
   @Override
    public String getToolTipText(MouseEvent event) {
        if (!VirtualKeyboard.isHeatmapOn) return null;

        long count;
        String label;

        // Fix: Only use "Shift+" tooltip if this key is actually in the shiftMap (e.g. 1 -> !)
        if (VirtualKeyboard.isShifted && VirtualKeyboard.isKeyShifted(this.keyCode)) {
            String baseName = VirtualKeyboard.getName(this.keyCode);
            String comboKey = "Shift+" + baseName;
            count = HeatmapManager.comboCounts.getOrDefault(comboKey, 0L);
            label = comboKey;
        } else {
            count = HeatmapManager.getCount(this.keyCode);
            label = "Key " + VirtualKeyboard.getName(this.keyCode);
        }
        return label + " | Presses: " + count;
    }

    public int getKeyCode() {
        return keyCode;
    }

    public void resetStyle() {
        isSelectedSource = false; // Unlock
        isSelectedDest = false;   // Unlock
        
        this.setBackground(null);
        this.setForeground(null);
        this.setOpaque(true);
        this.setContentAreaFilled(true);
        this.setBorderPainted(true);
        this.setToolTipText(null); 
        this.repaint();
    }
    
    // ... keep your setSelectedSource / setSelectedDest / resetColor methods ...
    public void setSelectedSource() {
        isSelectedSource = true; // Lock
        this.setBackground(new Color(255, 200, 200)); 
    }
    
    public void setSelectedDest() {
        isSelectedDest = true;
        this.setBackground(new Color(200, 255, 200)); 
    }
    
    public void resetColor() {
        resetStyle();
    }
    public boolean isLocked() {
        return isSelectedSource || isSelectedDest;
    }
}