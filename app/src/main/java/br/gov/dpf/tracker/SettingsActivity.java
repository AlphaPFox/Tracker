package br.gov.dpf.tracker;

import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatCheckBox;
import android.support.v7.widget.Toolbar;
import android.text.InputType;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import br.gov.dpf.tracker.Entities.Tracker;

public class SettingsActivity extends AppCompatActivity {

    //Default device model
    private String mModel = "tk102b";

    //Default color option
    private String mColor = "#99ff0000";

    //Object representing the tracker to be inserted/updated
    private Tracker tracker;

    //Define possible result operations
    public static int RESULT_CANCELED = 0;
    public static int RESULT_SUCCESS = 1;
    public static int RESULT_ERROR = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_settings);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = findViewById(R.id.fab);

        //Check action bar
        if(getSupportActionBar() != null)
        {
            //Set back button on toolbar
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        //Set floating action button event
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onSettingsConfirmed(null);
            }
        });

        //Try to load tracker from intent (if edit mode)
        tracker = getIntent().getParcelableExtra("Tracker");

        //Handle click events for color options
        loadColors((GridLayout) findViewById(R.id.vwColors));

        //Handle click events for model options
        loadModels((LinearLayout) findViewById(R.id.vwModels));

        //Check if activity was called to edit existing tracker
        if(getIntent().getBooleanExtra("UpdateTracker", false))
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
        //Load tracker data on text views
        ((EditText) findViewById(R.id.txtTrackerName)).setText(tracker.getName());
        ((EditText) findViewById(R.id.txtTrackerDescription)).setText(tracker.getDescription());
        ((EditText) findViewById(R.id.txtTrackerIdentification)).setText(tracker.getIdentification());

        //Disable update of tracker unique ID
        findViewById(R.id.txtTrackerIdentification).setEnabled(false);

        //Check support action bar
        if(getSupportActionBar() != null)
        {
            //Change activity title
            getSupportActionBar().setTitle(tracker.getName());
        }
    }

    //Called when editing or inserting a new tracker
    public void onSettingsConfirmed(final MenuItem item)
    {
        //Get tracker name and identification
        String trackerName = ((EditText) findViewById(R.id.txtTrackerName)).getText().toString();
        String trackerIdentification = ((EditText) findViewById(R.id.txtTrackerIdentification)).getText().toString();

        //Check user input
        if(trackerName.isEmpty() || trackerIdentification.isEmpty())
        {
            //Alert user, not optional fields
            Snackbar.make(findViewById(R.id.fab), "Preencha os campos nome e identificação do aparelho", Snackbar.LENGTH_LONG).show();
        }
        else
        {
            //Check if activity is in edit mode
            boolean editMode = getIntent().getBooleanExtra("UpdateTracker", false);

            //Create intent to send to MainActivity
            final Intent intent = new Intent();

            //Tracker object containing data to be inserted or updated
            Tracker tracker = new Tracker();

            //Save tracker data
            tracker.setName(trackerName);
            tracker.setDescription(((EditText) findViewById(R.id.txtTrackerDescription)).getText().toString());
            tracker.setIdentification(trackerIdentification);
            tracker.setModel(mModel);

            //Save tracker color
            tracker.setBackgroundColor(mColor);

            //Put tracker data on intent
            intent.putExtra("Tracker", tracker);

            //Check if activity is in edit mode
            if(editMode)
            {
                //Inform tracker position on main activity
                intent.putExtra("TrackerPosition", getIntent().getIntExtra("TrackerPosition",0));

                // Set loading indicator on menu
                item.setActionView(new ProgressBar(this));

                // If editing an existing tracker
                FirebaseFirestore.getInstance()
                        .collection("Tracker")
                        .document(tracker.getIdentification())
                        .set(tracker, SetOptions.merge())
                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {

                                //Set result editing result - OK
                                setResult(RESULT_SUCCESS, intent);

                                //End activity (returns to parent activity -> OnActivityResult)
                                finish();

                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {

                                //Set result editing result - OK
                                setResult(RESULT_ERROR, intent);

                                //End activity (returns to parent activity -> OnActivityResult)
                                finish();
                            }
                        });
            }
            else
            {
                //Inform activity intention: insert new tracker
                intent.putExtra("InsertTracker", true);

                //Define intent to open a new activity
                intent.setClass(this, TrackerActivity.class);

                //Start next activity
                startActivityForResult(intent, 0);
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.settings, menu);

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
                onSettingsConfirmed(item);
                return true;

        }
        return super.onOptionsItemSelected(item);
    }


    public void changeLabels(String model)
    {
        //Get text fields from layout
        TextView lblTrackerID = findViewById(R.id.lblTrackerIdentification);
        TextView lblTrackerSubtitle = findViewById(R.id.lblTrackerIdentificationSubtitle);
        EditText txtTrackerID = findViewById(R.id.txtTrackerIdentification);

        //Get resources manager
        Resources resources = getResources();

        //Check model value
        switch (model) {
            case "spot":
                //Set text values
                lblTrackerID.setText(resources.getString(R.string.lblFeedID));
                lblTrackerSubtitle.setText(resources.getString(R.string.lblFeedIDSubtitle));
                txtTrackerID.setHint(resources.getString(R.string.txtFieldIDHint));

                //Change input type to allow text
                txtTrackerID.setInputType(InputType.TYPE_CLASS_TEXT);
                break;
            case "st940":
                //Set text values
                lblTrackerID.setText(resources.getString(R.string.lblSuntechID));
                lblTrackerSubtitle.setText(resources.getString(R.string.lblSuntechSubtitle));
                txtTrackerID.setHint(resources.getString(R.string.txtSuntechHint));

                //Change input type to allow text
                txtTrackerID.setInputType(InputType.TYPE_CLASS_NUMBER);
                break;
            default:
                //Set text values
                lblTrackerID.setText(resources.getString(R.string.lblPhoneNumber));
                lblTrackerSubtitle.setText(resources.getString(R.string.lblPhoneNumberSubtitle));
                txtTrackerID.setHint(resources.getString(R.string.txtPhoneNumberHint));

                //Change input type to allow text
                txtTrackerID.setInputType(InputType.TYPE_CLASS_PHONE);

                break;
        }
    }

    public void loadModels(final LinearLayout vwModels)
    {
        //For each device model
        for (int i = 0; i < vwModels.getChildCount(); i++)
        {

            //Get checkbox representing a color
            LinearLayout vwModel = (LinearLayout) vwModels.getChildAt(i);

            vwModel.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View view) {

                    //When clicked, for each device model
                    for (int i = 0; i < vwModels.getChildCount(); i++)
                    {
                        //Set background transparent = not selected
                        vwModels.getChildAt(i).setBackgroundColor(getResources().getColor(R.color.Transparent));
                    }

                    //Set background color only for the selected device model
                    view.setBackgroundColor(getResources().getColor(R.color.colorSelected));

                    //Get current selected model
                    mModel = view.getTag().toString();

                    //Change layout labels according to selected model
                    changeLabels(mModel);
                }
            });

            //If activity is in edit mode and this is the corresponding tracker color
            if(tracker != null && vwModel.getTag().equals(tracker.getModel()))
            {
                //Set background color only for the selected device model
                vwModel.setBackgroundColor(getResources().getColor(R.color.colorSelected));

                //Get current selected model
                mModel = tracker.getModel();
            }
        }

        //If activity is not in edit mode
        if(tracker == null)
        {
            //Select first item as default
            vwModels.getChildAt(0).setBackgroundColor(getResources().getColor(R.color.colorSelected));
        }

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
