package ca.itquality.patrol.messages.adapter;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;

import butterknife.Bind;
import butterknife.ButterKnife;
import ca.itquality.patrol.R;
import ca.itquality.patrol.library.util.auth.data.User;
import ca.itquality.patrol.messages.GroupActivity;

public class GroupAdapter extends RecyclerView.Adapter<GroupAdapter.ViewHolder> {

    private final GroupActivity mActivity;
    private ArrayList<User> mContacts;
    private ArrayList<String> mSelectedIds = new ArrayList<>();

    public GroupAdapter(GroupActivity activity, ArrayList<User> contacts) {
        mActivity = activity;
        mContacts = contacts;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate
                (R.layout.item_group, parent, false));
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        User contact = mContacts.get(position);
        holder.nameTxt.setText(contact.getName());
        holder.emailTxt.setText(contact.getEmail());
        Glide.with(mActivity).load(contact.getPhoto()).placeholder(R.drawable.avatar_placeholder)
                .into(holder.img);
    }

    @Override
    public int getItemCount() {
        return mContacts.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener,
            CompoundButton.OnCheckedChangeListener {

        @Bind(R.id.group_check_box)
        CheckBox checkBox;
        @Bind(R.id.group_img)
        ImageView img;
        @Bind(R.id.group_name_txt)
        TextView nameTxt;
        @Bind(R.id.group_email_txt)
        TextView emailTxt;

        ViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);

            itemView.setOnClickListener(this);
            checkBox.setOnCheckedChangeListener(this);
        }

        @Override
        public void onClick(View view) {
            checkBox.setChecked(!checkBox.isChecked());
        }

        @Override
        public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
            if (checked) {
                mSelectedIds.add(mContacts.get(getAdapterPosition()).getUserId());
            } else {
                mSelectedIds.remove(mContacts.get(getAdapterPosition()).getUserId());
            }
        }
    }

    public ArrayList<String> getSelectedIds() {
        return mSelectedIds;
    }
}