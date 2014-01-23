package com.retroarch.browser.vektorgui.ui;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.retroarch.browser.ModuleWrapper;
import com.retroarch.browser.mainmenu.MainMenuActivity;
import com.retroarch.browser.preferences.util.UserPreferences;
import com.retroarch.browser.vektorgui.VektorGuiActivity;
import com.retroarch.browser.vektorgui.VektorGuiPlatformHelper;

import com.retroarch.R;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.ActionProvider;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.SubMenu;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;

public class VektorPlatformPickActionProvider extends ActionProvider implements
		OnMenuItemClickListener {

	static final int LIST_LENGTH = 3;

	Context mContext;
	VektorGuiActivity theActivity = VektorGuiActivity.getTheActivity();

	public VektorPlatformPickActionProvider(Context context) {
		super(context);
		mContext = context;
	}

	@Override
	public View onCreateActionView() {
		Log.d(this.getClass().getSimpleName(), "onCreateActionView");

		TextView textView = new TextView(mContext);
		textView.setText("");

		return null; //
	}

	@Override
	public boolean onPerformDefaultAction() {
		Log.d(this.getClass().getSimpleName(), "onPerformDefaultAction");

		return super.onPerformDefaultAction();
	}

	@Override
	public boolean hasSubMenu() {
		Log.d(this.getClass().getSimpleName(), "hasSubMenu");

		return true;
	}

	@Override
	public void onPrepareSubMenu(SubMenu subMenu) {
		Log.d(this.getClass().getSimpleName(), "onPrepareSubMenu");

		subMenu.clear();
		String[] pList = VektorGuiPlatformHelper.getPlatformList();
		for (int i = 0; i < pList.length; i++) {
			subMenu.add(0, i, i, pList[i])
					.setIcon(
							VektorGuiPlatformHelper.getIcon(mContext, pList[i]))
					.setOnMenuItemClickListener(this);
		}

	}

	@Override
	public boolean onMenuItemClick(MenuItem item) {
		final String platform = item.getTitle().toString();
		AlertDialog.Builder builderSingle = new AlertDialog.Builder(mContext);
		builderSingle.setIcon(R.drawable.ic_launcher);

		builderSingle.setTitle("Select core for " + platform);

		final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(
				mContext, android.R.layout.select_dialog_singlechoice);
		final List<ModuleWrapper> cores = VektorGuiPlatformHelper.getCoreList(mContext);
		prepareAdapter(platform, cores, arrayAdapter);
		builderSingle.setNegativeButton("Cancel",
				new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
					}
				});

		builderSingle.setAdapter(arrayAdapter,
				new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						String strName = arrayAdapter.getItem(which);
						
						final ModuleWrapper item = VektorGuiPlatformHelper.findCore(cores, strName);
						theActivity.setModuleAndPlatform(item
								.getUnderlyingFile().getAbsolutePath(), item
								.getText(), platform,item.getSupportedExtensions(),true);
						UserPreferences.updateConfigFile(theActivity);

					}
				});
		builderSingle.show();
		return true;
	}
	
	/**
	 * 
	 * @param platform
	 * @param cores
	 * @param adapter
	 */
	private void prepareAdapter(String platform, List<ModuleWrapper> cores,
			ArrayAdapter<String> adapter) {
		if ("Sega Genesis".equals(platform)) {
			adapter.add("Genesis Plus GX");
			adapter.add("Picodrive");
		} else if ("Sega Game Gear".equals(platform)
				|| "Sega Master System".equals(platform)) {
			adapter.add("Genesis Plus GX");
		} else if ("Game Boy".equals(platform)) {
			adapter.add("Gambatte");
		} else if("Nintendo Entertainment System".equals(platform)){
			adapter.add("FCEUmm");
			adapter.add("Nestopia");
			adapter.add("QuickNES");
		} else if("MAME".equals(platform)){
			adapter.add("MAME 2003 (0.78)");
			adapter.add("MAME 2010 (0.139)");
		}
		else
			for (int i = 0; i < cores.size(); i++) {
				if (cores.get(i).getEmulatedSystemName().contains(platform)) {
					adapter.add(cores.get(i).getText());
				}
			}

	}

}