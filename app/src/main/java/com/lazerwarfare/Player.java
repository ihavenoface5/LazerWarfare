package com.lazerwarfare;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;

import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import cz.msebera.android.httpclient.Header;

public class Player {
	static String TAG = "Player";
	static int gunId = -1;
	static int teamId = 0;
	static int teamSelect = 0;
	static int gameId = 0;
	static int playerId = 0;
	static int shotsFired = 0;
	static int team1Score = 0;
	static int team2Score = 0;
	static int score = 0;
	static String team;
	static String name = "";
	static String status = "111";
	static int statusArray[];
	static int health = 100;
	static int ammo = 30;
	static int ammoReload = 15;
	static int hitDamage = 10;
	static String gameType;
	static String gameState;
	static JSONArray teams = new JSONArray();
	static int score_limit = 0;
	static int time_limit = 30;
	static int time_left = 0;
	static int delay = 60;
	static int respawnTime = 10;
	static int respawn = 0;
	static JSONArray hitArray = new JSONArray();
	static boolean readyToSync = false;
	static boolean sync = true;
	static boolean startGame = false;
	static boolean canStartGame = true;
	static String gamestate = "";
	static String json = "application/json";
	static JSONArray games = new JSONArray();
	static JSONArray tempGames = new JSONArray();
	static boolean updateStatus = false;
	static ArrayAdapter<Integer> gunFrequencies;
	static int numFrequencies = 20;
	static MediaPlayer mp;
	
	static RequestParams buildPlayer() throws JSONException {
		RequestParams buildPlayer = new RequestParams();
		buildPlayer.put("username", Player.name);
		buildPlayer.put("team_name", Player.team);
		buildPlayer.put("gun_id", Player.gunId);
		teams = new JSONArray();
		if (Player.gameType == "TEAMS")
		{
			teams.put("Empire"); //Hard-coded teams
			teams.put("Alliance");
		}
		else
		{
		}
		return buildPlayer;
	}
	
