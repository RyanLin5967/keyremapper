package com.ryanlin.remapper;

import java.util.*;
import java.util.stream.Collectors;

public class WrappedAnalyzer {

    public static WrappedData analyze() {
        WrappedData data = new WrappedData();
        Map<String, Long> allStats = new HashMap<>();
        
        // 1. Merge Single Keys & Combos
        for (Map.Entry<Integer, Long> entry : HeatmapManager.counter.entrySet()) {
            String name = VirtualKeyboard.getName(entry.getKey());
            // Filter out internal names if needed
            if (name.startsWith("Num ")) name = name.substring(4); 
            allStats.merge(name, entry.getValue(), Long::sum);
            data.totalPresses += entry.getValue();
        }
        
        for (Map.Entry<String, Long> entry : HeatmapManager.comboCounts.entrySet()) {
            allStats.merge(entry.getKey(), entry.getValue(), Long::sum);
        }

        // 2. Find Top Key
        Map.Entry<String, Long> topKey = allStats.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .orElse(new AbstractMap.SimpleEntry<>("None", 0L));
            
        data.topKeyName = topKey.getKey();
        data.topKeyCount = topKey.getValue();

        // 3. Extract Specific Stats
        data.backspaceCount = allStats.getOrDefault("Backspace", 0L);
        data.enterCount = allStats.getOrDefault("Enter", 0L);
        data.copyCount = allStats.getOrDefault("Ctrl+C", 0L) + allStats.getOrDefault("Ctrl+c", 0L); // Covers case variations
        data.pasteCount = allStats.getOrDefault("Ctrl+V", 0L) + allStats.getOrDefault("Ctrl+v", 0L);

        // 4. Determine Archetype
        determineArchetype(data, allStats);

        // 5. Get Top 5
        data.top5Keys = allStats.entrySet().stream()
            .sorted((k1, k2) -> k2.getValue().compareTo(k1.getValue())) 
            .limit(5)
            .collect(Collectors.toList());

        return data;
    }

    private static void determineArchetype(WrappedData data, Map<String, Long> stats) {
        long wasdCount = stats.getOrDefault("W", 0L) + stats.getOrDefault("A", 0L) + 
                         stats.getOrDefault("S", 0L) + stats.getOrDefault("D", 0L);
                         
        long codeCount = stats.getOrDefault("{", 0L) + stats.getOrDefault("}", 0L) + 
                         stats.getOrDefault(";", 0L) + stats.getOrDefault("Shift+9", 0L) + 
                         stats.getOrDefault("Shift+0", 0L); 
                         
        long editCount = stats.getOrDefault("Backspace", 0L) + stats.getOrDefault("Delete", 0L);
        
        long copyPasteCount = stats.getOrDefault("Ctrl+C", 0L) + stats.getOrDefault("Ctrl+V", 0L);

        long maxScore = 0;
        String type = "The Generalist";
        String desc = "Balanced. Efficient. You use the whole keyboard.";

        // Gamer Check
        if (wasdCount > maxScore) {
            maxScore = wasdCount;
            type = "The Gamer";
            desc = "WASD is your home. You spent the year moving forward.";
        }
        
        // Developer Check (Weighted x5 because code symbols are rarer than letters)
        if ((codeCount * 5) > maxScore) { 
            maxScore = codeCount * 5;
            type = "The Developer";
            desc = "Semicolons & Brackets. You speak the language of machines.";
        }
        
        // Editor Check
        if (editCount > maxScore) {
            maxScore = editCount;
            type = "The Editor";
            desc = "Perfectionist. You deleted almost as much as you wrote.";
        }
        
        // StackOverflow User Check
        if ((copyPasteCount * 100) > maxScore) {
             maxScore = copyPasteCount * 100;
             type = "The Vibe Coder";
             desc = "Ctrl+C, Ctrl+V. Why write code when it already exists?";
        }

        data.archetype = type;
        data.archetypeDescription = desc;
    }
}