package com.lazerwarfare;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.Spinner;
import android.widget.Toast;

public class StartGame extends Activity {
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_start_game);
		
		final Spinner gameMode = (Spinner) findViewById(R.id.gameTypeSpinner);
		final Spinner scoreLimit = (Spinner) findViewById(R.id.scoreLimitSpinner);
		final Spinner timeLimit = (Spinner) findViewById(R.id.timeLimitSpinner);
		final Spinner teamSelect = (Spinner) findViewById(R.id.teamSpinner);
		
		 gameMode.setOnItemSelectedListener(new OnItemSelectedListener() {
	            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
	            	String mode = gameMode.getItemAtPosition(position).toString();
	            	Log.i("GameMode", gameMode.getItemAtPosition(position).toString());
	            	if (mode.equals("Team Deathmatch"))
	            	{
	            		Player.gameType = "TEAMS";
	            	}
	            	else if (mode.equals("Free For All"))
	            	{
	            		Player.gameType = "FREE";
	            	}
	            	Log.i("Start JSON", "Game Mode: " + Player.gameType);
	            }
	            
				public void onNothingSelected(AdapterView<?> arg0) {
					// TODO Auto-generated method stub
					//Do nothing
				}
		 });
		 
		 scoreLimit.setOnItemSelectedListener(new OnItemSelectedListener() {
	            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
	            	if (scoreLimit.getItemAtPosition(position).equals("No Limit"))
	            	{
	            		Player.score_limit = 998;
	            	}
	            	else
	            	{
	            		Player.score_limit = Integer.parseInt(scoreLimit.getItemAtPosition(position).toString());
	            	}
	            	Log.i("Start JSON", "Score Limit: " + Integer.toString(Player.score_limit));
	            }
	            
				public void onNothingSelected(AdapterView<?> arg0) {
					// TODO Auto-generated method stub
					//Do nothing
				}
		 });
		 
		 timeLimit.setOnItemSelectedListener(new OnItemSelectedListener() {
	            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
	            	if (timeLimit.getItemAtPosition(position).equals("No Limit"))
	            	{
	            		Player.time_limit = 998;
	            	}
	            	else
	            	{
	            		Player.time_limit = Integer.parseInt(timeLimit.getItemAtPosition(position).toString());
	            	}
	            	Log.i("Start JSON", "Time Limit: " + Integer.toString(Player.time_limit));
	            }
	            
				public void onNothingSelected(AdapterView<?> arg0) {
					// TODO Auto-generated method stub
					//Do nothing
				}
		 });
		 
		 teamSelect.setOnItemSelectedListener(new OnItemSelectedListener() {
	            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
	            	Player.team = teamSelect.getItemAtPosition(position).toString();
	            	Log.i("Start JSON", "Team: " + Player.team);
	            }
	            
				public void onNothingSelected(AdapterView<?> arg0) {
					// TODO Auto-generated method stub
					//Do nothing
				}
		 });
	};

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.start_game, menu);
		return true;
	}

	@Override
	public void onBackPressed()
	{
		super.onBackPressed();
		overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
	}

	public void startGame(View v)
	{
		if (HttpConnect.isNetworkAvailable(getApplicationContext()))
		{
			Player.startGame(this);
			Toast.makeText(this, "Connection failed. Please check connection and try again.", Toast.LENGTH_LONG).show();
		}
		else
		{
			Log.i("startGame", "NO WIFI!!!!\n");
			Toast.makeText(this, "No Wifi connectivity!", Toast.LENGTH_LONG).show(); 
		}
	}

}