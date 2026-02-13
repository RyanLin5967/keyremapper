package com.ryanlin.remapper;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

public class VirtualKeyboard extends JPanel {
    private ActionListener keyListener;
    private static final HashMap<Integer, String> codeToNameMap = new HashMap<>();
    private static ScheduledExecutorService executor = Executors.newScheduledThreadPool(4);

    private Timer refreshTimer;
    private final int UNIT_WIDTH = 45;
    private final int UNIT_HEIGHT = 45;
    private final int FUNCTION_GAP_SIZE = 29;

    private JPanel customKeysPanel;
    private RemapperGUI parentGUI;

    // --- STATE FLAGS ---
    public static boolean isHeatmapOn = false;
    public static boolean isShifted = false; // Tracks if Shift is toggled
    
    // --- SHIFT MAPPING ---
    private static final Map<Integer, String> shiftMap = new HashMap<>();
    static {
        // Map Base ID -> Shifted Symbol
        shiftMap.put(192, "~"); shiftMap.put(49, "!"); shiftMap.put(50, "@"); 
        shiftMap.put(51, "#"); shiftMap.put(52, "$"); shiftMap.put(53, "%"); 
        shiftMap.put(54, "^"); shiftMap.put(55, "&"); shiftMap.put(56, "*"); 
        shiftMap.put(57, "("); shiftMap.put(48, ")"); shiftMap.put(189, "_"); 
        shiftMap.put(187, "+"); 
        
        shiftMap.put(219, "{"); shiftMap.put(221, "}"); shiftMap.put(220, "|");
        shiftMap.put(186, ":"); shiftMap.put(222, "\"");
        shiftMap.put(188, "<"); shiftMap.put(190, ">"); shiftMap.put(191, "?");
    }

    public VirtualKeyboard(RemapperGUI gui) {
        this.keyListener = gui;
        this.parentGUI = gui;
        
        // Timer updates UI every 1 second
        refreshTimer = new Timer(1000, e -> {
            if (isHeatmapOn) {
                repaintHeatmap();
            }
        });
        
        customKeysPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        customKeysPanel.setBorder(BorderFactory.createTitledBorder("Custom Keys"));
        customKeysPanel.setPreferredSize(new Dimension(800, 150)); 
        rebuildCustomKeys();
        render100Percent();
    }

    // --- TOGGLE METHODS ---

    public void toggleHeatmap() {
        isHeatmapOn = !isHeatmapOn;
        if (isHeatmapOn) {
            refreshTimer.start(); 
        } else {
            refreshTimer.stop();  
        }
        repaintHeatmap();
    }
    
    public void toggleShiftMode() {
        isShifted = !isShifted;
        updateKeyLabels(); // Change text (1 -> !)
        repaintHeatmap();  // Change colors (Heatmap for 1 -> Heatmap for !)
    }

    // Updates button text based on Shift state
    private void updateKeyLabels() {
        List<KeyButton> buttons = getAllButtons();
        for (KeyButton btn : buttons) {
            int code = btn.getKeyCode();
            if (isShifted && shiftMap.containsKey(code)) {
                btn.setText(shiftMap.get(code));
            } else {
                // Revert to original name (but keep short version)
                String name = getName(code);
                // Fix: Ensure "Num 1" displays as "1"
                if (name.startsWith("Num ")) {
                    btn.setText(name.substring(4)); 
                } else {
                    btn.setText(name);
                }  
            }
            btn.repaint();
        }
    }

    public void repaintHeatmap() {
    // 1. USE GLOBAL MAX (Fixes "Red Hashtag" issue)
    long maxCount = HeatmapManager.getGlobalMax(); 

    java.util.List<KeyButton> buttons = getAllButtons();

    for (KeyButton btn : buttons) {
        // FIX: If button is locked (Selected for mapping), SKIP IT.
        if (btn.isLocked()) continue;

        if (!isHeatmapOn) {
            btn.resetStyle();
            continue;
        }
        
        btn.setToolTipText(""); 
        btn.setOpaque(true);
        btn.setBorderPainted(true); 
        btn.setContentAreaFilled(true); 

        int code = btn.getKeyCode();
        long count = 0;

        // 2. SMART COUNT LOOKUP
        // Only look up "Shift+" if the key actually changes (1 -> !)
        if (isShifted && shiftMap.containsKey(code)) {
            String baseName = getName(code);
            String comboKey = "Shift+" + baseName;
            count = HeatmapManager.comboCounts.getOrDefault(comboKey, 0L);
        } else {
            count = HeatmapManager.getCount(code);
        }

        float ratio = (maxCount > 0) ? (float) count / (float) maxCount : 0f;
        float hue = 0.66f - (ratio * 0.66f);
        if (hue < 0) hue = 0; 

        Color col = Color.getHSBColor(hue, 0.8f, 0.9f);
        btn.setBackground(col);
        btn.repaint();
    }
    
    this.revalidate();
    this.repaint();
}

