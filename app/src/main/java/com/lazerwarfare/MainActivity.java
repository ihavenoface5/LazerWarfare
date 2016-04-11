package com.lazerwarfare;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.TextView;

public class MainActivity extends Activity {
    public String TAG = "MainActivity";
	final Context context = this;
	static final boolean DEBUG = true;
	public static String url = "";
	public static String profile = "profile.dat";
	public static boolean hasGuns = false;
	public static int gunSelected = 0;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Intent videoIntent = new Intent(this, IntroVideo.class);

		//Check to see if the user has created a profile, if not, load the intro video
		if (!Player.loadProfile(this)) {
			startActivity(videoIntent);
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
			Player.createProfile(this);
		}
		setContentView(R.layout.activity_main);

		TextView clickableStart;
		TextView clickableJoin;
		TextView clickableOffline;
		TextView clickableSettings;

		clickableStart = (TextView) findViewById(R.id.start);
		clickableStart.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				if (HttpConnect.isNetworkAvailable(context)) {
                    HttpConnect.init();
                    Log.i(TAG, "Base URL is: " + HttpConnect.BASE_URL);
					if (HttpConnect.BASE_URL == "") {
						Player.promptServer(context, "start");
					}
                    else {
                        Intent intent = new Intent(MainActivity.this, StartGame.class);
                        startActivity(intent);
                        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                    }
				} else {
					Player.basicDialog(context, "Network Unavailable", "Please connect to wifi before using this feature.");
				}
			}

		});

		clickableJoin = (TextView) findViewById(R.id.join);
		clickableJoin.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (HttpConnect.isNetworkAvailable(context)) {
                    HttpConnect.init();
					if (HttpConnect.BASE_URL == "") {
						Player.promptServer(context, "join");
					}
                    else {
                        Intent intent = new Intent(MainActivity.this, JoinGame.class);
                        startActivity(intent);
                        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                    }
				} else {
					Player.basicDialog(context, "Network Unavailable", "Please connect to wifi before using this feature.");
				}
			}
		});

		clickableOffline = (TextView) findViewById(R.id.offline);
		clickableOffline.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Player.sync = false;
				Player.time_limit = 999;
				Intent intent = new Intent(getApplicationContext(), Game.class);
				startActivity(intent);
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
			}
		});

		clickableSettings = (TextView) findViewById(R.id.settings);
		clickableSettings.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(getApplicationContext(), Settings.class);
                startActivity(intent);
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
			}
		});
	}


	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
}
