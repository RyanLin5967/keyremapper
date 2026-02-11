import javax.swing.*;
import java.awt.*;

public class KeyButton extends JButton {
    private int keyCode;
    private Color defaultColor; // Stores the "Resting" color (White or Blue)

    public KeyButton(String text, int keyCode, int w, int h) {
        super(text);
        this.keyCode = keyCode;
        this.setPreferredSize(new Dimension(w, h));
        this.setMargin(new Insets(0, 0, 0, 0));
        this.setFocusable(false);
        this.setFont(new Font("SansSerif", Font.BOLD, 10));
        
        // Initialize default
        this.defaultColor = Color.WHITE;
        this.setBackground(defaultColor);
        this.setBorder(BorderFactory.createLineBorder(Color.GRAY));
    }

    @Override
    public void setBackground(Color bg) {
        super.setBackground(bg);
        if (bg != Color.RED && bg != Color.GREEN) {
            this.defaultColor = bg;
        }
    }

    public void setSelectedSource() {
        super.setBackground(Color.RED);
    }

    public void setSelectedDest() {
        super.setBackground(Color.GREEN);
    }

    public void resetColor() {
        super.setBackground(this.defaultColor); // Reverts to Blue (if custom) or White
    }

    public int getKeyCode() {
        return keyCode;
    }
}