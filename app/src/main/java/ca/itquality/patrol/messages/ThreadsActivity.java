package ca.itquality.patrol.messages;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
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
import ca.itquality.patrol.messages.adapter.ThreadsAdapter;
import ca.itquality.patrol.messages.data.MessageThread;
import ca.itquality.patrol.util.DeviceUtil;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;

import static ca.itquality.patrol.messages.ChatActivity.INCOMING_MESSAGE_INTENT;

public class ThreadsActivity extends AppCompatActivity {

    // Views
    @Bind(R.id.toolbar)
    Toolbar mToolbar;
    @Bind(R.id.threads_placeholder_layout)
    View mPlaceholderLayout;
    @Bind(R.id.threads_recycler)
    RecyclerView mRecycler;

    // Usual variables
    private ThreadsAdapter mAdapter;
    private ArrayList<MessageThread> mThreads = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_threads);
        ButterKnife.bind(this);

        initActionBar();
        initRecycler();
        registerIncomingMessagesListener();
    }

    private void registerIncomingMessagesListener() {
        IntentFilter intentFilter = new IntentFilter(INCOMING_MESSAGE_INTENT);
        registerReceiver(mMessagesReceiver, intentFilter);
    }

    private BroadcastReceiver mMessagesReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            loadThreads();
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        loadThreads();
    }

    private void initRecycler() {
        mRecycler.setLayoutManager(new LinearLayoutManager(this));
        mRecycler.setHasFixedSize(true);
        mRecycler.setItemAnimator(new DefaultItemAnimator());
        mAdapter = new ThreadsAdapter(this, mThreads);
        mRecycler.setAdapter(mAdapter);
    }

    private void loadThreads() {
        ApiInterface apiService = ApiClient.getClient().create(ApiInterface.class);
        Call<ArrayList<MessageThread>> call = apiService.getThreads(DeviceUtil.getToken());
        call.enqueue(new Callback<ArrayList<MessageThread>>() {
            @Override
            public void onResponse(Call<ArrayList<MessageThread>> call,
                                   Response<ArrayList<MessageThread>> response) {
                if (response.isSuccessful()) {
                    ArrayList<MessageThread> threads = response.body();

                    mThreads.clear();
                    mThreads.addAll(threads);
                    mAdapter.notifyDataSetChanged();

                    if (mThreads.size() == 0) {
                        mPlaceholderLayout.setVisibility(View.VISIBLE);
                    } else {
                        mPlaceholderLayout.setVisibility(View.GONE);
                    }
                } else {
                    Toast.makeText(ThreadsActivity.this, "Can't send message", Toast.LENGTH_SHORT)
                            .show();
                }
            }

            @Override
            public void onFailure(Call<ArrayList<MessageThread>> call, Throwable t) {
                Toast.makeText(ThreadsActivity.this, "Server error.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void initActionBar() {
        setSupportActionBar(mToolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
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

    public void onCreateButtonClicked(View view) {
        startActivity(new Intent(this, ContactsActivity.class));
    }

    public void openChat(MessageThread thread) {
        startActivity(new Intent(this, ChatActivity.class)
                .putExtra(ChatActivity.EXTRA_THREAD_ID, thread.getId())
                .putExtra(ChatActivity.EXTRA_THREAD_TITLE, thread.getTitle()));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            unregisterReceiver(mMessagesReceiver);
        } catch (Exception e) {
            // Received wasn't registered
        }
    }
}
