package com.ryanlin.remapper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import java.util.concurrent.ConcurrentHashMap;

public class AppConfig {
    public Map<Integer, Integer> simpleMappings = new HashMap<>();   

    public List<CustomKey> cusKeys = new ArrayList<>();
    
    public ConcurrentHashMap<Integer, Long> keyHeatmap = new ConcurrentHashMap<>(); 
    public Map<String, Long> comboHeatmap = new HashMap<>();
}