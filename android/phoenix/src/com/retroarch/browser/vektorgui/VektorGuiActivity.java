package com.retroarch.browser.vektorgui;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Enumeration;
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
import com.retroarch.browser.vektorgui.utils.VektorGuiArcadeHits;
import com.retroarch.browser.vektorgui.utils.VektorGuiBroadcastReceiver;
import com.retroarch.browser.vektorgui.utils.VektorGuiDatabaseHelper;
import com.retroarch.browser.vektorgui.utils.VektorGuiRomIdEngine;
import com.retroarch.browser.vektorgui.utils.VektorGuiTheGamesDB;
import com.retroarch.R;
import com.retroarch.browser.vektorgui.ui.VektorGuiLeftMenu;
import com.retroarch.browser.vektorgui.ui.VektorGuiRomAdapter;
import com.retroarch.browser.vektorgui.ui.VektorGuiRomItem;
import com.retroarch.browser.vektorgui.ui.VektorGuiRomList;
import com.retroarch.browser.vektorgui.ui.VektorPlatformNCorePicker;
import com.retroarch.browser.vektorgui.ui.views.VektorGuiButton;
import com.retroarch.browser.vektorgui.ui.views.VektorGuiTextView;
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
import android.view.View.OnKeyListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Toast;

public class VektorGuiActivity extends Activity {
	private static VektorGuiActivity theActivity;
	private ImageView gamecover;
	private VektorGuiTextView gamedesc;
	private boolean creatingUI;
	private VektorGuiRomList romList;
	private VektorGuiLeftMenu leftMenu;
	private VektorPlatformNCorePicker picker;
	private VektorGuiRomIdEngine romEngine;
	private List<String> supported_extensions;
	private MediaPlayer mp;
	private String platformPath;
	private VektorGuiBroadcastReceiver receiver;
	/**
	 * The list of rom elements
	 */
	private ArrayList<VektorGuiRomItem> roms = new ArrayList<VektorGuiRomItem>();
	private ROMInfo ridROMInfoDat;
	private int xmlId;
	private File romFolder;
	private VektorGuiDatabaseHelper myDbHelper;
	private DownloadManager mManager;

	private void defineUI() {
		gamedesc = (VektorGuiTextView) findViewById(R.id.vektor_gui_gamedesc);
		gamedesc.setMovementMethod(new ScrollingMovementMethod());
		gamecover = (ImageView) findViewById(R.id.vektor_gui_gamecover);
		picker = new VektorPlatformNCorePicker(this);
		romList = new VektorGuiRomList(this);
		leftMenu = new VektorGuiLeftMenu(this);
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		VektorGuiActivity.theActivity = this;
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
				WindowManager.LayoutParams.FLAG_FULLSCREEN);
		mp = MediaPlayer.create(getApplicationContext(), R.raw.button11);
		romEngine = new VektorGuiRomIdEngine(this);
		mManager = (DownloadManager) getSystemService(Activity.DOWNLOAD_SERVICE);
		setContentView(R.layout.vektor_gui_layout);
		defineUI();
		SharedPreferences prefs = UserPreferences.getPreferences(this);
		String core = prefs.getString("libretro_path", null);
		String name = prefs.getString("libretro_name", null);
		String platform = prefs.getString("vektor_gui_last_platform", null);
		if (null != core && null != name && null != platform) {
			setModuleAndPlatform(core, name, platform, VektorGuiPlatformHelper
					.findCore(name, this).getSupportedExtensions(), true);
		}
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

	public void setModuleAndPlatform(String core_path, String core_name,
			String platform, List<String> supported_extensions, boolean init) {
		if (!init)
			return;
		romEngine.clearQueues();
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
				if (roms.size() > 0)
					updateUI(roms.get(romList.getSelectedItem()),
							romList.getSelectedItem());
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
		romList.populate();
		if (roms.size() > 0) {
			romList.setSelection(0);
			updateUI(romList.getItem(0), 0);
			File resStor = new File(romFolder,"Resources");
			File coverStor = new File(resStor,VektorGuiPlatformHelper.cleanName(romList.getItem(0).getROMPath().getName())+"-CV.jpg");
			if(coverStor.exists())
			addDecodingJob(
					Uri.fromFile(romList.getItem(0).getROMPath()),
					romList.getItem(0));
			romList.selectRow(0);
		} else {
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
			File propStor = new File(resStor,
					VektorGuiPlatformHelper.cleanName(item.getROMPath()
							.getName()) + ".prop");
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
						} else if (gameName.indexOf("[") != -1) {
							item.setGameName(gameName.substring(0,
									gameName.indexOf("[")));
						} else
							item.setGameName(gameName);
					} else
						item.setGameName(VektorGuiPlatformHelper.cleanName(item
								.getGameName().substring(0,
										item.getGameName().lastIndexOf("."))));
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
					if (null != item.getGameName())
						item.setGameName(VektorGuiPlatformHelper.cleanName(item
								.getROMPath().getName()));
				}
			}
			File coverStor = new File(resStor,
					VektorGuiPlatformHelper.cleanName(item.getROMPath()
							.getName()) + "-CV.jpg");
			if (!coverStor.exists()) {
				romEngine.identifyRom(resStor,item,mManager);
			}
		}
	}

	public void addDecodingJob(final Uri fileUri, final VektorGuiRomItem item) {
		romEngine.addDecodingJob(fileUri, item);
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
				new BitmapDrawable(this.getResources(),
						BitmapFactory
								.decodeStream(new FileInputStream(fExtRes)));
				this.runOnUiThread(new Runnable() {
					@Override
					public void run() {
						android.os.Process
								.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);
						if (null != romList && romList.getSelectedItem() > -1) {
							VektorGuiRomItem item = romList.getItem(romList
									.getSelectedItem());
							if (gameName.equals(item.getGameName())) {
								creatingUI = true;
								updateUI(item, romList.getSelectedItem());
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

	public void romExecute(String absolutePath) {
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
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		return super.onKeyUp(keyCode, event);
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		return super.onKeyDown(keyCode, event);
	}

	public void updateUI(VektorGuiRomItem item, int position) {
		gamedesc.setText(item.getGameDescription());
		if (null != item.getGameCover())
			gamecover.setImageDrawable(item.getGameCover());
		else
			gamecover.setImageDrawable(getResources().getDrawable(
					R.drawable.vektor_nocover));
		if (position != romList.getSelectedItem() && !creatingUI) {
			mp.start();
		}
		romList.selectRow(position);
		romList.setSelection(position);
		if (null != romList.getSelectedView())
			romList.getSelectedView().requestFocus();
		romList.notifyDataSetChanged();
	}

	public void addGameToReceiver(long dlId, VektorGuiRomItem item) {
		receiver.addGameId(dlId, item);
	}

	public List<VektorGuiRomItem> getRoms() {
		return roms;
	}

	public File getRomFolder() {
		return romFolder;
	}

	public String getPlatformPath() {
		return platformPath;
	}

	public VektorGuiRomList getRomList() {
		return romList;
	}

	public ImageView getGameCover() {
		return gamecover;
	}
}
