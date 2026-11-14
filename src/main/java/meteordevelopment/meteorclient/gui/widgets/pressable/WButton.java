/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.gui.widgets.pressable;

import meteordevelopment.meteorclient.gui.renderer.packer.GuiTexture;
import meteordevelopment.meteorclient.utils.misc.Translate;

public abstract class WButton extends WPressable {
    protected String text;
    protected String displayText;
    protected double textWidth;

    protected GuiTexture texture;

    public WButton(String text, GuiTexture texture) {
        this.text = text;
        this.displayText = Translate.text(text);
        this.texture = texture;

        if (text == null) instantTooltips = true;
    }

    @Override
    protected void onCalculateSize() {
        double pad = pad();

        String text = getDisplayText();

        if (text != null) {
            textWidth = theme.textWidth(text);

            width = pad + textWidth + pad;
            height = pad + theme.textHeight() + pad;
        }
        else {
            double s = theme.textHeight();

            width = pad + s + pad;
            height = pad + s + pad;
        }
    }

    public void set(String text) {
        String displayText = Translate.text(text);

        if (this.text == null || Math.round(theme.textWidth(displayText)) != textWidth) invalidate();

        this.text = text;
        this.displayText = displayText;
    }

    public String getText() {
        return text;
    }

    /**
     * 实际绘制用的文本（已汉化），{@link #getText()} 始终返回原始文本。
     */
    public String getDisplayText() {
        return displayText;
    }
}
