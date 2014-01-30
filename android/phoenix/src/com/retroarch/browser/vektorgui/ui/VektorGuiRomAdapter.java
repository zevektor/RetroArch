package com.retroarch.browser.vektorgui.ui;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.retroarch.R;
import com.retroarch.browser.vektorgui.VektorGuiActivity;
import com.retroarch.browser.vektorgui.ui.views.VektorGuiTextView;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class VektorGuiRomAdapter extends BaseAdapter implements OnClickListener {

	private List<VektorGuiRomItem> roms;
	private int selectedItem = -1;
	private VektorGuiActivity rootActivity;

	public VektorGuiRomAdapter(VektorGuiActivity rootActivity) {
		this.rootActivity = rootActivity;
		this.roms = rootActivity.getRoms();
		Log.i("VektorGuiRomAdapter", "Populating list with " + roms.size()
				+ " roms");
		Collections.sort(roms, new Comparator<VektorGuiRomItem>() {
			public int compare(VektorGuiRomItem vgri1, VektorGuiRomItem vgri2) {
				return vgri1.getGameName().toLowerCase()
						.compareTo(vgri2.getGameName().toLowerCase());
			}
		});
	}

	@Override
	public int getCount() {
		return roms.size();
	}

	@Override
	public VektorGuiRomItem getItem(int position) {
		return roms.get(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public View getView(int position, View v, ViewGroup vg) {
		VektorGuiRomItem entry = roms.get(position);
		if (null == v) {
			LayoutInflater inflater = (LayoutInflater) rootActivity
					.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			v = inflater.inflate(R.layout.vektor_gui_game, null);
		}
		VektorGuiTextView gameTitle = (VektorGuiTextView) v
				.findViewById(R.id.vektor_gui_list_gamename);
		ImageButton playGame = (ImageButton) v
				.findViewById(R.id.vektor_gui_list_playbtn);
		RelativeLayout gameBg = (RelativeLayout) v
				.findViewById(R.id.vektor_gui_list_bg);
		/*
		if (position == selectedItem) {
			gameTitle.setTextColor(Color.WHITE);
			gameTitle.setSelected(true);
			gameBg.setBackgroundColor(Color.GRAY);
			playGame.setVisibility(View.VISIBLE);
			playGame.setOnClickListener(this);
		} else {
			gameTitle.setTextColor(Color.BLACK);
			gameTitle.setSelected(false);
			gameBg.setBackgroundColor(Color.TRANSPARENT);
			playGame.setVisibility(View.GONE);
		}*/
		gameTitle.setText(entry.getGameName()
				+ (null == entry.getGameYear() ? "" : " - (" + entry.getGameYear()
						+ ")"));
		return v;
	}

	@Override
	public void notifyDataSetChanged() {
		super.notifyDataSetChanged();
	}
/*
	public void selectRow(int position) {
		selectedItem = position;
		notifyDataSetChanged();
	}

	public int getSelectedItem() {
		return selectedItem;
	}
*/
	@Override
	public void onClick(View v) {
		if (v.getId() == R.id.vektor_gui_list_playbtn) {
			//rootActivity.romExecute(getItem(getSelectedItem()).getRomPath());
		}
	}
}
