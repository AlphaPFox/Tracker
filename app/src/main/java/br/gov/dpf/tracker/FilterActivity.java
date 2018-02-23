package br.gov.dpf.tracker;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SwitchCompat;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.TextView;

import com.xw.repo.BubbleSeekBar;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class FilterActivity extends AppCompatActivity implements View.OnClickListener, BubbleSeekBar.OnProgressChangedListener
{

    private SharedPreferences sharedPreferences;
    private TextView txtCoordinatesNumber;
    private EditText startDate, endDate;
    private SwitchCompat swShowPolyline, swShowCircle;
    private BubbleSeekBar seekBar;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_filter);

        //Get shared preferences
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        //Get layout used on multiple methods
        swShowPolyline = findViewById(R.id.swShowPolyline);
        swShowCircle = findViewById(R.id.swShowCircle);
        startDate = findViewById(R.id.txtStartDate);
        endDate = findViewById(R.id.txtEndDate);
        seekBar = findViewById(R.id.sbPeriodicUpdate);
        txtCoordinatesNumber = findViewById(R.id.txtCoordinatesNumber);

        //Get layout items used on this method only
        Toolbar toolbar = findViewById(R.id.toolbar);
        FloatingActionButton fab = findViewById(R.id.fab);

        //Set toolbar
        setSupportActionBar(toolbar);

        //Set floating action button action
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                applyFilter();
            }
        });

        //Search selected number of coordinates
        int coordinatesNumber = sharedPreferences.getInt("CoordinatesNumber", 5);

        //Set coordinates number text
        txtCoordinatesNumber.setText(getResources().getString(R.string.txtCoordinatesNumber, coordinatesNumber));

        //Set show preferences
        swShowPolyline.setChecked(sharedPreferences.getInt("Map_Path", 0) > 0);
        swShowCircle.setChecked(sharedPreferences.getInt("Map_Radius", 2) > 0);

        //Set progress and listener
        seekBar.setProgress(coordinatesNumber);
        seekBar.setOnProgressChangedListener(this);

        //Set date field click listener
        startDate.setOnClickListener(this);
        endDate.setOnClickListener(this);

        //Get activity intent
        Intent intent = getIntent();

        // Check if user already selected date previously
        if(intent.hasExtra("StartDate") && intent.hasExtra("EndDate"))
        {
            //Set user defined dates
            startDate.setText(intent.getStringExtra("StartDate"));
            endDate.setText(intent.getStringExtra("EndDate"));
        }
        else
        {
            // Get Current Date
            final Calendar baseDate = Calendar.getInstance();

            //Set end date value to today
            endDate.setText(new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(baseDate.getTime()));

            //Subtract 7 days from today
            baseDate.add(Calendar.DAY_OF_YEAR, -7);

            //Set start date value
            startDate.setText(new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(baseDate.getTime()));
        }

        //Set back button on toolbar
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    public void applyFilter()
    {
        //Create intent to return data
        Intent returnIntent = getIntent();

        //Open shared preferences editor
        SharedPreferences.Editor editor = sharedPreferences.edit();

        //If user selected to show path and previous preference was to hide
        if(swShowPolyline.isChecked() &&  sharedPreferences.getInt("Map_Path", 2) == 0)
        {
            //Save preference to show default option
            editor.putInt("Map_Path", 2);
        }
        else if(!swShowPolyline.isChecked())
        {
            //Save preference to hide map path
            editor.putInt("Map_Path", 0);
        }

        //If user selected to show radius and previous preference was to hide
        if(swShowCircle.isChecked() &&  sharedPreferences.getInt("Map_Radius", 3) == 0)
        {
            //Save preference to show default option
            editor.putInt("Map_Radius", 3);
        }
        else if(!swShowCircle.isChecked())
        {
            //Save preference to hide map radius
            editor.putInt("Map_Radius", 0);
        }

        //Save user selected settings
        editor.putInt("CoordinatesNumber", seekBar.getProgress());

        //Save date settings
        returnIntent.putExtra("StartDate", startDate.getText().toString());
        returnIntent.putExtra("EndDate", endDate.getText().toString());

        //Apply settings on editor
        editor.apply();

        //Set result
        setResult(RESULT_OK, returnIntent);

        //End activity (returns to DetailActivity.OnActivityResult)
        finish();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu;
        getMenuInflater().inflate(R.menu.filter, menu);

        //Return super call
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home:
                finish();
                return true;

            case R.id.action_apply:
                applyFilter();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onClick(final View textView) {

        //Text from edit field
        String date = ((TextView) textView).getText().toString();

        //Create date picker dialog
        DatePickerDialog datePickerDialog = new DatePickerDialog(this, new DatePickerDialog.OnDateSetListener()
        {
            @Override
            public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth)
            {
                //Get Current Date
                final Calendar baseDate = Calendar.getInstance();

                //Set base date
                baseDate.set(year, monthOfYear, dayOfMonth);

                //Show selected date on text field
                ((TextView) textView).setText(new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(baseDate.getTime()));
            }

        }, Integer.valueOf(date.substring(6)), Integer.valueOf(date.substring(3, 5)) - 1, Integer.valueOf(date.substring(0, 2)));

        //Check if view is end date (defining date range)
        if(textView.getId() == R.id.txtEndDate)
        {
            //Get start date from text field
            String datetime = ((TextView) findViewById(R.id.txtStartDate)).getText().toString();

            try
            {
                //Parse to date format
                Date startDate = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).parse(datetime);

                //Set start date as min date
                datePickerDialog.getDatePicker().setMinDate(startDate.getTime());

                //Set today as max date
                datePickerDialog.getDatePicker().setMaxDate(new Date().getTime());
            }
            catch (Exception ex)
            {
                //Log error
                Log.e("FilterActivity", "Error parsing start datetime", ex);
            }
        }
        else
        {
            //Get start date from text field
            String datetime = ((TextView) findViewById(R.id.txtEndDate)).getText().toString();

            try
            {
                //Parse to date format
                Date endDate = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).parse(datetime);

                //Set today as max date
                datePickerDialog.getDatePicker().setMaxDate(endDate.getTime());
            }
            catch (Exception ex)
            {
                //Log error
                Log.e("FilterActivity", "Error parsing end datetime", ex);
            }
        }

        //Show dialog
        datePickerDialog.show();
    }

    @Override
    public void onProgressChanged(BubbleSeekBar bubbleSeekBar, int progress, float progressFloat) {

        //Set coordinates number text
        txtCoordinatesNumber.setText(getResources().getString(R.string.txtCoordinatesNumber, progress));
    }

    @Override
    public void getProgressOnActionUp(BubbleSeekBar bubbleSeekBar, int progress, float progressFloat) {

    }

    @Override
    public void getProgressOnFinally(BubbleSeekBar bubbleSeekBar, int progress, float progressFloat) {

    }
}
