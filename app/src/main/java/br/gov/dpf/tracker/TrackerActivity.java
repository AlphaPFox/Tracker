package br.gov.dpf.tracker;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.SwitchCompat;
import android.support.v7.widget.Toolbar;
import android.text.method.LinkMovementMethod;
import android.util.SparseArray;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.WriteBatch;
import com.google.firebase.messaging.FirebaseMessaging;
import com.xw.repo.BubbleSeekBar;

import br.gov.dpf.tracker.Entities.Tracker;

import static br.gov.dpf.tracker.SettingsActivity.RESULT_ERROR;
import static br.gov.dpf.tracker.SettingsActivity.RESULT_SUCCESS;

public class TrackerActivity extends AppCompatActivity {

    //Object representing the tracker to be inserted/updated
    private Tracker tracker;

    //Menu item used to confirm settings
    private MenuItem confirmMenu;

    //Shared preferences used on notification options
    private SharedPreferences sharedPreferences;

    //Firebase Cloud Messaging
    private FirebaseMessaging notifications;

    //Firebase Firestore DB
    private FirebaseFirestore firestoreDB;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Get shared preferences
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        //Get FCM instance
        notifications = FirebaseMessaging.getInstance();

        //Get DB instance
        firestoreDB = FirebaseFirestore.getInstance();

        //Get tracker from intent
        tracker = getIntent().getParcelableExtra("Tracker");

