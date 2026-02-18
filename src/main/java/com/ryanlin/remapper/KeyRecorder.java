package com.ryanlin.remapper;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

public class KeyRecorder extends JDialog {
    private JLabel statusLabel;
    public List<Integer> result = null;
    private javax.swing.Timer pollingTimer;
    private List<Integer> latchedKeys = new ArrayList<>();
    private boolean allKeysReleased = true;

    public KeyRecorder(JFrame parent) {
        super(parent, "Record Key Combo/Custom Key", true);
        setSize(400, 200);
        setLayout(new BorderLayout());
        setLocationRelativeTo(parent);

        statusLabel = new JLabel("Press keys...", SwingConstants.CENTER);
        statusLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
        add(statusLabel, BorderLayout.CENTER);

        JPanel btnPanel = new JPanel();
        JButton clearBtn = new JButton("Clear");
        clearBtn.setFocusable(false);
        JButton cancelBtn = new JButton("Cancel");
        cancelBtn.setFocusable(false);
        JButton doneBtn = new JButton("Done");
        doneBtn.setFocusable(false);
        btnPanel.add(clearBtn); 
        btnPanel.add(cancelBtn); 
        btnPanel.add(doneBtn);
        add(btnPanel, BorderLayout.SOUTH);
  
        Main.recordingBuffer.clear();
        Main.isRecording = true;

        // In case user tabs out
        addWindowFocusListener(new WindowAdapter() {
            @Override
            public void windowLostFocus(WindowEvent e) {
                Main.isRecording = false;
                statusLabel.setText("Paused (Click window to resume)");
            }

            @Override
            public void windowGainedFocus(WindowEvent e) { 
                Main.isRecording = true;
                if (latchedKeys.isEmpty()) {  
                    statusLabel.setText("Press keys...");
                } else {
                    statusLabel.setText("Detected: " + getKeyNames(latchedKeys));
                }
            }
        });

        pollingTimer = new javax.swing.Timer(30, e -> {
            Set<Integer> currentPresses = new HashSet<>(Main.recordingBuffer);

            if (currentPresses.isEmpty()) {
                allKeysReleased = true;
                if (latchedKeys.isEmpty() && Main.isRecording) statusLabel.setText("Press keys...");
            } else {
                if (allKeysReleased) {
                    latchedKeys.clear();
                    allKeysReleased = false;
                }

                List<Integer> newKeys = new ArrayList<>();
                for (Integer code : currentPresses) {
                    if (!latchedKeys.contains(code)) newKeys.add(code);
                }

                newKeys.sort((k1, k2) -> {
                     if (isModifier(k1) && !isModifier(k2)) return -1;
                     if (!isModifier(k1) && isModifier(k2)) return 1;
                     return 0;
                });
                
                latchedKeys.addAll(newKeys);

                if (latchedKeys.contains(134)) {
                    latchedKeys.removeIf(k -> k == 16 || k == 160 || k == 161 || 
                                              k == 524 || k == 91 || k == 92);   
                }

                statusLabel.setText("Detected: " + getKeyNames(latchedKeys));
            }
        });
        pollingTimer.start();

        clearBtn.addActionListener(e -> { 
            Main.recordingBuffer.clear(); 
            latchedKeys.clear(); 
            allKeysReleased = true; 
            statusLabel.setText("Cleared."); 
        });
        cancelBtn.addActionListener(e -> closeDialog(false));
        doneBtn.addActionListener(e -> closeDialog(true));
        
        addWindowListener(new WindowAdapter() { 
            public void windowClosing(WindowEvent e) { 
                closeDialog(false); 
            }
        });
    }

    private void closeDialog(boolean save) {
        pollingTimer.stop();
        Main.isRecording = false; 
        Main.recordingBuffer.clear();
        result = (save && !latchedKeys.isEmpty()) ? new ArrayList<>(latchedKeys) : null;
        dispose();
    }
    
    private boolean isModifier(int c) { return (c >= 16 && c <= 18) || (c >= 160 && c <= 165) || c == 524 || c == 91 || c == 92; }

    private String getKeyNames(List<Integer> codes) {
        if (codes.isEmpty()) return "";
        return codes.stream().map(c -> (c == 134) ? "Copilot" : KeyEvent.getKeyText(c == 13 ? 10 : c))
                             .collect(Collectors.joining(" + "));
    }
}