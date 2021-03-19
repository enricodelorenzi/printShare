package com.androidexam.printshare;

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
import java.util.List;
import java.util.StringJoiner;
import java.util.stream.Collector;

public abstract class ConnectionTemplate {

    public enum OPERATIONS  {POST, PATCH, PUT}

    public final JSONObject template(String operation, String path, String data, List<String> queries){
        JSONObject response = null;
        HttpURLConnection urlConnection = null;
        if(isLimitedRead()){
            urlConnection = createURL("GET", path, limitRead(data));
        } else
            urlConnection = createURL(operation, path, queries);
        if(connect(urlConnection)) {
            response = execute(urlConnection, data);
        }
        return response;
    }

    abstract boolean isLimitedRead();
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
                urlConnection.setRequestProperty("Accept", "application/json");
                urlConnection.setRequestProperty("Content-Type", "application/json; utf-8");
                urlConnection.setDoOutput(true);
            }
            urlConnection.setRequestMethod(operation);

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
                if(builder.toString().startsWith("{")) {
                    object = new JSONObject(builder.toString());
                }
                else {
                    object = new JSONObject("{\"readed\":"+builder.toString()+"}");
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
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
    private List<String> limitRead(String start_key){
        ArrayList<String> queries = new ArrayList<>();
        queries.add("orderBy=\"$key\"");
        if(start_key != null){
            queries.add("startAt=\""+start_key+"\"");
            queries.add("limitToFirst=10");
        } else {
            queries.add("limitToFirst=20");
        }
        return queries;
    }
}
