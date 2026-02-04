import com.sun.jna.*;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef.*;
import com.sun.jna.platform.win32.WinUser;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

import java.util.HashMap;
import java.util.Map;

import java.util.ArrayList;

import com.github.kwhat.jnativehook.GlobalScreen;
import com.github.kwhat.jnativehook.NativeHookException;
import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent;
import com.github.kwhat.jnativehook.keyboard.NativeKeyListener;

//MAKE UI LOOK VERY GOOD
//save everything into a text file so it will all reload once you close
//how to ignore the keypress of the user that he wants to remap??
public class Main implements NativeKeyListener{
    
    static HashMap<Integer, Integer> codeToCode = new HashMap<>();

    //maybe make it return string
    public void nativeKeyPressed(NativeKeyEvent e){
        if (codeToCode.containsKey(e.getKeyCode())){
            
        }
        //use esc key to stop program
        if (e.getKeyCode() == NativeKeyEvent.VC_ESCAPE){
            try {
                GlobalScreen.unregisterNativeHook();
            } catch (NativeHookException ex){
                ex.printStackTrace();
            }
        }
    }

    public void nativeKeyReleased(NativeKeyEvent e) {
		System.out.println("Key Released: " + NativeKeyEvent.getKeyText(e.getKeyCode()));
	}

	public void nativeKeyTyped(NativeKeyEvent e) {
        //System.out.println("Key Typed: " + e.getRawCode()); for testing 
        if (e.getRawCode() >= 65 && e.getRawCode() <=90){
            System.out.println("Key Typed: " + Character.toUpperCase((char)e.getRawCode()));
        // have to manually map space + backspace since it will return "Undefined" otherwise
        }else if(e.getRawCode() == 32) {
            System.out.println("Key Typed: "+ "Space");
        }else if(e.getRawCode() == 8){
            System.out.println("Key Typed: "+ "Backspace");
        }
	}
    public static void main(String[] args){
        //also need to allow users to put their unique keycodes
        //or allow users to see their keycodes
        //don't forget to clear keymap object after
        
        KeyMap keymap = new KeyMap();
        HashMap<String, Integer> stringToKeyCode = new HashMap<>();
        stringToKeyCode.put("space", KeyEvent.VK_SPACE);
        stringToKeyCode.put("tab", KeyEvent.VK_TAB);
        stringToKeyCode.put("caps lock",KeyEvent.VK_CAPS_LOCK);
        stringToKeyCode.put("shift",KeyEvent.VK_SHIFT);
        stringToKeyCode.put("backspace",KeyEvent.VK_BACK_SPACE);
        stringToKeyCode.put("ctrl", KeyEvent.VK_CONTROL);
        //stringToKeyCode.put("fn")


        JFrame frame = new JFrame("Keyremapper");
        frame.setExtendedState(JFrame.MAXIMIZED_BOTH);

        JButton chooseKeyToMap = new JButton("submit");
        JLabel chooseKey = new JLabel("choose the key to remap");
        JTextField enterKeyToMap = new JTextField(20);

        JButton chooseKeyToRemap = new JButton("submit");
        JLabel chooseRemap = new JLabel("choose the key it'll remap to");
        JTextField enterKeyToRemap = new JTextField(20);

        chooseKey.setVisible(false);
        chooseKeyToMap.setVisible(false);
        enterKeyToMap.setVisible(false);

        chooseKeyToRemap.setVisible(false);
        chooseRemap.setVisible(false);
        enterKeyToRemap.setVisible(false);

        JButton createMapping = new JButton("create keymap");
        frame.add(createMapping);
        createMapping.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e){
                chooseKey.setVisible(true);
                chooseKeyToMap.setVisible(true);
                enterKeyToMap.setVisible(true);
            }
        });
        //allow user to get the key they want to map
        chooseKeyToMap.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent e){
                String text = enterKeyToMap.getText();
                enterKeyToMap.setText("");
                if (stringToKeyCode.containsKey(text)){
                    int code = stringToKeyCode.get(text);
                    keymap.setInitKeyCode(code);
                    chooseRemap.setVisible(true);
                    enterKeyToRemap.setVisible(true);
                    chooseKeyToRemap.setVisible(true);
                }else{
                    enterKeyToMap.setText("Error in registering the key you want. ensure it is spelled correctly with spaces if nessesary");
                }
            }
        });
        
        chooseKeyToRemap.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent e){
                String text = enterKeyToRemap.getText();
                enterKeyToRemap.setText("");
                if(stringToKeyCode.containsKey(text)){
                    int code = stringToKeyCode.get(text);
                    keymap.setFinalKeyCode(code);
                    codeToCode.put(keymap.getInitKeyCode(), keymap.getFinalKeyCode());
                    keymap.clear();
                }else{
                    enterKeyToMap.setText("Error in registering the key you want. ensure it is spelled correctly with spaces if nessesary");
                }
            }
        });
        frame.add(chooseKey);
        frame.add(chooseKeyToMap);
        frame.add(enterKeyToMap);

        frame.add(chooseRemap);
        frame.add(chooseKeyToRemap);
        frame.add(enterKeyToRemap);
        
        frame.setLayout(new FlowLayout());
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setUndecorated(true);
        frame.setVisible(true);
        try{
            Thread.sleep(1000);
        } catch(InterruptedException e){
            System.err.println("sleep error");
        }
        try{
            Robot bot = new Robot();
            
        } catch(AWTException ex){
            System.err.println("error in initializing robot");
            ex.printStackTrace();
        }
        
        try{
            GlobalScreen.registerNativeHook();
        } catch(NativeHookException ex){
            System.err.println("error registering native hook");
            ex.printStackTrace();
        }
        GlobalScreen.addNativeKeyListener(new Main());
    }
    
}
