package com.retroarch.browser.vektorgui.utils;

import java.util.HashMap;

import com.retroarch.browser.vektorgui.VektorGuiActivity;
import com.retroarch.browser.vektorgui.ui.VektorGuiRomItem;

import android.app.DownloadManager;
import android.app.DownloadManager.Query;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

public class VektorGuiBroadcastReceiver<item> extends BroadcastReceiver {
	private DownloadManager mManager;
	private VektorGuiActivity rootActivity;
	// Downloads Map here to register just one receiver!
	private HashMap<Long, VektorGuiRomItem> activeDls = new HashMap<Long, VektorGuiRomItem>();

	public VektorGuiBroadcastReceiver(DownloadManager mManager,
			VektorGuiActivity rootActivity) {
		this.mManager = mManager;
		this.rootActivity = rootActivity;
	}

	@Override
	public void onReceive(Context context, Intent intent) {
		if (intent.getAction().equals(DownloadManager.ACTION_DOWNLOAD_COMPLETE)) {
			final long dlId = intent.getLongExtra(
					DownloadManager.EXTRA_DOWNLOAD_ID, 0);
			if (activeDls.containsKey(dlId)) {
				Log.i("VektorGuiBroadcastReceiver::onReceive()", dlId + " - "
						+ activeDls.get(dlId).getGameName());
				Query q = new Query().setFilterById(dlId);
				Cursor c = mManager.query(q);
				if (c.moveToFirst()) {
					int columnIndex = c
							.getColumnIndex(DownloadManager.COLUMN_STATUS);
					if (c.getInt(columnIndex) == DownloadManager.STATUS_SUCCESSFUL) {
						String uriString = c
								.getString(c
										.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI));
						final Uri fileUri = Uri.parse(uriString);
						rootActivity.runOnUiThread(new Runnable() {
							@Override
							public void run() {
								// TODO Auto-generated method stub
								rootActivity.addDecodingJob(fileUri,
										activeDls.get(dlId));
							}

						});
						activeDls.remove(dlId);
					}
				}
				c.close();
			}
		}
	}

	public void addGameId(long dlId, VektorGuiRomItem item) {
		activeDls.put(dlId, item);
	}

}