    // Helper to get all buttons
    private List<KeyButton> getAllButtons() {
        List<KeyButton> list = new ArrayList<>();
        collectButtons(this, list); 
        return list;
    }   

    private void collectButtons(java.awt.Container container, java.util.List<KeyButton> list) {
        for (java.awt.Component comp : container.getComponents()) {
            if (comp instanceof KeyButton) {
                list.add((KeyButton) comp);
            } else if (comp instanceof java.awt.Container) {
                collectButtons((java.awt.Container) comp, list);
            }
        }
    }
    
    public static String getName(int code) {
        if (code >= 10000) {
            CustomKey ck = CustomKeyManager.getByPseudoCode(code);
            return (ck != null) ? ck.getName() : "Custom(" + code + ")";
        }
        return codeToNameMap.getOrDefault(code, "Key " + code);
    }

    // --- REBUILD & RENDER METHODS ---

    public void rebuildCustomKeys() {
        if (customKeysPanel == null) return;
        customKeysPanel.removeAll();
        List<CustomKey> keys = CustomKeyManager.customKeys;
        for (CustomKey ck : keys) {
            if (ck.isHidden()) continue;
            KeyButton btn = new KeyButton(ck.getName(), ck.getPseudoCode(), UNIT_WIDTH + 20, UNIT_HEIGHT);
            if (isHeatmapOn) btn.setOpaque(true); else btn.setBackground(null);
            btn.addActionListener(keyListener);
            btn.addMouseListener(new MouseAdapter() {
                public void mousePressed(MouseEvent e) {
                    if (SwingUtilities.isRightMouseButton(e)) {
                        JPopupMenu menu = new JPopupMenu();
                        JMenuItem delItem = new JMenuItem("Remove " + ck.getName());
                        delItem.addActionListener(ev -> {
                            int confirm = JOptionPane.showConfirmDialog(VirtualKeyboard.this, 
                                "Delete custom key '" + ck.getName() + "'?", "Confirm", JOptionPane.YES_NO_OPTION);
                            if (confirm == JOptionPane.YES_OPTION) {
                                parentGUI.removeMappingByPseudoCode(ck.getPseudoCode());
                                Main.deleteCustomKey(ck);
                                CustomKeyManager.remove(ck);
                                rebuildCustomKeys();
                                refresh();
                            }
                        });
                        menu.add(delItem);
                        menu.show(btn, e.getX(), e.getY());
                    }
                }
            });
            customKeysPanel.add(btn);
        }
        refresh();
    }

