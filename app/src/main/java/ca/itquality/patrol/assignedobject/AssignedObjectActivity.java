package ca.itquality.patrol.assignedobject;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import butterknife.Bind;
import butterknife.ButterKnife;
import ca.itquality.patrol.R;

public class AssignedObjectActivity extends AppCompatActivity {

    @Bind(R.id.toolbar)
    Toolbar mToolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_assigned_object);
        ButterKnife.bind(this);

        initToolbar();
    }

    private void initToolbar() {
        setSupportActionBar(mToolbar);
    }
}
