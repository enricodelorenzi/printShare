package com.androidexam.printshare.utilities;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Picture;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.androidexam.printshare.NotificationActivity;
import com.androidexam.printshare.R;

import org.jetbrains.annotations.NotNull;
import org.json.JSONException;
import org.json.JSONObject;

public class CheckNotifyWork extends Worker {

    private static final String CHANNEL_ID ="printShare";
    private static final String FIREBASE_DB_ROOT_URL = "https://printshare-77932-default-rtdb.firebaseio.com/";
    private final Context mContext;
    private NotificationManagerCompat manager;
    public CheckNotifyWork(@NonNull @NotNull Context context, @NonNull @NotNull WorkerParameters workerParams) {
        super(context, workerParams);
        mContext = context;
    }

    @NonNull
    @NotNull
    @Override
    public Result doWork() {
        DbCommunication db = new DbCommunication();
        SessionManager sessionManager = new SessionManager(mContext);
        String to_user = sessionManager.getUsername();
        JSONObject obj = db.template("GET",FIREBASE_DB_ROOT_URL+"notify/"+to_user,null,null);
        try {
            if(obj.has("readed") && obj.getString("readed").equals("null"))
                return Result.failure();
            else {
                obj.keys().forEachRemaining(from_user -> {
                    try {
                        JSONObject current = obj.getJSONObject(from_user);
                        String type = current.getString("type");

                        NotificationCompat.Builder builder = new NotificationCompat.Builder(mContext, CHANNEL_ID);
                        builder.setSmallIcon(R.mipmap.printshare_icon_round)
                                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                                .setAutoCancel(true)
                                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

                        Intent intent = new Intent(mContext, NotificationActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        PendingIntent pendingIntent = PendingIntent.getActivity(mContext, 0, intent, 0);

                        builder.setContentIntent(pendingIntent);

                        manager = NotificationManagerCompat.from(mContext);

                        if(type.equals("contact")){
                            if(current.getBoolean("flag")){
                                //elimino il flag (notifica visualizzata sul dispositivo)
                                notificationShowed(from_user, to_user);
                                builder.setContentText(from_user + " need your help!")
                                        .setContentTitle("Hey! Someone contacted you!");
                                manager.notify(0, builder.build());
                            }
                        } else if (type.equals("reply")){
                            //TODO show notification true -> open gmail; false -> button read.
                            if(current.getBoolean("flag")){
                                //elimino il flag (notifica visualizzata sul dispositivo)
                                notificationShowed(from_user, to_user);

                                //l'utente ha rifiutato la richiesta
                                if(current.getString("content").equals("refused")){
                                    builder.setContentText(from_user + "refused your request.")
                                            .setContentTitle("Unfortunately your request was denied :(");
                                } else {
                                    builder.setContentText("Contact "+ current.getString("content"))
                                            .setContentTitle(from_user +" accepted your request!");
                                }
                                manager.notify(0, builder.build());
                            }
                        }

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                });
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return Result.success();
    }

    private void notificationShowed(String from_user, String to_user){
        new DbCommunication().template("PATCH",FIREBASE_DB_ROOT_URL+"notify/"+to_user+"/"+from_user,"{\"flag\":\"false\"}", null);
    }

}
