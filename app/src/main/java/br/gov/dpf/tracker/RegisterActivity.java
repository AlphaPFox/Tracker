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
import android.support.v4.app.NavUtils;
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
import android.widget.TextView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.xw.repo.BubbleSeekBar;

import br.gov.dpf.tracker.Components.ImageDownloader;
import br.gov.dpf.tracker.Entities.Model;

public class RegisterActivity extends AppCompatActivity
{
    //Default device model
    private String mModel = "TK 102B";

    //Default color option
    private String mColor = "#90FF0000";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_register);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = findViewById(R.id.fab);

        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onAddButtonClicked();
            }
        });

        //Handle click events for color options
        onColorSelect((GridLayout) findViewById(R.id.vwColors));

        //Set back button on toolbar
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

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

        //Get model collection
        FirebaseFirestore.getInstance().collection("Model")
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful())
                        {
                            //Layout parent
                            final LinearLayout vwModels = findViewById(R.id.vwModels);

                            //Value exists if activity is in edit mode
                            String trackerModel = getIntent().getStringExtra("RegisterActivity_TrackerModel");

                            //For each model retrieved
                            for (DocumentSnapshot document : task.getResult())
                            {
                                //Get model data
                                Model model = document.toObject(Model.class);

                                //Create item layout
                                final View vwModel = getLayoutInflater().inflate(R.layout.model_layout_item, vwModels, false);

                                //Set item text
                                ((TextView)vwModel.findViewById(R.id.txtModel)).setText(model.getName());

                                //Set item image
                                ImageDownloader modelIcon = new ImageDownloader((ImageView) vwModel.findViewById(R.id.imgModel), model.getName(), model.getImagePath());

                                //Execute image search from disk or URL
                                modelIcon.execute();

                                //Add item to parent
                                vwModels.addView(vwModel);

                                //If activity is in edit mode and this is the corresponding tracker model
                                if(trackerModel != null && trackerModel.equals(model.getName()))
                                {
                                    //Set background color only for the selected device model
                                    vwModel.setBackgroundColor(getResources().getColor(R.color.colorSelected));

                                    //Scroll to position
                                    vwModel.post(new Runnable() {
                                        @Override
                                        public void run()
                                        {
                                            //Scroll to selected model position
                                            findViewById(R.id.vwModelScroll).scrollTo(vwModel.getLeft(), 0);
                                        }
                                    });

                                    //Get current selected model
                                    mModel = trackerModel;

                                    //Change layout labels according to selected model
                                    changeLabels(mModel);
                                }

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
                                        mModel = ((TextView) view.findViewById(R.id.txtModel)).getText().toString();

                                        //Change layout labels according to selected model
                                        changeLabels(mModel);
                                    }
                                });
                            }

                            //If there is any model available and activity is not in edit mode
                            if(vwModels.getChildCount() > 0 && trackerModel == null)
                            {
                                //Set first item as selected by default
                                vwModels.getChildAt(0).setBackgroundColor(getResources().getColor(R.color.colorSelected));

                                //Get current selected model
                                mModel = ((TextView) vwModels.getChildAt(0).findViewById(R.id.txtModel)).getText().toString();
                            }
                        }
                        else
                        {
                            // Show a snack bar on errors
                            Snackbar.make(findViewById(android.R.id.content),
                                    "Database error: check logs for info.", Snackbar.LENGTH_LONG).show();

                            //Log error
                            Log.e("Firestore DB", "Error", task.getException());
                        }
                    }
                });

        //Get activity intent
        Intent intent = getIntent();

        //Check if activity was called to edit existing tracker
        if(intent.hasExtra("RegisterActivity_TrackerID"))
        {
            //Load tracker data
            loadData(intent);

            //Hide add floating action button
            fab.setVisibility(View.GONE);
        }

    }

    //Called when editing an existing tracker
    public void loadData(Intent intent)
    {
        //Get trackerID from intent
        String trackerID = intent.getStringExtra("RegisterActivity_TrackerID");

        //Get shared preferences
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        //Load tracker data on text views
        ((EditText) findViewById(R.id.txtTrackerName)).setText(intent.getStringExtra("RegisterActivity_TrackerName"));
        ((EditText) findViewById(R.id.txtTrackerDescription)).setText(intent.getStringExtra("RegisterActivity_TrackerDescription"));
        ((EditText) findViewById(R.id.txtTrackerID)).setText(intent.getStringExtra("RegisterActivity_TrackerIdentification"));

        //Set update interval options
        ((BubbleSeekBar) findViewById(R.id.seekBar)).setProgress(getSectionByUpdateInterval(intent.getIntExtra("RegisterActivity_TrackerUpdateInterval", 60)));

        //Set notification options using shared preferences
        ((SwitchCompat)findViewById(R.id.swLowBattery)).setChecked(sharedPreferences.getBoolean(trackerID + "_NotifyLowBattery", false));
        ((SwitchCompat)findViewById(R.id.swMovement)).setChecked(sharedPreferences.getBoolean(trackerID + "_NotifyMovement", false));
        ((SwitchCompat)findViewById(R.id.swStopped)).setChecked(sharedPreferences.getBoolean(trackerID + "_NotifyStopped", false));
        ((SwitchCompat)findViewById(R.id.swTrackerStatus)).setChecked(sharedPreferences.getBoolean(trackerID + "_NotifyStatus", false));
        ((SwitchCompat)findViewById(R.id.swAvailable)).setChecked(sharedPreferences.getBoolean(trackerID + "_NotifyAvailable", false));
        ((SwitchCompat)findViewById(R.id.swSMSResponse)).setChecked(sharedPreferences.getBoolean(trackerID + "_NotifySMSResponse", false));

        //Check support action bar
        if(getSupportActionBar() != null)
        {
            //Change activity title
            getSupportActionBar().setTitle(intent.getStringExtra("RegisterActivity_TrackerName"));
        }
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

            //Check if activity is in edit mode
            if(getIntent().hasExtra("RegisterActivity_TrackerID"))
            {
                //Add tracker ID on main activity intent
                intent.putExtra("RegisterActivity_TrackerID", getIntent().getStringExtra("RegisterActivity_TrackerID"));

                //Inform activity intention: update existing tracker
                intent.putExtra("RegisterActivity_UpdateTracker", true);

                //Inform tracker position on main activity
                intent.putExtra("TrackerPosition", getIntent().getIntExtra("TrackerPosition",0));
            }
            else
            {
                //Inform activity intention: update existing tracker
                intent.putExtra("RegisterActivity_InsertTracker", true);
            }

            //Save tracker data on intent
            intent.putExtra("TrackerName", trackerName);
            intent.putExtra("TrackerDescription", ((EditText) findViewById(R.id.txtTrackerDescription)).getText().toString());
            intent.putExtra("TrackerIdentification", trackerIdentification);
            intent.putExtra("TrackerModel", mModel);

            //Get update interval
            intent.putExtra("TrackerUpdateInterval", getUpdateIntervalBySection(((BubbleSeekBar) findViewById(R.id.seekBar)).getProgress()));
            intent.putExtra("TrackerColor", mColor);

            //Save notifications preferences
            intent.putExtra("NotifyLowBattery", ((SwitchCompat)findViewById(R.id.swLowBattery)).isChecked());
            intent.putExtra("NotifyMovement", ((SwitchCompat)findViewById(R.id.swMovement)).isChecked());
            intent.putExtra("NotifyStopped", ((SwitchCompat)findViewById(R.id.swStopped)).isChecked());
            intent.putExtra("NotifyStatus", ((SwitchCompat)findViewById(R.id.swTrackerStatus)).isChecked());
            intent.putExtra("NotifyAvailable", ((SwitchCompat)findViewById(R.id.swAvailable)).isChecked());
            intent.putExtra("NotifySMSResponse", ((SwitchCompat)findViewById(R.id.swSMSResponse)).isChecked());

            //Set result
            setResult(RESULT_OK, intent);

            //End activity (returns to MainActivity.OnActivityResult)
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
                .setMessage("Confirma a exclusão do rastreador " + getIntent().getStringExtra("RegisterActivity_TrackerName") + "?")
                .setPositiveButton("Sim", new DialogInterface.OnClickListener()
                {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        //Create intent to return on main activity
                        Intent intent = new Intent();

                        //Add tracker ID on intent
                        intent.putExtra("RegisterActivity_TrackerID", getIntent().getStringExtra("RegisterActivity_TrackerID"));

                        //Inform activity intention: delete existing tracker
                        intent.putExtra("RegisterActivity_DeleteTracker", true);

                        //Set result
                        setResult(RESULT_OK, intent);

                        //End activity (returns to MainActivity.OnActivityResult)
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
        delete.setVisible(getIntent().hasExtra("RegisterActivity_TrackerID"));

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home:
                NavUtils.navigateUpFromSameTask(this);
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

    public void onColorSelect(final GridLayout vwColors){

        //Check if activity is in edit mode
        String trackerColor = getIntent().getStringExtra("RegisterActivity_TrackerColor");

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
            if(trackerColor != null && vwColor.getTag().equals(trackerColor))
            {
                //Set checked status
                vwColor.setChecked(true);

                //Get current selected color
                mColor = trackerColor;
            }
        }

        //If activity is not in edit mode
        if(trackerColor == null)
        {
            //Select first item as default
            ((AppCompatCheckBox) vwColors.getChildAt(0)).setChecked(true);
        }
    }
}
