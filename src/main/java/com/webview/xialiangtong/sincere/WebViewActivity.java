package com.webview.xialiangtong.sincere;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.webkit.WebView;

import android.view.Menu;
import android.view.MenuItem;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.concurrent.ExecutionException;

public class WebViewActivity extends AppCompatActivity {
    private WebView webView;
    private int page = 1;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.webview);
        callSearchEngine();
    }

    private void callSearchEngine(){
        String word = getIntent().getStringExtra("q");
        String rank = getIntent().getStringExtra("orderBy");

        new connectServer(this).execute(new String[]{word,rank,String.valueOf(page)});
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.pagemenu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.home:
                homeActivity();
                return true;
            case R.id.next:
                moveNext();
                return true;
            case R.id.pre:
                movePre();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void homeActivity() {
        finish();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event)  {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            finish();
            return true;
        }

       return super.onKeyDown(keyCode, event);
    }

    public void moveNext(){
        page++;
        callSearchEngine();
    }

    public void movePre(){
        page--;
        if(page<1){
            page = 1;
            return;
        }
        callSearchEngine();
    }


    /**create a AsyncTask class*/
    private class connectServer extends AsyncTask<String,Void,String> {
        Context context;
        ProgressDialog progress;

        public connectServer(Context contexts) {
            context = contexts;
        }

        @Override
        protected void onPreExecute() {
            progress = new ProgressDialog(context);
            progress.setTitle("Loading");
            progress.setMessage("Wait while loading...");
            progress.show();
        }

        @Override
        protected void onPostExecute(String result) {
            String data = "";
            WebView wv1 = (WebView)findViewById(R.id.webView1);
            try {
                JSONObject jsonRootObject = new JSONObject(result);

                //Get the instance of JSONArray that contains JSONObjects
                JSONArray jsonArray = jsonRootObject.optJSONArray("results");

                //Iterate the jsonArray and print the info of JSONObjects
                for(int i=0; i < jsonArray.length(); i++){
                    JSONObject jsonObject = jsonArray.getJSONObject(i);

                    String group = jsonObject.optString("group").toString();
                    String link = jsonObject.optString("link").toString();
                    String likes = jsonObject.optString("likes").toString();
                    String shares = jsonObject.optString("shares").toString();
                    String comments = jsonObject.optString("comments").toString();
                    String date = jsonObject.optString("createdtime").toString();
                    String excerpt = "<font face=\"Verdana\" size=\"3\" color=\"gray\">"+jsonObject.optString("excerpt").toString()+"</font>";

                    String firstLine = "<a href = http://www.facebook.com/" + link+ ">" + group + "</a>";
                    data += firstLine+"<br>"+date+"<br>"+likes+" Likes, "+ shares+" Shares, "+ comments +" Comments"+"<br>"+excerpt+"<br><br>";
                }
                //output.setText(data);
                if(data.equals(""))
                    data = "No Result!";

                wv1.loadData(data, "text/html;charset=utf-8", "UTF-8");
            } catch (JSONException e) {
                e.printStackTrace();
                wv1.loadData("No Result!", "text/html;charset=utf-8", "UTF-8");
            }

            if(progress.isShowing())
                progress.dismiss();
        }

        @Override
        protected String doInBackground(String... params) {
            String result = null;
            InputStream is = null;
            StringBuilder sb;
            String word = params[0];
            String rank = params[1].toLowerCase();
            String page = params[2];

            word = word.replaceAll(" ","%20");
            word = word.replaceAll("\n","%20");
            //http post
            try{
                HttpClient httpclient = new DefaultHttpClient();
                String url = "http://cyrus.cs.ucdavis.edu/sincere/q.php?q="+word+"&orderBy="+rank+"&page="+page;
                HttpGet httpget = new HttpGet(url);
                HttpResponse response = httpclient.execute(httpget);
                HttpEntity entity = response.getEntity();
                is = entity.getContent();
            }catch(Exception e){
                Log.e("log_tag", "Error in http connection" + e.toString());
            }
            //convert response to string
            try{
                //BufferedReader reader = new BufferedReader(new InputStreamReader(is,"ISO-8859-1"),8);
                BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
                sb = new StringBuilder();
//            sb.append(reader.readLine() + "\n");
                String line=null;
                while ((line = reader.readLine()) != null) {
                    sb.append(line + "\n");
                }
                is.close();
                result=sb.toString();
                //there are \u0092, \u0093, \u0094 among the string, which are the control character
                //replace it with blank string
                //the range of regex is very important, if we expand the range, the create time will be changed too which is not expected
//            String regex = "([\\\\u009]+[2-4])";
//            result = result.replaceAll(regex,"");
            }catch(Exception e){
                Log.e("log_tag", "Error converting result "+e.toString());
            }

            return result;
        }
    }
}


