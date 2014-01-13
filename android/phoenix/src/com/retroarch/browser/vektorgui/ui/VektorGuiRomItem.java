package com.retroarch.browser.vektorgui.ui;

import java.io.File;
import java.util.Properties;

import android.graphics.drawable.Drawable;
import android.util.Log;


public class VektorGuiRomItem {

	private Drawable bmGameCover = null, bmGameBackground = null;
	private File fROMPath = null;
	private String strGameName = null, 
			//strGameCode = null,
			strGameCRC = null,
			strGameDescription = null,
			strGameYear = null;

	public VektorGuiRomItem(File romPath, String gameCRC) {
		fROMPath = romPath;
		strGameName = fROMPath.getName();
		strGameCRC = gameCRC;
	}

	public File getROMPath() {
		return fROMPath;
	}

	public void setGameYear(String gameYear){
		strGameYear = gameYear;
	}
	
	public String getGameYear(){	
		return strGameYear;
	}
	/*
	public void setGameBackground(Drawable gameBackground) {
		bmGameBackground = gameBackground;
	}

	public Drawable getGameBackground() {
		return bmGameBackground;
	}
	*/
	public void setGameCover(Drawable cover) {
		bmGameCover = cover;
	}

	public Drawable getGameCover() {
		return bmGameCover;
	}

	public void setGameName(String gameName) {
		strGameName = gameName;
	}

	public String getGameName() {
		return strGameName;
	}

	public String getGameCRC() {
		return strGameCRC;
	}
	
	public void setGameDescription(String gameDescription) {
		strGameDescription = gameDescription;
	}

	public String getGameDescription() {
		return strGameDescription;
	}
	
	public Properties toProperties(){
		Properties props = new Properties();
		props.setProperty("Title", strGameName);
		props.setProperty("Description", strGameDescription);
		props.setProperty("Year", strGameYear);
		return props;
	}
	public VektorGuiRomItem(Properties props){
		strGameName=props.getProperty("Title");
		strGameDescription=props.getProperty("Description");
		strGameYear=props.getProperty("Year");
	}
	public void fromProperties(Properties props){
		strGameName=props.getProperty("Title");
		strGameDescription=props.getProperty("Description");
		strGameYear=props.getProperty("Year");
		//Log.i("VektorGuiRomItem::fromProperties()","Title="+strGameName+" Description="+strGameDescription+" Year="+strGameYear);
	}
}