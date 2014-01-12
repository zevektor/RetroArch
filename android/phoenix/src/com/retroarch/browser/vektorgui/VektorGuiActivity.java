package com.retroarch.browser.vektorgui;

import com.retroarch.R;
import com.retroarch.browser.vektorgui.ui.VektorGuiTextView;

import android.app.Activity;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.widget.ImageView;
import android.widget.TextView;

public class VektorGuiActivity extends Activity {
	private VektorGuiTextView gametitle;
	private ImageView gamecover;
	private VektorGuiTextView gamedesctitle;
	private VektorGuiTextView gamedesc;
	
	private void defineUI(){
		gametitle = (VektorGuiTextView) findViewById(R.id.vektor_gui_gametitle);
		gamedesc = (VektorGuiTextView) findViewById(R.id.vektor_gui_gamedesc);
		gamedesc.setMovementMethod(new ScrollingMovementMethod());
		gamecover = (ImageView) findViewById(R.id.vektor_gui_gamecover);
		gamedesctitle = (VektorGuiTextView) findViewById(R.id.vektor_gui_gamedesc_title);
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.vektor_gui_layout);
		defineUI();
	}
}
