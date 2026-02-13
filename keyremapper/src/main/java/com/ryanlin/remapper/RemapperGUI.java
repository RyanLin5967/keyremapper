package com.ryanlin.remapper;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RemapperGUI extends JFrame implements ActionListener {
    private DefaultTableModel model;
    private JTable table;
    private KeyMap keymap = new KeyMap();

    private JTextField enterKeyToMap = new JTextField(20);
    private JTextField enterKeyToRemap = new JTextField(20);
    private JLabel chooseKey = new JLabel("choose the key to remap");
    private JLabel chooseRemap = new JLabel("choose the key it'll remap to");
    private JButton chooseKeyToRemap = new JButton("submit");

    private VirtualKeyboard virtualKeyboard;
    private int visualStep = 0; // 0: Idle, 1: Source Selected, 2: Destination Selected
    private KeyButton sourceKeyBtn = null;
    private KeyButton destKeyBtn = null;
    private JButton confirmVisualBtn = new JButton("Confirm Mapping");
    
    public RemapperGUI() {
        // ConfigManager.load() was already called in Main, so data is ready
        
        initUI();
        loadExistingMappings(); // Now loads from memory
        pack();
        setLocationRelativeTo(null);
    }

    private void initUI() {
        setTitle("Keyremapper");
        setUndecorated(false);    
        setLayout(new BorderLayout());    
      

        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        topPanel.setBackground(null);

        JButton heatmapBtn = new JButton("Show Heatmap");
        heatmapBtn.setFocusable(false);

        heatmapBtn.addActionListener(e -> {
            virtualKeyboard.toggleHeatmap();
            if (VirtualKeyboard.isHeatmapOn == true) {
                heatmapBtn.setText("Hide Heatmap");
            } else {
                heatmapBtn.setText("Show Heatmap");
            }
        });

        topPanel.add(heatmapBtn);

        JButton shiftToggleBtn = new JButton("Shift");
        shiftToggleBtn.setFocusable(false);
                        
        shiftToggleBtn.addActionListener(e -> {
            virtualKeyboard.toggleShiftMode();
            
            // Update button text to show state
            if (VirtualKeyboard.isShifted) {
                shiftToggleBtn.setText("Unshift");
            } else {
                shiftToggleBtn.setText("Shift");
                shiftToggleBtn.setBackground(null);
            }
        });

        topPanel.add(shiftToggleBtn);
        JButton wrappedBtn = new JButton("2026 Wrapped");
        wrappedBtn.setFocusable(false);
        wrappedBtn.setBackground(null); // Distinct Green Color

        wrappedBtn.addActionListener(e -> {
            // Open the Wrapped Window
            WrappedWindow wrapped = new WrappedWindow((JFrame) SwingUtilities.getWindowAncestor(this));
            wrapped.setVisible(true);
        });

        // Add it to your topPanel or wherever you keep the main buttons
        topPanel.add(wrappedBtn);
        JButton createMapping = new JButton("create keymap");
        JButton removeMapping = new JButton("remove selected mapping");
        JButton removeAllMappings = new JButton("remove all mappings");
        JButton addCustomKeyBtn = new JButton("Add Custom Key");

        createMapping.setFocusable(false);
        removeMapping.setFocusable(false);
        removeAllMappings.setFocusable(false);
        addCustomKeyBtn.setFocusable(false);

        String[] layoutOptions = {"100%", "75%", "65%"};
        JComboBox<String> layoutSelector = new JComboBox<>(layoutOptions);
        layoutSelector.addActionListener(e -> {
            String selected = (String) layoutSelector.getSelectedItem();
            if ("100%".equals(selected)) virtualKeyboard.render100Percent();
            else if ("75%".equals(selected)) virtualKeyboard.render75Percent();
            else if ("65%".equals(selected)) virtualKeyboard.render65Percent();
            if (VirtualKeyboard.isHeatmapOn == true) {
                heatmapBtn.setText("Hide Heatmap");
            } else {
                heatmapBtn.setText("Show Heatmap");
            }
        });
        layoutSelector.setFocusable(false);

        confirmVisualBtn.setEnabled(false); 

        topPanel.add(createMapping);
        topPanel.add(removeMapping);
        topPanel.add(removeAllMappings);
        topPanel.add(new JLabel("  "));
        topPanel.add(addCustomKeyBtn);
        topPanel.add(new JLabel(" Size: "));
        topPanel.add(layoutSelector);
        topPanel.add(confirmVisualBtn);

        virtualKeyboard = new VirtualKeyboard(this); 
        JScrollPane kbScroll = new JScrollPane(virtualKeyboard);

        model = new DefaultTableModel(new String[]{"Key", "Mapped To"}, 0) {
            @Override public boolean isCellEditable(int row, int col) { return false; }
        };
        table = new JTable(model);
        table.setFocusable(false);
        table.setRowSelectionAllowed(true);
        JScrollPane tableScroll = new JScrollPane(table);
        tableScroll.setPreferredSize(new Dimension(300, 0));

        JPanel northContainer = new JPanel(new GridLayout(1, 1));
        northContainer.add(topPanel);

        addCustomKeyBtn.addActionListener(e -> recordNewCustomKey());

        createMapping.addActionListener(e -> {
            toggleInputFields(true);
            visualStep = 1; 
        });

        confirmVisualBtn.addActionListener(e -> {
            if (sourceKeyBtn != null && destKeyBtn != null) {
                
                int finalSourceCode;
                
                // CHECK IF WE ARE MAPPING A SHIFT-COMBO
                Boolean isShifted = (Boolean) sourceKeyBtn.getClientProperty("isShiftedCombo");
                if (isShifted != null && isShifted) {
                    // Use the Custom Key ID (e.g. 90001) we found earlier
                    finalSourceCode = (int) sourceKeyBtn.getClientProperty("shiftedPseudoCode");
                } else {
                    // Use normal ID (e.g. 49)
                    finalSourceCode = sourceKeyBtn.getKeyCode();
                }

                int destCode = destKeyBtn.getKeyCode();

                if (Main.codeToCode.containsKey(finalSourceCode)) {
                    JOptionPane.showMessageDialog(this, "This mapping already exists. Remove it first.");
                    return; 
                }

                executeMapping(finalSourceCode, destCode);
                resetVisualSelection();
            }
        });

        removeMapping.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row != -1) {
                String keyNameInTable = (String) table.getValueAt(row, 0);
                Main.codeToCode.entrySet().removeIf(entry -> 
                    getKeyName(entry.getKey()).equals(keyNameInTable)
                );
                model.removeRow(row);
                ConfigManager.save(); // Save to JSON
            }
        });

        removeAllMappings.addActionListener(e -> {
            Main.codeToCode.clear();
            model.setRowCount(0);
            ConfigManager.save(); // Save to JSON
        });

        add(northContainer, BorderLayout.NORTH);
        add(kbScroll, BorderLayout.CENTER);
        add(tableScroll, BorderLayout.EAST);

        setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
        setVisible(true);
    }

    private void recordNewCustomKey() {
        KeyRecorder recorder = new KeyRecorder(this);
        recorder.setVisible(true);

        if (recorder.result != null && !recorder.result.isEmpty()) {
            
            // --- CHECK 1: DUPLICATE COMBINATION ---
            if (CustomKeyManager.isCombinationTaken(recorder.result)) {
                JOptionPane.showMessageDialog(this, 
                    "This key combination is already used by another Custom Key!", 
                    "Duplicate Combination", 
                    JOptionPane.ERROR_MESSAGE);
                return; 
            }

            String name = JOptionPane.showInputDialog(this, "Name this custom key (e.g. 'Copilot'):");
            
            if (name != null && !name.trim().isEmpty()) {
                name = name.trim();

                // --- CHECK 2: DUPLICATE NAME ---
                if (CustomKeyManager.isNameTaken(name)) {
                    JOptionPane.showMessageDialog(this, 
                        "A Custom Key with this name already exists!", 
                        "Duplicate Name", 
                        JOptionPane.ERROR_MESSAGE);
                    return;
                }

                // Create and Save (calls ConfigManager internally)
                CustomKeyManager.add(name, recorder.result);
                
                virtualKeyboard.rebuildCustomKeys();
            }
        }
    }

    private void executeMapping(int init, int fin) {
        Main.codeToCode.put(init, fin);
        ConfigManager.save(); // Save to JSON
        
        String nameInit = getKeyName(init);
        String nameFin = getKeyName(fin);

        model.addRow(new Object[]{ nameInit, nameFin });
        keymap.clear();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() instanceof KeyButton) {
            KeyButton clickedBtn = (KeyButton) e.getSource();
            int keyCode = clickedBtn.getKeyCode();

            if (keyCode == 0) {
                JOptionPane.showMessageDialog(this, "The Fn key cannot be remapped.", "Error", JOptionPane.ERROR_MESSAGE);
                return; 
            }
            
            // --- STEP 1: SOURCE SELECTION ---
            if (visualStep == 1) { 
                if (sourceKeyBtn != null) sourceKeyBtn.resetColor();
                sourceKeyBtn = clickedBtn;
                
                // CHECK FOR SHIFT-COMBO MAPPING (Silent Logic)
                if (VirtualKeyboard.isShifted && VirtualKeyboard.isKeyShifted(keyCode)) {
                    String baseName = VirtualKeyboard.getName(keyCode);
                    String customName = "Shift+" + baseName;
                    
                    java.util.List<Integer> combo = new java.util.ArrayList<>();
                    combo.add(16); // Shift
                    combo.add(keyCode);

                    CustomKey existing = null;
                    for (CustomKey ck : CustomKeyManager.customKeys) {
                        if (ck.matches(new java.util.HashSet<>(combo))) {
                            existing = ck;
                            break;                                                                                                                         
                        }                                  
                    }

                    if (existing == null) {                                                                                                 
                        // Create the key                                                                                                                                                                
                        CustomKeyManager.add(customName, combo);
                        
                        // Retrieve it
                        existing = CustomKeyManager.getByPseudoCode(
                            CustomKeyManager.customKeys.get(CustomKeyManager.customKeys.size()-1).getPseudoCode()
                        );
                        
                        // FIX: MARK IT AS HIDDEN
                        existing.setHidden(true);
                        
                        // We still call rebuild, but now it won't show up!
                        virtualKeyboard.rebuildCustomKeys();
                    }                                                          
                                                                                        
                    // ... rest of logic attaches the ID to the button ...
                    sourceKeyBtn.putClientProperty("isShiftedCombo", true);
                    sourceKeyBtn.putClientProperty("shiftedPseudoCode", existing.getPseudoCode());
                }

                sourceKeyBtn.setSelectedSource();
                visualStep = 2; 
            } 
            // --- STEP 2: DESTINATION SELECTION ---
            else if (visualStep == 2) { 
                if (clickedBtn == sourceKeyBtn) {
                    resetVisualSelection();
                    return; 
                }
                if (destKeyBtn != null) destKeyBtn.resetColor();
                destKeyBtn = clickedBtn;
                destKeyBtn.setSelectedDest();
                confirmVisualBtn.setEnabled(true);
            }
        }
    }

    private void resetVisualSelection() {
    if (sourceKeyBtn != null) sourceKeyBtn.resetStyle();
    if (destKeyBtn != null) destKeyBtn.resetStyle();
    
    sourceKeyBtn = null;
    destKeyBtn = null;
    visualStep = 0;
    confirmVisualBtn.setEnabled(false);

    if (VirtualKeyboard.isHeatmapOn) {
        virtualKeyboard.repaintHeatmap();
    }
}

    private void toggleInputFields(boolean visible) {
        chooseKey.setVisible(visible);
        enterKeyToMap.setVisible(visible);
        chooseRemap.setVisible(visible);
        enterKeyToRemap.setVisible(visible);
        chooseKeyToRemap.setVisible(visible);
    }

    private void loadExistingMappings() {
        // Updated: Iterates over memory (Main.codeToCode) instead of reading a file
        for (Map.Entry<Integer, Integer> entry : Main.codeToCode.entrySet()) {
            int init = entry.getKey();
            int fin = entry.getValue();
            model.addRow(new Object[]{ getKeyName(init), getKeyName(fin) });
        }
    }
    
    private String getKeyName(int code) {
        if (code >= 10000) {
            CustomKey ck = CustomKeyManager.getByPseudoCode(code);
            return (ck != null) ? ck.getName() : "Unknown";
        }
        return VirtualKeyboard.getName(code);
    }
    
    public void removeMappingByPseudoCode(int customKeyCode) {
        String keyName = VirtualKeyboard.getName(customKeyCode);
        for (int i = model.getRowCount() - 1; i >= 0; i--) {
            String rowKey = (String) model.getValueAt(i, 0);
            String rowMap = (String) model.getValueAt(i, 1);
            
            if (rowKey.equals(keyName) || rowMap.equals(keyName)) {
                model.removeRow(i);
            }
        }
        
        Main.codeToCode.entrySet().removeIf(entry -> 
            entry.getKey() == customKeyCode || entry.getValue() == customKeyCode
        );
        
        ConfigManager.save(); // Save to JSON
    }
}