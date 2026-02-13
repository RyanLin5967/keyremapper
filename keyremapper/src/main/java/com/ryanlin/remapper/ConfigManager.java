package com.ryanlin.remapper;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.util.HashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.io.*;
import java.util.concurrent.ConcurrentHashMap;

import java.util.concurrent.TimeUnit;

public class ConfigManager {
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private static ScheduledExecutorService autoSaver = Executors.newScheduledThreadPool(4);

    public static void load(){
        try(FileReader fr = new FileReader("config.json")){

            AppConfig config = gson.fromJson(fr, AppConfig.class);
            Main.codeToCode = new HashMap<>(config.simpleMappings);

            CustomKeyManager.customKeys = config.cusKeys;
            CustomKeyManager.recalculateNextId();
            HeatmapManager.load(config.keyHeatmap, config.comboHeatmap);

        } catch(IOException e){}
    }
    public static void save(){
        AppConfig config = new AppConfig();
        config.cusKeys = CustomKeyManager.customKeys;
        config.simpleMappings = Main.codeToCode;
        config.keyHeatmap = HeatmapManager.counter;
        config.comboHeatmap = new HashMap<>(HeatmapManager.comboCounts);
        try(FileWriter fw = new FileWriter("config.json")){
            gson.toJson(config, fw);
        } catch (IOException e){}
    }
    public static void autoSave(){
        autoSaver.scheduleAtFixedRate(() -> {
            save();
        },5, 5, TimeUnit.MINUTES);
    }
}
