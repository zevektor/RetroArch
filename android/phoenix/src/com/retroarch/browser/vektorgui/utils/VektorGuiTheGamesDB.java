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
	private String gameName;
	private VektorGuiActivity callerActivity;
	private File romRoot;
	private VektorGuiRomItem item;

	public VektorGuiTheGamesDB(File romRoot, VektorGuiActivity callerActivity,
			String gameName, VektorGuiRomItem item) {
		this(romRoot, callerActivity, gameName, null, item);
	}

	public VektorGuiTheGamesDB(File romRoot, VektorGuiActivity callerActivity,
			String gameName, String platform, VektorGuiRomItem item) {
		this.romRoot = romRoot;
		this.gameName = gameName;
		this.platform = platform;
		this.callerActivity = callerActivity;
		this.item = item;
	}

	private static String xml = null;

	private void getXmlFromUrl(String url) {
		AsyncHttpResponseHandler resHandlerDir = new AsyncHttpResponseHandler() {
			@Override
			public void onSuccess(String response) {
				//Log.i("Success", "OK=" + response);
				VektorGuiTheGamesDB.xml = response;
			}
		};
		return;
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
		Log.i("Match", "ROMNAME=" + romName + " DBNAME=" + dbName + " ->"
				+ rate + "%. Time elapsed:"
				+ (System.currentTimeMillis() - start) + " ms.");
		return rate;

	}

	private Long dlid = null;

	public boolean DownloadFromUrl() {
		if (this.isOnline()) {
			getCoverLink();
		}
		return this.isOnline();
	}

	private String coverURL = null;

	@SuppressWarnings("deprecation")
	private final void getCoverLink() {
		final long start = System.currentTimeMillis();

		final String cleanedGN = gameName.split("\\[")[0].split("\\(")[0];
		String URL = "GetGame.php?name=" + URLEncoder.encode(cleanedGN);
		if (null != this.platform && !this.platform.equalsIgnoreCase("other")) {
			URL += "&platform=" + URLEncoder.encode(getPlatform());
		}
		AsyncHttpResponseHandler resHandlerDir = new AsyncHttpResponseHandler() {
			@Override
			public void onFailure(Throwable error) {
				dlid = (long) -1;
			}

			@TargetApi(Build.VERSION_CODES.HONEYCOMB)
			@Override
			public void onSuccess(String response) {
				VektorGuiTheGamesDB.xml = response;
				int matchRate = 0, bestMatchId = -1;
				ArrayList<gameClass> games = VektorGuiGameSAXParser.parse(xml);
				for (int j = 0; j < games.size(); j++) {
					int rate = stringMatch(games.get(j).getTitle(), cleanedGN);
					if (rate > matchRate
							&& rate > VektorGuiTheGamesDB.THRESHOLD) {
						matchRate = rate;
						bestMatchId = j;
						if (matchRate == 101)
							break;
					}
				}
				if (bestMatchId > -1) { //If there's a candidate, we download data for it. 
					coverURL = VektorGuiGameSAXParser.getBaseURL()
							+ games.get(bestMatchId).getURL();
					item.setGameName(games.get(bestMatchId).getTitle());
					item.setGameYear(games.get(bestMatchId).getYear());
					item.setGameDescription(games.get(bestMatchId)
							.getDescription());
					int i = 0;
					i = bestMatchId;
					
					String DownloadUrl = (coverURL == null ? null : coverURL
							.replace("http://", ""));
					if (DownloadUrl != null) {
						if (!romRoot.exists()) {
							romRoot.mkdirs();
						}
						DownloadUrl = ("http://" + Uri.encode(DownloadUrl))
								.replace("%2F", "/");
						File props = new File(romRoot, item.getGameName()
								+ ".prop");
						try {
							item.toProperties().store(
									new FileWriter(props, true), "");
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}

						File file = new File(romRoot, gameName + "-CV.jpg");
						DownloadManager manager = (DownloadManager) callerActivity
								.getSystemService(Activity.DOWNLOAD_SERVICE);
						Request req = new Request(Uri.parse(DownloadUrl))
								.setNotificationVisibility(
										Request.VISIBILITY_VISIBLE)
								.setTitle(gameName)
								.setDescription(getPlatform())
								.setDestinationUri(Uri.fromFile(file))
								.setAllowedNetworkTypes(
										Request.NETWORK_MOBILE
												| Request.NETWORK_WIFI);
						long dlId = manager.enqueue(req);
						callerActivity.addGameToReceiver(dlId,item);
					}
				}
			}
		};
		TheGamesDBClient.get(URL, null, resHandlerDir);
	}
}