        //Check model value
        switch (tracker.getModel())
        {
            case "tk102b":
                //Define TK 102B tracker model layout
                loadTK102B();
                break;
            case "spot":
                //Define SPOT Trace layout
                loadSPOT();
                break;
            case "st940":
                //Define SPOT Trace layout
                loadST940();
                break;
            default:
                //Unsupported model, just finish activity
                finish();
                break;
        }
    }

    private void loadToolbar(String trackerTitle)
    {
        //Set toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        //Check action bar
        if(getSupportActionBar() != null)
        {
            //Set back button on toolbar
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        //Find floating action button
        FloatingActionButton fab = findViewById(R.id.fab);

        //Set floating action button event
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //onSettingsConfirmed(null);
            }
        });

        //Check support action bar
        if(getIntent().getBooleanExtra("InsertTracker", true) && getSupportActionBar() != null)
        {
            //Change activity title
            getSupportActionBar().setTitle(trackerTitle);
        }
        else if(getIntent().getBooleanExtra("UpdateTracker", true) && getSupportActionBar() != null)
        {
            //Change activity title
            getSupportActionBar().setTitle(tracker.getName());
        }
    }


    private void loadSPOT()
    {
        //Load specific layout to this model
        setContentView(R.layout.activity_tracker_spot);

        //Load toolbar
        loadToolbar(getResources().getString(R.string.title_tracker_spot_settings));

        //Set support text
        TextView lblSpotConfig = findViewById(R.id.lblSpotConfig);
        lblSpotConfig.setText(getResources().getText(R.string.lblSpotConfig));
        lblSpotConfig.setClickable(true);
        lblSpotConfig.setMovementMethod(LinkMovementMethod.getInstance());

        //Set visibility events for specific switches
        changeLayoutVisibility(R.id.swNotifications, R.id.vwNotificationOptions, -1);
    }

    private void loadST940()
    {
        //Load specific layout to this model
        setContentView(R.layout.activity_tracker_st940);

        //Load toolbar
        loadToolbar(getResources().getString(R.string.title_tracker_st940_settings));

        //Set support text
        ((TextView) findViewById(R.id.lblUpdateInterval)).setText(getResources().getText(R.string.lblUpdateInterval));

        //Set seek bar sections
        ((BubbleSeekBar) findViewById(R.id.seekBarActive)).setCustomSectionTextArray(new BubbleSeekBar.CustomSectionTextArray() {
            @NonNull
            @Override
            public SparseArray<String> onCustomize(int sectionCount, @NonNull SparseArray<String> array) {
                array.clear();

                //Update interval options
                array.put(0, "1 minuto");
                array.put(1, "15 min");
                array.put(2, "30 min");
                array.put(3, "45 min");
                array.put(4, "1 hora");

                return array;
            }
        });

        //Set seek bar sections
        ((BubbleSeekBar) findViewById(R.id.seekBarIdle)).setCustomSectionTextArray(new BubbleSeekBar.CustomSectionTextArray() {
            @NonNull
            @Override
            public SparseArray<String> onCustomize(int sectionCount, @NonNull SparseArray<String> array) {
                array.clear();

                //Update interval options
                array.put(0, "1 hora");
                array.put(1, "3 horas");
                array.put(2, "6 horas");
                array.put(3, "12 horas");
                array.put(4, "1 dia");

                return array;
            }
        });

        //Set visibility events for specific switches
        changeLayoutVisibility(R.id.swEmergency, R.id.vwEmergency, R.id.cbEmergency);
        changeLayoutVisibility(R.id.swMagnetAlert, -1, R.id.cbMagnet);
        changeLayoutVisibility(R.id.swUpdateInterval, R.id.vwUpdateInterval, -1);
        changeLayoutVisibility(R.id.swNotifications, R.id.vwNotificationOptions, -1);
    }

    private void loadTK102B()
    {
        //Load specific layout to this model
        setContentView(R.layout.activity_tracker_tk102b);

        //Load toolbar
        loadToolbar(getResources().getString(R.string.title_tracker_tk102b_settings));

        //Set support text
        ((TextView) findViewById(R.id.lblUpdateInterval)).setText(getResources().getText(R.string.lblUpdateInterval));

        //Set seek bar sections
        ((BubbleSeekBar) findViewById(R.id.seekBar)).setCustomSectionTextArray(new BubbleSeekBar.CustomSectionTextArray() {
            @NonNull
            @Override
            public SparseArray<String> onCustomize(int sectionCount, @NonNull SparseArray<String> array) {
                array.clear();

                //Update interval options
                array.put(0, "5min");
                array.put(1, "30min");
                array.put(2, "1h");
                array.put(3, "3h");
                array.put(4, "6h");
                array.put(5, "12h");
                array.put(6, "1 dia");

                return array;
            }
        });

        //Set visibility events for specific switches
        changeLayoutVisibility(R.id.swLowBattery, -1, R.id.cbLowBattery);
        changeLayoutVisibility(R.id.swMoveoutAlert, R.id.vwMoveoutAlert, R.id.cbMoveout);
        changeLayoutVisibility(R.id.swSpeedAlert, R.id.vwSpeedLimit, R.id.cbSpeedLimit);
        changeLayoutVisibility(R.id.swStatusCheck, -1, R.id.cbStatusCheck);
        changeLayoutVisibility(R.id.swUpdateInterval, R.id.seekBar, -1);
        changeLayoutVisibility(R.id.swNotifications, R.id.vwNotificationOptions, -1);
    }

    //Change view visibility according to switch compat option (checked = visible)
    private void changeLayoutVisibility(int switchCompatID, final int viewID, final int checkboxID)
    {
        //Find switch compat
        ((SwitchCompat) findViewById(switchCompatID)).setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

            //Define click event
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

                //Find checkbox
                View view = findViewById(viewID);

                //Check switch status
                if(view != null)
                {
                    //Show view
                    view.setVisibility((isChecked ? View.VISIBLE : View.GONE));
                }

                //Find checkbox
                CheckBox checkBox = findViewById(checkboxID);

                //If checkbox ID is valid
                if(checkBox != null)
                {
                    //Enable or disable according to option selected
                    checkBox.setEnabled(isChecked);
                    checkBox.setChecked(isChecked);
                }
            }
        });
    }


    //Get update interval in minutes for each section
    public int getUpdateIntervalBySection(int section)
    {
        //For each section option
        switch (section) {
            case 0:
                //Section 0 = 5 minutes
                return 5;
            case 1:
                //Section 1 = 30 minutes
                return 30;
            case 2:
                //Section 2 = 1 hour
                return 60;
            case 3:
                //Section 3 = 3 hours
                return 60 * 3;
            case 4:
                //Section 4 = 6 hours
                return 60 * 6;
            case 5:
                //Section 5 = 12 hours
                return 60 * 12;
            case 6:
                //Section 5 = 1 day
                return 60 * 24;
            default:
                //Default 60 minutes
                return 60;
        }
    }

    //Get section corresponding to update interval in minutes
    public int getSectionByUpdateInterval(int updateInterval)
    {
        //For each section option
        switch (updateInterval) {
            case 5:
                //Section 0 = 5 minutes
                return 0;
            case 30:
                //Section 1 = 30 minutes
                return 1;
            case 60:
                //Section 2 = 1 hour
                return 2;
            case 60 * 3:
                //Section 3 = 3 hours
                return 3;
            case 60 * 6:
                //Section 4 = 6 hours
                return 4;
            case 60 * 12:
                //Section 5 = 12 hours
                return 5;
            case 60 * 24:
                //Section 6 = 1 day
                return 6;
            default:
                //Default 60 minutes
                return 2;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.settings, menu);

        //Save action menu item
        confirmMenu = menu.findItem(R.id.action_add);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home:

                //Set result
                setResult(RESULT_CANCELED, getIntent());

                //End activity (returns to parent activity -> OnActivityResult)
                finish();

                //End method
                return true;

            case R.id.action_add:
                onSettingsConfirmed();
                return true;

        }
        return super.onOptionsItemSelected(item);
    }

    //Called by floating action button and menu item
    private void onSettingsConfirmed()
    {
        // Get shared preferences editor
        final SharedPreferences.Editor editor = sharedPreferences.edit();

        // Initialize a new DB transaction
        WriteBatch transaction = firestoreDB.batch();

        // Set loading indicator on menu
        confirmMenu.setActionView(new ProgressBar(this));

        //Check activity intent
        if (getIntent().getBooleanExtra("InsertTracker", true))
        {
            //If inserting a new tracker, add operation on DB transaction
            transaction.set(firestoreDB.collection("Tracker").document(tracker.getIdentification()), tracker);
        }

        //Get notification options switch
        boolean showNotifications = ((SwitchCompat) findViewById(R.id.swNotifications)).isChecked();

        //Check model value
        switch (tracker.getModel())
        {
            case "tk102b":

                //Update user notification preferences
                updateNotificationOption(R.id.cbCoordinates, "Coordinates", showNotifications, editor);
                updateNotificationOption(R.id.cbLowBattery, "LowBattery", showNotifications, editor);
                updateNotificationOption(R.id.cbStatusCheck, "StatusCheck", showNotifications, editor);
                updateNotificationOption(R.id.cbMoveout, "MoveOut", showNotifications, editor);
                updateNotificationOption(R.id.cbSpeedLimit, "SpeedLimit", showNotifications, editor);
                updateNotificationOption(R.id.cbAvailable, "Available", showNotifications, editor);

                break;
            case "st940":

                //Update user notification preferences
                updateNotificationOption(R.id.cbCoordinates, "Coordinates", showNotifications, editor);
                updateNotificationOption(R.id.cbEmergency, "Emergency", showNotifications, editor);
                updateNotificationOption(R.id.cbMagnet, "Magnet", showNotifications, editor);
                updateNotificationOption(R.id.cbAvailable, "Available", showNotifications, editor);

                break;
            case "spot":

                //Update user notification preferences
                updateNotificationOption(R.id.cbCoordinates, "Coordinates", showNotifications, editor);
                updateNotificationOption(R.id.cbLowBattery, "LowBattery", showNotifications, editor);
                updateNotificationOption(R.id.cbStatusCheck, "TurnOff", showNotifications, editor);
                updateNotificationOption(R.id.cbFunctioning, "Functioning", showNotifications, editor);

                break;
            default:

                break;
        }

        //Execute DB operation
        transaction
                .commit()
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {

                        //Commit changes on shared preferences
                        editor.apply();

                        //Set activity result
                        setResult(RESULT_SUCCESS);

                        //Finish activity
                        finish();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {

                        //Set result editing result - OK
                        setResult(RESULT_ERROR);

                        //End activity (returns to parent activity -> OnActivityResult)
                        finish();
                    }
                });
    }

    private void updateNotificationOption(int checkBoxID, String topic, boolean showNotifications, SharedPreferences.Editor editor)
    {
        //Retrieve checkbox view
        CheckBox checkBox = findViewById(checkBoxID);

        //If user wants to receive this notification
        if(checkBox.isChecked() && showNotifications)
        {
            //Subscribe to notification topic
            notifications.subscribeToTopic(tracker.getIdentification() + "_" + topic);

            //Save option on shared preferences
            editor.putBoolean(tracker.getIdentification() + "_" + topic, true);
        }
        else
        {
            //Unsubscribe to notification topic
            notifications.unsubscribeFromTopic(tracker.getIdentification() + "_" + topic);

            //Remove option from shared preferences
            editor.putBoolean(tracker.getIdentification() + "_" + topic, false);
        }
    }
}
