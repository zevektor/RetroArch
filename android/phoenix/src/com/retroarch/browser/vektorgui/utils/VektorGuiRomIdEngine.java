package com.retroarch.browser.vektorgui.utils;

import java.io.File;
import java.util.ArrayList;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import android.app.DownloadManager;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;

import com.retroarch.browser.vektorgui.VektorGuiActivity;
import com.retroarch.browser.vektorgui.ui.VektorGuiRomItem;

public class VektorGuiRomIdEngine {
	private VektorGuiActivity rootActivity;
	/**
	 * Setting the ThreadPoolExecutor(s).
	 */
	private static final int CORE_POOL_SIZE = 5;
	private static final int MAXIMUM_POOL_SIZE = 128;
	private static final int NUMBER_OF_CORES = Runtime.getRuntime()
			.availableProcessors();
	private final BlockingQueue<Runnable> mDownloadWorkQueue = new LinkedBlockingQueue<Runnable>();
	private final BlockingQueue<Runnable> mDecodeWorkQueue = new LinkedBlockingQueue<Runnable>();
	private final ThreadPoolExecutor mDownloadThreadPool = new ThreadPoolExecutor(
			CORE_POOL_SIZE, MAXIMUM_POOL_SIZE, KEEP_ALIVE_TIME,
			KEEP_ALIVE_TIME_UNIT, mDownloadWorkQueue);
	private final ThreadPoolExecutor mDecodeThreadPool = new ThreadPoolExecutor(
			NUMBER_OF_CORES, NUMBER_OF_CORES, KEEP_ALIVE_TIME,
			KEEP_ALIVE_TIME_UNIT, mDecodeWorkQueue);
	private static final TimeUnit KEEP_ALIVE_TIME_UNIT;
	private static final int KEEP_ALIVE_TIME = 10;
	static {
		KEEP_ALIVE_TIME_UNIT = TimeUnit.SECONDS;
	}

	public VektorGuiRomIdEngine(final VektorGuiActivity rootActivity) {
		this.rootActivity = rootActivity;
		mDownloadThreadPool
				.setRejectedExecutionHandler(new RejectedExecutionHandler() {
					@Override
					public void rejectedExecution(Runnable r,
							ThreadPoolExecutor executor) {
					}
				});
		mDecodeThreadPool
				.setRejectedExecutionHandler(new RejectedExecutionHandler() {
					@Override
					public void rejectedExecution(Runnable r,
							ThreadPoolExecutor executor) {
					}
				});
	}

	public void clearQueues() {
		if (mDownloadWorkQueue.size() > 0) {
			mDownloadWorkQueue.drainTo(new ArrayList<Runnable>());
			mDownloadThreadPool.shutdownNow();
		}
		if (mDecodeWorkQueue.size() > 0) {
			mDecodeWorkQueue.drainTo(new ArrayList<Runnable>());
			mDecodeThreadPool.shutdownNow();
		}
	}

	private class VektorGuiROMTask implements Runnable {
		private File resStor;
		private VektorGuiActivity activity;
		private DownloadManager mManager;
		private VektorGuiRomItem item;

		public VektorGuiROMTask(VektorGuiActivity activity, File resStor,
				VektorGuiRomItem item, DownloadManager mManager) {
			this.resStor = resStor;
			this.activity = activity;
			this.mManager = mManager;
			this.item = item;
		}

		@SuppressWarnings("rawtypes")
		public void run() {
			try {
				Thread.sleep(0);
				android.os.Process
						.setThreadPriority(android.os.Process.THREAD_PRIORITY_DEFAULT);
				String platformPath = rootActivity.getPlatformPath();
				Log.i("ROMTask", "Game: " + item.getGameName());
				if (!platformPath.equalsIgnoreCase("MAME")) {
					VektorGuiTheGamesDB tgdb = new VektorGuiTheGamesDB(resStor,
							activity, platformPath, item, mManager);
					tgdb.DownloadFromUrl();
				} else if ("MAME".equalsIgnoreCase(platformPath)) {
					VektorGuiArcadeHits vgah = new VektorGuiArcadeHits(resStor,
							activity, platformPath, item, mManager);
					vgah.DownloadFromUrl();
				}
			} catch (InterruptedException ie) {
				return;
			}
			return;
		}

	}

	public void addDecodingJob(final Uri fileUri, final VektorGuiRomItem item) {
		mDecodeThreadPool.execute(new Runnable() {

			@Override
			public void run() {
				android.os.Process
						.setThreadPriority(android.os.Process.THREAD_PRIORITY_DISPLAY);
				BitmapFactory.Options opts = new BitmapFactory.Options();
				opts.inJustDecodeBounds = true;
				BitmapFactory.decodeFile(fileUri.getPath(), opts);
				int w = opts.outWidth;
				int h = opts.outHeight;
				opts.inSampleSize = Math.max(w, h) / 128;
				opts.inJustDecodeBounds = false;
				final BitmapDrawable bd = new BitmapDrawable(rootActivity
						.getResources(), BitmapFactory.decodeFile(
						fileUri.getPath(), opts));
				item.setGameCover(bd);
				Log.i("addDecodingJob()",
						(rootActivity.getRomList().getSelectedItem() > -1 ? ""
								+ rootActivity.getRomList().getSelectedItem()
								+ " "
								+ rootActivity
										.getRomList()
										.getItem(
												rootActivity.getRomList()
														.getSelectedItem())
										.getGameName() : "-1"));
				if (rootActivity.getRomList().getSelectedItem() > -1
						&& rootActivity
								.getRomList()
								.getItem(
										rootActivity.getRomList()
												.getSelectedItem())
								.getGameName()
								.equalsIgnoreCase(item.getGameName())) {
					rootActivity.runOnUiThread(new Runnable() {

						@Override
						public void run() {
							// TODO Auto-generated method stub
							rootActivity.getGameCover().setImageDrawable(bd);
						}

					});
				}
			}
		});
	}

	public void identifyRom(File resStor, VektorGuiRomItem item,
			DownloadManager mManager) {
		mDownloadThreadPool.execute(new VektorGuiROMTask(rootActivity, resStor,
				item, mManager));
	}
}
