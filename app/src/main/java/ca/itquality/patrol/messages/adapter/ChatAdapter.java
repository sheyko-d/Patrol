package ca.itquality.patrol.messages.adapter;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;

import butterknife.Bind;
import butterknife.ButterKnife;
import ca.itquality.patrol.R;
import ca.itquality.patrol.library.util.Util;
import ca.itquality.patrol.messages.data.Message;
import ca.itquality.patrol.util.DeviceUtil;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ViewHolder> {

    // Constants
    private static final int ITEM_OUT_MESSAGE = 0;
    private static final int ITEM_IN_MESSAGE = 1;

    // Usual variables
    private ArrayList<Message> mMessages;
    private Context mContext;

    public ChatAdapter(Context context, ArrayList<Message> messages) {
        mContext = context;
        mMessages = messages;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate
                (viewType == ITEM_OUT_MESSAGE ? R.layout.item_message_out
                        : R.layout.item_message_in, parent, false));
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        Message message = mMessages.get(position);
        holder.textTxt.setText(message.getText());
        String time = Util.formatTime(mContext, message.getTime());
        holder.timeTxt.setText(message.isPending() ? "Sending..." : time);
        holder.textTxt.setAlpha(message.isPending() ? 0.5f : 1);
        holder.timeTxt.setAlpha(message.isPending() ? 0.5f : 1);
    }

    @Override
    public int getItemCount() {
        return mMessages.size();
    }

    @Override
    public int getItemViewType(int position) {
        return mMessages.get(position).getUserId().equals(DeviceUtil.getUserId())
                ? ITEM_OUT_MESSAGE : ITEM_IN_MESSAGE;
    }

    class ViewHolder extends RecyclerView.ViewHolder {

        @Bind(R.id.messages_text_txt)
        TextView textTxt;
        @Bind(R.id.messages_time_txt)
        TextView timeTxt;

        ViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }
}