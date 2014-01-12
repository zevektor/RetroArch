package com.retroarch.browser.vektorgui.ui;

import android.content.Context;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.widget.TextView;

public class VektorGuiTextView extends TextView {

    public VektorGuiTextView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    public VektorGuiTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public VektorGuiTextView(Context context) {
        super(context);
        init();
    }

    public void init() {
        Typeface tf = Typeface.createFromAsset(getContext().getAssets(), "fonts/lunchds.ttf");
        setTypeface(tf ,1);

    }

}
