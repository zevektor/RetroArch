package com.retroarch.browser.vektorgui.ui;

import java.util.ArrayList;
import java.util.List;

import com.retroarch.R;
import com.retroarch.browser.ModuleWrapper;
import com.retroarch.browser.preferences.util.UserPreferences;
import com.retroarch.browser.vektorgui.VektorGuiActivity;
import com.retroarch.browser.vektorgui.VektorGuiPlatformHelper;
import com.retroarch.browser.vektorgui.ui.views.VektorGuiButton;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.ImageView;

public class VektorPlatformNCorePicker implements OnClickListener {
	private ImageView platformIcon;
	private VektorGuiButton coreName;
	private VektorGuiActivity rootActivity;

	public VektorPlatformNCorePicker(VektorGuiActivity rootActivity) {
		this.rootActivity = VektorGuiActivity.getTheActivity();
		platformIcon = (ImageView) rootActivity
				.findViewById(R.id.vektor_gui_core_icon);
		coreName = (VektorGuiButton) rootActivity
				.findViewById(R.id.vektor_gui_platform_pick);
		coreName.setOnClickListener(this);
		SharedPreferences prefs = UserPreferences.getPreferences(rootActivity);
		String platform = prefs.getString("vektor_gui_last_platform", null);
		String name = prefs.getString("libretro_name", null);
		if(null!=platform && null != name){
			platformIcon.setImageDrawable(VektorGuiPlatformHelper.getIcon(rootActivity, platform));
			coreName.setText(name);
		}
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.vektor_gui_platform_pick:
			showPickDialog();
			break;
		}
	}

	private void showPickDialog() {
		AlertDialog.Builder builderSingle = new AlertDialog.Builder(
				rootActivity);
		builderSingle.setIcon(R.drawable.ic_launcher);
		builderSingle.setTitle(rootActivity.getResources().getString(
				R.string.vektor_gui_platform_pick));
		final ArrayAdapter<String> platformAdapter = new ArrayAdapter<String>(
				rootActivity, android.R.layout.select_dialog_singlechoice);
		String[] platforms = VektorGuiPlatformHelper.getPlatformList();
		for (String platform : platforms) {
			platformAdapter.add(platform);
		}
		builderSingle.setNegativeButton(
				rootActivity.getResources().getString(R.string.close),
				new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
					}
				});

		builderSingle.setAdapter(platformAdapter,
				new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						final String platform = platformAdapter.getItem(which);
						AlertDialog.Builder builderInner = new AlertDialog.Builder(
								rootActivity);
						builderInner.setIcon(R.drawable.ic_launcher);
						// builderInner.setMessage(platform);
						builderInner.setTitle(rootActivity.getResources()
								.getString(R.string.vektor_gui_core_pick));
						final ArrayAdapter<String> coreAdapter = new ArrayAdapter<String>(
								rootActivity,
								android.R.layout.select_dialog_singlechoice);
						builderInner.setCancelable(false);
						ArrayList<String> cores = VektorGuiPlatformHelper
								.prepareAdapter(platformAdapter.getItem(which),
										rootActivity);
						for (String core : cores) {
							coreAdapter.add(core);
						}
						builderInner.setAdapter(coreAdapter,
								new DialogInterface.OnClickListener() {

									@Override
									public void onClick(DialogInterface dialog,
											int which) {
										String core = coreAdapter
												.getItem(which);
										ModuleWrapper coremw = VektorGuiPlatformHelper
												.findCore(core, rootActivity);
										rootActivity.setModuleAndPlatform(
												coremw.getUnderlyingFile()
														.getAbsolutePath(),
												core,
												platform,
												coremw.getSupportedExtensions(),
												true);
										UserPreferences.updateConfigFile(rootActivity);
										platformIcon.setImageDrawable(VektorGuiPlatformHelper.getIcon(rootActivity, platform));
										coreName.setText(core);
										dialog.dismiss();
									}
								});
						/*
						 * builderInner.setPositiveButton(rootActivity
						 * .getResources().getString(R.string.ok), new
						 * DialogInterface.OnClickListener() {
						 * 
						 * @Override public void onClick(DialogInterface dialog,
						 * int which) { dialog.dismiss(); } });
						 */
						builderInner.show();
					}
				});
		builderSingle.show();
	}
}
