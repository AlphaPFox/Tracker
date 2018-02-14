package br.gov.dpf.tracker;

import android.app.ActivityOptions;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.transition.Fade;
import android.util.Pair;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.FirebaseFirestoreSettings;
import com.google.firebase.firestore.Query;

import java.util.Collections;
import java.util.Comparator;

import br.gov.dpf.tracker.Components.GridAutoLayoutManager;
import br.gov.dpf.tracker.Entities.Tracker;
import br.gov.dpf.tracker.Firestore.TrackerAdapter;

public class MainActivity
        extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {


    //Load components used on recycler view
    public GridAutoLayoutManager mRecyclerLayoutManager;
    private RecyclerView mRecyclerView;
    private View mEmptyView, mLoadingView;
    private TrackerAdapter mAdapter;

    //Store current Firestore DB instance
    private FirebaseFirestore mFireStoreDB;

    //Save current activity scroll position
    private int scrollPosition = 0;

    //Define possible result operations
    public static int RESULT_ERROR = -1;
    public static int RESULT_SUCCESS = 0;
    public static int RESULT_CANCELED = 1;

    //Define possible request operations
    public static int REQUEST_INSERT = 2;
    public static int REQUEST_UPDATE = 3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Check if google maps is available for this device
        checkGoogleMapsAPI();

        //Build toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        //Set drawer layout components
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle mDrawerToggle = new ActionBarDrawerToggle(this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);

        //Set drawer toggle
        drawer.addDrawerListener(mDrawerToggle);
        mDrawerToggle.syncState();

        //Initialize navigation view
        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        //Get views to replace recycler view while loading or if empty
        mLoadingView = findViewById(R.id.vwLoadingCardView);
        mEmptyView = findViewById(R.id.vwEmptyCardView);

        //Initialize db instance
        mFireStoreDB = FirebaseFirestore.getInstance();

        //Disable offline data
        //mFireStoreDB.setFirestoreSettings(new FirebaseFirestoreSettings.Builder().setPersistenceEnabled(false).build());

        // RecyclerView
        mAdapter = new TrackerAdapter(this, mFireStoreDB.collection("Tracker").orderBy("lastUpdate", Query.Direction.DESCENDING))
        {
            @Override
            protected void onDataChanged()
            {
                //Get data status
                boolean isEmpty = getItemCount() == 0;

                //Change views visibility based on data set
                ((View) mRecyclerView.getParent()).setVisibility(isEmpty ? View.GONE : View.VISIBLE);
                mEmptyView.setVisibility(isEmpty ? View.VISIBLE : View.GONE);

                //Hide loading view
                mLoadingView.setVisibility(View.GONE);

            }

            @Override
            protected void onError(FirebaseFirestoreException e) {

                // Show a snack bar on errors
                Snackbar.make(findViewById(android.R.id.content), "Erro ao carregar informações.", Snackbar.LENGTH_LONG).show();
            }
        };

        // Lookup the recycler view in activity layout
        mRecyclerView = findViewById(R.id.tracker_recycler_view);

        // Set linear layout for orientation
        mRecyclerLayoutManager = new GridAutoLayoutManager(mRecyclerView, getResources().getDimensionPixelSize(R.dimen.recycler_item_min_width), getResources().getDimensionPixelSize(R.dimen.recycler_item_min_height));

        // Set layout manager to position the items
        mRecyclerView.setLayoutManager(mRecyclerLayoutManager);

        // Attach the adapter to the recycler view to populate items
        mRecyclerView.setAdapter(mAdapter);

        //Find add button and set click event listener
        findViewById(R.id.btnAddTracker).setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {

            //Create intent to open settings activity
            Intent intent = new Intent(getApplicationContext(), DefaultSettingsActivity.class);

            //Define intent to insert a new tracker
            intent.putExtra("Request", REQUEST_INSERT);

            //Start settings activity and wait for the return
            startActivityForResult(intent, REQUEST_INSERT);

            }
        });
    }

    @Override
    protected void onActivityResult (int requestCode, int resultCode, final Intent intent) {

        // Collect data from the intent and use it
        if(resultCode == RESULT_SUCCESS)
        {
            // Show success message
            Snackbar.make(findViewById(android.R.id.content), "Rastreador " + (requestCode == REQUEST_INSERT ? "cadastrado" : "atualizado") + " com sucesso.", Snackbar.LENGTH_LONG).show();

            //Scroll to previously saved position
            mRecyclerLayoutManager.scrollToPosition(scrollPosition);
        }
        else if (resultCode == RESULT_ERROR)
        {
            // Show error message
            Snackbar.make(findViewById(android.R.id.content), "Erro ao executar operação de " + (requestCode == REQUEST_INSERT ? "cadastro." : "atualização."), Snackbar.LENGTH_LONG).show();
        }
    }

    public void OnTrackerSelected(Tracker tracker, View viewRoot) {

        // Go to the details page for the selected restaurant
        final Intent intent = new Intent(this, DetailActivity.class);

        // Put tracker data on intent
        intent.putExtra("Tracker", tracker);

        //If device supports shared element transition
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setEnterTransition(new Fade(Fade.IN));
            getWindow().setEnterTransition(new Fade(Fade.OUT));

            //Check if loading animation is still visible
            int transitionID = (viewRoot.findViewById(R.id.loadingBackground).getAlpha() == 0 ? R.id.googleMap : R.id.loadingBackground);

            //Save transition ID on intent
            intent.putExtra("DetailActivity_BackgroundTransition", transitionID);

            //Define shared elements to perform transition
            final ActivityOptions options = ActivityOptions
                    .makeSceneTransitionAnimation(this,
                            new Pair<>(viewRoot.findViewById(transitionID),
                                    getString(R.string.transition_drawable)),
                            new Pair<>(viewRoot.findViewById(R.id.imgTracker),
                                    getString(R.string.transition_icon)),
                            new Pair<>(viewRoot.findViewById(R.id.lblTrackerName),
                                    getString(R.string.transition_title)),
                            new Pair<>(viewRoot.findViewById(R.id.txtTrackerModel),
                                    getString(R.string.transition_subtitle)));

            //Wait for loading animation to show
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run()
                {

                    //Start activity after 500ms
                    startActivity(intent, options.toBundle());
                }
            }, 500);

        }
        else
        {
            //No support to shared transition, start activity without animation
            startActivity(intent);
        }
    }

    public void OnTrackerDelete(final Tracker tracker)
    {
        //Create confirmation dialog
        new AlertDialog.Builder(this)
            .setIcon(android.R.drawable.ic_dialog_alert)
            .setTitle("Deletar rastreador")
            .setMessage("Confirma a exclusão do rastreador " + tracker.getName() + "?")
            .setPositiveButton("Sim", new DialogInterface.OnClickListener()
            {
                @Override
                public void onClick(DialogInterface dialog, int which)
                {
                    //Execute DB operation
                    mFireStoreDB
                            .collection("Tracker")
                            .document(tracker.getIdentification())
                            .delete()
                            .addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void aVoid) {

                                    // Show a snack bar success message
                                    Snackbar.make(findViewById(android.R.id.content), "Rastreador excluído com sucesso.", Snackbar.LENGTH_LONG).show();
                                }
                            })
                            .addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {

                                    // Show a snack bar success message
                                    Snackbar.make(findViewById(android.R.id.content), "Erro ao excluir rastreador.", Snackbar.LENGTH_LONG).show();

                                }
                            });
                }

            })
            .setNegativeButton("Não", null)
            .show();
    }

    public void OnTrackerEdit(final Tracker tracker, final View viewRoot)
    {
        //Create a new popup menu
        PopupMenu popup = new PopupMenu(this, viewRoot.findViewById(R.id.imgEdit));

        //Get layout inflater
        popup.getMenuInflater().inflate(R.menu.tracker, popup.getMenu());

        //Define click actions
        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item)
            {

                // Create an intent to open Settings activity
                final Intent intent = new Intent();

                //Define request intent to update an existing tracker
                intent.putExtra("Request", REQUEST_UPDATE);

                // Put tracker data on intent
                intent.putExtra("Tracker", tracker);

                // Save current position
                scrollPosition = mRecyclerView.getScrollState();

                switch (item.getItemId())
                {
                    case R.id.action_detail:

                        //Call method to open detail activity
                        OnTrackerSelected(tracker, viewRoot);

                        //End method
                        return true;

                    case R.id.action_default_settings:

                        //Set intent to open DefaultSettingsActivity
                        intent.setClass(MainActivity.this, DefaultSettingsActivity.class);

                        //Start edit activity
                        startActivityForResult(intent, REQUEST_UPDATE);

                        //End method
                        return true;

                    case R.id.action_tracker_settings:

                        //Set intent to open DefaultSettingsActivity
                        intent.setClass(MainActivity.this, TrackerSettingsActivity.class);

                        //Start edit activity
                        startActivityForResult(intent, REQUEST_UPDATE);

                        //End method
                        return true;


                    case R.id.action_notification_settings:

                        //Set intent to open DefaultSettingsActivity
                        intent.setClass(MainActivity.this, TrackerSettingsActivity.class);

                        //Define request intent to update an existing tracker
                        intent.putExtra("UpdateNotifications", true);

                        //Start edit activity
                        startActivityForResult(intent, REQUEST_UPDATE);

                        //End method
                        return true;

                    case R.id.action_delete:

                        //Call method to confirm tracker deletion
                        OnTrackerDelete(tracker);

                        //End method
                        return true;
                }
                return false;
            }
        });

        //Show popup menu
        popup.show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        switch (id)
        {
            case R.id.action_add:

                // Create an intent to open Settings activity
                Intent intent = new Intent(getApplicationContext(), DefaultSettingsActivity.class);

                // Define request intent to update an existing tracker
                intent.putExtra("Request", REQUEST_INSERT);

                // Start register activity and wait for result
                startActivityForResult(intent, REQUEST_INSERT);

                // End method
                return true;

            case R.id.action_refresh:

                return true;

            case R.id.action_settings:
                // Open navigation drawer
                DrawerLayout drawer = findViewById(R.id.drawer_layout);
                drawer.openDrawer(GravityCompat.START);
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_camera) {
            // Handle the camera action
        } else if (id == R.id.nav_gallery) {

        } else if (id == R.id.nav_slideshow) {

        } else if (id == R.id.nav_manage) {

        } else if (id == R.id.nav_share) {

        } else if (id == R.id.nav_send) {

        }

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }


    @Override
    protected void onStart() {
        super.onStart();
        mAdapter.startListening();
    }

    @Override
    protected void onPause()
    {
        //Save current scroll position
        scrollPosition = mRecyclerLayoutManager.findFirstCompletelyVisibleItemPosition();
        super.onPause();
    }

    @Override
    protected void onResume()
    {
        //Scroll to previously saved position
        mRecyclerLayoutManager.scrollToPosition(scrollPosition);

        //Clear variable
        scrollPosition = 0;
        super.onResume();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mAdapter.stopListening();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig)
    {
        //Change recycler view items size if orientation changes
        mRecyclerLayoutManager.resizeOnOrientationChange();

        //Call super method
        super.onConfigurationChanged(newConfig);
    }

    //Check if API is available
    public void checkGoogleMapsAPI()
    {
        if (GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(this) != ConnectionResult.SUCCESS)
        {
            AlertDialog.Builder builder;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                builder = new AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert);
            } else {
                builder = new AlertDialog.Builder(this);
            }

            builder.setTitle("Erro ao carregar mapa do Google Maps")
                    .setMessage("Atualize a versão do Google Play Services")
                    .setNeutralButton("OK", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                        }
                    })
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .show();
        }
    }
}
