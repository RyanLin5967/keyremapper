package com.ryanlin.remapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class HeatmapManager {
    
    public static ConcurrentHashMap<Integer, Long> counter = new ConcurrentHashMap<>(); //map code -> count
    public static ConcurrentHashMap<String, Long> comboCounts = new ConcurrentHashMap<>();
    
    public static void load(Map<Integer, Long> keyData, Map<String, Long> comboData) {
        if (keyData != null) counter.putAll(keyData);
        if (comboData != null) comboCounts.putAll(comboData);
    }
    public static void setCount(int code){
        // long current = counter.get(code);
        // counter.put(code, current++);
        counter.merge(code, 1L, Long::sum);
    }
    public static long getCount(int code){
        return counter.getOrDefault(code, 0L);
    }
    public static void recordCombo(String comboKey) {
        comboCounts.merge(comboKey, 1L, Long::sum);
    }
    public static long getMaxCount(){
        long max = 0;
        for(long x: counter.values()){
            if(x > max){
                max = x;
            }
        }
        return max;
    }
    public static long getMaxComboCount(){
        long max = 0;
        for(long x: comboCounts.values()){
            if(x > max){
                max = x;
            }
        }
        return max;
    }
    public static String buildComboString(Set<Integer> heldModifiers, int currentKeyCode) {
        List<String> parts = new ArrayList<>();
        
        if (heldModifiers.contains(17)) parts.add("Ctrl");
        if (heldModifiers.contains(524)) parts.add("Win");
        if (heldModifiers.contains(18)) parts.add("Alt");
        if (heldModifiers.contains(16)) parts.add("Shift");
        String keyName = VirtualKeyboard.getName(currentKeyCode);
        parts.add(keyName);
        
        return String.join("+", parts);
    }
    // Add this to HeatmapManager.java

    public static long getGlobalMax() {
        long maxKey = counter.values().stream().mapToLong(l -> l).max().orElse(1L);
        long maxCombo = comboCounts.values().stream().mapToLong(l -> l).max().orElse(1L);
        return Math.max(maxKey, maxCombo);
    }
}