	static void startGame(final Context context) {
		RequestParams game = new RequestParams();

		try {
			game.put("mode", gameType);
			game.put("player", buildPlayer());
			game.put("teams", teams);
		}
		catch(Exception e)
		{

		}
		if (score_limit == 998)
		{
			game.put("score_limit", JSONObject.NULL);
		}
		else
		{
			game.put("score_limit", score_limit);
		}
		if (time_limit == 998)
		{
			game.put("time_limit", JSONObject.NULL);
		}
		else
		{
			game.put("time_limit", time_limit*60);
		}

		HttpConnect.post("start", game, new JsonHttpResponseHandler() {
			@Override
			public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
				Log.w(TAG, "Start Game Response: " + response.toString());
			}
		});
		Log.i("Start Game JSON: ", "Sent to post!");
	}
	
	static void joinGame(final Context context) {
		RequestParams currentPlayer = new RequestParams();
		Log.i("Join Game", "Join Game Called!");

		if (Player.teamId == -1)
		{
			currentPlayer.put("team_id", JSONObject.NULL);
		}
		else
		{
			currentPlayer.put("team_id", Player.teamId);
		}
		currentPlayer.put("username", Player.name);
		currentPlayer.put("gun_id", Player.gunId);

		Log.i("Join Game JSON", currentPlayer.toString());

		//TODO: Parameterize the gameId to the one selected
		HttpConnect.post("join/" + Integer.toString(Player.gameId), currentPlayer, new JsonHttpResponseHandler() {
            @Override
			public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                try {
                	Log.i("Join Game", "received response from join!");
                	Log.i("Join Game", response.toString());
                	Player.playerId = response.getInt("player_id");
                	if (Player.gameType.equals("TEAMS"))
                	{
                		Player.teamId = response.getInt("team_id");
                	}
                	if (response.isNull("time_limit"))
                	{
                		Player.time_limit = 998;
                		Player.delay = 0;
                	}
                	else
                	{
                		Player.time_limit = response.getInt("time_left") / 60;
                		if (response.getInt("time_limit") < response.getInt("time_left"))
            			{
            				Player.delay = response.getInt("time_left") - response.getInt("time_limit");
            			}
            			else
            			{
            				Player.delay = 0;
            			}
                	}
        			
        			Intent intent = new Intent(context, Game.class);
        			intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
	        		context.startActivity(intent);
				} catch (JSONException e) {
					e.printStackTrace();
				}
            }
        });
	}
	
	static void sync() {
		RequestParams syncPlayer = new RequestParams();
		Log.i("sync", "SYNCING!");

		syncPlayer.put("player_id", Player.playerId);
		syncPlayer.put("shots_fired", Player.shotsFired);
		syncPlayer.put("hits_taken", Player.hitArray);

		HttpConnect.post("sync/" + Integer.toString(Player.gameId), syncPlayer, new JsonHttpResponseHandler() {
			@Override
			public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
				//TODO: Process the response back from sync
				try {
					Log.i("SYNCResponse", response.toString());
					hitArray = new JSONArray();
					Log.i("SYNC", "Received response!");
					if (Player.gameType.equals("TEAMS")) {
						Player.team1Score = response.getJSONArray("team_scores").getJSONObject(0).getInt("score");
						Player.team2Score = response.getJSONArray("team_scores").getJSONObject(1).getInt("score");
					}
					Player.score = Integer.parseInt(response.getString("score"));
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

			public void onSuccess(JSONArray array) {
				Log.i("SYNC", "This was called!");
				Log.i("SYNC", array.toString());
			}
		});
	}

	static void shotFired()
	{
		if (ammo > 0) {
			ammo--;
		}
		else
		{
			status = "101";
		}
		shotsFired++;
	}

	static void receivedHit(Context ctx, String data)
	{
		//Vibrate when hit
		Vibrator v = (Vibrator) ctx.getSystemService(Context.VIBRATOR_SERVICE);
		v.vibrate(1000);

		char hit = data.charAt(1);
		int hitFrequency = Character.getNumericValue(hit);
		hitArray.put(hitFrequency);

		Log.i(TAG, "Hit by frequency: " + hitFrequency);

		if (health > 0)
		{
			health -= hitDamage;
		}
		if (health == 0)
		{
			status = "011";
		}
	}
	
	static void createProfile(final Context context) {
		//Alert box to have user create a profile or load a previous one    
        AlertDialog.Builder nameAlert = new AlertDialog.Builder(context);
        nameAlert.setTitle("Create a Profile"); //Set Alert dialog title here
        nameAlert.setMessage("Enter Your Name Here"); //Message here

        // Set an EditText view to get user input 
        final EditText input = new EditText(context);
        nameAlert.setView(input);

		nameAlert.setPositiveButton("OK", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				String entry = input.getEditableText().toString();
				if (entry == null) {
					Player.name = "Noname";
				} else {
					Player.name = entry;
				}

				Log.i(TAG, "Player name: " + name);

				ArrayList<Integer> frequencies = new ArrayList<Integer>();
				for (int i = 0; i <= numFrequencies; i++) {
					frequencies.add(i);
				}
				gunFrequencies = new ArrayAdapter<Integer>(context, android.R.layout.simple_spinner_item, frequencies);

				AlertDialog.Builder gunAlert = new AlertDialog.Builder(context);
				gunAlert.setTitle("Gun Frequency"); //Set Alert dialog title here
				gunAlert.setMessage("Select your weapon frequency"); //Message here

				final Spinner gunSpinner = new Spinner(context);
				gunSpinner.setAdapter(gunFrequencies);
				gunAlert.setView(gunSpinner);

				gunAlert.setPositiveButton("OK", new DialogInterface.OnClickListener() {
							@SuppressLint("DefaultLocale")
							public void onClick(DialogInterface dialog, int whichButton) {
								//Try to pair with the selected bluetooth device
								gunId = Integer.valueOf(gunSpinner.getSelectedItem().toString());
								Log.i(TAG, "Gun picked: " + Integer.toString(gunId));
								//Bluetooth.pair(context, gunSpinner.getSelectedItemPosition());
								saveProfile(context);
								dialog.dismiss();
							}
						}
				);

				gunAlert.setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						// Canceled.
						basicDialog(context, "No Weapon", "No weapon formed against you shall prosper. Unfortunately, your weapon won't prosper either.");
						dialog.cancel();
					}
				});

				AlertDialog alertDialog = gunAlert.create();
				alertDialog.show();

			}
		}); //End of alert.setPositiveButton
        nameAlert.setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				// Canceled.
				Player.name = "Noname";
				dialog.cancel();
			}
		}); //End of alert.setNegativeButton
        AlertDialog alertDialog = nameAlert.create();
        alertDialog.show();
	}
	
	static void basicDialog(Context context, String title, String message)
	{
		AlertDialog.Builder alert = new AlertDialog.Builder(context);
        alert.setTitle(title); //Set Alert dialog title here
        alert.setMessage(message); //Message here
        
        alert.setPositiveButton("OK", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				dialog.dismiss();
			}
		});
        AlertDialog alertDialog = alert.create();
        alertDialog.show();
	}

	static void promptExit(final Context context, final String message)
	{
		AlertDialog.Builder alert = new AlertDialog.Builder(context);
		alert.setTitle("Exit Game"); //Set Alert dialog title here
		alert.setMessage(message); //Message here

		alert.setPositiveButton("OK", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				((Activity) context).finish();
				((Activity) context).overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
			}
		});

		alert.setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				// Canceled.
				dialog.cancel();
			}
		});
		AlertDialog alertDialog = alert.create();
		alertDialog.show();
	}
	
	static void promptServer(final Context context, final String parameter)
	{
		AlertDialog.Builder alert = new AlertDialog.Builder(context);
        alert.setTitle("Server name or IP address"); //Set Alert dialog title here
        alert.setMessage("Ex: lazertag.byu.edu or 192.168.1.1"); //Message here
        
        final EditText input = new EditText(context);
        alert.setView(input);
        
        alert.setPositiveButton("OK", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				if (input.getText().toString().length() > 0) {
					Intent intent;
					HttpConnect.BASE_URL = input.getText().toString();
					Log.i(TAG, "Setting base url to: " + input.getText().toString());
					if (parameter.equals("start")) {
						intent = new Intent(context, StartGame.class);
					} else {
						intent = new Intent(context, JoinGame.class);
					}
					context.startActivity(intent);
					((Activity)context).overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
				} else {
					//Maybe tell the user to enter something?
				}
				Log.i("promptServer", input.getText().toString());
				dialog.dismiss();
			}
		});
        
        alert.setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				// Canceled.
				dialog.cancel();
			}
		});
        AlertDialog alertDialog = alert.create();
        alertDialog.show();
	}

	static boolean loadProfile(Context context) {
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
		String name = preferences.getString("name", "");
		if (name != "")
		{
			Player.name = name;
			Player.gunId = preferences.getInt("gunid", -1);
			return true;
		}
		return false;
	}

	static void saveProfile(Context context) {
//      Writing to file
		Log.w("MainActivity", "Saving Profile");
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
		SharedPreferences.Editor editor = preferences.edit();
		editor.putString("name", Player.name);
		editor.putInt("gunid", Player.gunId);
		editor.apply();
	}
}
