package com.androidexam.printshare;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.StringJoiner;
import java.util.stream.Collector;

public abstract class ConnectionTemplate {

    public enum OPERATIONS  {POST, PATCH, PUT, SEARCH_READ}

    public final JSONObject template(String operation, String path, String data, List<String> queries){
        JSONObject response = null;
        HttpURLConnection urlConnection = null;
        if(operation.equals(OPERATIONS.SEARCH_READ.toString())){
            if(data != null)
                urlConnection = createURL("GET", path,
                        firstLimitRead());
            else
                urlConnection = createURL("GET", path,
                        limitRead(data));
        } else
            urlConnection = createURL(operation, path, queries);
        if(connect(urlConnection)) {
            response = execute(urlConnection, data);
        }
        return response;
    }

    abstract String post_exec(JSONObject response);
    abstract boolean showResult();
    abstract void update(String output);

    private boolean writeOperation(String operation){
        return operation.equals(OPERATIONS.POST.toString()) || operation.equals(OPERATIONS.PATCH.toString())
                || operation.equals(OPERATIONS.PUT.toString());
    }
    private HttpURLConnection createURL(String operation, String path, List<String> queries) {
        HttpURLConnection urlConnection = null;
        StringBuilder builder = new StringBuilder();
        try {

            boolean isWrite = writeOperation(operation);
            if(isWrite)
                builder.append(path).append(".json");
            else
                builder.append(path).append(".json?");
            if(queries != null) {
                Collector<String, StringJoiner, String> collector = Collector.of(
                        () -> new StringJoiner("&"),
                        StringJoiner::add,
                        StringJoiner::merge,
                        StringJoiner::toString
                );
                builder.append(queries.stream().collect(collector));
            }
            urlConnection = (HttpURLConnection) new URL(builder.toString()).openConnection();

            if(isWrite){
                urlConnection.setRequestMethod(operation);
                urlConnection.setRequestProperty("Accept", "application/json");
                urlConnection.setRequestProperty("Content-Type", "application/json; utf-8");
                urlConnection.setDoOutput(true);
            } else {
                //TODO passare l'operazione di richiesta nel metodo template.
                urlConnection.setRequestMethod("GET");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return urlConnection;
    }
    private boolean connect(HttpURLConnection urlConnection){
        try{
            urlConnection.connect();
            return true;
        } catch (IOException e){
            e.printStackTrace();
        }
        return false;
    }
    private JSONObject execute(HttpURLConnection urlConnection, String data){
        InputStream inputStream = null;
        OutputStream outputStream = null;
        BufferedReader reader = null;
        StringBuilder builder = new StringBuilder();
        JSONObject object = null;
        try{
            if (data != null) {
                outputStream = urlConnection.getOutputStream();
                byte[] input = data.getBytes(StandardCharsets.UTF_8);
                outputStream.write(input, 0, input.length);
            }

            if(urlConnection.getResponseCode() == 200)
                inputStream = urlConnection.getInputStream();
            else
                inputStream = urlConnection.getErrorStream();

            reader = new BufferedReader(new InputStreamReader(inputStream));
            String line;

            while((line = reader.readLine()) != null){
                builder.append(line);
            }

            try {
                object = new JSONObject(builder.toString());
            } catch (JSONException e) {
                try {
                    object = new JSONObject("{\"readed\":"+builder.toString()+"}");
                } catch (JSONException jsonException) {
                    jsonException.printStackTrace();
                }
            }
            String status = object != null && object.length() != 0 ? "OK" : "ERROR";
            /*try {
                if (object != null) {
                    object.accumulate("status", status);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }*/

        } catch (IOException e){
            e.printStackTrace();
        } finally {
            try {
                if (reader != null)
                    reader.close();
                if (inputStream != null)
                    inputStream.close();
                if (outputStream != null)
                    outputStream.close();
            } catch(IOException e){
                e.printStackTrace();
            }
            urlConnection.disconnect();
        }
        return object;
    }

    public List<String> firstLimitRead(){
        ArrayList<String> queries = new ArrayList<>();
        Collections.addAll(queries,"orderBy=\"$key\"","limitToFirst=20");
        return queries;
    }

    public List<String> limitRead(String start_key){
        ArrayList<String> queries = new ArrayList<>();
        Collections.addAll(queries,"orderBy=\"$key\"","startAt=\""+start_key+"\"",
                "limitToFirst=10");
        return queries;
    }
}
