package ca.itquality.patrol.main.adapter;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;

import butterknife.Bind;
import butterknife.ButterKnife;
import ca.itquality.patrol.R;
import ca.itquality.patrol.library.util.Util;
import ca.itquality.patrol.main.MainActivity;
import ca.itquality.patrol.main.data.Watch;
import ca.itquality.patrol.util.DeviceUtil;

public class WatchesAdapter extends RecyclerView.Adapter<WatchesAdapter.ViewHolder> {

    // Usual variables
    private ArrayList<Watch> mWatches;
    private int mEditingPos = -1;
    private String mSelectedNodeId = null;
    private final MainActivity mActivity;

    public WatchesAdapter(MainActivity activity, ArrayList<Watch> watches, String connectedNodeId) {
        mActivity = activity;
        mWatches = watches;
        mSelectedNodeId = connectedNodeId;
        Util.Log("mSelectedNodeId = "+mSelectedNodeId);
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate
                (R.layout.item_watch, parent, false));
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        Watch watch = mWatches.get(position);

        if (DeviceUtil.getWatchNames().containsKey(watch.getId())) {
            holder.nameTxt.setText(DeviceUtil.getWatchNames().get(watch.getId()));
        } else {
            holder.nameTxt.setText(watch.getLabel());
        }

        if (mEditingPos == position) {
            holder.nameEditTxt.setVisibility(View.VISIBLE);
            holder.nameTxt.setVisibility(View.GONE);
            holder.editImg.setImageResource(R.drawable.watch_rename_done);
        } else {
            holder.nameEditTxt.setVisibility(View.GONE);
            holder.nameTxt.setVisibility(View.VISIBLE);
            holder.editImg.setImageResource(R.drawable.watch_rename);
        }

        holder.radioBtn.setChecked(watch.getId().equals(mSelectedNodeId));
    }

    @Override
    public int getItemCount() {
        return mWatches.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener,
            CompoundButton.OnCheckedChangeListener {

        @Bind(R.id.watch_name_txt)
        TextView nameTxt;
        @Bind(R.id.watch_name_edit_txt)
        EditText nameEditTxt;
        @Bind(R.id.watch_edit_img)
        ImageView editImg;
        @Bind(R.id.watch_radio_btn)
        RadioButton radioBtn;

        ViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);

            editImg.setOnClickListener(this);
            nameTxt.setOnClickListener(this);
            radioBtn.setOnCheckedChangeListener(this);
        }

        @Override
        public void onClick(View view) {
            if (getAdapterPosition() == -1) return;

            Watch watch = mWatches.get(getAdapterPosition());
            if (view.getId() == R.id.watch_edit_img) {
                if (getAdapterPosition() == mEditingPos) {
                    HashMap<String, String> watchNames = DeviceUtil.getWatchNames();
                    watchNames.put(watch.getId(),
                            nameEditTxt.getText().toString());
                    DeviceUtil.setWatchNames(watchNames);
                    mEditingPos = -1;
                } else {
                    mEditingPos = getAdapterPosition();
                }
            } else if (view.getId() == R.id.watch_name_txt) {
                mSelectedNodeId = watch.getId();
                mActivity.connectToWatch(mSelectedNodeId);
            }
            notifyDataSetChanged();
        }

        @Override
        public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
            if (checked) {
                mSelectedNodeId = mWatches.get(getAdapterPosition()).getId();
                mActivity.connectToWatch(mSelectedNodeId);
                compoundButton.post(new Runnable() {
                    @Override
                    public void run() {
                        notifyDataSetChanged();
                    }
                });
            }
        }
    }
}