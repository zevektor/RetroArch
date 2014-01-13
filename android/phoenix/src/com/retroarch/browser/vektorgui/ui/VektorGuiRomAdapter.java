package com.retroarch.browser.vektorgui.ui;

import java.util.List;

import com.retroarch.R;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class VektorGuiRomAdapter extends BaseAdapter {
	
	private List<VektorGuiRomItem> roms;
	private Context mContext;
	
	public VektorGuiRomAdapter(List<VektorGuiRomItem> roms, Context ctx){
		this.roms=roms;
		this.mContext=ctx;
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
		VektorGuiTextView gameTitle = (VektorGuiTextView) v.findViewById(R.id.vektor_gui_list_gamename);
		gameTitle.setText(entry.getGameName());
		return v;
	}
	
	@Override
	public void notifyDataSetChanged(){
		super.notifyDataSetChanged();
	}
}
