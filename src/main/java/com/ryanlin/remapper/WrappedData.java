package com.ryanlin.remapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class WrappedData {
    public long totalPresses = 0;
    public String topKeyName = "None";
    public long topKeyCount = 0;
    public String archetype = "The Generalist";
    public String archetypeDescription = "You use your keyboard like a well-balanced instrument.";
    
    // Fun Stats
    public long backspaceCount = 0;
    public long enterCount = 0;
    public long copyCount = 0;
    public long pasteCount = 0;
    
    // Top 5 List
    public List<Map.Entry<String, Long>> top5Keys = new ArrayList<>();
}