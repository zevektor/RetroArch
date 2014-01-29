package com.retroarch.browser.vektorgui.utils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import android.app.DownloadManager;
import android.app.DownloadManager.Request;
import android.net.Uri;
import android.util.Log;

import com.retroarch.R;
import com.retroarch.browser.vektorgui.VektorGuiActivity;
import com.retroarch.browser.vektorgui.VektorGuiPlatformHelper;
import com.retroarch.browser.vektorgui.ui.VektorGuiRomItem;

public class VektorGuiArcadeHits {
	private File resStor;
	private VektorGuiActivity activity;
	private String platformPath;
	private VektorGuiRomItem item;
	private DownloadManager mManager;
	private static String baseURL = "http://www.arcadehits.net/";
	public VektorGuiArcadeHits(File resStor, VektorGuiActivity activity,
			String platformPath, VektorGuiRomItem item, DownloadManager mManager) {
		this.resStor=resStor;
		this.activity=activity;
		this.platformPath=platformPath;
		this.item=item;
		this.mManager=mManager;
	}
	public boolean DownloadFromUrl() {
		boolean isOnline = VektorGuiPlatformHelper.isOnline(activity);
		if(isOnline){
			getCoverLink();
		}
		return isOnline;
	}
	private void getCoverLink() {
		try {
			Document doc = Jsoup.connect("http://arcadehits.net/index.php?p=roms&jeu="+item.getGameName()).timeout(0).get();
			Elements els = doc.getElementsByTag("h4");
			String gameName = els.get(0).text();
			if("Derniers jeux commentés".equalsIgnoreCase(gameName)) return;
			els = doc.select("a[href~=yearz]");
			if(els.size()>0) item.setGameYear(els.get(0).text());
			item.setGameName(gameName);
			item.setGameDescription(activity.getResources().getString(R.string.vektor_gui_game_no_description));
			try {
				File props = new File(resStor, VektorGuiPlatformHelper.cleanName(item.getROMPath()
						.getName()) + ".prop");
				item.toProperties()
						.store(new FileWriter(props), "");
			} catch (IOException e) {
			}
			els = doc.select("img.minithumb");
			String coverURL = null;
			if(els.size()>0) coverURL = baseURL + els.get(0).attr("src");
			if (coverURL != null) {
				coverURL = coverURL.replace("http://", "");
				coverURL = ("http://" + Uri.encode(coverURL)).replace(
						"%2F", "/");
				File cover = new File(resStor, VektorGuiPlatformHelper.cleanName(item.getROMPath()
						.getName()) + "-CV.jpg");
				Request req = new Request(Uri.parse(coverURL))
						.setTitle(item.getGameName())
						.setDescription("MAME")
						.setDestinationUri(Uri.fromFile(cover))
						.setAllowedNetworkTypes(
								Request.NETWORK_MOBILE
										| Request.NETWORK_WIFI)
						.setNotificationVisibility(
								Request.VISIBILITY_VISIBLE);
				activity.addGameToReceiver(mManager.enqueue(req),
						item);
			}
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
