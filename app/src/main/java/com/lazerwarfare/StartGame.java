package com.lazerwarfare;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.Spinner;
import android.widget.TextView;

import com.loopj.android.http.JsonHttpResponseHandler;

import org.json.JSONArray;
import org.json.JSONObject;

import cz.msebera.android.httpclient.Header;

public class StartGame extends Activity {
	public String TAG = "StartGame";
    public TextView connectionStatus;
    public String connectionText = "Connection Status: ";
    public int serverConnection = -1;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_start_game);
		
		final Spinner gameMode = (Spinner) findViewById(R.id.gameTypeSpinner);
		final Spinner scoreLimit = (Spinner) findViewById(R.id.scoreLimitSpinner);
		final Spinner timeLimit = (Spinner) findViewById(R.id.timeLimitSpinner);
		final Spinner teamSelect = (Spinner) findViewById(R.id.teamSpinner);

        connectionStatus = (TextView) findViewById(R.id.connectionStatus);

        checkConnection();
		
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
	            		Player.score_limit = 0;
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
                        Log.w(TAG, "IT happened HERE!");
	            	}
	            	else
	            	{
	            		Player.time_limit = Integer.parseInt(timeLimit.getItemAtPosition(position).toString());
						Player.time_limit *= 60;
                        Log.i(TAG, "Time limit: " + Player.time_limit);
	            	}
                    Log.i(TAG, "position selected: " + Integer.toString(position));
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

    public void checkConnection()
    {
        connectionStatus.setText(connectionText + "Connecting...");
		Log.i(TAG, "Connecting to: " + HttpConnect.BASE_URL);
        HttpConnect.get("games", null, new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONArray gamesArray) {
                connectionStatus.setText(connectionText + "Connected");
                serverConnection = 1;
				Player.saveServer(StartGame.this);
            }

            public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject response) {
                connectionStatus.setText(connectionText + "Connection Failed");
                Log.w(TAG, "Failed to get response");
                serverConnection = 0;
            }
        });
    }

    public void retryConnection(View v)
    {
        checkConnection();
    }

	public void startGame(View v)
	{
		if (serverConnection == 1)
		{
			Log.w(TAG, "Starting Game!");
			Player.startGame(this);
		}
		else if (serverConnection == 0)
        {
            Log.w(TAG, "Failed to connect to server");
            Player.basicDialog(this, "Connection to Server Failed",
                    "Please establish a connection to the server before starting a game");
        }
        else
        {
            //no response yet from checking server connection
        }
	}

}