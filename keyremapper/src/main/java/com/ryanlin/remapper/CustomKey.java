package com.ryanlin.remapper;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class CustomKey {
    private String name;
    private int pseudoCode;
    private List<Integer> rawCodes;
    private boolean isHidden = false;
    public CustomKey(String name, int pseudoCode, List<Integer> rawCodes) {
        this.name = name;
        this.pseudoCode = pseudoCode;
        this.rawCodes = rawCodes;
    }

    public String getName() { return name; }
    public int getPseudoCode() { return pseudoCode; }
    public List<Integer> getRawCodes() { return rawCodes; }

    // format: Name:unique code:key+key+key
    public String toFileString() {
        String codes = rawCodes.stream().map(String::valueOf).collect(Collectors.joining("+"));
        return name + ":" + pseudoCode + ":" + codes;
    }

    public static CustomKey fromFileString(String line) {
        try {
            String[] parts = line.split(":");
            String[] codeStrings = parts[2].split("\\+");
            List<Integer> codes = new ArrayList<>();
            for (String s : codeStrings) codes.add(Integer.parseInt(s));
            return new CustomKey(parts[0], Integer.parseInt(parts[1]), codes);
        } catch (Exception e) { return null; } 
    }
    // Inside CustomKey.java

    public boolean matches(java.util.Set<Integer> heldKeys) {
        // 1. If sizes don't match, it can't be a match
        if (heldKeys.size() != rawCodes.size()) {
            return false;
        }
        
        // 2. Check if every key in the combination is currently held
        return heldKeys.containsAll(rawCodes);
    }
    public boolean isHidden() { 
        return isHidden; 
    }
    public void setHidden(boolean hidden) { 
        this.isHidden = hidden; 
    }
}