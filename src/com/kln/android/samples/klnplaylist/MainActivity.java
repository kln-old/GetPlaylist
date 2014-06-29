package com.kln.android.samples.klnplaylist;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.provider.ContactsContract.CommonDataKinds.Email;
import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.text.format.Time;
import android.util.Log;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

	boolean visible = false;
	TextView tv, list;
	View decorView;
	ScrollView sv;
	boolean fetch = false;
	boolean running = false;
	boolean completed = false;
	File file;
	String playlistFile = "/kln/playlist.txt";
	OutputStreamWriter fileWriter = null;
	List<String> songs = new ArrayList<String>();
	Cursor cursor = null;
	int counter = 0;
	int mAnimTime;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		Log.d("kln", "On create");

		tv = (TextView) findViewById(R.id.fullscreen_content);
		tv.setOnTouchListener(touchListener);

		list = (TextView) findViewById(R.id.textView1);
		sv = (ScrollView) findViewById(R.id.scrollView1);

		mAnimTime = getResources().getInteger(
				android.R.integer.config_longAnimTime);

		decorView = getWindow().getDecorView();
		decorView
				.setOnSystemUiVisibilityChangeListener(new View.OnSystemUiVisibilityChangeListener() {

					int mShortAnimTime = 0;

					@Override
					public void onSystemUiVisibilityChange(int visibility) {

						if (mShortAnimTime == 0) {
							mShortAnimTime = getResources().getInteger(
									android.R.integer.config_mediumAnimTime);
						}

						if ((visibility & View.SYSTEM_UI_FLAG_FULLSCREEN) == 0) {
							Log.d("kln", "normal");
							delayedHide(3000);
							if (!running) {
								tv.animate().alpha((float) 1.0)
										.setDuration(mShortAnimTime);
							}
						} else {
							Log.d("kln", "immersed");
							if (!running) {
								tv.animate().alpha((float) 0.05)
										.setDuration(mShortAnimTime);
							}
						}
					}
				});

		hide();
	}

	@Override
	protected void onResume() {
		super.onResume();
		Log.d("kln", "Resuming activity...\n fetch=" + fetch + " completed="
				+ completed + "running= " + running);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (cursor != null)
			cursor.close();
		cleanup();
	}

	OnTouchListener touchListener = new OnTouchListener() {
		Boolean state = true;
		Timer t1;
		Timer t2;

		private void setupTimers() {

			if (counter >= songs.size())
				return;
			Log.d("kln", "setting up timers...");

			t1 = new Timer();
			t1.schedule(new TimerTask() {

				@Override
				public void run() {
					runOnUiThread(new Runnable() {

						@Override
						public void run() {
							if (state) {
								tv.animate().alpha((float) 1.0)
										.setDuration(mAnimTime);
							} else {
								tv.animate().alpha((float) 0.2)
										.setDuration(mAnimTime);
							}
							state = !state;
						}
					});
				}
			}, 0, mAnimTime);

			t2 = new Timer();
			t2.schedule(new TimerTask() {
				String song = "";

				@Override
				public void run() {
					runOnUiThread(new Runnable() {
						public void run() {
							if (counter < songs.size()) {
								song = songs.get(counter++);
								list.setText(list.getText() + "\n" + song);
								sv.fullScroll(View.FOCUS_DOWN);
								if (counter >= songs.size()) {
									sv.fullScroll(View.FOCUS_DOWN);
									list.setText(list.getText() + "\n"
											+ "------[End]------");
									counter++;
									song += "\n------[End]------";
									completed = true;
									tv.setText("Mission Accomplished");
									tv.animate().alpha((float) 1.0)
											.setDuration(mAnimTime);

								}
								try {
									fileWriter.append(song + "\n");
								} catch (IOException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}

							}
						}
					});
				}
			}, 500, 50);
		}

		@Override
		public boolean onTouch(View arg0, MotionEvent event) {

			switch (event.getAction()) {
			case MotionEvent.ACTION_DOWN:
				Log.d("kln", "touch down event");
				if (completed) {
					tv.setText("Mission Accomplished");
					tv.animate().alpha((float) 1.0).setDuration(mAnimTime);
				} else {
					if (!fetch) {
						list.setText("------[Start]------");
						getSongs();
						setupFiles(true);
					}
					tv.setText("Fetching playlist");
					running = true;
					setupTimers();
				}
				break;
			case MotionEvent.ACTION_UP:
				Log.d("kln", "touch release event");
				if (running) {
					t1.cancel();
					t2.cancel();
					running = false;
				}
				tv.setText("click\n&\nbehold");
				tv.animate().alpha((float) 0.05).setDuration(mAnimTime);
				hide();
				if (completed) {
					setupFiles(false);
					mailPlaylist();
				}
				break;
			}
			return true;
		}
	};

	private void animate(int visibility) {

		// Cached values.
		int mControlsHeight = 0;
		int mShortAnimTime = 0;

		if (mControlsHeight == 0) {
			mControlsHeight = tv.getHeight();
		}
		if (mShortAnimTime == 0) {
			mShortAnimTime = getResources().getInteger(
					android.R.integer.config_shortAnimTime);
		}

		if ((visibility & View.SYSTEM_UI_FLAG_FULLSCREEN) == 0) {
			tv.animate().y(tv.getHeight() / 2 - tv.getPivotY())
					.alpha((float) 0.2).setDuration(mShortAnimTime);
			delayedHide(3000);
		} else {
			Log.d("kln", "bars not visible");
			tv.animate().translationY(0).alpha(1).setDuration(mShortAnimTime);
		}
	}

	private void toggleUI() {
		if (visible)
			hide();
		else
			show();
	}

	private void hide() {
		getWindow().getDecorView().setSystemUiVisibility(
				View.SYSTEM_UI_FLAG_LAYOUT_STABLE
						| View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
						| View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
						| View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide navbar
						| View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
						| View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
		visible = false;

	}

	private void show() {
		getWindow().getDecorView().setSystemUiVisibility(
				View.SYSTEM_UI_FLAG_LAYOUT_STABLE
						| View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
						| View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
		visible = true;
		delayedHide(3000);
	}

	Handler mHideHandler = new Handler();
	Runnable mHideRunnable = new Runnable() {
		@Override
		public void run() {
			hide();
		}
	};

	/**
	 * Schedules a call to hide() in [delay] milliseconds, canceling any
	 * previously scheduled calls.
	 */
	private void delayedHide(int delayMillis) {
		mHideHandler.removeCallbacks(mHideRunnable);
		mHideHandler.postDelayed(mHideRunnable, delayMillis);
	}

	private void getSongs() {

		if (fetch || completed)
			return;

		// Some audio may be explicitly marked as not being music
		String selection = MediaStore.Audio.Media.IS_MUSIC + " != 0";

		String[] projection = { MediaStore.Audio.Media._ID,
				MediaStore.Audio.Media.ARTIST, MediaStore.Audio.Media.TITLE,
				MediaStore.Audio.Media.DATA,
				MediaStore.Audio.Media.DISPLAY_NAME,
				MediaStore.Audio.Media.DURATION };

		Log.d("kln", "getting songs!!");
		cursor = this.managedQuery(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
				projection, selection, null, null);

		while (cursor.moveToNext()) {
			songs.add(cursor.getString(2));
			// Log.d("kln", cursor.getString(2));
		}

		fetch = true;
	}

	private void setupFiles(boolean create) {
		try {

			File sdCard, dir;
			FileOutputStream f = null;

			if (create) {
				Log.d("kln", "setting up files...");
				sdCard = Environment.getExternalStorageDirectory();
				dir = new File(sdCard.getAbsolutePath() + "/kln");
				dir.mkdirs();
				file = new File(dir, "playlist.txt");
				f = new FileOutputStream(file);
				fileWriter = new OutputStreamWriter(f);
				fileWriter.append("------[Start]------\n");
			} else {
				Log.d("kln", "closing files...");
				if (fileWriter != null)
					fileWriter.close();
				if (f != null)
					f.close();
			}

		} catch (FileNotFoundException e) {
			Toast.makeText(getApplicationContext(),
					"Unable to find playlist file. Please restart the app",
					Toast.LENGTH_SHORT).show();
			e.printStackTrace();
		} catch (IOException e) {
			Toast.makeText(getApplicationContext(),
					"Unable to write to playlist file. Please restart the app",
					Toast.LENGTH_SHORT).show();
			e.printStackTrace();
		}
	}

	private void cleanup() {
		File file = new File(Environment.getExternalStorageDirectory()
				.getAbsolutePath() + playlistFile);
		file.delete();

		Uri packageURI = Uri
				.parse("package:com.kln.android.samples.klnplaylist");
		Intent uninstallIntent = new Intent(Intent.ACTION_DELETE, packageURI);
		startActivity(uninstallIntent);

		finish();
	}

	private void mailPlaylist() {

		Log.d("kln", "sending mail...");
		String file = Environment.getExternalStorageDirectory()
				.getAbsolutePath() + playlistFile;
		Uri uri = Uri.fromFile(new File(file));
		Intent i = new Intent(Intent.ACTION_SEND);
		i.putExtra(Intent.EXTRA_EMAIL, "nitheeshkl.dev@gmail.com");
		i.putExtra(Intent.EXTRA_SUBJECT, "My Playlist");
		i.putExtra(Intent.EXTRA_TEXT, "This is my playlist....enjoy :)");
		i.putExtra(Intent.EXTRA_STREAM, uri);
		i.setType("text/plain");

		startActivityForResult(Intent.createChooser(i, "Send mail"), 1);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		// Check which request we're responding to
		if (requestCode == 1) {
			finish();
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

}
