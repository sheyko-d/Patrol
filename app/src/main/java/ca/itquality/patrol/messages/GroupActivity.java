package ca.itquality.patrol.messages;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import java.util.ArrayList;

import butterknife.Bind;
import butterknife.ButterKnife;
import ca.itquality.patrol.R;
import ca.itquality.patrol.api.ApiClient;
import ca.itquality.patrol.api.ApiInterface;
import ca.itquality.patrol.auth.data.User;
import ca.itquality.patrol.library.util.Util;
import ca.itquality.patrol.messages.adapter.GroupAdapter;
import ca.itquality.patrol.util.DeviceUtil;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;

public class GroupActivity extends AppCompatActivity {

    // Views
    @Bind(R.id.toolbar)
    Toolbar mToolbar;
    @Bind(R.id.group_title_edit_txt)
    EditText mTitleEditTxt;
    @Bind(R.id.group_placeholder_layout)
    View mPlaceholderLayout;
    @Bind(R.id.group_recycler)
    RecyclerView mRecycler;
    @Bind(R.id.group_layout)
    View mLayout;

    // Usual variables
    private GroupAdapter mAdapter;
    private ArrayList<User> mContacts = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group);
        ButterKnife.bind(this);

        initActionBar();
        initRecycler();
        loadContacts();
    }

    private void initActionBar() {
        setSupportActionBar(mToolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    private void initRecycler() {
        mRecycler.setHasFixedSize(true);
        mRecycler.setLayoutManager(new LinearLayoutManager(this));
        mRecycler.setItemAnimator(new DefaultItemAnimator());
        mAdapter = new GroupAdapter(this, mContacts);
        mRecycler.setAdapter(mAdapter);
    }

    private void loadContacts() {
        ApiInterface apiService = ApiClient.getClient().create(ApiInterface.class);
        Call<ArrayList<User>> call = apiService.getContacts(DeviceUtil.getToken());
        call.enqueue(new Callback<ArrayList<User>>() {
            @Override
            public void onResponse(Call<ArrayList<User>> call, Response<ArrayList<User>> response) {
                if (response.isSuccessful()) {
                    ArrayList<User> contacts = response.body();

                    mContacts.clear();
                    mContacts.addAll(contacts);

                    mAdapter.notifyDataSetChanged();

                    if (contacts.size() == 0) {
                        mPlaceholderLayout.setVisibility(View.VISIBLE);
                        mLayout.setVisibility(View.GONE);
                    } else {
                        mPlaceholderLayout.setVisibility(View.GONE);
                        mLayout.setVisibility(View.VISIBLE);
                    }
                } else {
                    Toast.makeText(GroupActivity.this, "Can't retrieve contracts",
                            Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ArrayList<User>> call, Throwable t) {
                Toast.makeText(GroupActivity.this, "Server error.",
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            startActivity(new Intent(this, ContactsActivity.class));
        } else {
            openGroupChat();
        }
        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_group, menu);
        return true;
    }

    /**
     * Required for the font library.
     */
    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(CalligraphyContextWrapper.wrap(newBase));
    }

    public void openGroupChat() {
        String groupTitle = mTitleEditTxt.getText().toString();
        if (TextUtils.isEmpty(groupTitle)) {
            Toast.makeText(this, "Title is empty", Toast.LENGTH_SHORT).show();
            return;
        }

        if (mAdapter.getSelectedIds().size() <= 1) {
            Toast.makeText(this, "Select at least 2 people", Toast.LENGTH_SHORT).show();
            return;
        }

        ArrayList<String> participants = new ArrayList<>();
        for (String id : mAdapter.getSelectedIds()){
            participants.add(id);
            Util.Log("add selected id: "+id);
        }
        startActivity(new Intent(this, ChatActivity.class)
                .putStringArrayListExtra(ChatActivity.EXTRA_PARTICIPANTS, participants)
                .putExtra(ChatActivity.EXTRA_THREAD_TITLE, groupTitle));
        finish();
    }

    @Override
    public void onBackPressed() {
        finish();
        startActivity(new Intent(this, ContactsActivity.class));
    }

}
