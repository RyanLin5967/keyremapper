import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.HashMap;

public class RemapperGUI extends JFrame implements ActionListener {
    private DefaultTableModel model;
    private JTable table;
    private HashMap<String, Integer> stringToKeyCode;
    private KeyMap keymap = new KeyMap();

    // UI Components
    private JTextField enterKeyToMap = new JTextField(20);
    private JTextField enterKeyToRemap = new JTextField(20);
    private JLabel chooseKey = new JLabel("choose the key to remap");
    private JLabel chooseRemap = new JLabel("choose the key it'll remap to");
    private JButton chooseKeyToRemap = new JButton("submit");

    private VirtualKeyboard virtualKeyboard;
    private int visualStep = 0; // 0: Idle, 1: Source Selected, 2: Dest Selected
    private KeyButton sourceKeyBtn = null;
    private KeyButton destKeyBtn = null;
    private JButton confirmVisualBtn = new JButton("Confirm Mapping");

    public RemapperGUI() {
        // Ensure data is loaded
        CustomKeyManager.load();
        setupData();
        initUI();
        loadExistingMappings();
    }

    private void setupData() {
        stringToKeyCode = new HashMap<>();
        stringToKeyCode.put("space", KeyEvent.VK_SPACE);
        stringToKeyCode.put("tab", KeyEvent.VK_TAB);
        stringToKeyCode.put("caps lock", KeyEvent.VK_CAPS_LOCK);
        stringToKeyCode.put("shift", KeyEvent.VK_SHIFT);
        stringToKeyCode.put("backspace", KeyEvent.VK_BACK_SPACE);
        stringToKeyCode.put("ctrl", KeyEvent.VK_CONTROL); 
        stringToKeyCode.put("enter", 13); 
    }

    private void initUI() {
        setTitle("Keyremapper");
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setUndecorated(true);
        setLayout(new BorderLayout());

        // --- 1. TOP PANEL ---
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        topPanel.setBackground(new Color(240, 240, 240));

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
        });

        confirmVisualBtn.setEnabled(false); 

        topPanel.add(createMapping);
        topPanel.add(removeMapping);
        topPanel.add(removeAllMappings);
        topPanel.add(new JLabel(" | "));
        topPanel.add(addCustomKeyBtn);
        topPanel.add(new JLabel(" | Size: "));
        topPanel.add(layoutSelector);
        topPanel.add(confirmVisualBtn);

        // --- 2. CENTER: Virtual Keyboard ---
        virtualKeyboard = new VirtualKeyboard(this); 
        JScrollPane kbScroll = new JScrollPane(virtualKeyboard);

        // --- 3. EAST: The Table ---
        model = new DefaultTableModel(new String[]{"Key", "Mapped To"}, 0) {
            @Override public boolean isCellEditable(int row, int col) { return false; }
        };
        table = new JTable(model);
        table.setFocusable(false);
        table.setRowSelectionAllowed(true);
        JScrollPane tableScroll = new JScrollPane(table);
        tableScroll.setPreferredSize(new Dimension(300, 0));

        // --- TOP PANEL CONTAINER ---
        JPanel northContainer = new JPanel(new GridLayout(1, 1));
        northContainer.add(topPanel);

        // --- ACTION LISTENERS ---

        addCustomKeyBtn.addActionListener(e -> recordNewCustomKey());

        createMapping.addActionListener(e -> {
            toggleInputFields(true);
            visualStep = 1; 
            JOptionPane.showMessageDialog(this, "Click the physical key you want to change (turns red).");
        });

        confirmVisualBtn.addActionListener(e -> {
            if (sourceKeyBtn != null && destKeyBtn != null) {
                int sourceCode = sourceKeyBtn.getKeyCode();
                
                if (Main.codeToCode.containsKey(sourceCode)) {
                    String keyName = VirtualKeyboard.getName(sourceCode);
                    JOptionPane.showMessageDialog(this, 
                        "The key '" + keyName + "' is already mapped!\nPlease remove the existing mapping from the list before remapping it.", 
                        "Duplicate Mapping Error", JOptionPane.ERROR_MESSAGE);
                    return; 
                }
                executeMapping(sourceKeyBtn.getKeyCode(), destKeyBtn.getKeyCode());
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
                Main.updateTextFile();
            }
        });

        removeAllMappings.addActionListener(e -> {
            Main.codeToCode.clear();
            model.setRowCount(0);
            Main.clearFile();
        });

        add(northContainer, BorderLayout.NORTH);
        add(kbScroll, BorderLayout.CENTER);
        add(tableScroll, BorderLayout.EAST);

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setVisible(true);
    }

    // --- HELPER METHODS ---

    private void recordNewCustomKey() {
        KeyRecorder recorder = new KeyRecorder(this);
        recorder.setVisible(true);
        
        // BUG FIX: Check if recorder.result is null (cancelled)
        if (recorder.result != null && !recorder.result.isEmpty()) {
            String name = JOptionPane.showInputDialog(this, "Name this custom key (e.g. 'Copilot'):");
            if (name != null && !name.trim().isEmpty()) {
                CustomKeyManager.add(name, recorder.result);
                virtualKeyboard.rebuildCustomKeys();
            }
        }
    }

    private void executeMapping(int init, int fin) {
        Main.codeToCode.put(init, fin);
        Main.saveSingleMapping(init, fin);
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
                JOptionPane.showMessageDialog(this, 
                    "The Fn key is handled by your keyboard's hardware.\nIt cannot be remapped by the operating system.", 
                    "Hardware Restriction", JOptionPane.ERROR_MESSAGE);
                return; 
            }
            
            if (visualStep == 1) { 
                if (sourceKeyBtn != null) sourceKeyBtn.resetColor();
                sourceKeyBtn = clickedBtn;
                sourceKeyBtn.setSelectedSource();
                visualStep = 2; 
            } 
            else if (visualStep == 2) { 
                if (clickedBtn == sourceKeyBtn) {
                    sourceKeyBtn.resetColor();
                    sourceKeyBtn = null;
                    if (destKeyBtn != null) { destKeyBtn.resetColor(); destKeyBtn = null; }
                    visualStep = 1; 
                    confirmVisualBtn.setEnabled(false);
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
        if (sourceKeyBtn != null) sourceKeyBtn.resetColor();
        if (destKeyBtn != null) destKeyBtn.resetColor();
        sourceKeyBtn = null;
        destKeyBtn = null;
        visualStep = 0;
        confirmVisualBtn.setEnabled(false);
    }

    private void toggleInputFields(boolean visible) {
        chooseKey.setVisible(visible);
        enterKeyToMap.setVisible(visible);
        chooseRemap.setVisible(visible);
        enterKeyToRemap.setVisible(visible);
        chooseKeyToRemap.setVisible(visible);
    }

    private void loadExistingMappings() {
        try (BufferedReader reader = new BufferedReader(new FileReader("src/mappings.txt"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                String[] codes = line.split(",");
                int init = Integer.parseInt(codes[0]);
                int fin = Integer.parseInt(codes[1]);
                Main.codeToCode.put(init, fin);
                // Use getKeyName to show proper names for custom keys
                model.addRow(new Object[]{ getKeyName(init), getKeyName(fin) });
            }
        } catch (IOException e) { e.printStackTrace(); }
    }
    
    private String getKeyName(int code) {
        if (code >= 10000) {
            CustomKey ck = CustomKeyManager.getByPseudoCode(code);
            return (ck != null) ? ck.getName() : "Unknown";
        }
        return VirtualKeyboard.getName(code);
    }
}