package com.retroarch.browser.vektorgui.ui;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.retroarch.R;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

public class VektorGuiRomAdapter extends BaseAdapter {

	private List<VektorGuiRomItem> roms;
	private int selectedItem = -1;
	private Context mContext;

	public VektorGuiRomAdapter(List<VektorGuiRomItem> roms, Context ctx) {
		this.roms = roms;
		this.mContext = ctx;
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
			LayoutInflater inflater = (LayoutInflater) mContext
					.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			v = inflater.inflate(R.layout.vektor_gui_game, null);
		}
		VektorGuiTextView gameTitle = (VektorGuiTextView) v
				.findViewById(R.id.vektor_gui_list_gamename);
		LinearLayout gameBg = (LinearLayout) v
				.findViewById(R.id.vektor_gui_list_bg);
		if (position == selectedItem) {
			gameTitle.setTextColor(Color.BLACK);
			gameTitle.setSelected(true);
			gameBg.setBackgroundColor(Color.WHITE);

		} else {
			gameTitle.setTextColor(Color.WHITE);
			gameTitle.setSelected(false);
			gameBg.setBackgroundColor(Color.BLACK);
		}
		gameTitle.setText(entry.getGameName());
		return v;
	}

	@Override
	public void notifyDataSetChanged() {
		super.notifyDataSetChanged();
	}

	public void selectRow(int position) {
		selectedItem = position;
		notifyDataSetChanged();
	}

	public int getSelectedItem() {
		return selectedItem;
	}
}
