package com.retroarch.browser.vektorgui.utils;

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
	private long enqueued;
	private DownloadManager mManager;
	private VektorGuiActivity rootActivity;
	private VektorGuiRomItem item;
	//private XPMBSubmenu_ROM submenu;

	public VektorGuiBroadcastReceiver(long enqueued, DownloadManager mManager,
			VektorGuiActivity rootActivity, VektorGuiRomItem item) {
		this.enqueued = enqueued;
		Log.i("BroadcastReceiver", "DownID=" + enqueued);
		this.mManager = mManager;
		this.rootActivity = rootActivity;
		this.item = item;
		//this.submenu = submenu;
	}

	@Override
	public void onReceive(Context context, Intent intent) {
		String action = intent.getAction();
		if (DownloadManager.ACTION_DOWNLOAD_COMPLETE.equals(action)) {
			long downloadId = intent.getLongExtra(
					DownloadManager.EXTRA_DOWNLOAD_ID, 0);
			if (downloadId == enqueued) {
				Query query = new Query();
				query.setFilterById(enqueued); // dlid
				Cursor c = mManager.query(query);
				if (c.moveToFirst()) {
					int columnIndex = c
							.getColumnIndex(DownloadManager.COLUMN_STATUS);
					if (DownloadManager.STATUS_SUCCESSFUL == c
							.getInt(columnIndex)) {
						String uriString = c
								.getString(c
										.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI));
						final Uri fileUri = Uri.parse(uriString);
						rootActivity.addDecodingJob(fileUri, item.getGameName(),
								item);
						c.close();
						return;
					}
				}
				c.close();
			}
		}
	}

}
