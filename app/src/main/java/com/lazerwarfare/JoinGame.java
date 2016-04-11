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
import android.widget.TextView;

import com.loopj.android.http.JsonHttpResponseHandler;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;

import cz.msebera.android.httpclient.Header;

public class JoinGame extends Activity {
    public String TAG = "JoinGame";
	public Vibrator v;
	public RadioGroup teamRadio;
	public GridView gamesGrid;
	public ArrayAdapter<String> gameAdapter;
	public TextView connectionStatus;
	public String connectionText = "Status: ";
	public int serverConnection = -1;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_join_game);
		v = (Vibrator) this.getSystemService(Context.VIBRATOR_SERVICE);
		
		teamRadio = (RadioGroup) findViewById(R.id.radioGroup1);
		gamesGrid = (GridView) findViewById(R.id.gamesGrid);

		connectionStatus = (TextView) findViewById(R.id.connectionStatus);
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
                Log.i(TAG, "Joining game: " + Integer.toString(position) + " with id: " + Integer.toString(Player.gameId));
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
		connectionStatus.setText(connectionText + "Connecting");
		Log.i("Join Game", "Getting available games...");
		HttpConnect.get("games?joinable", null, new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONArray gamesArray) {
                Log.i(TAG, "Found games: " + gamesArray.toString());
				serverConnection = 1;
				Player.saveServer(JoinGame.this);
				connectionStatus.setText(connectionText + "Connected");
            	Player.games = gamesArray;
            	Player.tempGames = gamesArray;
            	Log.i("Join Game", "Response Received!");
            	Log.i("Join Game", Player.games.toString());
            	gameAdapter.clear();
            	for (int i = 0; i < gamesArray.length(); i++)
            	{
            		try {
                        String gameEntry = "";

                        gameEntry += Integer.toString(gamesArray.getJSONObject(i).getInt("id"));
						gameEntry += ":\t" + gamesArray.getJSONObject(i).getString("mode");

                        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
                        SimpleDateFormat display = new SimpleDateFormat("MM/dd");
						Date timePlayed = format.parse(gamesArray.getJSONObject(i).getString("time_played"));
                        gameEntry += "\t" + display.format(timePlayed);


                        gameAdapter.add(gameEntry);
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
            	}
                gameAdapter.notifyDataSetChanged();
            	gamesGrid.setAdapter(gameAdapter);
            }
            public void onFailure(int status, Header[] headers, Throwable throwable, JSONObject response)
            {
                if (status == 400)
                {
                    connectionStatus.setText(connectionText + "Bad Request");
                }
                else {
                    serverConnection = 0;
                    connectionStatus.setText(connectionText + "Offline");
                }
            }
        });
	}
	
}

