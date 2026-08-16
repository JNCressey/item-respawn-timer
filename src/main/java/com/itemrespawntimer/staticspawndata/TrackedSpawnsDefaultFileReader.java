package com.itemrespawntimer.staticspawndata;

import com.itemrespawntimer.ItemRespawnTimerConfig;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class TrackedSpawnsDefaultFileReader {

    public static final String resourceFilename = "TrackedSpawnsDefault.csv";

    public static String readResource(){
        try (InputStream in = ItemRespawnTimerConfig.class.getClassLoader().getResourceAsStream(resourceFilename)) {
            if (in == null) {
                throw new IOException("Resource not found: " + resourceFilename);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch(IOException e){
            return "";
        }
    }
}
