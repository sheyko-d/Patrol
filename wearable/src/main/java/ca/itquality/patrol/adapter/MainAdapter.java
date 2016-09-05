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
    private final static int POS_SHIFT = 2;
    private final static int POS_ACTIVITY = 3;
    private final static int POS_STEPS = 4;
    private final static int POS_HEART_RATE = 5;
    private final static int POS_LOCATION = 6;

    // Usual variables
    private ArrayList<ListItem> mItems;
    private String mLastMessageTitleText;
    private String mLastMessageText;
    private String mShiftTitle;
    private String mShift;
    private String mActivityStatus;
    private Integer mStepsCount;
    private Integer mHeartRate;
    private String mLocation;
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
            holder.titleTxt.setText(mLastMessageTitleText);
            holder.valueTxt.setText(TextUtils.isEmpty(mLastMessageText) ? "—" : mLastMessageText);
        } else if (position == POS_SHIFT) {
            holder.titleTxt.setText(TextUtils.isEmpty(mShiftTitle)
                    ? mContext.getString(R.string.main_shift_title_placeholder) : mShiftTitle);
            holder.valueTxt.setText(TextUtils.isEmpty(mShift)
                    ? mContext.getString(R.string.main_shift_placeholder) : mShift);
        } else if (position == POS_ACTIVITY) {
            holder.valueTxt.setText(TextUtils.isEmpty(mActivityStatus) ? "—" : mActivityStatus);
        } else if (position == POS_STEPS) {
            holder.valueTxt.setText(mStepsCount == null ? "—" : String.valueOf(mStepsCount));
        } else if (position == POS_HEART_RATE) {
            holder.valueTxt.setText(mHeartRate == null ? "—" : mContext.getString
                    (R.string.main_heart_rate, mHeartRate));
        } else if (position == POS_LOCATION) {
            holder.valueTxt.setText(TextUtils.isEmpty(mLocation) ? "—" : mLocation);
        }
        holder.img.setImageResource(item.getImage());
        holder.img.setBackgroundResource(item.getBackground());
    }

    @Override
    public int getItemCount() {
        return mItems.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {

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

    public void updateLastMessage(String title, String text) {
        mLastMessageTitleText = title;
        mLastMessageText = text;
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

    public void updateLocation(String location) {
        mLocation = location;
        notifyItemChanged(POS_LOCATION);
    }

    public void updateShift(String shiftTitle, String shift) {
        mShiftTitle = shiftTitle;
        mShift = shift;
        notifyItemChanged(POS_SHIFT);
    }
}