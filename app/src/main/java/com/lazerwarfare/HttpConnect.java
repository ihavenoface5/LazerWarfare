package com.lazerwarfare;

import android.content.Context;
import android.net.ConnectivityManager;

import com.loopj.android.http.*;

import org.json.JSONArray;

import cz.msebera.android.httpclient.Header;

public class HttpConnect {
	  public static boolean serverConnection = false;
	  public static String BASE_URL = "none"; //TODO: remove and parameterize
	
	  final static int DEFAULT_TIMEOUT = 20 * 1000; //TODO: allow user to configure / parameterize
	  private static AsyncHttpClient client = new AsyncHttpClient();
	
	  public static void get(String url, RequestParams params, AsyncHttpResponseHandler responseHandler) {
	      client.get(getAbsoluteUrl(url), params, responseHandler);
	  }
	
	  public static void post(String url, RequestParams params, AsyncHttpResponseHandler responseHandler) {
	      client.post(getAbsoluteUrl(url), params, responseHandler);
	  }
	
	  private static String getAbsoluteUrl(String relativeUrl) {
	      return BASE_URL + relativeUrl;
	  }
	  
	  static void init() {
		  client.setTimeout(DEFAULT_TIMEOUT);
	  }
	  
	  static void setTimeout(int timeout) {
		  client.setTimeout(timeout * 1000);
	  }
	  
	  public static boolean isNetworkAvailable(Context context) 
	  {
		  return ((ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE)).getActiveNetworkInfo() != null;
	  }
	  
	  public static void serverConnection()
	  {
		  get("games", null, new JsonHttpResponseHandler() {
	            @Override
	            public void onSuccess(int statusCode, Header[] headers, JSONArray gamesArray) {
	            	serverConnection = true;
	            }
	        });
	  }
}
