/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.gui.widgets;

import meteordevelopment.meteorclient.gui.widgets.pressable.WPressable;
import meteordevelopment.meteorclient.utils.misc.Translate;
import meteordevelopment.meteorclient.utils.render.color.Color;
import net.minecraft.client.input.MouseButtonEvent;

public abstract class WLabel extends WPressable {
    public Color color;

    protected String text;
    protected String displayText;
    protected boolean title;

    public WLabel(String text, boolean title) {
        this.text = text;
        this.displayText = Translate.text(text);
        this.title = title;
    }

    @Override
    protected void onCalculateSize() {
        width = theme.textWidth(displayText, displayText.length(), title);
        height = theme.textHeight(title);
    }

    @Override
    public boolean onMouseClicked(MouseButtonEvent click, boolean doubled) {
        if (action != null) return super.onMouseClicked(click, doubled);
        return false;
    }

    @Override
    public boolean onMouseReleased(MouseButtonEvent click) {
        if (action != null) return super.onMouseReleased(click);
        return false;
    }

    public void set(String text) {
        String displayText = Translate.text(text);

        if (Math.round(theme.textWidth(displayText, displayText.length(), title)) != width) invalidate();

        this.text = text;
        this.displayText = displayText;
    }

    public String get() {
        return text;
    }

    /**
     * 实际绘制用的文本（已汉化），{@link #get()} 始终返回原始文本。
     */
    public String getDisplayText() {
        return displayText;
    }

    public WLabel color(Color color) {
        this.color = color;
        return this;
    }
}
