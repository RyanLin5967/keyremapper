package com.ryanlin.remapper;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef.*;
import com.sun.jna.platform.win32.WinUser;
import com.sun.jna.platform.win32.WinUser.HHOOK;
import com.sun.jna.platform.win32.WinUser.KBDLLHOOKSTRUCT;
import com.sun.jna.platform.win32.WinUser.LowLevelKeyboardProc;
import com.sun.jna.platform.win32.WinUser.MSG;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.List;
import java.util.concurrent.*;

import com.formdev.flatlaf.FlatDarkLaf; 


public class Main {
    private static HHOOK hhk;
    private static LowLevelKeyboardProc keyboardHook;
    
    static Robot robot;
    static HashMap<Integer, Integer> codeToCode = new HashMap<>();
    
    private static Set<Integer> heldKeys = new HashSet<>();

    private static ConcurrentHashMap<Integer, ScheduledFuture<?>> activeRepeaters = new ConcurrentHashMap<>();
    private static ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(4);

    private static CustomKey activeCustomKey = null;
    private static int activeTargetCode = -1;
    public static volatile boolean isRecording = false;

    public static LinkedHashSet<Integer> recordingBuffer = new LinkedHashSet<>();
    private static int activeCustomTriggerKey = -1;
    
    // modifiers
    private static final Map<Integer, Integer> windowsToJavaModifiers = new HashMap<>();
    static {
        windowsToJavaModifiers.put(0xA0, KeyEvent.VK_SHIFT);   // VK_LSHIFT
        windowsToJavaModifiers.put(0xA1, KeyEvent.VK_SHIFT);   // VK_RSHIFT
        windowsToJavaModifiers.put(0xA2, KeyEvent.VK_CONTROL); // VK_LCONTROL
        windowsToJavaModifiers.put(0xA3, KeyEvent.VK_CONTROL); // VK_RCONTROL
        windowsToJavaModifiers.put(0xA4, KeyEvent.VK_ALT);     // VK_LMENU
        windowsToJavaModifiers.put(0xA5, KeyEvent.VK_ALT);     // VK_RMENU
        windowsToJavaModifiers.put(0x5B, KeyEvent.VK_WINDOWS); // VK_LWIN
        windowsToJavaModifiers.put(0x5C, KeyEvent.VK_WINDOWS); // VK_RWIN
    }

