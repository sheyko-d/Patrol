package ca.itquality.patrol.main.adapter;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.RadioButton;
import android.widget.TextView;

import java.util.ArrayList;

import butterknife.Bind;
import butterknife.ButterKnife;
import ca.itquality.patrol.R;
import ca.itquality.patrol.library.util.auth.data.User;
import ca.itquality.patrol.main.MainActivity;
import ca.itquality.patrol.util.DeviceUtil;

public class AccountsAdapter extends RecyclerView.Adapter<AccountsAdapter.ViewHolder> {

    // Usual variables
    private ArrayList<User> mAccounts;
    private int mEditingPos = -1;
    private String mSelectedNodeId = null;
    private final MainActivity mActivity;

    public AccountsAdapter(MainActivity activity, ArrayList<User> accounts) {
        mActivity = activity;
        mAccounts = accounts;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate
                (R.layout.item_account, parent, false));
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        User account = mAccounts.get(position);

        holder.nameTxt.setText(account.getName());
        holder.radioBtn.setChecked(account.getUserId().equals(DeviceUtil.getUserId()));
    }

    @Override
    public int getItemCount() {
        return mAccounts.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener,
            CompoundButton.OnCheckedChangeListener {

        @Bind(R.id.account_name_txt)
        TextView nameTxt;
        @Bind(R.id.account_radio_btn)
        RadioButton radioBtn;

        ViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);

            nameTxt.setOnClickListener(this);
            radioBtn.setOnCheckedChangeListener(this);
        }

        @Override
        public void onClick(View view) {
            if (getAdapterPosition() == -1) return;

            User account = mAccounts.get(getAdapterPosition());
            mActivity.changeAccount(account);
            notifyDataSetChanged();
        }

        @Override
        public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
            if (checked) {
                mActivity.changeAccount(mAccounts.get(getAdapterPosition()));
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