package com.retroarch.browser.vektorgui.ui;

import com.retroarch.R;
import com.retroarch.browser.mainmenu.MainMenuActivity;
import com.retroarch.browser.vektorgui.VektorGuiActivity;

import android.content.Intent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;

public class VektorGuiLeftMenu implements OnClickListener {
	private ImageButton homeButton;
	private VektorGuiActivity rootActivity;

	public VektorGuiLeftMenu(VektorGuiActivity rootActivity) {
		this.rootActivity=rootActivity;
		this.homeButton=(ImageButton)rootActivity.findViewById(R.id.vektor_gui_goto_home);
		homeButton.setOnClickListener(this);
	}

	@Override
	public void onClick(View v) {
		if(v.getId()==R.id.vektor_gui_goto_home){
			Intent i = new Intent(rootActivity,MainMenuActivity.class);
			rootActivity.startActivity(i);
		}
	}
}
