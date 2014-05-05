package edu.purdue.lab6;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.zip.GZIPInputStream;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.util.Log;

public class JSON_Parse extends AsyncTask<String,String,JSONObject>{
	
	static InputStream inputStream;
	static JSONObject jObj = null;
	static String json ="";
	Activity mAct;
	Context mContext;
	List<NameValuePair> list;
	String method;
	JSONObject jObject;
	
	DatabaseHandler db;
	
	ProgressDialog progDial;
	// constructor
	JSON_Parse(Activity act,Context context, String method, DatabaseHandler db){
		this.mAct = act;
		//this.list = list;
		this.db = db;
		this.method = method;
		this.mContext = context;
	}
	
	@Override
	protected void onPreExecute() {
		super.onPreExecute();
		progDial = new ProgressDialog(mContext);
		progDial.setIndeterminate(false);
		progDial.setProgressStyle(ProgressDialog.STYLE_SPINNER);
		progDial.setCancelable(true);
		progDial.show();
		
	}
	

	@Override
	protected JSONObject doInBackground(String... params) {
		//HttpResponse httpResponse = null;
		HttpResponse httpResponse;
		try{
			if(method == "POST"){
				HttpClient httpClient = new DefaultHttpClient();
				HttpPost httpPost = new HttpPost(params[0]);
				httpPost.setEntity(new UrlEncodedFormEntity(list));

				httpResponse = httpClient.execute(httpPost);
				StatusLine statusLine = httpResponse.getStatusLine();
				int statusCode = statusLine.getStatusCode();
				if(statusCode == 200){
					HttpEntity httpEntity = httpResponse.getEntity();
					inputStream = httpEntity.getContent();
				}
				else{
					Log.e("LOG", "Failed to download result..");
				}

				Header contentencoding = httpResponse.getFirstHeader("Content-Encoding");
				if(contentencoding != null && contentencoding.getValue().equalsIgnoreCase("gzip")){
					inputStream = new GZIPInputStream(inputStream);
				}

				BufferedReader mBufferedReader = new BufferedReader( new InputStreamReader(inputStream, "iso-8859-1"),8);
				StringBuilder stringBuilder = new StringBuilder();
				String line = null;
				while((line = mBufferedReader.readLine()) != null ){
					stringBuilder.append(line +"\n");
				}
				inputStream.close();
				json = stringBuilder.toString();

				jObj = new JSONObject(json);

			}
			else if(method == "GET"){
				
				HttpClient httpClient = new DefaultHttpClient();
				//String paramString = URLEncodedUtils.format(list, "utf-8");
				//params[0]+= "?" +paramString;
				HttpGet httpGet = new HttpGet(params[0]);
				
				httpResponse = httpClient.execute(httpGet);
				StatusLine statusLine = httpResponse.getStatusLine();
				int statusCode = statusLine.getStatusCode();
				if(statusCode == 200){
				
					HttpEntity httpEntity = httpResponse.getEntity();
					inputStream = httpEntity.getContent();
				}
				Header contentencoding = httpResponse.getFirstHeader("Content-Encoding");
				if(contentencoding != null && contentencoding.getValue().equalsIgnoreCase("gzip")){
					inputStream = new GZIPInputStream(inputStream);
				}

				BufferedReader mBufferedReader = new BufferedReader( new InputStreamReader(inputStream, "iso-8859-1"),8);
				StringBuilder stringBuilder = new StringBuilder();
				String line = null;
				while((line = mBufferedReader.readLine()) != null ){
					stringBuilder.append(line +"\n");
				}
				inputStream.close();
				json = stringBuilder.toString();

				jObj = new JSONObject(json);
			}
			else{
				Log.e("Request Alert","Wrong Method Name");
			}
		}
		catch(UnsupportedEncodingException e){
			Log.e("UnsupportEncodingException", e.getMessage().toString());
		} catch (ClientProtocolException e) {
			Log.e("ClientProtocalException", e.getMessage().toString());
		} catch (IOException e) {
			Log.e("IOException",e.getMessage().toString());
		} catch (JSONException e) {
			Log.e("JSONException", e.getMessage().toString());
		}

		return jObj;

	}
	
	@Override
	protected void onPostExecute(JSONObject obj){
		SQLiteDatabase sq = db.getWritableDatabase();
		ContentValues cv = new ContentValues();
		try {
			JSONObject jData = obj.getJSONObject("data");
			
			/* get location*/
			JSONArray jRegion = jData.getJSONArray("request");
			for(int n = 0; n < jRegion.length();n++){
				JSONObject jZip = jRegion.getJSONObject(n);
				
				String zip = jZip.getString("query");
				cv.put("LOCATION_ID", zip);
			}
			
			/* Get weather information for a day */
			JSONArray jArray = jData.getJSONArray("weather");
			for(int i = 0; i< jArray.length();i++){
				JSONObject oneObject = jArray.getJSONObject(i);
				
				String date = oneObject.getString("date");
				String tempLow = oneObject.getString("tempMinF");
				String tempHigh = oneObject.getString("tempMaxF");
				/* Get Weather Description*/
				JSONArray jDesc = oneObject.getJSONArray("weatherDesc");
				for(int j = 0; j < jDesc.length(); j++){
					JSONObject descObject = jDesc.getJSONObject(i);
					
					String weatherDesc = descObject.getString("value");		
					cv.put("WEATHER_DESC", weatherDesc);
				}
				/* Get Weather icon URLs*/
				JSONArray jURL = oneObject.getJSONArray("weatherIconURL");
				for(int k = 0; k< jURL.length(); k++ ){
					JSONObject urlObject = jURL.getJSONObject(i);
					String weatherImageUrl = urlObject.getString("value");
					cv.put("WEATHERICON_URL", weatherImageUrl);
				}
				
				String windDirection = oneObject.getString("winddirection");
				String windSpeed = oneObject.getString("windSpeedMiles");
				cv.put("DAY_ID", date);
				cv.put("TEMPERATURE_ID", tempHigh);	
				cv.put("WINDSPEED_ID", windSpeed);
				cv.put("WINDDIRECTION_ID", windDirection);
				
				sq.insert("TABLE_WEATHER",null,cv);
			}
			
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		
		progDial.dismiss();
	}
}
