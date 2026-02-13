package com.ryanlin.remapper;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class ParticlePanel extends JPanel {
    public static final int THEME_DEFAULT = 0;
    public static final int THEME_FIRE = 1;
    public static final int THEME_MATRIX = 2;
    public static final int THEME_GAMER = 3;
    public static final int THEME_RAIN = 4;
    public static final int THEME_GOLD = 5;

    private int currentTheme = THEME_DEFAULT;
    private List<Particle> particles = new ArrayList<>();
    private Random random = new Random();

    public ParticlePanel() {
        // CRITICAL: Set Layout to GridBag so we can center text inside this panel
        super(new GridBagLayout()); 
        
        setBackground(Color.BLACK);
        setOpaque(true); 
        
        // Initialize particles
        for (int i = 0; i < 150; i++) {
            particles.add(new Particle());
        }
    }

    public void setTheme(int theme) {
        this.currentTheme = theme;
        int w = getWidth() > 0 ? getWidth() : 1920;
        int h = getHeight() > 0 ? getHeight() : 1080;
        for (Particle p : particles) p.reset(w, h, theme);
    }

    public void updateParticles() {
        int w = getWidth();
        int h = getHeight();
        if (w == 0) return;
        for (Particle p : particles) p.update(w, h);
    }

    @Override
    protected void paintComponent(Graphics g) {
        // 1. Paint the custom gradient background
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        Color top = Color.BLACK;
        Color bot = new Color(20, 20, 30);
        
        if (currentTheme == THEME_FIRE) bot = new Color(50, 10, 10);
        if (currentTheme == THEME_MATRIX) bot = new Color(0, 20, 0);
        if (currentTheme == THEME_GAMER) bot = new Color(40, 0, 0);
        if (currentTheme == THEME_RAIN) bot = new Color(0, 0, 40);

        GradientPaint gp = new GradientPaint(0, 0, top, 0, getHeight(), bot);
        g2.setPaint(gp);
        g2.fillRect(0, 0, getWidth(), getHeight());

        // 2. Paint Particles
        for (Particle p : particles) {
            p.draw(g2);
        }
        
        // Note: We DO NOT call super.paintComponent(g) here because we just manually filled the screen.
        // Calling it might paint the default grey over our work.
    }

    // --- INNER CLASS ---
    private class Particle {
        float x, y, vx, vy, size, alpha;
        Color color;
        int shape = 0; 

        public Particle() { reset(1920, 1080, THEME_DEFAULT); }

        public void reset(int w, int h, int theme) {
            x = random.nextInt(Math.max(1, w));
            y = random.nextInt(Math.max(1, h));
            switch (theme) {
                case THEME_FIRE:
                    vx = (random.nextFloat() - 0.5f) * 2; vy = -random.nextFloat() * 8 - 2;
                    color = new Color(255, 100 + random.nextInt(100), 0);
                    size = random.nextInt(8) + 2; shape = 0;
                    break;
                case THEME_MATRIX:
                    vx = 0; vy = random.nextFloat() * 10 + 5;
                    color = new Color(0, 255, 50);
                    size = 16; shape = 2;
                    break;
                case THEME_GAMER:
                    vx = (random.nextFloat() - 0.5f) * 20; vy = (random.nextFloat() - 0.5f) * 20;
                    color = new Color(255, 0, 0);
                    size = random.nextInt(6) + 3; shape = 1;
                    break;
                case THEME_RAIN:
                    vx = 0; vy = 15;
                    color = new Color(150, 150, 255);
                    size = 2; shape = 0;
                    break;
                default:
                    vx = (random.nextFloat() - 0.5f); vy = (random.nextFloat() - 0.5f);
                    color = new Color(255,255,255);
                    size = random.nextInt(3) + 1; shape = 0;
            }
            alpha = random.nextFloat();
        }

        public void update(int w, int h) {
            x += vx; y += vy;
            if (x < 0) x = w; if (x > w) x = 0;
            if (y < 0) y = h; if (y > h) y = 0;
        }

        public void draw(Graphics2D g2) {
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
            g2.setColor(color);
            if (shape == 0) g2.fillOval((int)x, (int)y, (int)size, (int)size);
            else if (shape == 1) g2.fillRect((int)x, (int)y, (int)size, (int)size);
            else if (shape == 2) {
                g2.setFont(new Font("Monospaced", Font.BOLD, (int)size));
                g2.drawString(String.valueOf((char)(random.nextInt(26) + 'A')), x, y);
            }
        }
    }
}