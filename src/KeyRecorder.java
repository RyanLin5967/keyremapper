import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

public class KeyRecorder extends JDialog {
    private JLabel statusLabel;
    public List<Integer> result = null;
    private javax.swing.Timer pollingTimer;

    public KeyRecorder(JFrame parent) {
        super(parent, "Record Key Combo", true);
        setSize(400, 200);
        setLayout(new BorderLayout());
        setLocationRelativeTo(parent);

        statusLabel = new JLabel("Press keys (e.g., Win, then Shift, then S)...", SwingConstants.CENTER);
        statusLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
        add(statusLabel, BorderLayout.CENTER);

        JPanel btnPanel = new JPanel();
        JButton clearBtn = new JButton("Clear");
        JButton cancelBtn = new JButton("Cancel");
        JButton doneBtn = new JButton("Done");

        btnPanel.add(clearBtn);
        btnPanel.add(cancelBtn);
        btnPanel.add(doneBtn);
        add(btnPanel, BorderLayout.SOUTH);

        // START RECORDING IN MAIN
        Main.recordingBuffer.clear();
        Main.isRecording = true;

        // Timer to update UI based on Main's buffer
        pollingTimer = new javax.swing.Timer(50, e -> {
            if (!Main.recordingBuffer.isEmpty()) {
                statusLabel.setText("Detected: " + getKeyNames(Main.recordingBuffer));
            } else {
                statusLabel.setText("Press keys...");
            }
        });
        pollingTimer.start();

        // --- BUTTON ACTIONS ---
        
        clearBtn.addActionListener(e -> { 
            Main.recordingBuffer.clear();
            statusLabel.setText("Cleared.");
        });

        cancelBtn.addActionListener(e -> {
            closeDialog(false); // False = Cancelled
        });

        doneBtn.addActionListener(e -> {
            closeDialog(true); // True = Save
        });
 
        // Handle "X" button correctly
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                closeDialog(false);
            }
        });
    }

    private void closeDialog(boolean save) {
        pollingTimer.stop();
        Main.isRecording = false; // Stop the hook from blocking keys
        
        if (save && !Main.recordingBuffer.isEmpty()) {
            result = new ArrayList<>(Main.recordingBuffer);
        } else {
            result = null; // Explicitly null so GUI knows we cancelled
        }
        dispose();
    }

    private String getKeyNames(Set<Integer> codes) {
        StringBuilder sb = new StringBuilder();
        for (int i : codes) sb.append(KeyEvent.getKeyText(i)).append("+");
        if (sb.length() > 0) sb.setLength(sb.length() - 1);
        return sb.toString();
    }
}