    public interface Win32User32 extends Library {
        Win32User32 INSTANCE = Native.load("user32", Win32User32.class);
        void keybd_event(byte bVk, byte bScan, int dwFlags, int dwExtraInfo);
        short GetAsyncKeyState(int vKey);
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(new FlatDarkLaf());
            JFrame.setDefaultLookAndFeelDecorated(true);
        } catch (Exception ex) {
            System.err.println("Failed to initialize LaF");
        }
        try { robot = new Robot(); } catch (AWTException e) {}
        ConfigManager.load();
        ConfigManager.autoSave();
        SwingUtilities.invokeLater(() -> new RemapperGUI());
        new Thread(Main::installHook).start();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            ConfigManager.save();
        }));
    }

    private static void installHook() {
        HMODULE hMod = Kernel32.INSTANCE.GetModuleHandle(null);
        
        keyboardHook = new LowLevelKeyboardProc() {
            @Override
            public LRESULT callback(int nCode, WPARAM wParam, KBDLLHOOKSTRUCT info) {
                if (nCode >= 0) {
                    boolean isInjected = (info.flags & 16) != 0; // If injected by software
                    boolean wasDown = (info.flags & 0x40000000) != 0;
                    
                    if (isInjected) { // Just ignore the simulated keypress
                        return User32.INSTANCE.CallNextHookEx(hhk, nCode, wParam, new LPARAM(com.sun.jna.Pointer.nativeValue(info.getPointer())));
                    }

                    int originalCode = info.vkCode; 
                    int code = originalCode;

                    boolean isPress = (wParam.intValue() == WinUser.WM_KEYDOWN || wParam.intValue() == WinUser.WM_SYSKEYDOWN);
                    boolean isRelease = (wParam.intValue() == WinUser.WM_KEYUP || wParam.intValue() == WinUser.WM_SYSKEYUP);

                    // 1. Normalize keys
                    if (code == 91 || code == 92) code = KeyEvent.VK_WINDOWS;
                    if (code == 160 || code == 161) code = KeyEvent.VK_SHIFT;
                    if (code == 162 || code == 163) code = 17; // Ctrl
                    if (code == 164 || code == 165) code = 18; // Alt
                    if (code == 10) code = 13; // Enter

                    // 2. Update held keys
                    if (isPress) {
                        if (code == 134 || code == 123) {
                            heldKeys.remove(16);
                            heldKeys.remove(524);
                        }
                        heldKeys.add(code);
                    } else if (isRelease) {
                        heldKeys.remove(code); 
                    }

                    // 3. recoding
                    if (isRecording) {
                        if (isPress && !wasDown) {
                            if (code == 134 || code == 123) { 
                                recordingBuffer.remove((Integer)16);
                                recordingBuffer.remove((Integer)524);
                            }
                            if (!recordingBuffer.contains(code)) recordingBuffer.add(code);
                        }
                        return new LRESULT(1);
                    }

                    // 4. check if a key has been released and for custom keys
                    if (isRelease && activeCustomKey != null) {
                        boolean isPartOfCombo = activeCustomKey.getRawCodes().contains(code) || activeCustomKey.getRawCodes().contains(originalCode);

                        if (isPartOfCombo) {
                            if (activeRepeaters.containsKey(code)) {
                                stopRepeater(code);
                            }
                            int stopKey = (activeCustomTriggerKey != -1) ? activeCustomTriggerKey 
                            : code;
                            if (activeRepeaters.containsKey(stopKey)) {
                                stopRepeater(stopKey);
                            } else {
                                try { simulateKeyPress(activeTargetCode, false); } catch (Exception e) {}
                            }                            
                            activeCustomKey = null;
                            activeTargetCode = -1;
                            activeCustomTriggerKey = -1;
                            return new LRESULT(1); 
                        }
                    }
                    if (isRelease && activeRepeaters.containsKey(code)) {
                         stopRepeater(code);
                    }
                    // 5. update heatmap + combos
                    if (isPress && !wasDown) { 
                        int codeToLog = code;
                        if (activeCustomKey == null && codeToCode.containsKey(code)) {
                                codeToLog = codeToCode.get(code);
                        }
                        
                        HeatmapManager.setCount(codeToLog);
                        boolean hasModifier = heldKeys.contains(17) || heldKeys.contains(18) || heldKeys.contains(16) || heldKeys.contains(524);
                        boolean isCurrentKeyModifier = (code == 17 || code == 18 || code == 16 || code == 524);
                        if (code == 134) {
                            HeatmapManager.recordCombo("Win+Shift+Key 134");
                        }
                        if (hasModifier && !isCurrentKeyModifier) {
                            String combo = Main.buildStandardCombo(heldKeys, code);                            
                            HeatmapManager.recordCombo(combo);
                        }
                    }

                    // 6. load custom keys
                    if (isPress && activeCustomKey == null && !activeRepeaters.containsKey(code)) {
                        CustomKey matched = CustomKeyManager.match(heldKeys);
                        
                        if (matched != null && heldKeys.size() == matched.getRawCodes().size()) {
                            int pseudoID = matched.getPseudoCode();
                            
                            if (!wasDown) HeatmapManager.setCount(pseudoID);

                            if (codeToCode.containsKey(pseudoID)) {
                                activeCustomKey = matched;
                                activeTargetCode = codeToCode.get(pseudoID);
                                activeCustomTriggerKey = code; 
                                
                                boolean isModifierHeavy = false;

                                if (activeTargetCode >= 10000) {
                                    CustomKey outputKey = CustomKeyManager.getByPseudoCode(activeTargetCode);
                                    if (outputKey != null) {
                                        for (int c : outputKey.getRawCodes()) {
                                            if (isModifier(c)) { isModifierHeavy = true; break; }
                                        }
                                    }
                                } else if (isModifier(activeTargetCode)) {
                                    isModifierHeavy = true;
                                }

                                if (isModifierHeavy) {
                                    cleanPhysicalModifiers(); 
                                    try { simulateKeyPress(activeTargetCode, true); } catch (Exception e) {}
                                } else {
                                    startRepeater(code, activeTargetCode); 
                                }                                
                                return new LRESULT(1); 
                            }
                        }
                    }                     

                    if (activeCustomKey != null) {
                        boolean isPartOfCombo = activeCustomKey.getRawCodes().contains(code) || 
                                                activeCustomKey.getRawCodes().contains(originalCode);
                        if (isPartOfCombo) {
                            return new LRESULT(1); 
                        }
                    }

                    if (activeCustomKey == null && codeToCode.containsKey(code)) {
                        int target = codeToCode.get(code);

                        if (isPress) {
                            if (isModifier(target)) {
                                if (!wasDown) {
                                    cleanPhysicalModifiers();
                                    try { simulateKeyPress(target, true); } catch (Exception e) {}
                                }
                            } else {
                                if (!activeRepeaters.containsKey(code)) {
                                    startRepeater(code, target);
                                }
                            }
                        } 
                        else if (isRelease) {
                            if (isModifier(target)) {
                                try { simulateKeyPress(target, false); } catch (Exception e) {}
                            } else {
                                stopRepeater(code);
                            }
                        }
                        return new LRESULT(1);
                    }
                }
                return User32.INSTANCE.CallNextHookEx(hhk, nCode, wParam, new LPARAM(com.sun.jna.Pointer.nativeValue(info.getPointer())));
            }
        };

        hhk = User32.INSTANCE.SetWindowsHookEx(WinUser.WH_KEYBOARD_LL, keyboardHook, hMod, 0);

        MSG msg = new MSG();
        while (User32.INSTANCE.GetMessage(msg, null, 0, 0) != 0) {
            User32.INSTANCE.TranslateMessage(msg);
            User32.INSTANCE.DispatchMessage(msg);
        }
        User32.INSTANCE.UnhookWindowsHookEx(hhk);
    }

    private static void startRepeater(int sourceKey, int targetKey) {
        if (activeRepeaters.containsKey(sourceKey)) return;               
        cleanPhysicalModifiers();
        try { simulateKeyPress(targetKey, true); } catch (Exception e) {}

        Runnable spamTask = () -> {
            try {
                // cleanPhysicalModifiers();
                simulateKeyPress(targetKey, true);
            } catch (Exception e) { e.printStackTrace(); }
        };

        ScheduledFuture<?> future = scheduler.scheduleAtFixedRate(spamTask, 500, 30, TimeUnit.MILLISECONDS);
        activeRepeaters.put(sourceKey, future);
    }

    private static void stopRepeater(int sourceKey) {
        ScheduledFuture<?> future = activeRepeaters.remove(sourceKey);
        if (future != null) {
            future.cancel(true);
            
            if (codeToCode.containsKey(sourceKey)) {
                try { simulateKeyPress(codeToCode.get(sourceKey), false); } catch (Exception e) {}
            } else if (activeCustomKey != null) {
                 try { simulateKeyPress(activeTargetCode, false); } catch (Exception e) {}
            }
        }
    }


    private static void cleanPhysicalModifiers() {
        if (robot == null) return;
        for (Map.Entry<Integer, Integer> entry : windowsToJavaModifiers.entrySet()) {
            int winCode = entry.getKey();
            int javaCode = entry.getValue();
            if ((Win32User32.INSTANCE.GetAsyncKeyState(winCode) & 0x8000) != 0) {
                robot.keyRelease(javaCode);
            }
        }
    }

    private static void doSafePress(int code, boolean pressed) {
        if (code == 13) code = 10;
        if (code == 134 || code == KeyEvent.VK_F23) {
            if (pressed) {
                if (robot != null) {
                    robot.keyPress(KeyEvent.VK_SHIFT);
                    robot.keyPress(KeyEvent.VK_WINDOWS);
                }
                Win32User32.INSTANCE.keybd_event((byte) 134, (byte) 0x6E, 0, 0);
            } else {
                Win32User32.INSTANCE.keybd_event((byte) 134, (byte) 0x6E, 2, 0);
                if (robot != null) {
                    robot.keyRelease(KeyEvent.VK_WINDOWS);
                    robot.keyRelease(KeyEvent.VK_SHIFT);
                    
                }
            }
            return;
        }
        if (robot != null) {
            if (pressed) robot.keyPress(code);
            else robot.keyRelease(code);
        }
    }
    

    public static void simulateKeyPress(int code, boolean pressed) throws AWTException {
        if (code <= 0) return;
        if (code >= 10000) {
            CustomKey ck = CustomKeyManager.getByPseudoCode(code);
            simulateCombo(ck, pressed);
            return;
        }
        doSafePress(code, pressed);
    }
    
    public static void simulateCombo(CustomKey ck, boolean pressed) {
        if (ck != null) {
            for (int c : ck.getRawCodes()) {
                doSafePress(c, pressed);
            }
        }
    }

    public static void deleteCustomKey(CustomKey key) {
        if (key == null) return;  

        if (codeToCode.containsKey(key.getPseudoCode())) {
            codeToCode.remove(key.getPseudoCode());
        }
        CustomKeyManager.remove(key); 
    }
    public static boolean isModifier(int code) {
        return code == 16 || code == 160 || code == 161 || // Shift (L/R)
            code == 17 || code == 162 || code == 163 || // Ctrl (L/R)
            code == 18 || code == 164 || code == 165 || // Alt (L/R)
            code == 524 || code == 91 || code == 92;    // Windows
    }

    public static boolean isPureModifierCustom(List<Integer> codes) {
        for (int code : codes) {
            if (!isModifier(code)) return false; 
        }
        return true; 
    }
    public static String buildStandardCombo(java.util.Collection<Integer> keys, int triggerKey) {
        java.util.List<Integer> sortedMods = new java.util.ArrayList<>();
        for (int k : keys) {
            if (k != triggerKey && (k == 16 || k == 17 || k == 18 || k == 524)) {
                sortedMods.add(k);
            }
        }
        java.util.Collections.sort(sortedMods);

        StringBuilder sb = new StringBuilder();
        for (int mod : sortedMods) {
            if (mod == 16) sb.append("Shift+");
            else if (mod == 17) sb.append("Ctrl+");
            else if (mod == 18) sb.append("Alt+");
            else if (mod == 524) sb.append("Win+");
        }

        sb.append(VirtualKeyboard.getName(triggerKey));
        
        return sb.toString();
    }
}