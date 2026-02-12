package com.ryanlin.remapper;

import java.util.*;

public class CustomKeyManager {
    public static List<CustomKey> customKeys = new ArrayList<>();
    private static int nextPseudoCode = 10000; 

    // Called by ConfigManager after loading JSON to ensure IDs don't conflict
    public static void recalculateNextId() {
        int max = 10000;
        for (CustomKey ck : customKeys) {
            if (ck.getPseudoCode() >= max) max = ck.getPseudoCode() + 1;
        }
        nextPseudoCode = max;
    }

    public static CustomKey add(String name, List<Integer> codes) {
        CustomKey ck = new CustomKey(name, nextPseudoCode++, codes);
        customKeys.add(ck);
        ConfigManager.save(); // Save to JSON
        return ck;
    }

    public static void remove(CustomKey key) {
        customKeys.remove(key);
        ConfigManager.save(); // Save to JSON
    }

    public static CustomKey getByPseudoCode(int code) {
        for (CustomKey ck : customKeys) if (ck.getPseudoCode() == code) return ck;
        return null;
    }

    public static CustomKey match(Set<Integer> pressedKeys) {
        for (CustomKey ck : customKeys) {
            if (pressedKeys.containsAll(ck.getRawCodes())) return ck;
        }
        return null;
    }

    // --- VALIDATION ---
    public static boolean isNameTaken(String name) {
        if (name == null) return false;
        for (CustomKey ck : customKeys) {
            if (ck.getName().equalsIgnoreCase(name.trim())) return true;
        }
        return false;
    }

    public static boolean isCombinationTaken(List<Integer> newCodes) {
        Set<Integer> newSet = new HashSet<>(newCodes);
        for (CustomKey ck : customKeys) {
            if (new HashSet<>(ck.getRawCodes()).equals(newSet)) return true;
        }
        return false;
    }
}