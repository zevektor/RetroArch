package com.retroarch.browser.vektorgui;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.retroarch.R;
import com.retroarch.browser.ModuleWrapper;
import com.retroarch.browser.preferences.util.UserPreferences;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

public class VektorGuiPlatformHelper {
	public static String[] getPlatformList() {
		String[] pList = { "Nintendo Entertainment System",
				"Super Nintendo Entertainment System", "Game Boy",
				"Game Boy Color", "Game Boy Advance","MAME", "Nintendo 64",
				"Nintendo DS", "PlayStation", "Sega Genesis",
				"Sega Master System", "Sega Game Gear", "TurboGrafx-16" };
		return pList;
	}

	public static Drawable getIcon(Context ctx, String name) {
		if ("Nintendo Entertainment System".equals(name))
			return ctx.getResources().getDrawable(R.drawable.platform_nes);
		else if ("Super Nintendo Entertainment System".equals(name))
			return ctx.getResources().getDrawable(R.drawable.platform_snes);
		else if ("Nintendo 64".equals(name))
			return ctx.getResources().getDrawable(R.drawable.platform_n64);
		else if ("Game Boy Advance".equals(name))
			return ctx.getResources().getDrawable(R.drawable.platform_gba);
		else if ("PlayStation".equals(name))
			return ctx.getResources().getDrawable(R.drawable.platform_psx);
		else if ("Sega Genesis".equals(name))
			return ctx.getResources().getDrawable(R.drawable.platform_md);
		else if ("Game Boy".equals(name))
			return ctx.getResources().getDrawable(R.drawable.platform_gb);
		else if ("Game Boy Color".equals(name))
			return ctx.getResources().getDrawable(R.drawable.platform_gbc);
		else if ("Sega Master System".equals(name))
			return ctx.getResources().getDrawable(R.drawable.platform_ms);
		else if ("Sega Game Gear".equals(name))
			return ctx.getResources().getDrawable(R.drawable.platform_gg);
		else if ("TurboGrafx-16".equals(name))
			return ctx.getResources().getDrawable(R.drawable.platform_pce);
		else if ("Nintendo DS".equals(name))
			return ctx.getResources().getDrawable(R.drawable.platform_nds);
		else if ("MAME".equals(name))
			return ctx.getResources().getDrawable(R.drawable.platform_mame);
		else
			return null;
	}

	public static List<ModuleWrapper> getCoreList(Context ctx) {
		final String cpuInfo = UserPreferences.readCPUInfo();
		final boolean cpuIsNeon = cpuInfo.contains("neon");
		// Populate the list
		final List<ModuleWrapper> cores = new ArrayList<ModuleWrapper>();
		final File[] libs = new File(ctx.getApplicationInfo().dataDir, "cores")
				.listFiles();
		for (final File lib : libs) {
			String libName = lib.getName();

			// Never append a NEON lib if we don't have NEON.
			if (libName.contains("neon") && !cpuIsNeon)
				continue;

			// If we have a NEON version with NEON capable CPU,
			// never append a non-NEON version.
			if (cpuIsNeon && !libName.contains("neon")) {
				boolean hasNeonVersion = false;
				for (final File lib_ : libs) {
					String otherName = lib_.getName();
					String baseName = libName.replace(".so", "");
					if (otherName.contains("neon")
							&& otherName.startsWith(baseName)) {
						hasNeonVersion = true;
						break;
					}
				}

				if (hasNeonVersion)
					continue;
			}

			cores.add(new ModuleWrapper(ctx, lib));
		}

		// Sort the list of cores alphabetically
		Collections.sort(cores);
		return cores;
	}

	public static ModuleWrapper findCore(List<ModuleWrapper> cores,
			String strName) {
		for (ModuleWrapper core : cores) {
			if (null != core && strName.equals(core.getText()))
				return core;
		}
		return null;
	}

	public static String cleanName(String str) {
		if (str.startsWith(".") && str.lastIndexOf(".") == 0) {
			return str;
		} else if (str.lastIndexOf(".") != -1) {
			return str.substring(0, str.lastIndexOf("."));
		} else
			return str;
	}
	
	public static boolean isOnline(Context ctx) {
		ConnectivityManager cm = (ConnectivityManager) ctx
				.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo netInfo = cm.getActiveNetworkInfo();
		if (netInfo != null && netInfo.isConnected()) {
			return true;
		}
		return false;
	}
}
