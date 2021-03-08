package com.androidexam.printshare;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class NetworkUtils{
    private static final String LOG_TAG = NetworkUtils.class.getSimpleName();
    private static final String apiKey = "AIzaSyAAOOVTL6XHetI4hpeIAzYuSU3B_JVPajw";

    static String findPlace(String place_name){
        HttpURLConnection urlConnection = null;
        BufferedReader reader = null;
        String responseJSONString = null;
        try{
            URL requestURL = new URL("https://maps.googleapis.com/maps/api/place/findplacefromtext/json?input="
                    +place_name+"&inputtype=textquery&fields=place_id,name,types&key="+apiKey);
            urlConnection = (HttpURLConnection) requestURL.openConnection();
            urlConnection.setRequestMethod("GET");
            urlConnection.connect();

            InputStream inputStream = urlConnection.getInputStream();
            reader = new BufferedReader(new InputStreamReader((inputStream)));
            StringBuilder builder = new StringBuilder();

            String line;
            while((line = reader.readLine()) != null){
                builder.append(line);
            }
            if (builder.length() == 0)
                return null;
            responseJSONString = builder.toString();
        } catch (IOException e){
            e.printStackTrace();
        } finally {
            if(urlConnection != null)
                urlConnection.disconnect();
            if(reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return responseJSONString;
    }
}
