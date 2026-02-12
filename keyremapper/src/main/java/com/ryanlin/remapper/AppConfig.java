package com.ryanlin.remapper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AppConfig {
//blueprint for json
    public Map<Integer, Integer> simpleMappings = new HashMap<>();


    public List<CustomKey> cusKeys = new ArrayList<>();
    
    public Map<Integer, Long> keyHeatmap = new HashMap<>(); //for later
}