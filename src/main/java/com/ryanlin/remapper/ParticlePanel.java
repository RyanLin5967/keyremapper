package com.ryanlin.remapper;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Iterator;

public class ParticlePanel extends JPanel {
    public static final int THEME_DEFAULT = 0;
    public static final int THEME_FIRE = 1;
    public static final int THEME_MATRIX = 2;
    public static final int THEME_GAMER = 3;
    public static final int THEME_RAIN = 4;
    public static final int THEME_GOLD = 5;

    private int currentTheme = THEME_DEFAULT;
    private List<Particle> particles = new ArrayList<>();
    private List<Confetti> confettiList = new ArrayList<>(); 
    private Random random = new Random();
    
    private BufferedImage img1, img2;

    public ParticlePanel() {
        super(new GridBagLayout());
        setBackground(Color.BLACK);
        setOpaque(true);
        for (int i = 0; i < 150; i++) particles.add(new Particle());
    }

    public void setTheme(int theme) {
        this.currentTheme = theme;
        int w = getWidth() > 0 ? getWidth() : 1920;
        int h = getHeight() > 0 ? getHeight() : 1080;
        for (Particle p : particles) p.reset(w, h, theme);
    }
    
    public void setImages(BufferedImage i1, BufferedImage i2) {
        this.img1 = i1;
        this.img2 = i2;
    }
    
    public void spawnConfetti() {
        int w = getWidth();
        int h = getHeight();
        for (int i = 0; i < 200; i++) {
            confettiList.add(new Confetti(w/2, h/2));
        }
    }

    public void updateParticles() {
        int w = getWidth();
        int h = getHeight();
        if (w == 0) return;
        
        for (Particle p : particles) p.update(w, h);
        
        Iterator<Confetti> it = confettiList.iterator();
        while (it.hasNext()) {
            Confetti c = it.next();
            c.update();
            if (c.y > h + 50) it.remove();
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

        // 1. Background
        Color top = Color.BLACK;
        Color bot = new Color(20, 20, 30);
        if (currentTheme == THEME_FIRE) bot = new Color(50, 10, 10);
        if (currentTheme == THEME_MATRIX) bot = new Color(0, 20, 0);
        if (currentTheme == THEME_GAMER) bot = new Color(40, 0, 0); 
        
        GradientPaint gp = new GradientPaint(0, 0, top, 0, getHeight(), bot);
        g2.setPaint(gp);
        g2.fillRect(0, 0, getWidth(), getHeight());

        // 2. Images
        if (img1 != null) {
            double time = System.currentTimeMillis() * 0.001;
            int x = 100 + (int)(Math.sin(time) * 20);
            int y = 200 + (int)(Math.cos(time) * 20);
            g2.drawImage(img1, x, y, 300, 300, null);
        }
        if (img2 != null) {
            double time = System.currentTimeMillis() * 0.001;
            int x = getWidth() - 400 + (int)(Math.cos(time) * 20);
            int y = getHeight() - 400 + (int)(Math.sin(time) * 20);
            g2.drawImage(img2, x, y, 300, 300, null);
        }

        for (Particle p : particles) p.draw(g2);
        
        for (Confetti c : confettiList) c.draw(g2);
    }
    private class Particle {
        float x, y, vx, vy, size;
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
                    size = random.nextInt(8) + 2; shape = 0; break;
                case THEME_MATRIX:
                    vx = 0; vy = random.nextFloat() * 10 + 5;
                    color = new Color(0, 255, 50);
                    size = 16; shape = 2; break;
                case THEME_GAMER:
                    vx = (random.nextFloat() - 0.5f) * 20; vy = (random.nextFloat() - 0.5f) * 20;
                    color = new Color(255, 0, 0); 
                    size = random.nextInt(6) + 3; shape = 1; break;
                case THEME_RAIN:
                    vx = 0; vy = 15;
                    color = new Color(150, 150, 255);
                    size = 2; shape = 0; break;
                default:
                    vx = (random.nextFloat() - 0.5f); vy = (random.nextFloat() - 0.5f);
                    color = new Color(255,255,255);
                    size = random.nextInt(3) + 1; shape = 0;
            }
        }

        public void update(int w, int h) {
            x += vx; y += vy;
            if (x < 0) x = w; if (x > w) x = 0;
            if (y < 0) y = h; if (y > h) y = 0;
        }

        public void draw(Graphics2D g2) {
            g2.setColor(color);
            if (shape == 0) g2.fillOval((int)x, (int)y, (int)size, (int)size);
            else if (shape == 1) g2.fillRect((int)x, (int)y, (int)size, (int)size);
            else if (shape == 2) {
                g2.setFont(new Font("Monospaced", Font.BOLD, (int)size));
                g2.drawString(String.valueOf((char)(random.nextInt(26) + 'A')), x, y);
            }
        }     
    }
    
    private class Confetti {
        float x, y, vx, vy;
        Color color;
        int size;
        
        public Confetti(int startX, int startY) {
            x = startX;
            y = startY;
            vx = (random.nextFloat() - 0.5f) * 55;
            vy = (random.nextFloat() - 0.5f) * 30-20; 
            size = random.nextInt(8);
            color = Color.getHSBColor(random.nextFloat(), 0.8f, 1.0f);
        }
        
        public void update() {
            x += vx;
            y += vy;
            vy += 0.9;
            vx *= 0.95;
        }
        
        public void draw(Graphics2D g2) {
            g2.setColor(color);
            g2.fillRect((int)x, (int)y, size, size);
        }
    }
}