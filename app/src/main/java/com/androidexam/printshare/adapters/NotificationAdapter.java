package com.androidexam.printshare.adapters;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.androidexam.printshare.R;
import com.androidexam.printshare.utilities.DbCommunication;
import com.androidexam.printshare.utilities.NotificationListItem;
import com.androidexam.printshare.utilities.SessionManager;

import org.jetbrains.annotations.NotNull;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder> {
    protected static final String FIREBASE_DB_ROOT_URL = "https://printshare-77932-default-rtdb.firebaseio.com/";
    List<NotificationListItem> mData;
    Context mContext;
    SessionManager sessionManager;

    public NotificationAdapter(List<NotificationListItem> mData, Context mContext) {
        this.mData = mData;
        this.mContext = mContext;
        this.sessionManager = new SessionManager(mContext);
    }

    @NotNull
    @Override
    public NotificationAdapter.NotificationViewHolder onCreateViewHolder(@NotNull ViewGroup parent, int viewType) {
        View layout = LayoutInflater.from(mContext).inflate(R.layout.notification_element, parent, false);
        return new NotificationViewHolder(layout);
    }

    @Override
    public void onBindViewHolder(@NotNull NotificationAdapter.NotificationViewHolder holder, int position) {
        NotificationListItem currentItem = mData.get(position);
        holder.param.setText(currentItem.getFrom_user());
        if(currentItem.getType().equals("reply")){
            holder.accept_button.setVisibility(View.GONE);
            holder.refuse_button.setVisibility(View.GONE);
            //informo l'utente che la sua richiesta è stata rifiutata.
            if(currentItem.getContent().equals("refused")){
                holder.contact_button.setVisibility(View.GONE);
                holder.content.setText("refused your request.");
                holder.delete_button.setVisibility(View.VISIBLE);
            }
            //notifica per inviare una mail.
            else {
                holder.contact_button.setVisibility(View.VISIBLE);
                holder.content.setText(currentItem.getContent());
                holder.delete_button.setVisibility(View.VISIBLE);
            }
        }
        //notifica per la richiesta di una collaborazione.
        else {
            holder.contact_button.setVisibility(View.GONE);
            holder.delete_button.setVisibility(View.GONE);
            holder.accept_button.setVisibility(View.VISIBLE);
            holder.refuse_button.setVisibility(View.VISIBLE);
            holder.content.setText("need your help!");
        }
    }

    @Override
    public int getItemCount() {
        return mData.size();
    }

    public class NotificationViewHolder extends RecyclerView.ViewHolder{

        TextView content, param;
        ImageButton accept_button, refuse_button, contact_button,delete_button;

        public NotificationViewHolder(@NonNull @NotNull View itemView) {
            super(itemView);
            content = itemView.findViewById(R.id.notification_content);
            param = itemView.findViewById(R.id.notification_parameter);
            accept_button = itemView.findViewById(R.id.accept_notification_button);
            refuse_button = itemView.findViewById(R.id.refuse_notification_button);
            contact_button = itemView.findViewById(R.id.contact_notification_button);
            delete_button = itemView.findViewById(R.id.delete_notification_button);

            contact_button.setOnClickListener(v -> {
                contactUser(content.getText().toString());
            });

            accept_button.setOnClickListener(v -> {
                accepted(param.getText().toString());
            });

            refuse_button.setOnClickListener(v -> {
                refuse(param.getText().toString());
            });

            delete_button.setOnClickListener( v -> {
                delete(param.getText().toString());
            });

        }

    }

    private void contactUser(String email){
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/html");
        intent.putExtra(Intent.EXTRA_EMAIL, new String[]{email});
        intent.putExtra(Intent.EXTRA_SUBJECT, "PrintShare");
        intent.putExtra(Intent.EXTRA_TEXT, "Hey! I found you on PrintShare: let's work together!");

        try {
            mContext.startActivity(Intent.createChooser(intent, "Send Email"));
        } catch (ActivityNotFoundException e){
            e.printStackTrace();
        }
    }

    private void accepted(String key){
        String username = sessionManager.getUsername();
        String uid = sessionManager.getUid();
        DbCommunication db = new DbCommunication();

        String[] email = new String[1];

        ExecutorService executor = Executors.newFixedThreadPool(2);
        ArrayList<Callable<JSONObject>> tasks = new ArrayList<>();
        tasks.add(() -> db.template("DELETE", FIREBASE_DB_ROOT_URL + "notify/" + username+"/"+key, null, null));
        tasks.add(() -> db.template("GET", FIREBASE_DB_ROOT_URL + "users/" + uid+"/metadata/email", null, null));
        try {
            executor.invokeAll(tasks)
            .forEach(future -> {
                try {
                    JSONObject obj = future.get();
                    if(!obj.has("removed")){
                        email[0] = obj.getString("readed");
                    }
                } catch (ExecutionException | InterruptedException | JSONException e) {
                    e.printStackTrace();
                }
            });
            executor.shutdown();
            executor.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            if(!executor.isTerminated())
                executor.shutdownNow();
        }
        //send notification + clean data
        db.addNotification(username, key, "reply", true, email[0]);
        removeData(key);
    }

    private void refuse(String key){
        String username = sessionManager.getUsername();
        DbCommunication db = new DbCommunication();
        delete(key);
        db.addNotification(username, key, "reply", true, "refused");
    }

    private void delete(String key){
        //TODO informare che la richiesta è stata rifiutata
        String username = sessionManager.getUsername();
        DbCommunication db = new DbCommunication();
        Callable<JSONObject> task = () -> db.template("DELETE", FIREBASE_DB_ROOT_URL + "notify/" + username+"/"+key, null, null);
        ExecutorService executor = Executors.newFixedThreadPool(1);
        try {
            executor.submit(task);
            executor.shutdown();
            executor.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            if(!executor.isTerminated())
                executor.shutdownNow();
        }
        removeData(key);
    }

    private void removeData(String param){
        for(NotificationListItem notification: mData){
            if(notification.getFrom_user().equals(param)) {
                int position = mData.indexOf(notification);
                mData.remove(notification);
                notifyItemRemoved(position);
            }
        }
    }


}
