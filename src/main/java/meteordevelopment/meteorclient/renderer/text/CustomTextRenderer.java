/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.renderer.text;

import meteordevelopment.meteorclient.renderer.MeshBuilder;
import meteordevelopment.meteorclient.renderer.MeshRenderer;
import meteordevelopment.meteorclient.renderer.MeteorRenderPipelines;
import meteordevelopment.meteorclient.utils.render.color.Color;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.nio.ByteBuffer;

public class CustomTextRenderer implements TextRenderer {
    public static final Color SHADOW_COLOR = new Color(60, 60, 60, 180);

    private final MeshBuilder mesh = new MeshBuilder(MeteorRenderPipelines.UI_TEXT);
    private final MeshBuilder cjkMesh = new MeshBuilder(MeteorRenderPipelines.UI_TEXT);

    public final FontFace fontFace;

    private final Font[] fonts;
    private final @Nullable Font cjkFont;
    private Font font;

    private boolean building;
    private boolean scaleOnly;
    private double fontScale = 1;
    private double scale = 1;

    public CustomTextRenderer(FontFace fontFace) throws IOException {
        this.fontFace = fontFace;

        ByteBuffer buffer = fontFace.readToDirectByteBuffer();

        fonts = new Font[5];
        for (int i = 0; i < fonts.length; i++) {
            fonts[i] = new Font(buffer, (int) Math.round(27 * ((i * 0.5) + 1)));
        }

        // 中文图集所有字号共用，当前字体缺字时从这张图集里取
        cjkFont = CjkFont.get();
    }

    @Override
    public void setAlpha(double a) {
        mesh.alpha = a;
        cjkMesh.alpha = a;
    }

    @Override
    public void begin(GuiGraphicsExtractor graphics, double scale, boolean scaleOnly, boolean big) {
        if (building) throw new RuntimeException("CustomTextRenderer.begin() called twice");

        if (!scaleOnly) {
            mesh.begin();
            if (cjkFont != null) cjkMesh.begin();
        }

        if (big) {
            this.font = fonts[fonts.length - 1];
        } else {
            double scaleA = Math.floor(scale * 10) / 10;

            int scaleI;
            if (scaleA >= 3) scaleI = 5;
            else if (scaleA >= 2.5) scaleI = 4;
            else if (scaleA >= 2) scaleI = 3;
            else if (scaleA >= 1.5) scaleI = 2;
            else scaleI = 1;

            font = fonts[scaleI - 1];
        }

        this.building = true;
        this.scaleOnly = scaleOnly;

        this.fontScale = font.getHeight() / 27.0;
        this.scale = 1 + (scale - fontScale) / fontScale;
    }

    @Override
    public double getWidth(String text, int length, boolean shadow) {
        if (text.isEmpty()) return 0;

        Font font = building ? this.font : fonts[0];
        double width;

        if (cjkFont == null) {
            width = font.getWidth(text, length);
        } else {
            double ratio = font.getHeight() / (double) cjkFont.getHeight();
            width = 0;

            for (int i = 0; i < length; i++) {
                int cp = text.charAt(i);

                if (!font.hasGlyph(cp) && cjkFont.hasGlyph(cp)) width += cjkFont.getAdvance(cp) * ratio;
                else width += font.getAdvance(cp);
            }
        }

        return (width + (shadow ? 1 : 0)) * scale / 1.5;
    }

    @Override
    public double getHeight(boolean shadow) {
        Font font = building ? this.font : fonts[0];
        return (font.getHeight() + 1 + (shadow ? 1 : 0)) * scale / 1.5;
    }

    @Override
    public double render(String text, double x, double y, Color color, boolean shadow) {
        if (!building) throw new RuntimeException("VanillaTextRenderer.render() called without calling begin()");

        double width;
        if (shadow) {
            int preShadowA = SHADOW_COLOR.a;
            SHADOW_COLOR.a = (int) (color.a / 255.0 * preShadowA);

            double offset = fontScale * scale / 1.5;
            renderRuns(text, x + offset, y + offset, SHADOW_COLOR);
            width = renderRuns(text, x, y, color);

            SHADOW_COLOR.a = preShadowA;
        } else {
            width = renderRuns(text, x, y, color);
        }

        return width;
    }

    /**
     * 按字形来源把字符串切成若干段，分别写入当前字体的网格和中文图集的网格。
     */
    private double renderRuns(String text, double x, double y, Color color) {
        double scale = this.scale / 1.5;

        if (cjkFont == null) return font.render(mesh, text, x, y, color, scale);

        double cjkScale = scale * font.getHeight() / (double) cjkFont.getHeight();

        int length = text.length();
        int i = 0;

        while (i < length) {
            boolean cjk = useCjk(text.charAt(i));

            int j = i + 1;
            while (j < length && useCjk(text.charAt(j)) == cjk) j++;

            String run = text.substring(i, j);

            if (cjk) x = cjkFont.render(cjkMesh, run, x, y, color, cjkScale);
            else x = font.render(mesh, run, x, y, color, scale);

            i = j;
        }

        return x;
    }

    private boolean useCjk(int codepoint) {
        return cjkFont != null && !font.hasGlyph(codepoint) && cjkFont.hasGlyph(codepoint);
    }

    @Override
    public boolean isBuilding() {
        return building;
    }

    @Override
    public void end() {
        if (!building) throw new RuntimeException("CustomTextRenderer.end() called without calling begin()");

        if (!scaleOnly) {
            mesh.end();

            if (mesh.getIndicesCount() > 0) {
                MeshRenderer.begin()
                    .attachments(Minecraft.getInstance().gameRenderer.mainRenderTarget())
                    .pipeline(MeteorRenderPipelines.UI_TEXT)
                    .mesh(mesh)
                    .sampler("u_Texture", font.texture.getTextureView(), font.texture.getSampler())
                    .end();
            }

            if (cjkFont != null) {
                cjkMesh.end();

                if (cjkMesh.getIndicesCount() > 0) {
                    MeshRenderer.begin()
                        .attachments(Minecraft.getInstance().gameRenderer.mainRenderTarget())
                        .pipeline(MeteorRenderPipelines.UI_TEXT)
                        .mesh(cjkMesh)
                        .sampler("u_Texture", cjkFont.texture.getTextureView(), cjkFont.texture.getSampler())
                        .end();
                }
            }
        }

        building = false;
        scale = 1;
    }

    public void destroy() {
        for (Font font : this.fonts) {
            font.texture.close();
        }
    }
}
