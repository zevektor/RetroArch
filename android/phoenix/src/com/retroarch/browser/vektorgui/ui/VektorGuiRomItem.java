package com.retroarch.browser.vektorgui.ui;

import java.io.File;
import java.util.Properties;

import android.graphics.drawable.Drawable;
import android.util.Log;

public class VektorGuiRomItem {

	private Drawable bmGameCover = null, bmGameBackground = null;
	private File fROMPath = null;
	private String strGameName = null,
	// strGameCode = null,
			strGameCRC = null, strGameDescription = null, strGameYear = null;

	public VektorGuiRomItem(File romPath, String gameCRC) {
		fROMPath = romPath;
		strGameName = fROMPath.getName();
		strGameCRC = gameCRC;
	}

	public File getROMPath() {
		return fROMPath;
	}

	public void setGameYear(String gameYear) {
		strGameYear = gameYear;
	}

	public String getGameYear() {
		return strGameYear;
	}

	public String getRomPath() {
		return fROMPath.getAbsolutePath();
	}

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

	public void setGameCRC(String crc) {
		strGameCRC = crc;
	}

	public Properties toProperties() {
		Properties props = new Properties();
		if (null != strGameName)
			props.setProperty("Title", strGameName);
		if (null != strGameDescription)
			props.setProperty("Description", strGameDescription);
		if (null != strGameYear)
			props.setProperty("Year", strGameYear);
		if (null != strGameCRC)
			props.setProperty("CRC", strGameCRC);
		return props;
	}

	public VektorGuiRomItem(Properties props) {
		strGameName = props.getProperty("Title");
		strGameDescription = props.getProperty("Description");
		strGameYear = props.getProperty("Year");
		strGameCRC = props.getProperty("CRC");
	}

	public void fromProperties(Properties props) {
		strGameName = props.getProperty("Title");
		strGameDescription = props.getProperty("Description");
		strGameYear = props.getProperty("Year");
		strGameCRC = props.getProperty("CRC");
	}
}