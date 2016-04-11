package com.lazerwarfare;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Spinner;
import android.widget.TextView;

public class Settings extends Activity {
	public String TAG = "Settings";
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_settings);

		final Context context = this;
		final Spinner httpReceive;
		httpReceive = (Spinner) findViewById(R.id.wifiSpinner);

		TextView bluetoothDebug;
		bluetoothDebug = (TextView) findViewById(R.id.bluetoothDebug);

		bluetoothDebug.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				//Bluetooth.debug = true;
				Intent intent = new Intent(getApplicationContext(), Debug.class);
				startActivity(intent);
				overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
			}
		});

		TextView deleteProfile;
		deleteProfile = (TextView) findViewById(R.id.deleteProfile);
		deleteProfile.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(Settings.this);
				SharedPreferences.Editor editor = preferences.edit();
				editor.clear();
				editor.apply();
				HttpConnect.BASE_URL = "";
				Player.createProfile(context);
			}
		});

		TextView back;
		back = (TextView) findViewById(R.id.back);
		back.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				finish(); //take the user back to the main menu
				overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
			}
		});

		httpReceive.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
			public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
				HttpConnect.setTimeout((position+1)*5);
			}

			public void onNothingSelected(AdapterView<?> arg0) {
				// TODO Auto-generated method stub
				//Do nothing
			}
		});
	}

    @Override
    public void onBackPressed()
    {
        super.onBackPressed();
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }
}
