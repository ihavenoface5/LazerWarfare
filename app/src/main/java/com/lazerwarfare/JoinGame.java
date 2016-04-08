package com.lazerwarfare;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.Vibrator;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import com.loopj.android.http.JsonHttpResponseHandler;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import cz.msebera.android.httpclient.Header;

public class JoinGame extends Activity {
	public static String dateFormat = "yyyy-MM-ddThh:mm:ssZ";
	public Vibrator v;
	public RadioGroup teamRadio;
	public GridView gamesGrid;
	public ArrayAdapter<String> gameAdapter; 
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_join_game);
		v = (Vibrator) this.getSystemService(Context.VIBRATOR_SERVICE);
		
		teamRadio = (RadioGroup) findViewById(R.id.radioGroup1);
		gamesGrid = (GridView) findViewById(R.id.gamesGrid);
		
		refresh();
		
		gameAdapter = new ArrayAdapter<String> (this, android.R.layout.simple_list_item_1);
		
		gamesGrid.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> parent, View v,
				int position, long id) {
				final String teamName = ((RadioButton) findViewById(teamRadio.getCheckedRadioButtonId() )).getText().toString();
				if (teamName == "Empire")
				{
					Player.teamSelect = 0;
				}
				else
				{
					Player.teamSelect = 1;
				}
				try {
				Log.i("Join", "Team Selected: " + Integer.toString(Player.teamSelect));
				Player.gameId = Player.games.getJSONObject(position).getInt("id");
				Player.gameType = Player.games.getJSONObject(position).getString("mode");
				if (Player.gameType.equals("TEAMS"))
				{
					Player.teamId = Player.tempGames.getJSONObject(position).getJSONArray("teams").getJSONObject(Player.teamSelect).getInt("id");
				}
				else
				{
					Player.teamId = -1;
				}
				}
				catch (JSONException e)
				{
					//TODO: Something about it
				}
				Player.joinGame(getApplicationContext());
			}
		});
	
		refresh();
		
		ImageButton refresh_button;
		refresh_button = (ImageButton) findViewById(R.id.refreshButton);
		
		refresh_button.setOnClickListener( new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				refresh();
			}
		});
        	
	}

	@Override
	public void onBackPressed()
	{
		super.onBackPressed();
		overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		return true;
	}

	public void refresh() {
		Log.i("Join Game", "Getting available games...");
		HttpConnect.get("games?joinable", null, new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONArray gamesArray) {
            	Player.games = gamesArray;
            	Player.tempGames = gamesArray;
            	Log.i("Join Game", "Response Received!");
            	Log.i("Join Game", Player.games.toString());
            	gameAdapter.clear();
            	for (int i = 0; i < gamesArray.length(); i++)
            	{
            		try {
						gameAdapter.add(gamesArray.getJSONObject(i).getString("mode"));
					} catch (JSONException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
            	}
            	gamesGrid.setAdapter(gameAdapter);
            	v.vibrate(500);
            }
            public void onFailure(Throwable t, JSONObject object)
            {
            	Toast.makeText(getApplicationContext(), "Connection to server failed!", Toast.LENGTH_LONG).show();
            }
        });
	}
	
}

