/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.renderer.text;

import org.jspecify.annotations.Nullable;
import org.lwjgl.BufferUtils;
import org.lwjgl.stb.STBTTFontinfo;
import org.lwjgl.stb.STBTruetype;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 中文字形图集。
 *
 * <p>Meteor 自带的四个字体（Comfortaa、JetBrains Mono、Tw Cen MT、Pixelation）都不含汉字，
 * 而 {@link Font} 原本只打包拉丁字符集，所以开启自定义字体后中文会变成一片空白。
 * 这里额外准备一份只含中文的图集：从系统里找一个支持中文的字体，把 GB2312 的全部汉字
 * 打包进一张 4096×4096 的图集，渲染时遇到当前字体没有的字形就从这张图集里取。</p>
 *
 * <p>只打包一次、所有字号共用（缩放使用），因此不会出现每个字号各占一张大图集的情况。</p>
 */
public final class CjkFont {
    private static final int ATLAS_SIZE = 4096;
    private static final int HEIGHT = 40;

    private static Font font;
    private static boolean loaded;

    private CjkFont() {
    }

    /**
     * 取中文字体图集，找不到可用的中文字体时返回 null（此时中文会退回原来的空白表现）。
     */
    public static @Nullable Font get() {
        if (!loaded) {
            loaded = true;

            try {
                font = create();
            } catch (Throwable t) {
                System.err.println("[Meteor] 加载中文字体失败: " + t);
            }

            if (font == null) {
                System.err.println("[Meteor] 未找到可用的中文字体，自定义字体下的中文将无法显示");
            }
        }

        return font;
    }

    private static @Nullable Font create() {
        int[] codepoints = gb2312();

        for (Candidate candidate : candidates()) {
            try {
                if (!Files.isReadable(candidate.path)) continue;

                ByteBuffer data = read(candidate.path);
                if (!supportsChinese(data, candidate.index)) continue;

                return new Font(data, HEIGHT, ATLAS_SIZE, candidate.index, codepoints);
            } catch (Throwable t) {
                System.err.println("[Meteor] 中文字体 " + candidate.path + " 加载失败: " + t);
            }
        }

        return null;
    }

    private static boolean supportsChinese(ByteBuffer data, int index) {
        STBTTFontinfo fontInfo = STBTTFontinfo.create();
        int offset = STBTruetype.stbtt_GetFontOffsetForIndex(data, index);
        if (offset < 0) return false;

        if (!STBTruetype.stbtt_InitFont(fontInfo, data, offset)) return false;
        return STBTruetype.stbtt_FindGlyphIndex(fontInfo, '中') != 0;
    }

    private static ByteBuffer read(Path path) throws Exception {
        byte[] bytes = Files.readAllBytes(path);

        ByteBuffer buffer = BufferUtils.createByteBuffer(bytes.length);
        buffer.put(bytes).flip();

        return buffer;
    }

    /**
     * GB2312 里的全部字符（6763 个汉字 + 符号区），覆盖日常中文绰绰有余。
     */
    private static int[] gb2312() {
        Charset charset = Charset.forName("GB2312");
        boolean[] present = new boolean[0x10000];
        int count = 0;

        for (int hi = 0xA1; hi <= 0xF7; hi++) {
            for (int lo = 0xA1; lo <= 0xFE; lo++) {
                String decoded = new String(new byte[]{(byte) hi, (byte) lo}, charset);
                if (decoded.length() != 1) continue;

                char c = decoded.charAt(0);
                if (c == '\uFFFD' || present[c]) continue;

                present[c] = true;
                count++;
            }
        }

        int[] codepoints = new int[count];
        int i = 0;
        for (int cp = 0; cp < present.length; cp++) {
            if (present[cp]) codepoints[i++] = cp;
        }

        return codepoints;
    }

    private static List<Candidate> candidates() {
        List<Candidate> list = new ArrayList<>();
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);

        if (os.contains("win")) {
            String root = System.getenv("SystemRoot");
            if (root == null) root = "C:\\Windows";

            Path fonts = Path.of(root, "Fonts");
            list.add(new Candidate(fonts.resolve("msyh.ttc"), 0)); // 微软雅黑
            list.add(new Candidate(fonts.resolve("NotoSansSC-VF.ttf"), 0)); // 思源黑体（可变字体）
            list.add(new Candidate(fonts.resolve("Deng.ttf"), 0)); // 等线
            list.add(new Candidate(fonts.resolve("simhei.ttf"), 0)); // 黑体
            list.add(new Candidate(fonts.resolve("simsun.ttc"), 0)); // 宋体

            String localAppData = System.getenv("LOCALAPPDATA");
            if (localAppData != null) {
                Path userFonts = Path.of(localAppData, "Microsoft", "Windows", "Fonts");
                list.add(new Candidate(userFonts.resolve("msyh.ttc"), 0));
                list.add(new Candidate(userFonts.resolve("NotoSansSC-VF.ttf"), 0));
            }
        } else if (os.contains("mac")) {
            list.add(new Candidate(Path.of("/System/Library/Fonts/PingFang.ttc"), 0));
            list.add(new Candidate(Path.of("/System/Library/Fonts/STHeiti Medium.ttc"), 0));
        } else {
            list.add(new Candidate(Path.of("/usr/share/fonts/opentype/noto/NotoSansCJK-Regular.ttc"), 0));
            list.add(new Candidate(Path.of("/usr/share/fonts/opentype/noto/NotoSansCJKsc-Regular.otf"), 0));
            list.add(new Candidate(Path.of("/usr/share/fonts/truetype/wqy/wqy-microhei.ttc"), 0));
            list.add(new Candidate(Path.of("/usr/share/fonts/truetype/droid/DroidSansFallbackFull.ttf"), 0));
        }

        return list;
    }

    private record Candidate(Path path, int index) {}
}
