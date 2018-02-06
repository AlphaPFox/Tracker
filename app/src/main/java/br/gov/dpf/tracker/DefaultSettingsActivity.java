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
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import br.gov.dpf.tracker.Entities.Tracker;

public class DefaultSettingsActivity extends AppCompatActivity {

    //Default device model
    private String mModel = "tk102b";

    //Default color option
    private String mColor = "#99ff0000";

    //Menu item used to confirm settings
    private MenuItem confirmMenu;

    //Object representing the tracker to be inserted/updated
    private Tracker tracker;

    //Flag indicating whether this activity is in edit mode
    private boolean editMode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        //Set activity layout
        setContentView(R.layout.activity_settings);

        //Load toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        //Load Floating Action Button
        FloatingActionButton fab = findViewById(R.id.fab);

        //Set click event
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                //Method called to save data
                onSettingsConfirmed();
            }
        });

        //Check action bar
        if(getSupportActionBar() != null)
        {
            //Set back button on toolbar
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        //Check if activity was called to edit existing tracker
        if(getIntent().getIntExtra("Request", MainActivity.REQUEST_INSERT) == MainActivity.REQUEST_UPDATE)
        {
            //Load tracker from intent
            tracker = getIntent().getParcelableExtra("Tracker");

            //Set activity in edit mode
            editMode = true;

            //Load tracker data
            loadData();

            //Hide add floating action button
            fab.setVisibility(View.GONE);
        }

        //Handle click events for color options
        loadColors((GridLayout) findViewById(R.id.vwColors));

        //Handle click events for model options
        loadModels((LinearLayout) findViewById(R.id.vwModels));
    }


    //Called when editing an existing tracker
    public void loadData()
    {
        //Load tracker data on text views
        ((EditText) findViewById(R.id.txtTrackerName)).setText(tracker.getName());
        ((EditText) findViewById(R.id.txtTrackerDescription)).setText(tracker.getDescription());
        ((EditText) findViewById(R.id.txtTrackerIdentification)).setText(tracker.getIdentification());

        //Change subtitle to alert that tracker identification cannot change
        ((TextView) findViewById(R.id.lblTrackerIdentificationSubtitle)).setText(getResources().getText(R.string.lblEditIDSubtitle));

        //Disable update of tracker unique ID
        findViewById(R.id.txtTrackerIdentification).setEnabled(false);

        //Hide model selected
        findViewById(R.id.vwModelCardView).setVisibility(View.GONE);

        //Check support action bar
        if(getSupportActionBar() != null)
        {
            //Change activity title
            getSupportActionBar().setTitle(tracker.getName());
        }
    }

    //Called when editing or inserting a new tracker
    public void onSettingsConfirmed()
    {
        //Get tracker name and identification
        String trackerName = ((EditText) findViewById(R.id.txtTrackerName)).getText().toString();
        String trackerIdentification = ((EditText) findViewById(R.id.txtTrackerIdentification)).getText().toString();

        //Check user input
        if(trackerName.isEmpty() || trackerIdentification.isEmpty())
        {
            //Alert user, not optional fields
            Snackbar.make(findViewById(android.R.id.content), "Preencha os campos nome e identificação do aparelho", Snackbar.LENGTH_LONG).show();
        }
        else if(mModel.equals("pt39") || mModel.equals("gt02"))
        {
            //Alert user, unsupported models
            Snackbar.make(findViewById(android.R.id.content), "Modelo atualmente não suportado pela plataforma", Snackbar.LENGTH_LONG).show();
        }
        else
        {
            // Input is validated, set loading indicator on menu
            confirmMenu.setActionView(new ProgressBar(this));

            //Check if activity is in edit mode
            if(editMode)
            {
                //Update tracker data
                tracker.setName(trackerName);
                tracker.setDescription(((EditText) findViewById(R.id.txtTrackerDescription)).getText().toString());
                tracker.setIdentification(trackerIdentification);

                //Update tracker color
                tracker.setBackgroundColor(mColor);

                //Check if user is changing tracker model
                if(!tracker.getModel().equals(mModel))
                {
                    //Save new tracker model
                    tracker.setModel(mModel);

                    //Create intent to call next activity (Tracker Configurations)
                    Intent intent = new Intent(DefaultSettingsActivity.this, TrackerSettingsActivity.class);

                    //Put tracker data on intent
                    intent.putExtra("Tracker", tracker);

                    //Inform activity intention: change tracker model
                    intent.putExtra("UpdateModel", true);

                    //Inform activity intention: insert new tracker
                    intent.putExtra("Request", MainActivity.REQUEST_UPDATE);

                    //Start next activity
                    startActivityForResult(intent, MainActivity.REQUEST_UPDATE);
                }
                else
                {
                    // If editing an existing tracker
                    FirebaseFirestore.getInstance()
                            .collection("Tracker")
                            .document(tracker.getIdentification())
                            .set(tracker, SetOptions.merge())
                            .addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void aVoid) {

                                    //Set result editing result - OK
                                    setResult(MainActivity.RESULT_SUCCESS);

                                    //End activity (returns to parent activity -> OnActivityResult)
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
            }
            else
            {
                //Then this is an INSERT operation, create a new tracker
                tracker = new Tracker();

                //Save tracker data
                tracker.setName(trackerName);
                tracker.setDescription(((EditText) findViewById(R.id.txtTrackerDescription)).getText().toString());
                tracker.setIdentification(trackerIdentification);
                tracker.setModel(mModel);

                //Save tracker color
                tracker.setBackgroundColor(mColor);

                //Run query to check if there is already a tracker with this identification
                FirebaseFirestore.getInstance()
                        .collection("Tracker")
                        .document(tracker.getIdentification())
                        .get()
                        .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                            @Override
                            public void onSuccess(DocumentSnapshot documentSnapshot) {

                                //If there is a tracker with the same ID
                                if(documentSnapshot.exists())
                                {
                                    // Show a snack bar on errors
                                    Snackbar.make(findViewById(android.R.id.content), "Erro: Já existe um rastreador com o mesmo identificador.", Snackbar.LENGTH_LONG).show();

                                    // Cancel loading indicator
                                    confirmMenu.setActionView(null);
                                }
                                else
                                {
                                    //Create intent to call next activity (Tracker Configurations)
                                    Intent intent = new Intent(DefaultSettingsActivity.this, TrackerSettingsActivity.class);

                                    //Put tracker data on intent
                                    intent.putExtra("Tracker", tracker);

                                    //Inform activity intention: insert new tracker
                                    intent.putExtra("Request", MainActivity.REQUEST_INSERT);

                                    //Start next activity
                                    startActivityForResult(intent, MainActivity.REQUEST_INSERT);
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
            }
        }
    }

    @Override
    protected void onActivityResult (int requestCode, int resultCode, final Intent intent)
    {
        // Check TrackerSettingsActivity result
        if(resultCode == MainActivity.RESULT_CANCELED)
        {
            // User canceled (back pressed), hide loading
            confirmMenu.setActionView(null);
        }
        else
        {
            // User completed operation, send result to MainActivity
            setResult(resultCode);

            // End activity (returns to parent activity -> OnActivityResult)
            finish();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.settings, menu);

        //Save action menu item
        confirmMenu = menu.findItem(R.id.action_add);

        //If in edit mode
        if(editMode)
        {
            //Add a new option
            menu.add(Menu.NONE, R.id.action_default_settings, Menu.NONE, "Alterar modelo do rastreador");
            menu.add(Menu.NONE, R.id.action_tracker_settings, Menu.NONE, "Configurações do dispositivo");
            menu.add(Menu.NONE, R.id.action_notification_settings, Menu.NONE, "Opções de notificação");
        }

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home:

                //Set result
                setResult(MainActivity.RESULT_CANCELED);

                //End activity (returns to parent activity -> OnActivityResult)
                finish();

                //End method
                return true;

            case R.id.action_add:

                //Method called to save data
                onSettingsConfirmed();

                //End method
                return true;

            case R.id.action_default_settings:

                //Method called to save data
                findViewById(R.id.vwModelCardView).setVisibility(View.VISIBLE);

                //End method
                return true;

            case R.id.action_tracker_settings:
            case R.id.action_notification_settings:

                //Create intent to call next activity (Tracker Configurations)
                Intent intent = new Intent(DefaultSettingsActivity.this, TrackerSettingsActivity.class);

                //Put tracker data on intent
                intent.putExtra("Tracker", tracker);

                //Inform activity intention: change tracker model
                intent.putExtra("UpdateNotifications", (item.getItemId() == R.id.action_notification_settings));

                //Inform activity intention: insert new tracker
                intent.putExtra("Request", MainActivity.REQUEST_UPDATE);

                //Start next activity
                startActivityForResult(intent, MainActivity.REQUEST_UPDATE);

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

        //If activity is not in edit mode
        if(!editMode)
        {
            //Clear any previous text on identification
            txtTrackerID.setText("");
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
            if(editMode && vwModel.getTag().equals(tracker.getModel()))
            {
                //Set checked status
                vwModel.setBackgroundColor(getResources().getColor(R.color.colorSelected));

                //Get current selected color
                mModel = tracker.getModel();
            }
        }

        //If activity is not in edit mode
        if(!editMode)
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
            if(editMode && vwColor.getTag().equals(tracker.getBackgroundColor()))
            {
                //Set checked status
                vwColor.setChecked(true);

                //Get current selected color
                mColor = tracker.getBackgroundColor();
            }
        }

        //If activity is not in edit mode
        if(!editMode)
        {
            //Select first item as default
            ((AppCompatCheckBox) vwColors.getChildAt(0)).setChecked(true);
        }
    }
}
