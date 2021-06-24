package com.androidexam.printshare.adapters;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityOptionsCompat;
import androidx.core.view.ViewCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.androidexam.printshare.AddPrinterActivity;
import com.androidexam.printshare.utilities.DbCommunication;
import com.androidexam.printshare.utilities.MaterialListItem;
import com.androidexam.printshare.utilities.NotificationListItem;
import com.androidexam.printshare.utilities.PrinterListItem;
import com.androidexam.printshare.ProfileActivity;
import com.androidexam.printshare.R;
import com.androidexam.printshare.utilities.SessionManager;

import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class PrinterAdapter extends RecyclerView.Adapter<PrinterAdapter.PrinterViewHolder> {

    private static final String FIREBASE_DB_ROOT_URL = "https://printshare-77932-default-rtdb.firebaseio.com/";
    List<PrinterListItem> mData;
    Context mContext;
    Object toActivity;

    public PrinterAdapter(List<PrinterListItem> mData, Context mContext, Object toActivity) {
        this.mData = mData;
        this.mContext = mContext;
        this.toActivity = toActivity;
    }

    @NotNull
    @Override
    public PrinterViewHolder onCreateViewHolder(@NotNull ViewGroup parent, int viewType) {
        View layout = LayoutInflater.from(mContext).inflate(R.layout.list_element, parent, false);
        return new PrinterViewHolder(layout, toActivity);
    }

    @Override
    public void onBindViewHolder(@NotNull PrinterViewHolder holder, int position) {
        holder.id = mData.get(position).getId();
        if (mData.get(position).isEdit()) {
            holder.delete.setVisibility(View.VISIBLE);
            holder.item.setClickable(false);
        } else {
            holder.delete.setVisibility(View.GONE);
            holder.item.setClickable(true);
        }
        holder.textView.setText(mData.get(position).getContent());
    }

    @Override
    public int getItemCount() {
        return mData.size();
    }

    public class PrinterViewHolder extends RecyclerView.ViewHolder{

        TextView textView;
        ImageView delete;
        RelativeLayout item;
        String id;

        public PrinterViewHolder(@NonNull @NotNull View itemView, Object toActivity) {
            super(itemView);
            textView = itemView.findViewById(R.id.rv_label);
            delete = itemView.findViewById(R.id.edit);

            delete.setOnClickListener(v -> {
                new AlertDialog.Builder(v.getContext())
                        .setTitle("Delete printer")
                        .setMessage("Do you really want to remove this printer?")
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setPositiveButton(android.R.string.yes, (dialog, whichButton) -> removePrinter(v.getContext(), id, textView.getText().toString()))
                        .setNegativeButton(android.R.string.no, null).show();
            });

            item = itemView.findViewById(R.id.item_layout);
            item.setOnClickListener(v -> {
                if(Objects.nonNull(toActivity)) {
                    //TODO open printer activity with animation.
                    Context c = v.getContext();

                    ActivityOptionsCompat options = ActivityOptionsCompat
                            .makeSceneTransitionAnimation((Activity) c, textView,
                                    Objects.requireNonNull(ViewCompat.getTransitionName(textView)));
                    if (toActivity instanceof ProfileActivity) {
                        c.startActivity(toProfile(c, textView.getText().toString()), options.toBundle());
                    } else {
                        c.startActivity(new Intent(c, toActivity.getClass()), options.toBundle());
                    }
                }
            });
        }

        private Intent toProfile( Context mContext, String username){
            Intent intent = new Intent(mContext, ProfileActivity.class);
            intent.putExtra("FROM", "SEARCH");
            intent.putExtra("USERNAME", username);
            return intent;
        }


    }

    private void removePrinter(Context context, String id, String model){
        String uid = new SessionManager(context).getUid();
        String printer_model = model;
        DbCommunication db = new DbCommunication();
        db.remove("users/"+uid+"/printers/"+id);
        db.remove("user_dim/"+uid+"-"+id);
        db.remove("printer_models/"+model+"/"+uid+"-"+id);
        Arrays.stream(context.getResources().getStringArray(R.array.materials))
                .forEach(material -> {
                    db.remove("materials/"+material+"/"+uid+"-"+id);
                });
        removeData(id);
    }

    private void removeData(String param){
        for(PrinterListItem printer: mData){
            if(printer.getId().equals(param)) {
                int position = mData.indexOf(printer);
                mData.remove(printer);
                notifyItemRemoved(position);
            }
        }
    }
}
