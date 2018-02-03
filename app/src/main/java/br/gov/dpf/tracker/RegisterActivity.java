package br.gov.dpf.tracker;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatCheckBox;
import android.support.v7.widget.SwitchCompat;
import android.support.v7.widget.Toolbar;
import android.text.InputType;
import android.util.Log;
import android.util.SparseArray;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.xw.repo.BubbleSeekBar;

import br.gov.dpf.tracker.Entities.Model;
import br.gov.dpf.tracker.Entities.Tracker;

public class RegisterActivity extends AppCompatActivity
{
    //Default device model
    private String mModel = "TK 102B";

    //Default color option
    private String mColor = "#99ff0000";

    //Object representing the tracker to be inserted/updated
    private Tracker tracker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_register);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = findViewById(R.id.fab);

        //Set back button on toolbar
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onAddButtonClicked();
            }
        });

        //Try to load tracker from intent (if edit mode)
        tracker = getIntent().getParcelableExtra("Tracker");

        //Handle click events for color options
        loadColors((GridLayout) findViewById(R.id.vwColors));

        //Set support text
        ((TextView) findViewById(R.id.txtIntervalAlert)).setText(getResources().getText(R.string.txtIntervalSubtitle));

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

        //Check if activity was called to edit existing tracker
        if(tracker != null)
        {
            //Load tracker data
            loadData();

            //Hide add floating action button
            fab.setVisibility(View.GONE);
        }
    }

    //Called when editing an existing tracker
    public void loadData()
    {
        //Get shared preferences
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        //Load tracker data on text views
        ((EditText) findViewById(R.id.txtTrackerName)).setText(tracker.getName());
        ((EditText) findViewById(R.id.txtTrackerDescription)).setText(tracker.getDescription());
        ((EditText) findViewById(R.id.txtTrackerID)).setText(tracker.getIdentification());

        //Disable update of tracker unique ID
        findViewById(R.id.txtTrackerID).setEnabled(false);

        //Set update interval options
        ((BubbleSeekBar) findViewById(R.id.seekBar)).setProgress(getSectionByUpdateInterval(tracker.getUpdateInterval()));

        //Set notification options using shared preferences
        ((SwitchCompat)findViewById(R.id.swLowBattery)).setChecked(sharedPreferences.getBoolean(tracker.getIdentification() + "_NotifyLowBattery", false));
        ((SwitchCompat)findViewById(R.id.swMovement)).setChecked(sharedPreferences.getBoolean(tracker.getIdentification() + "_NotifyMovement", false));
        ((SwitchCompat)findViewById(R.id.swStopped)).setChecked(sharedPreferences.getBoolean(tracker.getIdentification() + "_NotifyStopped", false));
        ((SwitchCompat)findViewById(R.id.swTrackerStatus)).setChecked(sharedPreferences.getBoolean(tracker.getIdentification() + "_NotifyStatus", false));
        ((SwitchCompat)findViewById(R.id.swAvailable)).setChecked(sharedPreferences.getBoolean(tracker.getIdentification() + "_NotifyAvailable", false));
        ((SwitchCompat)findViewById(R.id.swSMSResponse)).setChecked(sharedPreferences.getBoolean(tracker.getIdentification() + "_NotifySMSResponse", false));

        //Check support action bar
        if(getSupportActionBar() != null)
        {
            //Change activity title
            getSupportActionBar().setTitle(tracker.getName());
        }

        //Get main scroll view
        final ScrollView vwScrollMain = findViewById(R.id.vwMainScroll);

        //Perform action after layout ends
        vwScrollMain.post(new Runnable() {
            @Override
            public void run() {

                //Scroll to show update interval
                vwScrollMain.smoothScrollTo(0, findViewById(R.id.vwUpdateInterval).getTop());

                //Request focus on seek bar
                findViewById(R.id.seekBar).requestFocus();
            }
        });
    }

    //Called when editing or inserting a new tracker
    public void onAddButtonClicked()
    {

        //Get tracker name and identification
        String trackerName = ((EditText) findViewById(R.id.txtTrackerName)).getText().toString();
        String trackerIdentification = ((EditText) findViewById(R.id.txtTrackerID)).getText().toString();

        //Check user input
        if(trackerName.isEmpty() || trackerIdentification.isEmpty())
        {
            //Alert user, not optional fields
            Snackbar.make(findViewById(R.id.fab), "Preencha os campos nome e identificação do aparelho", Snackbar.LENGTH_LONG).show();
        }
        else
        {
            //Create intent to send to MainActivity
            Intent intent = new Intent();

            //Tracker object containing data to be inserted or updated
            Tracker tracker;

            //Check if activity is in edit mode
            if(getIntent().hasExtra("Tracker"))
            {
                //Inform activity intention: update existing tracker
                intent.putExtra("UpdateTracker", true);

                //Get tracker data from intent
                tracker = getIntent().getParcelableExtra("Tracker");
            }
            else
            {
                //Inform activity intention: insert new tracker
                intent.putExtra("InsertTracker", true);

                //Create new tracker
                tracker = new Tracker();
            }

            //Inform tracker position on main activity
            intent.putExtra("TrackerPosition", getIntent().getIntExtra("TrackerPosition",0));

            //Save tracker data on intent
            tracker.setName(trackerName);
            tracker.setDescription(((EditText) findViewById(R.id.txtTrackerDescription)).getText().toString());
            tracker.setIdentification(trackerIdentification);
            tracker.setModel(mModel);

            //Get update interval
            tracker.setUpdateInterval(getUpdateIntervalBySection(((BubbleSeekBar) findViewById(R.id.seekBar)).getProgress()));

            //Get tracker color
            tracker.setBackgroundColor(mColor);

            //Save notifications preferences
            intent.putExtra("NotifyLowBattery", ((SwitchCompat)findViewById(R.id.swLowBattery)).isChecked());
            intent.putExtra("NotifyMovement", ((SwitchCompat)findViewById(R.id.swMovement)).isChecked());
            intent.putExtra("NotifyStopped", ((SwitchCompat)findViewById(R.id.swStopped)).isChecked());
            intent.putExtra("NotifyStatus", ((SwitchCompat)findViewById(R.id.swTrackerStatus)).isChecked());
            intent.putExtra("NotifyAvailable", ((SwitchCompat)findViewById(R.id.swAvailable)).isChecked());
            intent.putExtra("NotifySMSResponse", ((SwitchCompat)findViewById(R.id.swSMSResponse)).isChecked());

            //Put tracker data on intent
            intent.putExtra("Tracker", tracker);

            //Set result
            setResult(RESULT_OK, intent);

            //End activity (returns to parent activity -> OnActivityResult)
            finish();
        }
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

    public void changeLabels(String model)
    {
        //Get text fields from layout
        TextView lblTrackerID = findViewById(R.id.lblTrackerID);
        TextView lblTrackerSubtitle = findViewById(R.id.lblTrackerIDSubtitle);
        EditText txtTrackerID = findViewById(R.id.txtTrackerID);

        //Get resources manager
        Resources resources = getResources();

        //Check model value
        if(model.equals("SPOT"))
        {
            //Set text values
            lblTrackerID.setText(resources.getString(R.string.lblFeedID));
            lblTrackerSubtitle.setText(resources.getString(R.string.lblFeedIDSubtitle));
            txtTrackerID.setHint(resources.getString(R.string.txtFieldIDHint));

            //Change input type to allow text
            txtTrackerID.setInputType(InputType.TYPE_CLASS_TEXT);
        }
        else
        {
            //Set text values
            lblTrackerID.setText(resources.getString(R.string.lblPhoneNumber));
            lblTrackerSubtitle.setText(resources.getString(R.string.lblPhoneNumberSubtitle));
            txtTrackerID.setHint(resources.getString(R.string.txtPhoneNumberHint));

            //Change input type to allow text
            txtTrackerID.setInputType(InputType.TYPE_CLASS_PHONE);
        }
    }

    public void onDeleteButtonClicked()
    {
        //Create confirmation dialog
        new AlertDialog.Builder(this)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle("Deletar rastreador")
                .setMessage("Confirma a exclusão do rastreador " + tracker.getName() + "?")
                .setPositiveButton("Sim", new DialogInterface.OnClickListener()
                {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        //Create intent to return on main activity
                        Intent intent = new Intent();

                        //Add tracker on intent
                        intent.putExtra("Tracker", tracker);

                        //Inform activity intention: delete existing tracker
                        intent.putExtra("DeleteTracker", true);

                        //Set result
                        setResult(RESULT_OK, intent);

                        //End activity (returns to parent activity -> OnActivityResult)
                        finish();
                    }

                })
                .setNegativeButton("Não", null)
                .show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.register, menu);

        //Get delete menu action
        MenuItem delete = menu.findItem(R.id.action_delete);

        //Set delete option visible if activity is in activity mode
        delete.setVisible(getIntent().hasExtra("Tracker"));

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
                onAddButtonClicked();
                return true;

            case R.id.action_delete:
                onDeleteButtonClicked();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void loadColors(final GridLayout vwColors){

        //For each device model
        for (int i = 0; i < vwColors.getChildCount(); i++)
        {
            //Get checkbox representing a color
            AppCompatCheckBox vwColor = (AppCompatCheckBox) vwColors.getChildAt(i);

            //Set on click event listener for this checkbox
            vwColor.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View view) {

                //When clicked, for each device model
                for (int i = 0; i < vwColors.getChildCount(); i++)
                {
                    //Set checkbox state to unchecked
                    ((AppCompatCheckBox) vwColors.getChildAt(i)).setChecked(false);
                }

                //Get selected checkbox
                AppCompatCheckBox checkbox = ((AppCompatCheckBox) view);

                //Set checked status
                checkbox.setChecked(true);

                //Get current selected color
                mColor = checkbox.getTag().toString();
                }
            });

            //If activity is in edit mode and this is the corresponding tracker color
            if(tracker != null && vwColor.getTag().equals(tracker.getBackgroundColor()))
            {
                //Set checked status
                vwColor.setChecked(true);

                //Get current selected color
                mColor = tracker.getBackgroundColor();
            }
        }

        //If activity is not in edit mode
        if(tracker == null)
        {
            //Select first item as default
            ((AppCompatCheckBox) vwColors.getChildAt(0)).setChecked(true);
        }
    }
}
