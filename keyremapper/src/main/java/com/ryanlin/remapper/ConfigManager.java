package com.ryanlin.remapper;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.util.HashMap;
import java.io.*;

public class ConfigManager {
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public static void load(){
        try(FileReader fr = new FileReader("config.json")){

            AppConfig config = gson.fromJson(fr, AppConfig.class);
            Main.codeToCode = new HashMap<>(config.simpleMappings);

            CustomKeyManager.customKeys = config.cusKeys;
            CustomKeyManager.recalculateNextId();

        } catch(IOException e){}
    }
    public static void save(){
        AppConfig config = new AppConfig();
        config.cusKeys = CustomKeyManager.customKeys;
        config.simpleMappings = Main.codeToCode;

        try(FileWriter fw = new FileWriter("config.json")){
            gson.toJson(config, fw);
        } catch (IOException e){}
    }
}
