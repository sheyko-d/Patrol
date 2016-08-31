package ca.itquality.patrol.messages.adapter;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;

import butterknife.Bind;
import butterknife.ButterKnife;
import ca.itquality.patrol.R;
import ca.itquality.patrol.library.util.Util;
import ca.itquality.patrol.messages.ThreadsActivity;
import ca.itquality.patrol.library.util.messages.data.MessageThread;
import ca.itquality.patrol.util.DeviceUtil;

public class ThreadsAdapter extends RecyclerView.Adapter<ThreadsAdapter.ViewHolder> {

    private ThreadsActivity mActivity;
    private ArrayList<MessageThread> mThreads;

    public ThreadsAdapter(ThreadsActivity activity, ArrayList<MessageThread> threads) {
        mActivity = activity;
        mThreads = threads;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate
                (R.layout.item_thread, parent, false));
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        MessageThread thread = mThreads.get(position);
        holder.titleTxt.setText(thread.getTitle());
        if (thread.getLastMessage().getUserId().equals(DeviceUtil.getUserId())) {
            holder.snippedTxt.setText("Me: " + thread.getLastMessage().getText());
        } else {
            holder.snippedTxt.setText(thread.getLastMessage().getText());
        }
        holder.timeTxt.setText(Util.formatTime(mActivity, thread.getLastMessage().getTime()));
        Glide.with(mActivity).load(thread.getPhoto()).error(R.drawable.avatar_placeholder)
                .into(holder.img);
    }

    @Override
    public int getItemCount() {
        return mThreads.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        @Bind(R.id.thread_img)
        ImageView img;
        @Bind(R.id.thread_title_txt)
        TextView titleTxt;
        @Bind(R.id.thread_snipped_txt)
        TextView snippedTxt;
        @Bind(R.id.thread_time_txt)
        TextView timeTxt;

        ViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);

            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            mActivity.openChat(mThreads.get(getAdapterPosition()));
        }
    }
}