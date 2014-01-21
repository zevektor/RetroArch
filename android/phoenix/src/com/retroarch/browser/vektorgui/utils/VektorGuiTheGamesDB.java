package com.retroarch.browser.vektorgui.utils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.Normalizer;
import java.text.Normalizer.Form;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;
import com.retroarch.browser.vektorgui.VektorGuiActivity;
import com.retroarch.browser.vektorgui.ui.VektorGuiRomItem;
import com.retroarch.browser.vektorgui.utils.Serialization.gameClass;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.DownloadManager;
import android.app.DownloadManager.Request;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

public class VektorGuiTheGamesDB {

	private static class TheGamesDBClient {
		public static final AsyncHttpClient client = new AsyncHttpClient();
		private static final String BASEURL = "http://thegamesdb.net/api/";

		public static void get(String url, RequestParams params,
				AsyncHttpResponseHandler responseHandler) {
			client.get(getAbsoluteUrl(url), params, responseHandler);
		}

		private static String getAbsoluteUrl(String relativeUrl) {
			return BASEURL + relativeUrl;
		}
	}

	private static final int THRESHOLD = 50;
	private String platform;
	private VektorGuiActivity callerActivity;
	private File romRoot;
	private VektorGuiRomItem item;
	private DownloadManager mManager;

	public VektorGuiTheGamesDB(File romRoot, VektorGuiActivity callerActivity,
			String platform, VektorGuiRomItem item, DownloadManager mManager) {
		this.romRoot = romRoot;
		this.callerActivity = callerActivity;
		this.platform = platform;
		this.item = item;
		this.mManager = mManager;
	}

	private String getPlatform() {
		if (platform.equals("PCE"))
			return "TurboGrafx 16";
		else if (platform.equals("MD"))
			return "Sega Genesis";
		else if (platform.equals("SMS"))
			return "Sega Master System";
		else if (platform.equals("GBA"))
			return "Nintendo Game Boy Advance";
		else if (platform.equals("GBC"))
			return "Nintendo Game Boy Color";
		else if (platform.equals("GB"))
			return "Nintendo Game Boy";
		else if (platform.equals("NES"))
			return "Nintendo Entertainment System (NES)";
		else if (platform.equals("SNES"))
			return "Super Nintendo (SNES)";
		else if (platform.equals("NDS"))
			return "Nintendo DS";
		else if (platform.equals("PSX")) {
			return "Sony Playstation";
		} else
			return "Other";
	}

	private boolean isOnline() {
		ConnectivityManager cm = (ConnectivityManager) callerActivity
				.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo netInfo = cm.getActiveNetworkInfo();
		if (netInfo != null && netInfo.isConnected()) {
			return true;
		}
		return false;
	}

	private int stringMatch(String romName, String dbName) {
		long start = System.currentTimeMillis();
		romName = Normalizer.normalize(romName, Form.NFD).replaceAll(
				"\\p{InCombiningDiacriticalMarks}+", "");
		dbName = Normalizer.normalize(dbName, Form.NFD).replaceAll(
				"\\p{InCombiningDiacriticalMarks}+", "");

		romName = romName.replaceAll("[^a-zA-Z0-9\\s]+", "");
		dbName = dbName.replaceAll("[^a-zA-Z0-9\\s]+", "");
		String[] rom = romName.split("\\s+");
		String[] db = dbName.split("\\s+");
		int match = 0;
		for (int i = 0; i < rom.length; i++) {
			for (int j = 0; j < db.length; j++) {

				if (rom[i].equalsIgnoreCase(db[j]))
					match++;
			}
		}

		int rate = (rom.length == db.length ? 1 : 0) + (match * 100)
				/ Math.max(rom.length, db.length);
		// Log.i("Match", "ROMNAME=" + romName + " DBNAME=" + dbName + " ->"
		// + rate + "%. Time elapsed:"
		// + (System.currentTimeMillis() - start) + " ms.");
		return rate;

	}

	public boolean DownloadFromUrl() {
		if (this.isOnline()) {
			getCoverLink();
		}
		return this.isOnline();
	}

	private String coverURL = null;

	@SuppressWarnings("deprecation")
	private void getCoverLink() {
		String url = "GetGame.php?name="
				+ URLEncoder.encode(item.getGameName());
		if (null != this.platform && !this.platform.equalsIgnoreCase("other")) {
			url += "&platform=" + URLEncoder.encode(getPlatform());
			// Log.i("GetCoverLink()", url);
			TheGamesDBClient.get(url, null, resHandlerDir);
		}
	}

	private AsyncHttpResponseHandler resHandlerDir = new AsyncHttpResponseHandler() {
		@Override
		public void onSuccess(String response) {
			// Log.i("onSuccess()",response);
			try {
				int matchRate = 0, bestMatch = -1;
				ArrayList<gameClass> games = VektorGuiGameSAXParser
						.parse(response);
				for (int i = 0; i < games.size(); i++) {
					int rate = stringMatch(games.get(i).getTitle(),
							item.getGameName());
					if (rate > THRESHOLD && rate > matchRate) {
						matchRate = rate;
						bestMatch = i;
						if (matchRate == 101)
							break;
					}
				}
				if (bestMatch > -1) {
					coverURL = VektorGuiGameSAXParser.getBaseURL()
							+ games.get(bestMatch).getURL();
					item.setGameName(games.get(bestMatch).getTitle());
					item.setGameDescription(games.get(bestMatch)
							.getDescription());
					item.setGameYear(games.get(bestMatch).getYear());
					if (coverURL != null) {
						coverURL = coverURL.replace("http://", "");
						coverURL = ("http://" + Uri.encode(coverURL)).replace(
								"%2F", "/");
						try {
							File props = new File(romRoot, item.getROMPath()
									.getName() + ".prop");
							item.toProperties()
									.store(new FileWriter(props), "");
						} catch (IOException e) {
						}
						File cover = new File(romRoot, item.getROMPath()
								.getName() + "-CV.jpg");
						Request req = new Request(Uri.parse(coverURL))
								.setTitle(item.getGameName())
								.setDescription(getPlatform())
								.setDestinationUri(Uri.fromFile(cover))
								.setAllowedNetworkTypes(
										Request.NETWORK_MOBILE
												| Request.NETWORK_WIFI)
								.setNotificationVisibility(
										Request.VISIBILITY_VISIBLE);
						callerActivity.addGameToReceiver(mManager.enqueue(req),
								item);
					}

				}
			} catch (NullPointerException e) {
				e.printStackTrace();
			}
		}
	};
}
