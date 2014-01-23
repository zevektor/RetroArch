package com.retroarch.browser.vektorgui;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import com.retroarch.browser.vektorgui.utils.ROMInfo;
import com.retroarch.browser.vektorgui.utils.VektorGuiBroadcastReceiver;
import com.retroarch.browser.vektorgui.utils.VektorGuiDatabaseHelper;
import com.retroarch.browser.vektorgui.utils.VektorGuiTheGamesDB;
import com.retroarch.R;
import com.retroarch.browser.vektorgui.ui.VektorGuiButton;
import com.retroarch.browser.vektorgui.ui.VektorGuiRomAdapter;
import com.retroarch.browser.vektorgui.ui.VektorGuiRomItem;
import com.retroarch.browser.vektorgui.ui.VektorGuiTextView;
import com.retroarch.browser.preferences.util.UserPreferences;
import com.retroarch.browser.retroactivity.RetroActivityFuture;
import com.retroarch.browser.retroactivity.RetroActivityPast;

import android.app.ActionBar;
import android.app.Activity;
import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.View.OnKeyListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Toast;

public class VektorGuiActivity extends Activity implements OnItemClickListener,
		OnClickListener, OnKeyListener {
	private static VektorGuiActivity theActivity;
	private VektorGuiTextView gametitle;
	private ImageView gamecover;
	private VektorGuiTextView gamedesc;
	private VektorGuiTextView gameyear;
	private VektorGuiTextView numGames;
	private VektorGuiButton playgame;
	private VektorGuiRomAdapter romListAdapter;
	private boolean creatingUI;
	private ListView romList;
	private List<String> supported_extensions;
	private MediaPlayer mp;
	private String platformPath;
	private VektorGuiBroadcastReceiver receiver;
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
	/**
	 * The list of rom elements
	 */
	private ArrayList<VektorGuiRomItem> roms = new ArrayList<VektorGuiRomItem>();
	private ROMInfo ridROMInfoDat;
	private int xmlId;
	private File romFolder;
	private VektorGuiDatabaseHelper myDbHelper;
	private DownloadManager mManager;
	private String platform;

	private void defineUI() {
		gametitle = (VektorGuiTextView) findViewById(R.id.vektor_gui_gametitle);
		gamedesc = (VektorGuiTextView) findViewById(R.id.vektor_gui_gamedesc);
		gamedesc.setMovementMethod(new ScrollingMovementMethod());
		gamecover = (ImageView) findViewById(R.id.vektor_gui_gamecover);
		gameyear = (VektorGuiTextView) findViewById(R.id.vektor_gui_gameyear);
		playgame = (VektorGuiButton) findViewById(R.id.vektor_gui_playgame_btn);
		numGames = (VektorGuiTextView) findViewById(R.id.vektor_gui_list_numgamesfound);
		numGames.setVisibility(View.GONE);
		ActionBar actionBar = getActionBar();
		actionBar.setDisplayHomeAsUpEnabled(true);
		this.getActionBar().setDisplayShowCustomEnabled(true);
		this.getActionBar().setDisplayShowTitleEnabled(false);
		LayoutInflater inflator = (LayoutInflater) this
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View v = inflator.inflate(R.layout.vektor_gui_titlecustom, null);
		((VektorGuiTextView) v.findViewById(R.id.vektor_gui_app_title))
				.setText(this.getTitle());
		this.getActionBar().setCustomView(v);
		playgame.setOnClickListener(this);
		// showLeftPanel(false);
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		VektorGuiActivity.theActivity = this;
		super.onCreate(savedInstanceState);
		this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
				WindowManager.LayoutParams.FLAG_FULLSCREEN);
		mp = MediaPlayer.create(getApplicationContext(), R.raw.button11);
		romListAdapter = new VektorGuiRomAdapter(roms, getApplicationContext());
		mManager = (DownloadManager) getSystemService(Activity.DOWNLOAD_SERVICE);
		setContentView(R.layout.vektor_gui_layout);
		defineUI();
		SharedPreferences prefs = UserPreferences.getPreferences(this);
		String core = prefs.getString("libretro_path", null);
		String name = prefs.getString("libretro_name", null);
		String platform = prefs.getString("vektor_gui_last_platform", null);
		setModuleAndPlatform(core, name, platform, VektorGuiPlatformHelper
				.findCore(VektorGuiPlatformHelper.getCoreList(this), name)
				.getSupportedExtensions(), true);
		receiver = new VektorGuiBroadcastReceiver(mManager, this);
		registerReceiver(receiver, new IntentFilter(
				DownloadManager.ACTION_DOWNLOAD_COMPLETE));
	}

	@Override
	protected void onPause() {
		mp.release();
		mp = null;
		super.onPause();
	}

	@Override
	protected void onStop() {
		try {
			unregisterReceiver(receiver);
		} catch (IllegalArgumentException ile) {

		}
		super.onStop();
	}

	@Override
	protected void onResume() {
		super.onResume();
		mp = MediaPlayer.create(getApplicationContext(), R.raw.button11);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);

		{
			VektorGuiActivity.theActivity = this;
			MenuInflater inflater = new MenuInflater(this);
			inflater.inflate(R.menu.vektor_gui_actionprovider_platform, menu);
		}

		return true;
	}

	public void setModuleAndPlatform(String core_path, String core_name,
			String platform, List<String> supported_extensions, boolean init) {
		if (!init)
			return;
		if (mDownloadWorkQueue.size() > 0) {
			mDownloadWorkQueue.drainTo(new ArrayList<Runnable>());
			mDownloadThreadPool.shutdownNow();
		}
		if (mDecodeWorkQueue.size() > 0) {
			mDecodeWorkQueue.drainTo(new ArrayList<Runnable>());
			mDecodeThreadPool.shutdownNow();
		}
		creatingUI = true;
		if (null == core_path || null == core_name || null == platform)
			return;
		UserPreferences.updateConfigFile(this);
		SharedPreferences prefs = UserPreferences.getPreferences(this);
		SharedPreferences.Editor edit = prefs.edit();
		edit.putString("libretro_path", core_path);
		edit.putString("libretro_name", core_name);
		edit.putString("vektor_gui_last_platform", platform);
		edit.commit();
		edit.commit();
		this.platform = platform;
		VektorGuiTextView platform_name = (VektorGuiTextView) findViewById(R.id.vektor_gui_list_platformname_title);
		VektorGuiTextView platform_core = (VektorGuiTextView) findViewById(R.id.vektor_gui_list_platformcore_title);
		platform_name.setText(getResources().getString(
				R.string.vektor_gui_platform_name)
				+ " " + platform);
		platform_core.setText(getResources().getString(
				R.string.vektor_gui_platform_core)
				+ " " + core_name);
		this.supported_extensions = supported_extensions;
		this.platformPath = consolePath(platform);
		this.xmlId = getProperXML(platform);
		Log.i("romDirPref",
				prefs.getString("rgui_browser_directory", "Not set."));
		romFolder = new File(prefs.getString("rgui_browser_directory", null),
				this.platformPath);
		initRoms();
		initMetaData();
		if (roms.size() > 0) {
			File resStor = new File(romFolder, "Resources");
			File coverStor = new File(resStor,
					VektorGuiPlatformHelper.cleanName(roms.get(0).getROMPath()
							.getName())
							+ "-CV.jpg");
			addDecodingJob(Uri.fromFile(coverStor), roms.get(0));
		}
		creatingUI = false;

	}

	private void initMetaData() {

		new AsyncTask<Void, Void, Void>() {
			@Override
			protected Void doInBackground(Void... params) {
				for (int i = 0; i < roms.size(); i++) {
					loadAssociatedMetadata(roms.get(i));
				}
				return null;
			}

			@Override
			protected void onPostExecute(Void result) {
				// for (int i = 0; i < roms.size(); i++) {
				updateUI(roms.get(romListAdapter.getSelectedItem()), romListAdapter.getSelectedItem());
				// }
			}
		}.execute();
	}

	private void initRoms() {
		if (!platformPath.equalsIgnoreCase("PSX")
				&& !platformPath.equalsIgnoreCase("MAME"))
			ridROMInfoDat = new ROMInfo(this.getResources().getXml(xmlId));
		SharedPreferences prefs = UserPreferences.getPreferences(this);
		if (null != prefs.getString("rgui_browser_directory", null)) {
			romFolder.mkdirs();
			if (!romFolder.isDirectory()) {
				Log.e("VektorGuiActivity::initRoms()",
						"Can't access/create folder "
								+ romFolder.getAbsolutePath());
				return;
			}
			File romResourcesDir = new File(romFolder, "Resources");
			if (!romResourcesDir.exists()) {
				romResourcesDir.mkdirs();
				if (!romResourcesDir.isDirectory()) {
					Log.e("VektorGuiActivity::initRoms()",
							"can't create or access "
									+ romResourcesDir.getAbsolutePath());
					return;
				}
			}
			roms.clear();
			File[] romFileList = romFolder.listFiles();
			for (File f : romFileList) {
				if (!f.getName().startsWith(".")
						&& f.getName().toLowerCase().endsWith(".zip")
						&& !platformPath.equalsIgnoreCase("PSX")
						&& !platformPath.equalsIgnoreCase("MAME")) {
					ZipFile zf;
					try {
						zf = new ZipFile(f, ZipFile.OPEN_READ);
						Enumeration<? extends ZipEntry> ze = zf.entries();
						while (ze.hasMoreElements()) {
							ZipEntry zef = ze.nextElement();
							if (extensionCheck(zef.getName())) {
								String gameCRC = Long
										.toHexString(zef.getCrc())
										.toUpperCase(
												this.getResources()
														.getConfiguration().locale);
								roms.add(new VektorGuiRomItem(f, gameCRC));
							}
						}
						zf.close();
					} catch (IOException e) {
					}
				} else if (!f.getName().startsWith(".")
						&& extensionCheck(f.getName())
						&& !platformPath.equalsIgnoreCase("PSX")) {
					roms.add(new VektorGuiRomItem(f, null));
				} else if (!f.getName().startsWith(".")
						&& extensionCheck(f.getName())
						&& platformPath.equalsIgnoreCase("MAME")) {
					roms.add(new VektorGuiRomItem(f, null));
				} else if (!f.getName().startsWith(".")
						&& !f.getName().contains("SCPH") // Skip bios .bin file
						&& extensionCheck(f.getName())
						&& platformPath.equalsIgnoreCase("PSX")) {
					VektorGuiRomItem cItem = new VektorGuiRomItem(f, null);
					roms.add(cItem);
				}
			}
		}
		// Once it's done, we populate the list.
		romList = (ListView) findViewById(R.id.vektor_gui_game_list);
		romList.setAdapter(romListAdapter);
		romListAdapter.notifyDataSetChanged();
		romList.setOnItemClickListener(this);
		romList.setOnKeyListener(this);
		if (roms.size() > 0) {
			romList.setSelection(0);
			// Log.d("VektorGuiActivity::initRoms()","Setting cursor at first element of list.");
			updateUI(romListAdapter.getItem(0), 0);
			addDecodingJob(
					Uri.fromFile(romListAdapter.getItem(0).getROMPath()),
					romListAdapter.getItem(0));
			romListAdapter.selectRow(0);
			numGames.setText(getResources().getString(
					R.string.vektor_gui_list_gamesfound).replace("[%d]",
					Integer.toString(roms.size())));
			numGames.setVisibility(View.VISIBLE);
		} else {
			gameyear.setText("19XX");
			gametitle.setText("Game Title");
			gamedesc.setText(getResources().getString(
					R.string.vektor_gui_game_no_description));
			gamecover.setImageDrawable(getResources().getDrawable(
					R.drawable.vektor_nocover));
		}
	}

	private boolean extensionCheck(String name) {
		for (String ext : this.supported_extensions) {
			if (name.toLowerCase(this.getResources().getConfiguration().locale)
					.endsWith(
							"."
									+ ext.toLowerCase(this.getResources()
											.getConfiguration().locale))) {
				return true;
			}
		}
		return false;
	}

	public static VektorGuiActivity getTheActivity() {
		return theActivity;
	}

	private String consolePath(String platform) {
		if (platform.equals("TurboGrafx-16"))
			return "PCE";
		else if (platform.equals("Sega Genesis"))
			return "MD";
		else if (platform.equals("Sega Master System"))
			return "SMS";
		else if (platform.equals("Game Boy Advance"))
			return "GBA";
		else if (platform.equals("Game Boy Color"))
			return "GBC";
		else if (platform.equals("Game Boy"))
			return "GB";
		else if (platform.equals("Nintendo Entertainment System"))
			return "NES";
		else if (platform.equals("Super Nintendo Entertainment System"))
			return "SNES";
		else if (platform.equals("Nintendo 64"))
			return "N64";
		else if (platform.equals("Nintendo DS"))
			return "NDS";
		else if (platform.equals("Sega Game Gear"))
			return "GG";
		else if (platform.equals("PlayStation"))
			return "PSX";
		else if (platform.equals("MAME"))
			return "MAME";
		else
			return "Other";
	}

	private int getProperXML(String platform) {
		if (platform.equals("TurboGrafx-16"))
			return R.xml.rominfo_pce;
		else if (platform.equals("Sega Genesis"))
			return R.xml.rominfo_genesis;
		else if (platform.equals("Sega Master System"))
			return R.xml.rominfo_sms;
		else if (platform.equals("Game Boy Advance"))
			return R.xml.rominfo_gba;
		else if (platform.equals("Game Boy Color"))
			return R.xml.rominfo_gbc;
		else if (platform.equals("Game Boy"))
			return R.xml.rominfo_gb;
		else if (platform.equals("Nintendo Entertainment System"))
			return R.xml.rominfo_nes;
		else if (platform.equals("Super Nintendo Entertainment System"))
			return R.xml.rominfo_snes;
		else if (platform.equals("Nintendo 64"))
			return R.xml.rominfo_n64;
		else if (platform.equals("Nintendo DS"))
			return R.xml.rominfo_nds;
		else if (platform.equals("Sega Game Gear"))
			return R.xml.rominfo_sms;
		else if (platform.equals("PlayStation"))
			return -1;
		else
			return -1;
	}

	private void showLeftPanel(boolean visible) {
		LinearLayout leftPanel = (LinearLayout) findViewById(R.id.vektor_gui_left_panel);
		for (int i = 0; i < leftPanel.getChildCount(); i++) {
			leftPanel.getChildAt(i).setVisibility(
					visible ? View.VISIBLE : View.GONE);
		}
	}

	private String calculateCRC32(File f) {
		String gameCRC = null;
		CRC32 cCRC = new CRC32();
		InputStream fi;
		try {
			fi = new BufferedInputStream(new FileInputStream(f));
			int cByte = 0;
			byte[] buf = new byte[1024 * 512];
			while ((cByte = fi.read(buf)) > 0) {
				cCRC.update(buf, 0, cByte);
			}
			fi.close();
			gameCRC = Long.toHexString(cCRC.getValue()).toUpperCase(
					this.getResources().getConfiguration().locale);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return gameCRC;
	}

	private String getPSXId(File f) {
		Log.i("GetPSXID", f.getName());
		FileInputStream fin;
		try {
			fin = new FileInputStream(f);
			fin.skip(32768);
			byte[] buffer = new byte[512 * 1024];
			// long start = System.currentTimeMillis();
			while (fin.read(buffer) != -1) {
				String buffered = new String(buffer);

				if (buffered.contains("BOOT = cdrom:\\")) {
					String tmp = "";
					int lidx = buffered.lastIndexOf("BOOT = cdrom:\\") + 14;
					for (int i = 0; i < 11; i++) {
						tmp += buffered.charAt(lidx + i);
					}
					tmp = tmp.toUpperCase().replace(".", "").replace("_", "-");
					fin.close();
					return tmp;
				}

			}
			fin.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return null;

	}

	private void loadAssociatedMetadata(VektorGuiRomItem item) {
		File resStor = new File(romFolder, "Resources");
		resStor.mkdirs();
		if (resStor.exists()) {
			File propStor = new File(resStor, item.getROMPath().getName()
					+ ".prop");
			if (propStor.exists()) {
				try {
					Properties props = new Properties();
					props.load(new FileReader(propStor));
					item.fromProperties(props);
				} catch (FileNotFoundException e) {
					// e.printStackTrace();
				} catch (IOException e) {
					// e.printStackTrace();
				}
			} else {
				if (!platformPath.equalsIgnoreCase("PSX")
						&& !platformPath.equalsIgnoreCase("MAME")) {
					item.setGameCRC(calculateCRC32(item.getROMPath()));
					ROMInfo.ROMInfoNode rNode = ridROMInfoDat.getNode(item
							.getGameCRC());
					if (rNode != null) {
						String gameName = "";
						if (rNode.getNumReleases() == 0)
							gameName = rNode.getGameName();
						else
							gameName = rNode.getReleaseData(0).getReleaseName();
						if (gameName.indexOf("(") != -1) {
							item.setGameName(gameName.substring(0,
									gameName.indexOf("(") - 1));
						} else if (gameName.lastIndexOf(".") != -1) {
							item.setGameName(gameName.substring(0,
									gameName.lastIndexOf(".")));
						} else
							item.setGameName(gameName);
					} else
						item.setGameName(item.getGameName().substring(0,
								item.getGameName().lastIndexOf(".")));
				} else if (platformPath.equalsIgnoreCase("PSX")) {
					// PS1 Games
					item.setGameCRC(getPSXId(item.getROMPath()));
					String title = myDbHelper.getGameTitle(item.getGameCRC());
					if (title != null) {
						if (title.indexOf("[") != -1)
							title = title.substring(0, title.indexOf("["));
						item.setGameName(title);
					} else
						item.setGameName(item
								.getROMPath()
								.getName()
								.substring(
										0,
										item.getROMPath().getName()
												.lastIndexOf(".")));
				} else if (platformPath.equalsIgnoreCase("MAME")) {
					item.setGameName(VektorGuiPlatformHelper.cleanName(item
							.getROMPath().getName()));
				}
			}
			File coverStor = new File(resStor,
					VektorGuiPlatformHelper.cleanName(item.getROMPath()
							.getName()) + "-CV.jpg");
			if (!coverStor.exists()) {
				// item.setGameCover(getResources().getDrawable(
				// R.drawable.vektor_nocover));
				mDownloadThreadPool.execute(new VektorGuiROMTask(this, resStor,
						item, mManager));
			}
		}
	}

	public void addDecodingJob(final Uri fileUri, final VektorGuiRomItem item) {
		mDecodeThreadPool.execute(new Runnable() {

			@Override
			public void run() {
				// Log.i("addDecodingJob()", new
				// File(fileUri.getPath()).getName());
				android.os.Process
						.setThreadPriority(android.os.Process.THREAD_PRIORITY_DISPLAY);
				BitmapFactory.Options opts = new BitmapFactory.Options();
				opts.inJustDecodeBounds = true;
				BitmapFactory.decodeFile(fileUri.getPath(), opts);
				int w = opts.outWidth;
				int h = opts.outHeight;
				opts.inSampleSize = Math.max(w, h) / 168;
				opts.inJustDecodeBounds = false;
				final BitmapDrawable bd = new BitmapDrawable(getResources(),
						BitmapFactory.decodeFile(fileUri.getPath(), opts));
				item.setGameCover(bd);
				// updateGameCover(item.getGameName());
				Log.i("addDecodingJob()",
						(romListAdapter.getSelectedItem() > -1 ? ""
								+ romListAdapter.getSelectedItem()
								+ " "
								+ romListAdapter.getItem(
										romListAdapter.getSelectedItem())
										.getGameName() : "-1"));
				if (romListAdapter.getSelectedItem() > -1
						&& romListAdapter
								.getItem(romListAdapter.getSelectedItem())
								.getGameName()
								.equalsIgnoreCase(item.getGameName())) {
					runOnUiThread(new Runnable() {

						@Override
						public void run() {
							// TODO Auto-generated method stub
							gamecover.setImageDrawable(bd);
						}

					});
				}
			}
		});
	}

	public void updateGameCover(final String gameName) {
		File resStor = new File(romFolder, "Resources");
		File fExtRes = new File(
				resStor,
				VektorGuiPlatformHelper.cleanName((gameName.contains("[") ? gameName
						.substring(0, gameName.indexOf("[")) : gameName))
						+ "-CV.jpg");
		if (fExtRes.exists()) {

			try {
				final BitmapDrawable gameCover = new BitmapDrawable(
						this.getResources(),
						BitmapFactory
								.decodeStream(new FileInputStream(fExtRes)));
				this.runOnUiThread(new Runnable() {
					@Override
					public void run() {
						android.os.Process
								.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);
						if (null != romListAdapter
								&& romListAdapter.getSelectedItem() > -1) {
							VektorGuiRomItem item = romListAdapter
									.getItem(romListAdapter.getSelectedItem());
							if (gameName.equals(item.getGameName())) {
								creatingUI = true;
								updateUI(item, romListAdapter.getSelectedItem());
								creatingUI = false;
							}
						}
					}
				});

			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}

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

	@Override
	public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
		VektorGuiRomItem item = romListAdapter.getItem(arg2);
		updateUI(item, arg2);
		File resStor = new File(romFolder, "Resources");
		File coverStor = new File(resStor,
				VektorGuiPlatformHelper.cleanName(item.getROMPath().getName())
						+ "-CV.jpg");
		Log.i("onItemClick",
				coverStor.exists() + " " + (null == item.getGameCover()));
		if (coverStor.exists() && item.getGameCover() == null)
			addDecodingJob(Uri.fromFile(coverStor), item);
	}

	private void updateUI(VektorGuiRomItem item, int position) {
		gametitle.setText(item.getGameName());
		gamedesc.setText(item.getGameDescription());
		gameyear.setText(item.getGameYear());
		if (null != item.getGameCover())
			gamecover.setImageDrawable(item.getGameCover());
		else
			gamecover.setImageDrawable(getResources().getDrawable(
					R.drawable.vektor_nocover));
		if (position != romListAdapter.getSelectedItem() && !creatingUI) {
			mp.start();
		}
		romListAdapter.selectRow(position);
		romList.setSelection(position);
		if (null != romList.getSelectedView())
			romList.getSelectedView().requestFocus();
		romListAdapter.notifyDataSetChanged();
	}

	private void romExecute(String absolutePath) {
		final SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(getApplicationContext());
		final String libretro_path = prefs.getString("libretro_path", "");

		UserPreferences.updateConfigFile(getApplicationContext());
		String current_ime = Settings.Secure.getString(getApplicationContext()
				.getContentResolver(), Settings.Secure.DEFAULT_INPUT_METHOD);
		Toast.makeText(getApplicationContext(),
				String.format(getString(R.string.loading_data), absolutePath),
				Toast.LENGTH_SHORT).show();

		Intent retro = getRetroActivity();
		retro.putExtra("ROM", absolutePath);
		retro.putExtra("LIBRETRO", libretro_path);
		retro.putExtra("CONFIGFILE",
				UserPreferences.getDefaultConfigPath(getApplicationContext()));
		retro.putExtra("IME", current_ime);
		startActivity(retro);
	}

	private Intent getRetroActivity() {
		if ((Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)) {
			return new Intent(getApplicationContext(),
					RetroActivityFuture.class);
		}
		return new Intent(getApplicationContext(), RetroActivityPast.class);
	}

	@Override
	public void onClick(View v) {

		if (v.getId() == R.id.vektor_gui_playgame_btn && null != romListAdapter) {
			if (romListAdapter.getCount() > 0) {

				romExecute(romListAdapter.getItem(
						romListAdapter.getSelectedItem()).getRomPath());
			}
		}
	}

	@Override
	public boolean onKey(View v, int keyCode, KeyEvent event) {
		if (event.getAction() == KeyEvent.ACTION_DOWN && null != romList
				&& null != romList.getSelectedView()
				&& romListAdapter.getCount() > 1) {
			int position = romListAdapter.getSelectedItem();
			switch (keyCode) {
			case KeyEvent.KEYCODE_DPAD_DOWN:
				if (position < romListAdapter.getCount() - 1) {
					updateUI(romListAdapter.getItem(position + 1), position + 1);
					File resStor = new File(romFolder, "Resources");
					File coverStor = new File(resStor,
							VektorGuiPlatformHelper.cleanName(romListAdapter
									.getItem(position + 1).getROMPath()
									.getName())
									+ "-CV.jpg");
					if (coverStor.exists()) {
						addDecodingJob(Uri.fromFile(coverStor),
								romListAdapter.getItem(position + 1));
					}
				}
				break;
			case KeyEvent.KEYCODE_DPAD_UP:
				if (position > 0) {
					updateUI(romListAdapter.getItem(position - 1), position - 1);
					File resStor = new File(romFolder, "Resources");
					File coverStor = new File(resStor,
							VektorGuiPlatformHelper.cleanName(romListAdapter
									.getItem(position - 1).getROMPath()
									.getName())
									+ "-CV.jpg");
					if (coverStor.exists()) {
						addDecodingJob(Uri.fromFile(coverStor),
								romListAdapter.getItem(position - 1));
					}
				}
				break;
			case KeyEvent.KEYCODE_ENTER:
				if (romListAdapter.getSelectedItem() > -1)
					this.romExecute(romListAdapter.getItem(
							romListAdapter.getSelectedItem()).getRomPath());
				break;
			}
		}
		return false;
	}

	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_VOLUME_UP
				|| keyCode == KeyEvent.KEYCODE_VOLUME_DOWN
				|| keyCode == KeyEvent.KEYCODE_VOLUME_MUTE)
			return false;
		if (null != romList.getSelectedView())
			romList.getSelectedView().requestFocus();
		return true;

	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_VOLUME_UP
				|| keyCode == KeyEvent.KEYCODE_VOLUME_DOWN
				|| keyCode == KeyEvent.KEYCODE_VOLUME_MUTE)
			return false;
		if (null != romList.getSelectedView())
			romList.getSelectedView().requestFocus();
		return true;

	}

	public void addGameToReceiver(long dlId, VektorGuiRomItem item) {
		receiver.addGameId(dlId, item);
	}
}
