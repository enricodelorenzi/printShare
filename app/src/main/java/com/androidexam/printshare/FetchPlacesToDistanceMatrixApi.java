package com.androidexam.printshare;

import android.os.Handler;
import android.widget.EditText;
import android.widget.TextView;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.concurrent.Executors;
import java.util.stream.Collector;

public class FetchPlacesToDistanceMatrixApi extends  ConnectionTemplate{

    //private static final String LOG_TAG = FetchPlacesToDistanceMatrixApi.class.getSimpleName();
    private static final String GOOGLE_DISTANCEMATRIX_API_BASE_URL = "https://maps.googleapis.com/maps/api/distancematrix/json?";
    private static final String FIREBASE_DB_ROOT_URL = "https://printshare-77932-default-rtdb.firebaseio.com/";
    private static final String DESTINATION = "destinations";
    private static final String ORIGIN = "origins";
    private static final String API_KEY = "key";
    private static final String apiKey = "AIzaSyAAOOVTL6XHetI4hpeIAzYuSU3B_JVPajw";
    private final WeakReference<TextView> mResultText;
    private final WeakReference<EditText> mInputText;
    private String destinations;
    private final Handler handler;

    {
        handler = new Handler();
    }

    FetchPlacesToDistanceMatrixApi(EditText input, TextView output){
        mInputText = new WeakReference<>(input);
        mResultText = new WeakReference<>(output);
    }

    public void launchAsyncTask(){

        Executors.newFixedThreadPool(1).execute(()->{
            destinations = produceInput(template("READ", FIREBASE_DB_ROOT_URL+"user_pos",null,null));
            List<String> queries = new ArrayList<>();
            Collections.addAll(queries, ORIGIN + "=" + mInputText.get().getText().toString(),
                                            DESTINATION+"="+destinations,
                                            API_KEY+"="+apiKey);
            JSONObject response = template(null,GOOGLE_DISTANCEMATRIX_API_BASE_URL,null, queries);
            handler.post(()->{
                String output = post_exec(response);
                update(output);
            });
        });
    }

    @Override
    String post_exec(JSONObject response) {
        StringBuilder result = new StringBuilder();
        try {
            boolean general_status_ok = response.getString("status").equals("OK");
            if(general_status_ok){
                String current_status;
                JSONArray elements = response.getJSONArray("rows").getJSONObject(0).getJSONArray("elements");
                int length = elements.length();
                for(int index = 0; index < length; index++){
                    JSONObject current_element = elements.getJSONObject(index);
                    current_status = current_element.getString("status");
                    if(current_status.equals("OK")){
                        int value = current_element.getJSONObject("distance").getInt("value");
                        if(value == 0)
                            result.append("User is in your same city!\n");
                        else
                            result.append(value).append(" String: ")
                                    .append(current_element.getJSONObject("distance").getString("text")).append("\n");
                    }
                    else if (current_status.equals("ZERO_RESULTS"))
                        result.append("Impossible to evaluate the distance.\n");
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return result.toString();
    }

    @Override
    boolean showResult() {
        return true;
    }

    @Override
    void update(String output) {
        mResultText.get().setText(output);
    }

    private String produceInput(JSONObject obj) {
        Collector<String, StringJoiner, String> place_collector = Collector.of(
                () -> new StringJoiner("|"),
                StringJoiner::add,
                StringJoiner::merge,
                StringJoiner::toString
        );

        Map<String,String> map = new HashMap<>();
        obj.keys().forEachRemaining(key -> {
            try {
                map.put(key,obj.getString(key));
            } catch (JSONException e) {
                e.printStackTrace();
            }
        });

        //build query paeram destinations
        return map.values().stream().collect(place_collector);

    }
}
