package br.gov.dpf.tracker;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.SwitchCompat;
import android.support.v7.widget.Toolbar;
import android.text.method.LinkMovementMethod;
import android.util.SparseArray;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.WriteBatch;
import com.google.firebase.messaging.FirebaseMessaging;
import com.xw.repo.BubbleSeekBar;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import br.gov.dpf.tracker.Entities.Configuration;
import br.gov.dpf.tracker.Entities.Tracker;

public class TrackerSettingsActivity extends AppCompatActivity
{
    //Object representing the tracker to be inserted/updated
    private Tracker tracker;

    //Flag indicating whether this activity is in edit mode
    private boolean editMode;

    //Flag indicating if a DB operation is running
    private boolean loading;

    //Menu item used to confirm and refresh settings
    private MenuItem confirmMenu, refreshMenu;

    //Shared preferences used on notification options
    private SharedPreferences sharedPreferences;

    //Firebase Cloud Messaging
    private FirebaseMessaging notifications;

    //Firebase Firestore DB
    private FirebaseFirestore firestoreDB;

    //Create an array list to save existing configurations
    private Map<String, Configuration> configurations;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Get shared preferences
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        //Get FCM instance
        notifications = FirebaseMessaging.getInstance();

        //Get DB instance
        firestoreDB = FirebaseFirestore.getInstance();

        //Check if activity intent has the required data
        if(getIntent().hasExtra("Tracker"))
        {
            //Load tracker from intent
            tracker = getIntent().getParcelableExtra("Tracker");

            //Check if activity is in edit mode
            editMode = (getIntent().getIntExtra("Request", MainActivity.REQUEST_INSERT) == MainActivity.REQUEST_UPDATE);

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
                    //Unsupported model
                    setResult(MainActivity.RESULT_ERROR);

                    //Finish with error code
                    finish();
                    break;
            }

