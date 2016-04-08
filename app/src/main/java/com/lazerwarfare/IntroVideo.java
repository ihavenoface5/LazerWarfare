package com.lazerwarfare;

import com.lazerwarfare.R;

import android.media.MediaPlayer;
import android.media.MediaPlayer.OnPreparedListener;
import android.net.Uri;
import android.os.Bundle;
import android.app.Activity;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.VideoView;

public class IntroVideo extends Activity {
	private VideoView videoPlayer;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_intro_video);
		
		videoPlayer = (VideoView) findViewById(R.id.videoPlayer);   
		try {
	        //set the uri of the video to be played
	        videoPlayer.setVideoURI(Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.lwintro));
		} catch (Exception e) {
			Log.e("Error", e.getMessage());
			e.printStackTrace();
		}
			videoPlayer.requestFocus();
			
			videoPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
		        public boolean onError(MediaPlayer mp, int what, int extra) {
		            finish();
					return false;
		        }
		    });
			//we also set an setOnPreparedListener in order to know when the video file is ready for playback
			videoPlayer.setOnPreparedListener(new OnPreparedListener() {
				public void onPrepared(MediaPlayer mediaPlayer) {
					videoPlayer.start();
				}
			});
			
			videoPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
			    @Override
			    public void onCompletion(MediaPlayer vmp) {
			        finish();           
			    }
			});
			
	    ProgressBar progressSpinner = (ProgressBar) findViewById(R.id.progressSpinner);
	    progressSpinner.setVisibility(View.VISIBLE);
    }

}
