package com.ryanlin.remapper;

import java.util.List;
import java.util.Set;

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

    public boolean matches(Set<Integer> heldKeys) {
        if (heldKeys.size() != rawCodes.size()) {
            return false;
        }
        return heldKeys.containsAll(rawCodes);
    }
    public boolean isHidden() { 
        return isHidden; 
    }
    public void setHidden(boolean hidden) { 
        this.isHidden = hidden; 
    }
}