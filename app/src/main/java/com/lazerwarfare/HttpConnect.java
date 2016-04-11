package com.lazerwarfare;

import android.content.Context;
import android.net.ConnectivityManager;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;
import com.loopj.android.http.ResponseHandlerInterface;

import cz.msebera.android.httpclient.HttpEntity;

public class HttpConnect {
	  public static String BASE_URL = ""; //TODO: remove and parameterize
	
	  final static int DEFAULT_TIMEOUT = 5 * 1000; //TODO: allow user to configure / parameterize
	  private static AsyncHttpClient client = new AsyncHttpClient();
	
	  public static void get(String url, RequestParams params, AsyncHttpResponseHandler responseHandler) {
	      client.get(getAbsoluteUrl(url), params, responseHandler);
	  }

		//post(Context context, String url, HttpEntity entity, String contentType, ResponseHandlerInterface responseHandler
	  public static void post(Context context, String url, HttpEntity entity, ResponseHandlerInterface responseHandler) {
	      client.post(context, getAbsoluteUrl(url), entity, "application/json", responseHandler);
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
}
