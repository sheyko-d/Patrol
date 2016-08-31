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
import ca.itquality.patrol.library.util.auth.data.User;
import ca.itquality.patrol.messages.ContactsActivity;

public class ContactsAdapter extends RecyclerView.Adapter<ContactsAdapter.ViewHolder> {

    private final ContactsActivity mActivity;
    private ArrayList<User> mContacts;

    public ContactsAdapter(ContactsActivity activity, ArrayList<User> contacts) {
        mActivity = activity;
        mContacts = contacts;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate
                (R.layout.item_contact, parent, false));
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

    class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        @Bind(R.id.contacts_img)
        ImageView img;
        @Bind(R.id.contacts_name_txt)
        TextView nameTxt;
        @Bind(R.id.contacts_email_txt)
        TextView emailTxt;

        ViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);

            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            mActivity.openPrivateChat(mContacts.get(getAdapterPosition()));
        }
    }
}