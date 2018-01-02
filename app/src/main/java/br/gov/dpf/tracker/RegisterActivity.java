package br.gov.dpf.tracker;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.NavUtils;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatCheckBox;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

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

        //Handle click events for device model
        onModelSelect((LinearLayout) findViewById(R.id.vwModels));

        //Handle click events for color options
        onColorSelect((GridLayout) findViewById(R.id.vwColors));

        //Set back button on toolbar
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

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

    public void onModelSelect(final LinearLayout vwModels){

        //For each device model
        for (int i = 0; i < vwModels.getChildCount(); i++)
        {
            //Set on click event listener
            vwModels.getChildAt(i).setOnClickListener(new View.OnClickListener() {

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