    public void render100Percent() {
        this.removeAll(); setLayout(new BorderLayout()); 
        JPanel mainBoard = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 20));
        JPanel kb = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        JPanel left = new JPanel(); left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS)); 
        left.add(buildFunctionRow()); left.add(buildMainAlphaBlock(110, 90, 115));
        JPanel right = new JPanel(); right.setLayout(new BoxLayout(right, BoxLayout.X_AXIS));
        JPanel navCol = new JPanel(); navCol.setLayout(new BoxLayout(navCol, BoxLayout.Y_AXIS));
        navCol.add(Box.createVerticalStrut(UNIT_HEIGHT + 2)); navCol.add(buildNavCluster6Key());
        navCol.add(Box.createVerticalStrut(UNIT_HEIGHT)); navCol.add(buildArrowCluster());
        JPanel numCol = new JPanel(); numCol.setLayout(new BoxLayout(numCol, BoxLayout.Y_AXIS));
        numCol.add(Box.createVerticalStrut(UNIT_HEIGHT + 2)); numCol.add(buildNumpad()); 
        right.add(navCol); right.add(Box.createHorizontalStrut(10)); right.add(numCol);
        kb.add(left); kb.add(right);
        mainBoard.add(kb);
        add(mainBoard, BorderLayout.NORTH); add(customKeysPanel, BorderLayout.CENTER); 
        refresh();
    }
    
    public void render75Percent() {
        this.removeAll(); setLayout(new BorderLayout());
        JPanel mainBoard = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 20));
        JPanel internal = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        JPanel left = new JPanel(); left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        left.add(buildFunctionRow()); left.add(buildCompactAlphaBlock()); 
        JPanel right = new JPanel(); right.setLayout(new BoxLayout(right, BoxLayout.Y_AXIS));
        right.add(Box.createVerticalStrut(UNIT_HEIGHT + 2)); right.add(buildCompactSideColumn(true)); 
        internal.add(left); internal.add(right);
        mainBoard.add(internal);
        add(mainBoard, BorderLayout.CENTER); add(customKeysPanel, BorderLayout.SOUTH);
        refresh();
    }

    public void render65Percent() {
        this.removeAll(); setLayout(new BorderLayout());
        JPanel mainBoard = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 20));
        JPanel internal = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        JPanel left = new JPanel(); left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        left.add(buildCompactAlphaBlock()); 
        JPanel right = new JPanel(); right.setLayout(new BoxLayout(right, BoxLayout.Y_AXIS));
        right.add(buildCompactSideColumn(false));
        internal.add(left); internal.add(right);
        mainBoard.add(internal);
        add(mainBoard, BorderLayout.CENTER); add(customKeysPanel, BorderLayout.SOUTH);
        refresh();
    }

    // --- BUILDER METHODS ---

    private JPanel buildFunctionRow() {
        JPanel row = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH; gbc.weighty = 1.0; gbc.insets = new Insets(1, 1, 1, 1); 
        String[] labels = {"Esc","F1","F2","F3","F4","F5","F6","F7","F8","F9","F10","F11","F12"};
        int[] codes = {27,112,113,114,115,116,117,118,119,120,121,122,123};
        addFixedBtn(row, labels[0], codes[0], gbc);
        addFixedGap(row, gbc);
        for(int i=1; i<=4; i++) addFixedBtn(row, labels[i], codes[i], gbc);
        addFixedGap(row, gbc);
        for(int i=5; i<=8; i++) addFixedBtn(row, labels[i], codes[i], gbc);
        addFixedGap(row, gbc);
        for(int i=9; i<=12; i++) addFixedBtn(row, labels[i], codes[i], gbc);
        return row;
    }

    private void addFixedBtn(JPanel p, String text, int code, GridBagConstraints gbc) {
        gbc.weightx = 0; 
        codeToNameMap.put(code, text);
        KeyButton btn = new KeyButton(text, code, UNIT_WIDTH, UNIT_HEIGHT);
        btn.setPreferredSize(new Dimension(UNIT_WIDTH, UNIT_HEIGHT));
        btn.addActionListener(keyListener);
        p.add(btn, gbc);
    }

    private void addFixedGap(JPanel p, GridBagConstraints gbc) {
        gbc.weightx = 0; JPanel gap = new JPanel(); gap.setOpaque(false);
        gap.setPreferredSize(new Dimension(FUNCTION_GAP_SIZE, UNIT_HEIGHT)); p.add(gap, gbc);
    }

    private JPanel buildMainAlphaBlock(int tabW, int capsW, int shiftW) {
        JPanel block = new JPanel(new GridLayout(0, 1, 0, 0));
        block.add(createRow(new String[]{"`","1","2","3","4","5","6","7","8","9","0","-","=","Backspace"}, new int[]{192,49,50,51,52,53,54,55,56,57,48,189,187,8}, new int[]{50,50,50,50,50,50,50,50,50,50,50,50,50,100}));
        block.add(createRow(new String[]{"Tab","Q","W","E","R","T","Y","U","I","O","P","[","]","\\"}, new int[]{9,81,87,69,82,84,89,85,73,79,80,219,221,220}, new int[]{75,50,50,50,50,50,50,50,50,50,50,50,50,75}));
        block.add(createRow(new String[]{"Caps","A","S","D","F","G","H","J","K","L",";","'","Enter"}, new int[]{20,65,83,68,70,71,72,74,75,76,186,222,13}, new int[]{90,50,50,50,50,50,50,50,50,50,50,50,110}));
        block.add(createRow(new String[]{"Shift","Z","X","C","V","B","N","M",",",".","/","Shift"}, new int[]{16,90,88,67,86,66,78,77,188,190,191,16}, new int[]{115,50,50,50,50,50,50,50,50,50,50,135}));
        block.add(createRow(new String[]{"Ctrl","Win","Alt","Space","Alt","Fn","Ctrl"}, new int[]{17,524,18,32,18,0,17}, new int[]{75,75,75,300,75,75,75}));
        return block;
    }

    private JPanel buildCompactAlphaBlock() {
        JPanel block = new JPanel(new GridLayout(0, 1, 0, 0));
        block.add(createRow(new String[]{"`","1","2","3","4","5","6","7","8","9","0","-","=","Backspace"}, new int[]{192,49,50,51,52,53,54,55,56,57,48,189,187,8}, new int[]{50,50,50,50,50,50,50,50,50,50,50,50,50,100}));
        block.add(createRow(new String[]{"Tab","Q","W","E","R","T","Y","U","I","O","P","[","]","\\"}, new int[]{9,81,87,69,82,84,89,85,73,79,80,219,221,220}, new int[]{75,50,50,50,50,50,50,50,50,50,50,50,50,75}));
        block.add(createRow(new String[]{"Caps","A","S","D","F","G","H","J","K","L",";","'","Enter"}, new int[]{20,65,83,68,70,71,72,74,75,76,186,222,13}, new int[]{90,50,50,50,50,50,50,50,50,50,50,50,110}));
        block.add(createRow(new String[]{"Shift","Z","X","C","V","B","N","M",",",".","/","Shift","▲"}, new int[]{16,90,88,67,86,66,78,77,188,190,191,16,38}, new int[]{115,50,50,50,50,50,50,50,50,50,50,85,50}));
        JPanel bottomRow = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH; gbc.weighty = 1.0; gbc.insets = new Insets(1, 1, 1, 1);
        String[] leftLabels = {"Ctrl","Windows","Alt","Space","Alt","Fn"};
        int[] leftCodes = {17,524,18,32,18,0};
        int[] leftWidths = {75,75,75,275,75,75}; 
        for(int i=0; i<leftLabels.length; i++) {
            codeToNameMap.put(leftCodes[i], leftLabels[i]);
            gbc.weightx = (double)leftWidths[i] / 50.0;
            int px = (int)((double)leftWidths[i]/50.0 * UNIT_WIDTH);
            KeyButton btn = new KeyButton(leftLabels[i], leftCodes[i], px, UNIT_HEIGHT);
            btn.addActionListener(keyListener);
            bottomRow.add(btn, gbc);
        }
        String[] arrowLabels = {"◄","▼"};
        int[] arrowCodes = {37, 40};
        for(int i=0; i<arrowLabels.length; i++) {
            codeToNameMap.put(arrowCodes[i], arrowLabels[i]);
            gbc.weightx = 1.0; 
            KeyButton btn = new KeyButton(arrowLabels[i], arrowCodes[i], UNIT_WIDTH, UNIT_HEIGHT);
            btn.addActionListener(keyListener);
            bottomRow.add(btn, gbc);
        }
        block.add(bottomRow);
        return block;
    }

    private JPanel buildCompactSideColumn(boolean is75Percent) {
        JPanel col = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.insets = new Insets(1, 0, 1, 1); 
        String[] labels; int[] codes;
        if (is75Percent) {
             labels = new String[]{"Home", "PgUp", "PgDn", "End", "►"};
             codes = new int[]{36, 33, 34, 35, 39};
        } else {
             labels = new String[]{"Delete", "PgUp", "PgDn", "End", "►"};
             codes = new int[]{127, 33, 34, 35, 39};
        }
        for(int i=0; i<labels.length; i++) {
            gbc.gridy = i;
            codeToNameMap.put(codes[i], labels[i]);
            KeyButton btn = new KeyButton(labels[i], codes[i], UNIT_WIDTH, UNIT_HEIGHT);
            btn.addActionListener(keyListener);
            col.add(btn, gbc);
        }
        return col;
    }

    private JPanel buildNumpad() {
        JPanel pad = new JPanel(new GridBagLayout());
        // Fix: Use Numpad names so they don't collide with Top Row
        addGBCButton(pad, "Num", 144, 0, 0, 1, 1);
        addGBCButton(pad, "/", 111, 1, 0, 1, 1);
        addGBCButton(pad, "*", 106, 2, 0, 1, 1);
        addGBCButton(pad, "-", 109, 3, 0, 1, 1);
        addGBCButton(pad, "7", 103, 0, 1, 1, 1);
        addGBCButton(pad, "8", 104, 1, 1, 1, 1);
        addGBCButton(pad, "9", 105, 2, 1, 1, 1);
        addGBCButton(pad, "+", 107, 3, 1, 1, 2); 
        addGBCButton(pad, "4", 100, 0, 2, 1, 1);
        addGBCButton(pad, "5", 101, 1, 2, 1, 1);
        addGBCButton(pad, "6", 102, 2, 2, 1, 1);
        addGBCButton(pad, "1", 97, 0, 3, 1, 1);
        addGBCButton(pad, "2", 98, 1, 3, 1, 1);
        addGBCButton(pad, "3", 99, 2, 3, 1, 1);
        addGBCButton(pad, "Enter", 13, 3, 3, 1, 2); 
        addGBCButton(pad, "0", 96, 0, 4, 2, 1); 
        addGBCButton(pad, ".", 110, 2, 4, 1, 1);
        
        // FIX: RENAME NUMPAD KEYS INTERNALLY to avoid collision with top row "1"
        codeToNameMap.put(97, "Num 1"); codeToNameMap.put(98, "Num 2"); codeToNameMap.put(99, "Num 3");
        codeToNameMap.put(100, "Num 4"); codeToNameMap.put(101, "Num 5"); codeToNameMap.put(102, "Num 6");
        codeToNameMap.put(103, "Num 7"); codeToNameMap.put(104, "Num 8"); codeToNameMap.put(105, "Num 9");
        codeToNameMap.put(96, "Num 0");
        return pad;
    }

    private void addGBCButton(JPanel p, String text, int code, int x, int y, int w, int h) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = x; c.gridy = y; c.gridwidth = w; c.gridheight = h;
        c.fill = GridBagConstraints.BOTH; c.weightx = 1.0; c.weighty = 1.0; c.insets = new Insets(1,1,1,1);
        codeToNameMap.put(code, text);
        int pxWidth = w * UNIT_WIDTH; 
        int pxHeight = h * UNIT_HEIGHT;
        KeyButton btn = new KeyButton(text, code, pxWidth, pxHeight);
        btn.addActionListener(keyListener); 
        p.add(btn, c);
    }

    private JPanel buildNavCluster6Key() {
        JPanel cluster = new JPanel(new GridLayout(2, 1, 0, 0));
        cluster.add(createRow(new String[]{"Insert","Home","PgUp"}, new int[]{45,36,33}, null));
        cluster.add(createRow(new String[]{"Delete","End","PgDn"}, new int[]{46,35,34}, null));
        return cluster;
    }

    private JPanel buildArrowCluster() {
        JPanel cluster = new JPanel(new GridLayout(2, 1, 0, 0));
        JPanel upRow = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH; gbc.weightx = 1.0; gbc.weighty = 1.0;
        upRow.add(Box.createHorizontalGlue(), gbc);
        KeyButton upBtn = new KeyButton("▲", 38, UNIT_WIDTH, UNIT_HEIGHT);
        upBtn.addActionListener(keyListener);
        codeToNameMap.put(38, "Up");
        gbc.weightx = 0; upRow.add(upBtn, gbc);   
        gbc.weightx = 1.0; upRow.add(Box.createHorizontalGlue(), gbc);
        cluster.add(upRow);
        cluster.add(createRow(new String[]{"◄","▼","►"}, new int[]{37,40,39}, null));
        return cluster;
    }

    private JPanel createRow(String[] labels, int[] codes, int[] widths) {
        JPanel row = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH; gbc.weighty = 1.0; gbc.insets = new Insets(1, 1, 1, 1);
        for (int i = 0; i < labels.length; i++) {
            int code = codes[i];
            String label = labels[i];
            codeToNameMap.put(code, label);
            int wValue = (widths != null && i < widths.length) ? widths[i] : 50;
            gbc.weightx = (double) wValue / 50.0;
            int calculatedWidth = (int)((double)wValue/50.0 * UNIT_WIDTH);
            KeyButton btn = new KeyButton(label, code, calculatedWidth, UNIT_HEIGHT);
            btn.addActionListener(keyListener);
            row.add(btn, gbc);
        }
        return row;
    } 

    private void refresh() {
        // Appended: Update labels based on shift state
        updateKeyLabels();
        
        if (isHeatmapOn) {
            repaintHeatmap(); 
        }
        revalidate();
        repaint();
    }
    public static boolean isKeyShifted(int code) {
    return shiftMap.containsKey(code);
    }
    
}