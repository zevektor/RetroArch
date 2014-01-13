package com.retroarch.browser.vektorgui;

import java.util.ArrayList;

import com.retroarch.R;
import android.content.Context;
import android.graphics.drawable.Drawable;

public class VektorGuiPlatformHelper {
	public static String[] getPlatformList(){
		String[] pList={"Nintendo Entertainment System","Super Nintendo Entertainment System","Game Boy","Game Boy Color","Game Boy Advance","Nintendo 64","Nintendo DS","PlayStation","Sega Genesis","Sega Master System","Sega Game Gear","TurboGrafx-16"};
		return pList;
	}
	
	public static Drawable getIcon(Context ctx,String name){
		if("Nintendo Entertainment System".equals(name)) return ctx.getResources().getDrawable(R.drawable.platform_nes);
		else if("Super Nintendo Entertainment System".equals(name)) return ctx.getResources().getDrawable(R.drawable.platform_snes);
		else if("Nintendo 64".equals(name)) return ctx.getResources().getDrawable(R.drawable.platform_n64);
		else if("Game Boy Advance".equals(name)) return ctx.getResources().getDrawable(R.drawable.platform_gba);
		else if("PlayStation".equals(name)) return ctx.getResources().getDrawable(R.drawable.platform_psx);
		else if("Sega Genesis".equals(name)) return ctx.getResources().getDrawable(R.drawable.platform_md);
		else if("Game Boy".equals(name)) return ctx.getResources().getDrawable(R.drawable.platform_gb);
		else if("Game Boy Color".equals(name)) return ctx.getResources().getDrawable(R.drawable.platform_gbc);
		else if("Sega Master System".equals(name)) return ctx.getResources().getDrawable(R.drawable.platform_ms);
		else if("Sega Game Gear".equals(name)) return ctx.getResources().getDrawable(R.drawable.platform_gg);
		else if("TurboGrafx-16".equals(name)) return ctx.getResources().getDrawable(R.drawable.platform_pce);
		else if("Nintendo DS".equals(name)) return ctx.getResources().getDrawable(R.drawable.platform_nds);
		return null;
	}
}
