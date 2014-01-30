package com.retroarch.browser.vektorgui.ui;

import java.io.File;

import com.retroarch.R;
import com.retroarch.browser.preferences.util.UserPreferences;
import com.retroarch.browser.vektorgui.VektorGuiActivity;
import com.retroarch.browser.vektorgui.VektorGuiPlatformHelper;
import com.retroarch.browser.vektorgui.ui.views.VektorGuiButton;

import android.content.SharedPreferences;
import android.net.Uri;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnKeyListener;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ImageView;
import android.widget.ListView;

public class VektorGuiRomList implements OnItemClickListener, OnItemSelectedListener {
	private ListView romList;
	private VektorGuiRomAdapter romListAdapter;
	private VektorGuiActivity rootActivity;
	
	public VektorGuiRomList(VektorGuiActivity rootActivity) {
		this.rootActivity = rootActivity.getTheActivity();
		romList = (ListView) rootActivity.findViewById(R.id.vektor_gui_game_list);
		romList.setSoundEffectsEnabled(false);
	}
	
	public int getSelectedItem(){
		return 0;
		//return romListAdapter.getSelectedItem();
	}

	public void populate() {
		romListAdapter = new VektorGuiRomAdapter(rootActivity);
		romList.setAdapter(romListAdapter);
		romListAdapter.notifyDataSetChanged();
		romList.setOnItemClickListener(this);
		romList.setOnItemSelectedListener(this);
		//romList.setOnKeyListener(this);
	}
	@Override
	public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
		VektorGuiRomItem item = romListAdapter.getItem(arg2);
		rootActivity.updateUI(item, arg2);
		File resStor = new File(rootActivity.getRomFolder(), "Resources");
		File coverStor = new File(resStor,
				VektorGuiPlatformHelper.cleanName(item.getROMPath().getName())
						+ "-CV.jpg");
		Log.i("onItemClick",
				coverStor.exists() + " " + (null == item.getGameCover()));
		if (coverStor.exists() && item.getGameCover() == null)
			rootActivity.addDecodingJob(Uri.fromFile(coverStor), item);
	}

	/*
	@Override
	public boolean onKey(View v, int keyCode, KeyEvent event) {
		if (event.getAction() == KeyEvent.ACTION_DOWN && null != romList
				&& null != romList.getSelectedView()
				&& romListAdapter.getCount() > 1) {
			int position = romListAdapter.getSelectedItem();
			switch (keyCode) {
			case KeyEvent.KEYCODE_DPAD_DOWN:
				if (position < romListAdapter.getCount() - 1) {
					rootActivity.updateUI(romListAdapter.getItem(position + 1), position + 1);
					romList.setSelection(position + 1);
					File resStor = new File(rootActivity.getRomFolder(), "Resources");
					File coverStor = new File(resStor,
							VektorGuiPlatformHelper.cleanName(romListAdapter
									.getItem(position + 1).getROMPath()
									.getName())
									+ "-CV.jpg");
					if (coverStor.exists()) {
						rootActivity.addDecodingJob(Uri.fromFile(coverStor),
								romListAdapter.getItem(position + 1));
					}
				}
				break;
			case KeyEvent.KEYCODE_DPAD_UP:
				if (position > 0) {
					rootActivity.updateUI(romListAdapter.getItem(position - 1), position - 1);
					romList.setSelection(position - 1);
					File resStor = new File(rootActivity.getRomFolder(), "Resources");
					File coverStor = new File(resStor,
							VektorGuiPlatformHelper.cleanName(romListAdapter
									.getItem(position - 1).getROMPath()
									.getName())
									+ "-CV.jpg");
					if (coverStor.exists()) {
						rootActivity.addDecodingJob(Uri.fromFile(coverStor),
								romListAdapter.getItem(position - 1));
					}
				}
				break;
			case KeyEvent.KEYCODE_ENTER:
			case KeyEvent.KEYCODE_BUTTON_START:
				if (romListAdapter.getSelectedItem() > -1)
					rootActivity.romExecute(romListAdapter.getItem(
							romListAdapter.getSelectedItem()).getRomPath());
				break;
			}
		}
		return false;
	}
*/
	public VektorGuiRomItem getItem(int selectedItem) {
		return romListAdapter.getItem(selectedItem);
	}

	public void selectRow(int position) {
	}

	public void setSelection(int position) {
		romList.setSelection(position);
	}

	public View getSelectedView() {
		return romList.getSelectedView();
	}

	public void notifyDataSetChanged() {
		romListAdapter.notifyDataSetChanged();
	}

	@Override
	public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2,
			long arg3) {
		rootActivity.updateUI(romListAdapter.getItem(arg2), arg2);
	}

	@Override
	public void onNothingSelected(AdapterView<?> arg0) {
		
	}
	
	
}