            //If activity is in edit mode
            if(editMode)
            {
                //Load tracker data
                loadData();
            }
        }
        else
        {
            //This activity can't start without tracker data
            setResult(MainActivity.RESULT_ERROR);

            //Finish with error code
            finish();
        }
    }

    private void loadData()
    {
        // Flag indicating DB operation in progress
        loading = true;

        // Initialize configuration mapped array
        configurations = new HashMap<>();

        // Disable switch controls
        enableControls((ViewGroup) findViewById(R.id.vwConfigurations), false);

        // If editing an existing tracker
        firestoreDB
                .collection("Tracker/" + tracker.getIdentification() + "/Configurations")
                .get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot documentSnapshots)
                    {
                        // Enable switch controls
                        enableControls((ViewGroup) findViewById(R.id.vwConfigurations), true);

                        //For each configuration available
                        for(DocumentSnapshot document : documentSnapshots)
                        {
                            //Transform document snapshot to configuration object
                            Configuration config = document.toObject(Configuration.class);

                            // Save current configurations
                            configurations.put(config.getName(), config);

                            // Update layout components to reflect configuration options
                            loadConfiguration(config);
                        }

                        // if loading indicator is available
                        if(confirmMenu != null)
                        {
                            //Cancel loading indicator
                            confirmMenu.setActionView(null);

                            //Enable refresh menu
                            refreshMenu.setVisible(true);
                        }

                        // Set loading flag to false
                        loading = false;

                        // If activity intent is to edit notifications
                        if(getIntent().getBooleanExtra("UpdateNotifications", false))
                        {
                            //Get notification view
                            final View notificationPanel = findViewById(R.id.vwNotificationsCardView);

                            //Wait until loading finishes
                            notificationPanel.postDelayed(new Runnable() {
                                @Override
                                public void run()
                                {
                                    //Scroll to notification panel
                                    ((ScrollView) findViewById(R.id.vwMainScroll)).smoothScrollTo(0, notificationPanel.getBottom());
                                }
                            }, 500);
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {

                        //Set result editing result - OK
                        setResult(MainActivity.RESULT_ERROR);

                        //End activity (returns to parent activity -> OnActivityResult)
                        finish();
                    }
                });

        //Get layout containing all notification preferences
        ViewGroup notificationOptions = findViewById(R.id.vwNotificationOptions);

        //For each children
        for(int i = 0; i < notificationOptions.getChildCount(); i++)
        {
            //Get child
            View view = notificationOptions.getChildAt(i);

            //Check if view is a checkbox
            if(view instanceof CheckBox)
            {
                //Get user defined option from shared preferences
                boolean notificationActive = sharedPreferences.getBoolean(tracker.getIdentification() + "_" + view.getTag().toString(), false);

                //Set option on checkbox
                ((CheckBox) view).setChecked(notificationActive);
            }
        }

        //Check user preference to hide notifications
        ((SwitchCompat) findViewById(R.id.swNotifications)).setChecked(sharedPreferences.getBoolean(tracker.getIdentification() + "_Notifications", false));
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

        //If in edit mode
        if(editMode)
        {
            //Hide floating action button
            fab.setVisibility(View.GONE);
        }
        else
        {
            //Set click event
            fab.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    //If still loading data
                    if(loading)
                    {
                        //Show alert to user
                        Snackbar.make(findViewById(android.R.id.content), "Carregando, por favor aguarde...", Snackbar.LENGTH_LONG);
                    }
                    else
                    {
                        //Call method to save data
                        onSettingsConfirmed();
                    }
                }
            });
        }

        if(getSupportActionBar() != null)
        {
            //Change activity title
            getSupportActionBar().setTitle((editMode ? tracker.getName() : trackerTitle));
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
        changeLayoutVisibility(R.id.swNotifications, R.id.vwNotificationOptions, -1, -1);
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
                array.put(1, "6 horas");
                array.put(2, "12 horas");
                array.put(3, "18 horas");
                array.put(4, "1 dia");

                return array;
            }
        });

        //Set visibility events for specific switches
        changeLayoutVisibility(R.id.swDeepsleep, -1, -1, R.id.imgDeepSleep);
        changeLayoutVisibility(R.id.swEmergency, R.id.vwEmergency, R.id.cbEmergency, R.id.imgEmergency);
        changeLayoutVisibility(R.id.swMagnetAlert, -1, R.id.cbMagnet, R.id.imgMagnet);
        changeLayoutVisibility(R.id.swTurnOff, -1, -1, R.id.imgTurnOff);
        changeLayoutVisibility(R.id.swPeriodicUpdate, R.id.vwUpdateInterval, -1, -1);
        changeLayoutVisibility(R.id.swNotifications, R.id.vwNotificationOptions, -1, -1);
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
        ((BubbleSeekBar) findViewById(R.id.sbPeriodicUpdate)).setCustomSectionTextArray(new BubbleSeekBar.CustomSectionTextArray() {
            @NonNull
            @Override
            public SparseArray<String> onCustomize(int sectionCount, @NonNull SparseArray<String> array) {
                array.clear();

                //Update interval options
                array.put(0, "5m");
                array.put(1, "30m");
                array.put(2, "1h");
                array.put(3, "3h");
                array.put(4, "6h");
                array.put(5, "12h");
                array.put(6, "1 dia");

                return array;
            }
        });

        //Set visibility events for specific switches
        changeLayoutVisibility(R.id.swShockAlert, -1, R.id.cbShockAlert, R.id.imgShockAlert);
        changeLayoutVisibility(R.id.swMoveoutAlert, -1, R.id.cbMoveout, R.id.imgMoveOut);
        changeLayoutVisibility(R.id.swSpeedAlert, R.id.vwSpeedLimit, R.id.cbSpeedLimit, R.id.imgSpeedLimit);
        changeLayoutVisibility(R.id.swStatusCheck, -1, R.id.cbStatusCheck, R.id.imgStatusCheck);
        changeLayoutVisibility(R.id.swPeriodicUpdate, R.id.sbPeriodicUpdate, -1, R.id.imgPeriodicUpdate);
        changeLayoutVisibility(R.id.swNotifications, R.id.vwNotificationOptions, -1, -1);
    }

    //Change view visibility according to switch compat option (checked = visible)
    private void changeLayoutVisibility(int switchCompatID, final int viewID, final int checkBoxID, final int imageViewID)
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
                CheckBox checkBox = findViewById(checkBoxID);

                //If checkbox ID is valid
                if(checkBox != null)
                {
                    //Enable or disable according to option selected
                    checkBox.setEnabled(isChecked);
                    checkBox.setChecked(sharedPreferences.getBoolean(tracker.getIdentification() + "_" + checkBox.getTag().toString(), isChecked));
                }

                //Find checkbox
                ImageView imageView = findViewById(imageViewID);

                //If checkbox ID is valid
                if(imageView != null && !isChecked)
                {
                    //Enable or disable according to option selected
                    imageView.setVisibility(View.GONE);
                }
            }
        });
    }

    //Get update interval in minutes for each section
    public String getUpdateIntervalBySection(int section)
    {
        //For each section option
        switch (section) {
            case 0:
                //Section 0 = 5 minutes
                return "005m";
            case 1:
                //Section 1 = 30 minutes
                return "030m";
            case 2:
                //Section 2 = 1 hour
                return "060m";
            case 3:
                //Section 3 = 3 hours
                return "003h";
            case 4:
                //Section 4 = 6 hours
                return "006h";
            case 5:
                //Section 5 = 12 hours
                return "012h";
            case 6:
                //Section 5 = 1 day
                return "024h";
            default:
                //Default 60 minutes
                return "060m";
        }
    }

    //Get section corresponding to update interval in minutes
    public int getSectionByUpdateInterval(String updateInterval)
    {
        //For each section option
        switch (updateInterval) {
            case "005m":
                //Section 0 = 5 minutes
                return 0;
            case "030m":
                //Section 1 = 30 minutes
                return 1;
            case "060m":
                //Section 2 = 1 hour
                return 2;
            case "003h":
                //Section 3 = 3 hours
                return 3;
            case "006h":
                //Section 4 = 6 hours
                return 4;
            case "012h":
                //Section 5 = 12 hours
                return 5;
            case "024h":
                //Section 6 = 1 day
                return 6;
            default:
                //Default 60 minutes
                return 2;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.settings, menu);

        //Save action menu item
        confirmMenu = menu.findItem(R.id.action_add);

        //Show loading indicator
        refreshMenu = menu.findItem(R.id.action_refresh);

        //If is loading data
        if(loading)
        {
            //Show loading indicator
            confirmMenu.setActionView(new ProgressBar(this));
        }

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home:

                //Set result
                setResult(MainActivity.RESULT_CANCELED, getIntent());

                //End activity (returns to parent activity -> OnActivityResult)
                finish();

                //End method
                return true;

            case R.id.action_add:

                //If still loading data
                if(loading)
                {
                    //Show alert to user
                    Snackbar.make(findViewById(android.R.id.content), "Carregando, por favor aguarde...", Snackbar.LENGTH_LONG);
                }
                else
                {
                    //Call method to save data
                    onSettingsConfirmed();
                }

                //End method
                return true;

            case R.id.action_refresh:

                //Show loading indicator
                confirmMenu.setActionView(new ProgressBar(this));

                //Hide loading icon
                item.setVisible(false);

                //Call method to load data again
                loadData();

                //End method
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed()
    {
        //Set result
        setResult(MainActivity.RESULT_CANCELED);

        //End activity (returns to parent activity -> OnActivityResult)
        finish();
    }

    //Called by floating action button and menu item
    private void onSettingsConfirmed()
    {
        // Get shared preferences editor
        final SharedPreferences.Editor editor = sharedPreferences.edit();

        // Set loading indicator on menu
        confirmMenu.setActionView(new ProgressBar(this));

        // Initialize a new DB transaction
        WriteBatch transaction = firestoreDB.batch();

        //Get configuration collection
        CollectionReference configCollection = firestoreDB.collection("Tracker/" + tracker.getIdentification() + "/Configurations");

        //If activity is inserting a new tracker
        if (!editMode)
        {
            //Initialize configuration array
            configurations = new HashMap<>();

            //Insert tracker on DB
            transaction.set(firestoreDB.collection("Tracker").document(tracker.getIdentification()), tracker);
        }
        else if (getIntent().getBooleanExtra("UpdateModel", false)) //If changing tracker model
        {
            //For each previous model configuration
            for(String configName : configurations.keySet())

                //Add to transaction delete old configuration
                transaction.delete(configCollection.document(configName));
        }

        //Get notification options switch
        boolean showNotifications = ((SwitchCompat) findViewById(R.id.swNotifications)).isChecked();

        //Save notification options
        editor.putBoolean(tracker.getIdentification() + "_Notifications", showNotifications);

        //Check model value
        switch (tracker.getModel())
        {
            case "tk102b":

                //Get tracker configuration options
                SwitchCompat swStatusCheck = findViewById(R.id.swStatusCheck);
                SwitchCompat swShockAlert = findViewById(R.id.swShockAlert);
                SwitchCompat swMoveOutAlert = findViewById(R.id.swMoveoutAlert);
                SwitchCompat swSpeedAlert = findViewById((R.id.swSpeedAlert));
                EditText txtSpeedAlert = findViewById(R.id.txtSpeedLimit);
                SwitchCompat swPeriodicUpdate = findViewById(R.id.swPeriodicUpdate);
                BubbleSeekBar sbPeriodicUpdate = findViewById(R.id.sbPeriodicUpdate);

                //Update device configuration
                updateConfiguration("StatusCheck", swStatusCheck.isChecked(), null, configCollection, transaction);
                updateConfiguration("Shock", swShockAlert.isChecked(), null, configCollection, transaction);
                updateConfiguration("MoveOut", swMoveOutAlert.isChecked(), null, configCollection, transaction);
                updateConfiguration("OverSpeed", swSpeedAlert.isChecked(), txtSpeedAlert.getText().toString(), configCollection, transaction);
                updateConfiguration("PeriodicUpdate", swPeriodicUpdate.isChecked(), getUpdateIntervalBySection(sbPeriodicUpdate.getProgress()), configCollection, transaction);

                //Update user notification preferences
                updateNotificationOption(R.id.cbMovement, showNotifications, editor);
                updateNotificationOption(R.id.cbStopped, showNotifications, editor);
                updateNotificationOption(R.id.cbLowBattery, showNotifications, editor);
                updateNotificationOption(R.id.cbStatusCheck,  showNotifications, editor);
                updateNotificationOption(R.id.cbMoveout, showNotifications, editor);
                updateNotificationOption(R.id.cbShockAlert, showNotifications, editor);
                updateNotificationOption(R.id.cbSpeedLimit, showNotifications, editor);
                updateNotificationOption(R.id.cbAvailable, showNotifications, editor);

                break;
            case "st940":

                //Get tracker configuration options
                SwitchCompat swDeepSleep = findViewById(R.id.swDeepsleep);
                SwitchCompat swEmergency = findViewById(R.id.swEmergency);
                EditText txtEmergency = findViewById(R.id.txtEmergency);
                SwitchCompat swTurnOff = findViewById(R.id.swTurnOff);
                SwitchCompat swMagnetAlert = findViewById(R.id.swMagnetAlert);
                SwitchCompat swInterval = findViewById(R.id.swPeriodicUpdate);
                BubbleSeekBar sbActive = findViewById(R.id.seekBarActive);
                BubbleSeekBar sbIdle = findViewById(R.id.seekBarIdle);

                //Update device configuration
                updateConfiguration("DeepSleep", swDeepSleep.isChecked(), null, configCollection, transaction);
                updateConfiguration("Emergency", swEmergency.isChecked(), txtEmergency.getText().toString(), configCollection, transaction);
                updateConfiguration("TurnOff", swTurnOff.isChecked(), null, configCollection, transaction);
                updateConfiguration("Magnet", swMagnetAlert.isChecked(), null, configCollection, transaction);
                updateConfiguration("UpdateIdle", swInterval.isChecked(), String.valueOf(Math.max(sbIdle.getProgress(), 1)), configCollection, transaction);
                updateConfiguration("UpdateActive", swInterval.isChecked(), String.valueOf(Math.max(sbActive.getProgress(), 1)), configCollection, transaction);

                //Update user notification preferences
                updateNotificationOption(R.id.cbMovement, showNotifications, editor);
                updateNotificationOption(R.id.cbStopped, showNotifications, editor);
                updateNotificationOption(R.id.cbEmergency, showNotifications, editor);
                updateNotificationOption(R.id.cbMagnet, showNotifications, editor);
                updateNotificationOption(R.id.cbAvailable, showNotifications, editor);

                break;
            case "spot":

                //Update only user notification preferences (SPOT MODEL do not support remote configurations)
                updateNotificationOption(R.id.cbMovement, showNotifications, editor);
                updateNotificationOption(R.id.cbStopped, showNotifications, editor);
                updateNotificationOption(R.id.cbLowBattery, showNotifications, editor);
                updateNotificationOption(R.id.cbTurnOff, showNotifications, editor);
                updateNotificationOption(R.id.cbFunctioning, showNotifications, editor);

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
                        setResult(MainActivity.RESULT_SUCCESS);

                        //Finish activity
                        finish();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {

                        //Set result editing result - OK
                        setResult(MainActivity.RESULT_ERROR);

                        //End activity (returns to parent activity -> OnActivityResult)
                        finish();
                    }
                });
    }

    private void loadConfiguration(final Configuration config)
    {
        //Get all views with tag corresponding to config
        ArrayList<View> views = getViewsByTag(findViewById(android.R.id.content), "Config_" + config.getName());

        //For each view related to this config
        for(View view : views)
        {
            //Check view type
            if(view instanceof SwitchCompat)
            {
                //Set switch value to represent configuration option
                ((SwitchCompat)view).setChecked(config.isEnabled());
            }
            else if(view instanceof EditText)
            {
                //If edit, set value as text
                ((EditText) view).setText(config.getValue());
            }
            else if(view instanceof BubbleSeekBar)
            {
                //Configuration for this specific view
                if(view.getId() == R.id.sbPeriodicUpdate)
                {
                    //Get progress value to section
                    ((BubbleSeekBar) view).setProgress(getSectionByUpdateInterval(config.getValue()));
                }
                else
                {
                    //If seek bar, set progress
                    ((BubbleSeekBar) view).setProgress(Integer.valueOf(config.getValue()));
                }
            }
            else if(view instanceof TextView)
            {
                //Define image listener
                view.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view)
                    {
                        //Show status message to user
                        Snackbar.make(findViewById(android.R.id.content), config.getStatus().get("description").toString(), Snackbar.LENGTH_LONG).show();
                    }
                });
            }
            else if(view instanceof ImageView)
            {
                //Get config status
                if(config.isEnabled())
                {
                    //Show current configuration status
                    if((boolean) config.getStatus().get("completed"))
                    {
                        //Load success icon
                        ((ImageView) view).setImageResource(R.drawable.status_ok);
                    }
                    else if (config.getStatus().get("step").equals("ERROR"))
                    {
                        //Load error icon
                        ((ImageView) view).setImageResource(R.drawable.status_error);
                    }
                    else
                    {
                        //Load info icon
                        ((ImageView) view).setImageResource(R.drawable.status_warning);
                    }

                    //Show image view
                    view.setVisibility(View.VISIBLE);

                    //Define image listener
                    view.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view)
                        {
                            //Show status message to user
                            Snackbar.make(findViewById(android.R.id.content), config.getStatus().get("description").toString(), Snackbar.LENGTH_LONG).show();
                        }
                    });
                }
            }
        }
    }

    private void updateConfiguration(String configName, boolean enabled, String value, CollectionReference collection, WriteBatch transaction)
    {
        //Try to get config previously loaded
        Configuration config = configurations.get(configName);

        //Check if configuration
        if(config == null)
        {
            //Insert new configuration
            transaction.set(collection.document(configName), new Configuration(configName, value, enabled));
        }
        else if(config.isEnabled() != enabled || !(config.getValue() == null ? value == null : config.getValue().equals(value)))
        {
            //User changed configuration, update data on DB
            config.setValue(value);
            config.setEnabled(enabled);

            //Initialize new status
            Map<String, Object> status = new HashMap<>();

            //Save new status settings
            status.put("step", "REQUESTED");
            status.put("description", "Status: Aguardando envio...");
            status.put("datetime", new Date());
            status.put("completed", false);

            //Update status on configuration
            config.setStatus(status);

            //Update configuration
            transaction.set(collection.document(configName), config);

            //Update tracker to request a new update from server
            transaction.update(firestoreDB.document("Tracker/" + tracker.getIdentification()), "lastUpdate", null);
        }
    }


    private void updateNotificationOption(int checkBoxID, boolean showNotifications, SharedPreferences.Editor editor)
    {
        //Retrieve checkbox view
        CheckBox checkBox = findViewById(checkBoxID);

        //If user wants to receive this notification
        if(checkBox.isChecked() && showNotifications)
        {
            //Subscribe to notification topic
            notifications.subscribeToTopic(tracker.getIdentification() + "_" + checkBox.getTag().toString());

            //Save option on shared preferences
            editor.putBoolean(tracker.getIdentification() + "_" + checkBox.getTag().toString(), true);
        }
        else
        {
            //Unsubscribe to notification topic
            notifications.unsubscribeFromTopic(tracker.getIdentification() + "_" + checkBox.getTag().toString());

            //Remove option from shared preferences
            editor.putBoolean(tracker.getIdentification() + "_" + checkBox.getTag().toString(), false);
        }
    }

    //Get all views with the same tag
    private ArrayList<View> getViewsByTag(View root, String tag)
    {
        //Create array list
        ArrayList<View> views = new ArrayList<>();

        //For each children
        final int childCount = ((ViewGroup) root).getChildCount();
        for (int i = 0; i < childCount; i++)
        {
            final View child = ((ViewGroup) root).getChildAt(i);

            //If it is another group, perform recursive call
            if (child instanceof ViewGroup) {
                views.addAll(getViewsByTag(child, tag));
            }

            //Else check if tag matches
            final Object tagObj = child.getTag();
            if (tagObj != null && tagObj.toString().startsWith(tag)) {
                views.add(child);
            }

        }
        return views;
    }

    //Set all switchs inside a layout enabled or disabled
    private void enableControls(ViewGroup root, boolean enabled)
    {
        //For each children
        for (int i = 0; i < root.getChildCount(); i++)
        {
            //If it is a switch control
            if (root.getChildAt(i) instanceof SwitchCompat)
            {
                //Change enabled status
                root.getChildAt(i).setEnabled(enabled);
                ((SwitchCompat) root.getChildAt(i)).setChecked(enabled);
            }
        }
    }
}
