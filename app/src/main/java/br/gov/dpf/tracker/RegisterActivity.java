package br.gov.dpf.tracker;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.NavUtils;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatCheckBox;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.BaseAdapter;
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

import br.gov.dpf.tracker.Components.ImageDownloader;
import br.gov.dpf.tracker.Entities.Model;
import br.gov.dpf.tracker.Firestore.TrackerAdapter;

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

                            for (DocumentSnapshot document : task.getResult())
                            {
                                //Get model data
                                Model trackerModel = document.toObject(Model.class);

                                //Create item layout
                                View vwModel = getLayoutInflater().inflate(R.layout.model_layout_item, vwModels, false);

                                //Set item text
                                ((TextView)vwModel.findViewById(R.id.txtModel)).setText(trackerModel.getName());

                                //Set item image
                                new ImageDownloader((ImageView) vwModel.findViewById(R.id.imgModel), trackerModel.getName()).execute(trackerModel.getImagePath());

                                //Add item to parent
                                vwModels.addView(vwModel);

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
                                    }
                                });
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
    }

    public void onAddButtonClicked()
    {

        String trackerName = ((EditText) findViewById(R.id.txtTrackerName)).getText().toString();
        String trackerPhone = ((EditText) findViewById(R.id.txtPhoneNumber)).getText().toString();

        if(trackerName.isEmpty() || trackerPhone.isEmpty())
        {
            Snackbar.make(findViewById(R.id.fab), "Preencha os campos nome e número de telefone", Snackbar.LENGTH_LONG).setAction("Action", null).show();
        }
        else
        {
            Intent intent = new Intent();
            intent.putExtra("TrackerName", trackerName);
            intent.putExtra("TrackerPhone", trackerPhone);
            intent.putExtra("TrackerModel", mModel);
            intent.putExtra("TrackerColor", mColor);
            setResult(RESULT_OK, intent);
            finish();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.register, menu);

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
        }
        return super.onOptionsItemSelected(item);
    }

    public void onColorSelect(final GridLayout vwColors){

        //For each device model
        for (int i = 0; i < vwColors.getChildCount(); i++)
        {
            //Set on click event listener
            vwColors.getChildAt(i).setOnClickListener(new View.OnClickListener() {

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
        }
    }
}
