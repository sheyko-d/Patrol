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
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import java.util.ArrayList;

import butterknife.Bind;
import butterknife.ButterKnife;
import ca.itquality.patrol.R;
import ca.itquality.patrol.api.ApiClient;
import ca.itquality.patrol.api.ApiInterface;
import ca.itquality.patrol.auth.data.User;
import ca.itquality.patrol.messages.adapter.ContactsAdapter;
import ca.itquality.patrol.util.DeviceUtil;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;

public class ContactsActivity extends AppCompatActivity {

    // Views
    @Bind(R.id.toolbar)
    Toolbar mToolbar;
    @Bind(R.id.contacts_placeholder_layout)
    View mPlaceholderLayout;
    @Bind(R.id.contacts_recycler)
    RecyclerView mRecycler;
    @Bind(R.id.contacts_layout)
    View mLayout;

    // Usual variables
    private ContactsAdapter mAdapter;
    private ArrayList<User> mContacts = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contacts);
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
        mAdapter = new ContactsAdapter(this, mContacts);
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
                    Toast.makeText(ContactsActivity.this, "Can't retrieve contracts",
                            Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ArrayList<User>> call, Throwable t) {
                Toast.makeText(ContactsActivity.this, "Server error.",
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        finish();
        return true;
    }

    /**
     * Required for the font library.
     */
    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(CalligraphyContextWrapper.wrap(newBase));
    }

    public void openPrivateChat(final User contact) {
        ArrayList<String> recipients = new ArrayList<>();
        recipients.add(contact.getUserId());
        startActivity(new Intent(this, ChatActivity.class)
                .putStringArrayListExtra(ChatActivity.EXTRA_PARTICIPANTS, recipients)
                .putExtra(ChatActivity.EXTRA_THREAD_TITLE, contact.getName()));
        finish();
    }
}
