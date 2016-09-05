package ca.itquality.patrol.messages;

import android.app.NotificationManager;
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
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import org.json.JSONArray;

import java.util.ArrayList;

import butterknife.Bind;
import butterknife.ButterKnife;
import ca.itquality.patrol.R;
import ca.itquality.patrol.library.util.api.ApiClient;
import ca.itquality.patrol.library.util.api.ApiInterface;
import ca.itquality.patrol.messages.adapter.ChatAdapter;
import ca.itquality.patrol.library.util.messages.data.Message;
import ca.itquality.patrol.library.util.messages.data.MessageThread;
import ca.itquality.patrol.util.DeviceUtil;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ChatActivity extends AppCompatActivity {

    // Constants
    static final String EXTRA_PARTICIPANTS = "Participants";
    public static final String EXTRA_THREAD_ID = "ThreadId";
    public static final String EXTRA_THREAD_TITLE = "ThreadTitle";
    public static final int NOTIFICATION_ID_MESSAGE = 0;
    public static final String INCOMING_MESSAGE_INTENT = "MessageIntent";

    // Views
    @Bind(R.id.toolbar)
    Toolbar mToolbar;
    @Bind(R.id.chat_edit_txt)
    EditText mEditTxt;
    @Bind(R.id.chat_send_btn)
    ImageButton mSendBtn;
    @Bind(R.id.chat_recycler)
    RecyclerView mRecycler;

    // Usual variables
    private ArrayList<String> mParticipants = null;
    public static String threadId = null;
    private String mThreadTitle = null;
    private ArrayList<Message> mMessages = new ArrayList<>();
    private ChatAdapter mAdapter;
    public static boolean chatOpened = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
        ButterKnife.bind(this);

        getExtras();
        initActionBar();
        initEditTxt();
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
            loadMessages();
        }
    };

    private void initRecycler() {
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        mRecycler.setLayoutManager(layoutManager);
        mRecycler.setHasFixedSize(true);
        mRecycler.setItemAnimator(new DefaultItemAnimator());
        mAdapter = new ChatAdapter(this, mMessages);
        mRecycler.setAdapter(mAdapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        chatOpened = true;
        loadMessages();
        hideNotification();
    }

    private void hideNotification() {
        NotificationManager notificationManager = (NotificationManager) getSystemService
                (NOTIFICATION_SERVICE);
        notificationManager.cancel(NOTIFICATION_ID_MESSAGE);
    }

    @Override
    protected void onPause() {
        super.onPause();
        chatOpened = false;
    }

    private void loadMessages() {
        ApiInterface apiService = ApiClient.getClient().create(ApiInterface.class);
        Call<ArrayList<Message>> call = apiService.getMessages(DeviceUtil.getUser().getToken(),
                threadId, new JSONArray(mParticipants).toString(), (mParticipants != null
                        && mParticipants.size() > 1) ? mThreadTitle : null);
        call.enqueue(new Callback<ArrayList<Message>>() {
            @Override
            public void onResponse(Call<ArrayList<Message>> call,
                                   Response<ArrayList<Message>> response) {
                if (response.isSuccessful()) {
                    ArrayList<Message> messages = response.body();

                    mMessages.clear();
                    mMessages.addAll(messages);
                    mAdapter.notifyDataSetChanged();
                    mRecycler.post(new Runnable() {
                        @Override
                        public void run() {
                            mRecycler.scrollToPosition(mMessages.size() - 1);
                        }
                    });

                    DeviceUtil.updateChatLastSeenTime();
                } else {
                    Toast.makeText(ChatActivity.this, "Can't retrieve messages",
                            Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ArrayList<Message>> call, Throwable t) {
                Toast.makeText(ChatActivity.this, "Server error.",
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void getExtras() {
        mParticipants = getIntent().getStringArrayListExtra(EXTRA_PARTICIPANTS);
        threadId = getIntent().getStringExtra(EXTRA_THREAD_ID);
        mThreadTitle = getIntent().getStringExtra(EXTRA_THREAD_TITLE);

        setTitle(mThreadTitle);
    }

    private void initEditTxt() {
        mSendBtn.setEnabled(false);
        mEditTxt.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                mSendBtn.setEnabled(mEditTxt.getText().length() > 0);
            }

            @Override
            public void afterTextChanged(Editable editable) {
            }
        });
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        getExtras();
        resetTextField();
    }

    private void resetTextField() {
        mEditTxt.setText("");
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

    public void onSendButtonClicked(View view) {
        String message = mEditTxt.getText().toString();
        mEditTxt.setText("");

        // Add pending message to the chat
        mMessages.add(new Message(null, DeviceUtil.getUser().getUserId(),
                DeviceUtil.getUser().getName(), DeviceUtil.getUser().getPhoto(), message,
                System.currentTimeMillis(), true));
        mAdapter.notifyItemInserted(mMessages.size() - 1);
        mRecycler.scrollToPosition(mMessages.size() - 1);

        // Upload message to server
        ApiInterface apiService = ApiClient.getClient().create(ApiInterface.class);
        Call<MessageThread> call = apiService.sendMessage(DeviceUtil.getUser().getToken(), threadId,
                new JSONArray(mParticipants).toString(),
                (mParticipants != null && mParticipants.size() > 1) ? mThreadTitle : null, message);
        call.enqueue(new Callback<MessageThread>() {
            @Override
            public void onResponse(Call<MessageThread> call, Response<MessageThread> response) {
                if (response.isSuccessful()) {
                    MessageThread thread = response.body();
                    threadId = thread.getId();
                    mParticipants = null;
                    mThreadTitle = null;
                    loadMessages();

                } else {
                    Toast.makeText(ChatActivity.this, "Can't send message",
                            Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<MessageThread> call, Throwable t) {
                Toast.makeText(ChatActivity.this, "Server error.",
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onDestroy() {
        try {
            unregisterReceiver(mMessagesReceiver);
        } catch (Exception e) {
            // Received wasn't registered
        }
        super.onDestroy();
    }
}
