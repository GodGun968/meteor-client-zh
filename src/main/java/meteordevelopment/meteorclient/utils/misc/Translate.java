/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.utils.misc;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.jspecify.annotations.Nullable;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * 中文汉化表。数据存放在 {@code assets/meteor-client/ui_zh.json} 中，分成两张表：
 * <ul>
 *     <li>{@code names}：以 kebab-case 的 id（如 {@code velocity-height}）为键，值是该选项/模块的标题；</li>
 *     <li>{@code texts}：以英文原文整句为键，值是对应的中文译文。</li>
 * </ul>
 * 查不到的条目原样返回，因此漏翻只会显示英文，不会显示空白或乱码。
 */
public final class Translate {
    private static final Map<String, String> NAMES = new HashMap<>();
    private static final Map<String, String> TEXTS = new HashMap<>();
    private static boolean loaded;

    private Translate() {
    }

    private static void load() {
        if (loaded) return;
        loaded = true;

        try (InputStream in = Translate.class.getResourceAsStream("/assets/meteor-client/ui_zh.json")) {
            if (in == null) return;

            JsonObject root = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
            read(root, "names", NAMES);
            read(root, "texts", TEXTS);
        } catch (Exception e) {
            System.err.println("[Meteor] 无法读取中文汉化表: " + e);
        }
    }

    private static void read(JsonObject root, String key, Map<String, String> out) {
        JsonElement element = root.get(key);
        if (element == null || !element.isJsonObject()) return;

        for (Map.Entry<String, JsonElement> entry : element.getAsJsonObject().entrySet()) {
            String value = entry.getValue().getAsString();
            if (!value.isEmpty()) out.put(entry.getKey(), value);
        }
    }

    /**
     * 按 id 取标题译文，例如 {@code Translate.name("velocity-height")} → {@code 速度高度}。
     */
    public static String name(@Nullable String id) {
        if (id == null || id.isEmpty()) return id;

        load();
        String translated = NAMES.get(id);
        return translated != null ? translated : id;
    }

    /**
     * 按英文原文取译文，例如 {@code Translate.text("Automatically jumps.")}。
     */
    public static String text(@Nullable String english) {
        if (english == null || english.isEmpty()) return english;

        load();
        String translated = TEXTS.get(english);
        return translated != null ? translated : english;
    }

    /**
     * 是否已经存在该原文的译文，主要用于自检。
     */
    public static boolean hasText(String english) {
        load();
        return TEXTS.containsKey(english);
    }
}
