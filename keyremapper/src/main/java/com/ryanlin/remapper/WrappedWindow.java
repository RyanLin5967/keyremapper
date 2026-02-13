package com.ryanlin.remapper;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

// SWITCH TO JFRAME (More stable than JDialog for full screen)
public class WrappedWindow extends JFrame {
    private WrappedData data;
    
    // State
    private int currentSlideIndex = 0;
    private int revealStage = 0; 
    private boolean isFinished = false;

    // Visuals
    private ParticlePanel mainPanel; 
    private JLabel mainText;
    private JLabel subText;
    private JLabel extraText;
    private Timer animationTimer;

    public WrappedWindow(Frame owner) {
        super("2026 Wrapped");
        
        // 1. DATA CHECK
        try {
            this.data = WrappedAnalyzer.analyze();
        } catch (Exception e) {
            this.data = new WrappedData(); 
            this.data.topKeyName = "Space";
        }

        // 2. STABLE FULL SCREEN SETUP (The Fix)
        setUndecorated(true); // Remove borders
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        
        // Instead of "setFullScreenWindow", we just maximize it manually.
        // This is 100x more compatible with Swing painting.
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setSize(Toolkit.getDefaultToolkit().getScreenSize());
        setLocation(0,0);
        setAlwaysOnTop(true); // Optional: Keeps it above taskbar

        // 3. SINGLE PANEL ARCHITECTURE
        mainPanel = new ParticlePanel();
        this.setContentPane(mainPanel); // Set directly as content pane

        // 4. ADD COMPONENTS
        initContentComponents();

        // 5. INPUTS
        mainPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                advanceStory();
            }
        });
        
        // Escape Key
        KeyStroke esc = KeyStroke.getKeyStroke("ESCAPE");
        mainPanel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(esc, "close");
        mainPanel.getActionMap().put("close", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });

        // 6. START
        loadSlide(0);
        
        // Animation Loop
        animationTimer = new Timer(16, e -> {
            mainPanel.updateParticles();
            mainPanel.repaint();
        });
        animationTimer.start();
        
        // Force visibility last
        setVisible(true);
    }

    private void initContentComponents() {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0; gbc.gridy = 0; gbc.insets = new Insets(10, 10, 10, 10);
        gbc.anchor = GridBagConstraints.CENTER;

        mainText = new JLabel("", SwingConstants.CENTER);
        mainText.setFont(new Font("Arial", Font.BOLD, 80));
        mainText.setForeground(Color.WHITE);
        mainPanel.add(mainText, gbc);

        gbc.gridy++;
        subText = new JLabel("", SwingConstants.CENTER);
        subText.setFont(new Font("Arial", Font.PLAIN, 40));
        subText.setForeground(Color.LIGHT_GRAY);
        mainPanel.add(subText, gbc);

        gbc.gridy++;
        extraText = new JLabel("", SwingConstants.CENTER);
        extraText.setFont(new Font("Arial", Font.ITALIC, 20));
        extraText.setForeground(Color.GRAY);
        mainPanel.add(extraText, gbc);
    }

    private void advanceStory() {
        if (isFinished) { dispose(); return; }

        if (revealStage == 0) {
            revealStage = 1;
            animateReveal(); 
        } else {
            currentSlideIndex++;
            revealStage = 0;
            loadSlide(currentSlideIndex);
        }
    }

    private void loadSlide(int index) {
        mainText.setText("");
        subText.setText("");
        extraText.setText("");
        mainText.setFont(new Font("Arial", Font.BOLD, 60));
        mainText.setForeground(Color.WHITE);
        mainPanel.setTheme(ParticlePanel.THEME_DEFAULT);

        switch (index) {
            case 0:
                mainText.setText("2026 UNWRAPPED");
                extraText.setText("(Click to start)");
                revealStage = 1; 
                break;
            case 1:
                mainText.setText("It was a busy year.");
                break;
            case 2:
                mainText.setText("One key ruled them all.");
                break;
            case 3:
                mainText.setText("Your Typing Personality...");
                break;
            case 4:
                mainText.setText("See you in 2027.");
                extraText.setText("(Click to close)");
                isFinished = true;
                break;
            default: dispose();
        }
    }

    private void animateReveal() {
        switch (currentSlideIndex) {
            case 1: // Total
                mainPanel.setTheme(ParticlePanel.THEME_RAIN);
                animateCountUp(mainText, data.totalPresses);
                subText.setText("Total Keypresses");
                break;
            case 2: // Top Key
                mainText.setFont(new Font("Arial", Font.BOLD, 150));
                mainText.setForeground(new Color(255, 100, 100)); // Neon Red
                mainText.setText(data.topKeyName);
                subText.setText(data.topKeyCount + " times");
                extraText.setText("That's a lot of " + data.topKeyName + "s.");
                mainPanel.setTheme(ParticlePanel.THEME_FIRE);
                break;
            case 3: // Archetype
                String type = data.archetype;
                mainText.setText(type.toUpperCase());
                subText.setText(data.archetypeDescription);
                if (type.contains("Gamer")) {
                    mainText.setForeground(new Color(50, 255, 50)); 
                    mainPanel.setTheme(ParticlePanel.THEME_GAMER);
                } else if (type.contains("Developer")) {
                    mainText.setForeground(Color.CYAN);
                    mainPanel.setTheme(ParticlePanel.THEME_MATRIX);
                } else {
                    mainText.setForeground(Color.ORANGE);
                    mainPanel.setTheme(ParticlePanel.THEME_GOLD);
                }
                break;
        }
    }

    private void animateCountUp(JLabel label, long target) {
        Timer t = new Timer(20, null);
        long start = System.currentTimeMillis();
        t.addActionListener(e -> {
            long now = System.currentTimeMillis();
            float p = (now - start) / 1000f; 
            if (p >= 1f) {
                label.setText(String.format("%,d", target));
                t.stop();
            } else {
                long val = (long)(target * Math.pow(p, 3)); 
                label.setText(String.format("%,d", val));
            }
        });
        t.start();
    }
    
    // --- MAIN METHOD FOR TESTING ---
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new WrappedWindow(null);
        });
    }
}