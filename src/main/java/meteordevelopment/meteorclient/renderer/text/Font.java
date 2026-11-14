/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.renderer.text;

import com.mojang.renderpearl.api.GpuFormat;
import com.mojang.renderpearl.api.textures.FilterMode;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import meteordevelopment.meteorclient.renderer.MeshBuilder;
import meteordevelopment.meteorclient.renderer.Texture;
import meteordevelopment.meteorclient.utils.render.color.Color;
import org.jspecify.annotations.Nullable;
import org.lwjgl.BufferUtils;
import org.lwjgl.stb.*;
import org.lwjgl.system.MemoryStack;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

public class Font {
    public final Texture texture;
    private final int height;
    private final float scale;
    private final float ascent;
    private final Int2ObjectOpenHashMap<CharData> charMap = new Int2ObjectOpenHashMap<>();
    private static final int size = 2048;

    public Font(ByteBuffer buffer, int height) {
        this(buffer, height, size, 0, null);
    }

    /**
     * @param atlasSize  字体图集的边长，单位像素
     * @param fontIndex  TTC 字体集合中的序号，普通 TTF 传 0
     * @param codepoints 额外需要打包进图集的码点（中文字符集），传 null 表示只打包原版的拉丁字符集
     */
    public Font(ByteBuffer buffer, int height, int atlasSize, int fontIndex, int @Nullable [] codepoints) {
        this.height = height;

        // Initialize font
        STBTTFontinfo fontInfo = STBTTFontinfo.create();
        int offset = STBTruetype.stbtt_GetFontOffsetForIndex(buffer, fontIndex);
        if (offset < 0 || !STBTruetype.stbtt_InitFont(fontInfo, buffer, offset)) {
            throw new IllegalStateException("Failed to initialize font");
        }

        // Allocate bitmap
        ByteBuffer bitmap = BufferUtils.createByteBuffer(atlasSize * atlasSize);

        // Collect the packing ranges
        List<Range> ranges = new ArrayList<>(6);

        if (codepoints == null) {
            ranges.add(new Range(height, 32, 95, null, (byte) 2)); // Basic Latin
            ranges.add(new Range(height, 160, 96, null, (byte) 2)); // Latin 1 Supplement
            ranges.add(new Range(height, 256, 128, null, (byte) 2)); // Latin Extended-A
            ranges.add(new Range(height, 880, 144, null, (byte) 2)); // Greek and Coptic
            ranges.add(new Range(height, 1024, 256, null, (byte) 2)); // Cyrillic
            ranges.add(new Range(height, 8734, 1, null, (byte) 2)); // infinity symbol
        } else {
            // Basic Latin is always packed so fallback text still renders
            ranges.add(new Range(height, 32, 95, null, (byte) 2));

            // Scattered codepoints (chinese) are packed from an explicit codepoint array,
            // they are far too fragmented to be expressed as a few contiguous ranges
            IntBuffer cps = BufferUtils.createIntBuffer(codepoints.length);
            cps.put(codepoints).flip();

            ranges.add(new Range(height, 0, codepoints.length, cps, (byte) 1));
        }

        for (Range range : ranges) {
            range.cdata = STBTTPackedchar.create(range.count);
        }

        // create and initialise packing context
        STBTTPackContext packContext = STBTTPackContext.create();
        STBTruetype.stbtt_PackBegin(packContext, bitmap, atlasSize, atlasSize, 0, 1);

        // create the pack range, populate with the specific packing ranges
        STBTTPackRange.Buffer packRange = STBTTPackRange.create(ranges.size());
        for (Range range : ranges) {
            packRange.put(STBTTPackRange.create().set(range.fontSize, range.first, range.array, range.count, range.cdata, range.oversample, range.oversample));
        }
        packRange.flip();

        // write and finish
        boolean packed = STBTruetype.stbtt_PackFontRanges(packContext, buffer, fontIndex, packRange);
        STBTruetype.stbtt_PackEnd(packContext);

        if (!packed) {
            System.err.println("[Meteor] 字体图集空间不足，部分字形未能打包 (atlas " + atlasSize + ")");
        }

        // Create texture object and get font scale
        texture = new Texture(atlasSize, atlasSize, GpuFormat.R8_UNORM, FilterMode.LINEAR, FilterMode.LINEAR);
        texture.upload(bitmap);
        scale = STBTruetype.stbtt_ScaleForPixelHeight(fontInfo, height);

        // Get font vertical ascent
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer ascent = stack.mallocInt(1);
            STBTruetype.stbtt_GetFontVMetrics(fontInfo, ascent, null, null);
            this.ascent = ascent.get(0);
        }

        for (Range range : ranges) {
            for (int i = 0; i < range.count; i++) {
                int codepoint = range.array != null ? range.array.get(i) : range.first + i;
                STBTTPackedchar packedChar = range.cdata.get(i);

                float ipw = 1f / atlasSize; // pixel width and height
                float iph = 1f / atlasSize;

                charMap.put(codepoint, new CharData(
                    packedChar.xoff(),
                    packedChar.yoff(),
                    packedChar.xoff2(),
                    packedChar.yoff2(),
                    packedChar.x0() * ipw,
                    packedChar.y0() * iph,
                    packedChar.x1() * ipw,
                    packedChar.y1() * iph,
                    packedChar.xadvance()
                ));
            }
        }
    }

    public double getWidth(String string, int length) {
        double width = 0;

        for (int i = 0; i < length; i++) {
            width += getAdvance(string.charAt(i));
        }

        return width;
    }

    public boolean hasGlyph(int codepoint) {
        return charMap.containsKey(codepoint);
    }

    public double getAdvance(int codepoint) {
        CharData c = charMap.get(codepoint);
        if (c == null) c = charMap.get(32);

        return c != null ? c.xAdvance : 0;
    }

    public int getHeight() {
        return height;
    }

    public double render(MeshBuilder mesh, String string, double x, double y, Color color, double scale) {
        y += ascent * this.scale * scale;

        int length = string.length();
        mesh.ensureCapacity(length * 4, length * 6);

        for (int i = 0; i < length; i++) {
            int cp = string.charAt(i);
            CharData c = charMap.get(cp);
            if (c == null) c = charMap.get(32);
            if (c == null) continue;

            mesh.quad(
                mesh.vec2(x + c.x0 * scale, y + c.y0 * scale).vec2(c.u0, c.v0).color(color).next(),
                mesh.vec2(x + c.x0 * scale, y + c.y1 * scale).vec2(c.u0, c.v1).color(color).next(),
                mesh.vec2(x + c.x1 * scale, y + c.y1 * scale).vec2(c.u1, c.v1).color(color).next(),
                mesh.vec2(x + c.x1 * scale, y + c.y0 * scale).vec2(c.u1, c.v0).color(color).next()
            );

            x += c.xAdvance * scale;
        }

        return x;
    }

    private static final class Range {
        final float fontSize;
        final int first;
        final int count;
        final @Nullable IntBuffer array;
        final byte oversample;
        STBTTPackedchar.Buffer cdata;

        Range(float fontSize, int first, int count, @Nullable IntBuffer array, byte oversample) {
            this.fontSize = fontSize;
            this.first = first;
            this.count = count;
            this.array = array;
            this.oversample = oversample;
        }
    }

    private record CharData(float x0, float y0, float x1, float y1, float u0, float v0, float u1, float v1, float xAdvance) {}
}
