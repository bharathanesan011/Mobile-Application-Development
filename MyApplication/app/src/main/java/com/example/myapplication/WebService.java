package com.example.myapplication;


import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.webkit.WebView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.net.URLEncoder;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public class WebService extends AppCompatActivity {
    private WebView browser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_web_service);

        Intent secondIntent = getIntent( );
        int Uid= secondIntent.getIntExtra("Uid",0);

        browser = (WebView) findViewById(R.id.webkit);
        try {
            URL pageURL = new URL(getString(R.string.url));
            trustAllHosts();
            HttpURLConnection con = (HttpURLConnection)pageURL.openConnection();

            String jsonString = getString(R.string.json);

            DatabaseHelper db = new DatabaseHelper(this);
            String JSONString= db.GetJSONString(Uid);
            Log.v(this.getClass().getName(), "Json String:"+JSONString);

            JsonThread myTask = new JsonThread(this, con, JSONString);
            Thread t1 = new Thread(myTask, "JSON Thread");
            t1.start();

        } catch (IOException e) { e.printStackTrace(); }

    }

    private void trustAllHosts() {
        // Create a trust manager that does not validate certificate chains
        TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
            public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                return new java.security.cert.X509Certificate[] {};
            }

            public void checkClientTrusted(X509Certificate[] chain,
                                           String authType) throws CertificateException {
            }

            public void checkServerTrusted(X509Certificate[] chain,
                                           String authType) throws CertificateException {
            }
        } };

        // Install the all-trusting trust manager
        try {
            SSLContext sc = SSLContext.getInstance("TLS");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
            HttpsURLConnection
                    .setDefaultSSLSocketFactory(sc.getSocketFactory());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    class JsonThread implements Runnable
    {
        private AppCompatActivity activity;
        private HttpURLConnection con;
        private String jsonPayLoad;

        public JsonThread(AppCompatActivity activity, HttpURLConnection con, String jsonPayload)
        {
            this.activity = activity;
            this.con = con;
            this.jsonPayLoad = jsonPayload;
        }

        @Override
        public void run()
        {
            String response = "";
            if (prepareConnection()) {
                response = postJson();
            } else {
                response = "Error preparing the connection";
            }
            showResult(response);
        }


        private void showResult(String response) {
            activity.runOnUiThread(new Runnable()
            {
                @Override
                public void run()
                {
                    String page = generatePage(response);
                    ((WebService)activity).browser.loadData(page, "text/html", "UTF-8");
                }
            });
        }

        private String postJson() {
            String response = "";
            try {
                String postParameters = "jsonpayload=" + URLEncoder.encode(jsonPayLoad, "UTF-8");
                con.setFixedLengthStreamingMode(postParameters.getBytes().length);
                PrintWriter out = new PrintWriter(con.getOutputStream());
                out.print(postParameters);
                out.close();
                int responseCode = con.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    response = readStream(con.getInputStream());
                } else {
                    response = "Error contacting server: " + responseCode;
                }
            } catch (Exception e) {
                response = e.toString();//"Error executing code";
            }
            return response;
        }

        private String readStream(InputStream in) {
            StringBuilder sb = new StringBuilder();
            try(BufferedReader reader = new BufferedReader(new InputStreamReader(in))) {
                String nextLine = "";
                while ((nextLine = reader.readLine()) != null) {
                    sb.append(nextLine);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            return sb.toString();
        }

        private String generatePage(String content) {
            return "<html><body><p>" + content + "</p></body></html>";
        }


        private boolean prepareConnection() {
            try {
                con.setDoOutput(true);
                con.setRequestMethod("POST");
                con.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                return true;

            } catch (ProtocolException e) {
                e.printStackTrace();
            }
            return false;
        }
    }
}