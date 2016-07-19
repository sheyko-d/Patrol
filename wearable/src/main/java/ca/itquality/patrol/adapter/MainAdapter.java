package ca.itquality.patrol.adapter;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

import butterknife.Bind;
import butterknife.ButterKnife;
import ca.itquality.patrol.R;
import ca.itquality.patrol.adapter.data.ListItem;
import ca.itquality.patrol.library.util.Util;

public class MainAdapter extends RecyclerView.Adapter<MainAdapter.ViewHolder> {

    // Constants
    private final static int POS_ALERT = 0;
    private final static int POS_MESSAGES = 1;
    private final static int POS_ACTIVITY = 2;
    private final static int POS_STEPS = 3;
    private final static int POS_HEART_RATE = 4;
    private final static int POS_FLOOR = 5;

    // Usual variables
    private ArrayList<ListItem> mItems;
    private String mLastMessage = "Dmitry: Hey there \uD83D\uDE0E";
    private String mActivityStatus;
    private Integer mStepsCount;
    private Integer mHeartRate;
    private Integer mFloorNumber;
    private final Context mContext;

    public MainAdapter(Context context, ArrayList<ListItem> items) {
        mContext = context;
        mItems = items;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate
                (R.layout.item_main, parent, false));
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        ListItem item = mItems.get(position);
        holder.titleTxt.setText(item.getTitle());
        if (position == POS_ALERT) {
            holder.valueTxt.setText("I need backup!");
        } else if (position == POS_MESSAGES) {
            holder.valueTxt.setText(TextUtils.isEmpty(mLastMessage) ? "—" : mLastMessage);
        } else if (position == POS_ACTIVITY) {
            holder.valueTxt.setText(TextUtils.isEmpty(mActivityStatus) ? "—" : mActivityStatus);
        } else if (position == POS_STEPS) {
            holder.valueTxt.setText(String.valueOf(mStepsCount));
        } else if (position == POS_HEART_RATE) {
            holder.valueTxt.setText(mHeartRate == null ? "—" : mContext.getString
                    (R.string.main_heart_rate, mHeartRate));
        } else if (position == POS_FLOOR) {
            holder.valueTxt.setText("Not implemented yet");
        }
        holder.img.setImageResource(item.getImage());
        holder.img.setBackgroundResource(item.getBackground());
    }

    @Override
    public int getItemCount() {
        return mItems.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        @Bind(R.id.main_title_txt)
        TextView titleTxt;
        @Bind(R.id.main_value_txt)
        TextView valueTxt;
        @Bind(R.id.main_img)
        ImageView img;

        public ViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }

    public void updateLastMessage(String lastMessage) {
        mLastMessage = lastMessage;
        notifyItemChanged(POS_MESSAGES);
    }

    public void updateActivityStatus(String activityStatus) {
        Util.Log("update activity status: " + activityStatus);
        mActivityStatus = activityStatus;
        notifyItemChanged(POS_ACTIVITY);
    }

    public void updateStepsCount(Integer stepsCount) {
        mStepsCount = stepsCount;
        notifyItemChanged(POS_STEPS);
    }

    public void updateHeartRate(Integer heartRate) {
        mHeartRate = heartRate;
        notifyItemChanged(POS_HEART_RATE);
    }

    public void updateFloorNumber(Integer floorNumber) {
        mFloorNumber = floorNumber;
        notifyItemChanged(POS_FLOOR);
    }
}