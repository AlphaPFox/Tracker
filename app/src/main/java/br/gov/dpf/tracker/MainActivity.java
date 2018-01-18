package br.gov.dpf.tracker;

import android.app.ActivityOptions;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.transition.Fade;
import android.util.Pair;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.WriteBatch;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.HashMap;
import java.util.Map;

import br.gov.dpf.tracker.Components.GridAutoLayoutManager;
import br.gov.dpf.tracker.Entities.Tracker;
import br.gov.dpf.tracker.Firestore.TrackerAdapter;

public class MainActivity
        extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, TrackerAdapter.OnTrackerSelectedListener {

    private RecyclerView mRecyclerView;
    public GridAutoLayoutManager mRecyclerLayoutManager;
    private View mEmptyView, mLoadingView;
    private SwipeRefreshLayout mSwipeRefresh;

    private FirebaseFirestore mFireStoreDB;
    private TrackerAdapter mAdapter;
    private Query mQuery;

    @Override
    protected void onStart() {
        super.onStart();
        mAdapter.startListening();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mAdapter.stopListening();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        checkGoogleMapsAPI();

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle mDrawerToggle = new ActionBarDrawerToggle(this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);

        drawer.addDrawerListener(mDrawerToggle);
        mDrawerToggle.syncState();

        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        mLoadingView = findViewById(R.id.vwLoadingCardView);
        mEmptyView = findViewById(R.id.vwEmptyCardView);

        mSwipeRefresh = findViewById(R.id.swipeRefreshLayout);
        mSwipeRefresh.setColorSchemeResources(R.color.colorAccent);

        //Initialize db instance
        mFireStoreDB = FirebaseFirestore.getInstance();

        //Define db search query
        mQuery = mFireStoreDB.collection("Tracker").orderBy("lastUpdate", Query.Direction.DESCENDING);

        // RecyclerView
        mAdapter = new TrackerAdapter(this, mQuery) {

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

                //Dismiss loading indicator if present
                dismissLoading();
            }

            @Override
            protected void onError(FirebaseFirestoreException e) {

                // Show a snack bar on errors
                Snackbar.make(findViewById(android.R.id.content),
                        "Error: check logs for info.", Snackbar.LENGTH_LONG).show();
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

            //On click, load register activity
            Intent intent = new Intent(getApplicationContext(), RegisterActivity.class);
            startActivityForResult(intent, 0);
            }
        });

        mSwipeRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {

                // Refresh items
                mAdapter.setQuery(mQuery);
            }
        });
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig)
    {
        //Change recycler view items size if orientation changes
        mRecyclerLayoutManager.resizeOnOrientationChange();

        //Call super method
        super.onConfigurationChanged(newConfig);
    }

    //Close loading indicator from swipe refresh
    public void dismissLoading()
    {
        //Keep loading indicator for UI response
        new Handler().postDelayed(new Runnable() {
            @Override public void run() {
                mSwipeRefresh.setRefreshing(false);
            }
        }, 600);
    }

    @Override
    protected void onActivityResult (int requestCode, int resultCode, final Intent intent) {

        // Collect data from the intent and use it
        if(resultCode == RESULT_OK)
        {
            if (intent.hasExtra("RegisterActivity_InsertTracker"))
            {
                //Load new tracker
                final Tracker tracker = new Tracker(
                        intent.getStringExtra("TrackerName"),
                        intent.getStringExtra("TrackerDescription"),
                        intent.getStringExtra("TrackerModel"),
                        intent.getStringExtra("TrackerIdentification"),
                        intent.getIntExtra("TrackerUpdateInterval", 60),
                        intent.getStringExtra("TrackerColor"));

                // Add a new document with a generated ID
                mFireStoreDB.collection("Tracker")
                        .add(tracker)
                        .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                            @Override
                            public void onSuccess(final DocumentReference documentReference) {

                                //Update notification options
                                updateTrackerNotifications(intent, documentReference.getId());

                                //Create snack bar to show feed back to user
                                Snackbar.make(findViewById(R.id.coordinator_layout), "Rastreador cadastrado!", Snackbar.LENGTH_LONG)
                                        .setAction("CANCELAR", new View.OnClickListener() {
                                            @Override
                                            public void onClick(View view) {
                                                //Undo tracker insert
                                                documentReference.delete();

                                                //Show message to user
                                                Snackbar.make(findViewById(R.id.coordinator_layout), "Inclusão cancelada com sucesso.", Snackbar.LENGTH_LONG).show();
                                            }
                                        }).show();

                                //Scroll to show newly inserted item
                                mRecyclerView.scrollToPosition(0);
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Snackbar.make(findViewById(R.id.coordinator_layout), "Erro durante o cadastramento: " + e.toString(), Snackbar.LENGTH_LONG).show();
                            }
                        });
            }
            else if (intent.hasExtra("RegisterActivity_UpdateTracker"))
            {

                //Create hash to store updates
                Map<String,Object> updates = new HashMap<>();

                //Save updates on hash map
                updates.put("name", intent.getStringExtra("TrackerName"));
                updates.put("description", intent.getStringExtra("TrackerDescription"));
                updates.put("model", intent.getStringExtra("TrackerModel"));
                updates.put("identification", intent.getStringExtra("TrackerIdentification"));
                updates.put("updateInterval", intent.getIntExtra("TrackerUpdateInterval", 60));
                updates.put("backgroundColor", intent.getStringExtra("TrackerColor"));

                //Update tracker on fire store DB
                mFireStoreDB.collection("Tracker").document(intent.getStringExtra("RegisterActivity_TrackerID"))
                        .update(updates)
                        .addOnSuccessListener(new OnSuccessListener<Void>()
                        {
                            @Override
                            public void onSuccess(Void aVoid) {

                                //Update notification options
                                updateTrackerNotifications(intent, intent.getStringExtra("RegisterActivity_TrackerID"));

                                //Show confirmation to user
                                Snackbar.make(findViewById(R.id.coordinator_layout), "Rastreador atualizado com sucesso.", Snackbar.LENGTH_LONG).show();
                            }
                        })
                        .addOnFailureListener(new OnFailureListener()
                        {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Snackbar.make(findViewById(R.id.coordinator_layout), "Erro durante a atualização: " + e.getMessage(), Snackbar.LENGTH_LONG).show();
                            }
                        });

            }
            else if (intent.hasExtra("RegisterActivity_DeleteTracker"))
            {
                //Transaction batch
                final WriteBatch batch = mFireStoreDB.batch();

                //Delete operation to be performed if user don't cancel
                batch.delete(mFireStoreDB.collection("Tracker").document(intent.getStringExtra("RegisterActivity_TrackerID")));

                //Inform user that an delete operation is going to happen
                final Snackbar message = Snackbar.make(findViewById(R.id.coordinator_layout), "", Snackbar.LENGTH_LONG);

                //Set message text
                message.setText("Exclusão em andamento...");

                //Set available action
                message.setAction("CANCELAR", new View.OnClickListener()
                    {
                        @Override
                        public void onClick(View view) {

                            //Undo tracker delete
                            message.dismiss();
                        }
                    });

                //Set dismiss event callback
                message.addCallback(new Snackbar.Callback() {

                    @Override
                    public void onDismissed(Snackbar snackbar, int event) {

                        //If user did not canceled delete operation
                        if(event == DISMISS_EVENT_TIMEOUT)
                        {
                            // Commit the delete operation
                            batch.commit()
                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {

                                            //Update notifications with empty intent to remove subscription to topics
                                            updateTrackerNotifications(new Intent(), intent.getStringExtra("RegisterActivity_TrackerID"));

                                            Snackbar.make(findViewById(R.id.coordinator_layout), "Exclusão finalizada com sucesso!", Snackbar.LENGTH_LONG).show();
                                        }
                                    })
                                    .addOnFailureListener(new OnFailureListener()
                                    {
                                        @Override
                                        public void onFailure(@NonNull Exception e) {
                                            Snackbar.make(findViewById(R.id.coordinator_layout), "Erro durante a exclusão: " + e.getMessage(), Snackbar.LENGTH_LONG).show();
                                        }
                                    });
                        }
                    }
                });

                //Show snack bar
                message.show();
            }

            //If returning from Detail Activity
            if(intent.hasExtra("TrackerPosition"))
            {
                //Wait for recycler to load and scroll to selected item position
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mRecyclerView.scrollToPosition(intent.getIntExtra("TrackerPosition", 0));
                    }
                }, 200);
            }
        }
    }

    public void updateTrackerNotifications(Intent i, String TrackerID)
    {
        //Get shared preferences
        SharedPreferences.Editor sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this).edit();

        //Get firebase messaging instance
        FirebaseMessaging notifications = FirebaseMessaging.getInstance();

        //Save options
        updateNotificationOption(sharedPreferences, notifications, i, TrackerID, "NotifyLowBattery");
        updateNotificationOption(sharedPreferences, notifications, i, TrackerID, "NotifyMovement");
        updateNotificationOption(sharedPreferences, notifications, i, TrackerID, "NotifyStopped");
        updateNotificationOption(sharedPreferences, notifications, i, TrackerID, "NotifyStatus");
        updateNotificationOption(sharedPreferences, notifications, i, TrackerID, "NotifyAvailable");
        updateNotificationOption(sharedPreferences, notifications, i, TrackerID, "NotifySMSResponse");

        //Commit changes on shared preferences
        sharedPreferences.apply();
    }

    public void updateNotificationOption(SharedPreferences.Editor sharedPreferences, FirebaseMessaging notifications, Intent i, String TrackerID, String topic)
    {
        //If user wants to receive this notification
        if(i.getBooleanExtra(topic, false))
        {
            //Subscribe to notification topic
            notifications.subscribeToTopic(TrackerID + "_" + topic);

            //Save option on shared preferences
            sharedPreferences.putBoolean(TrackerID + "_" + topic, true);
        }
        else
        {
            //Unsubscribe to notification topic
            notifications.unsubscribeFromTopic(TrackerID + "_" + topic);

            //Remove option from shared preferences
            sharedPreferences.putBoolean(TrackerID + "_" + topic, false);
        }
    }

    @Override
    public void OnTrackerSelected(String tracker_id, Tracker tracker, View viewRoot) {

        // Go to the details page for the selected restaurant
        final Intent i = new Intent(this, DetailActivity.class);

        // Put tracker data on intent
        i.putExtra("DetailActivity_TrackerID", tracker_id);
        i.putExtra("DetailActivity_TrackerName", tracker.getTitleName());
        i.putExtra("DetailActivity_TrackerModel", tracker.getModel());
        i.putExtra("DetailActivity_TrackerColor", tracker.getBackgroundColor());
        i.putExtra("DetailActivity_TrackerLastUpdate", (tracker.getLastUpdate() == null ? "" : String.valueOf(tracker.getLastUpdate().getTime())));

        //Save tracker position on array
        i.putExtra("TrackerPosition", mRecyclerView.getChildAdapterPosition(viewRoot));

        //If device supports shared element transition
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setEnterTransition(new Fade(Fade.IN));
            getWindow().setEnterTransition(new Fade(Fade.OUT));

            //Check if loading animation is still visible
            int transitionID = (viewRoot.findViewById(R.id.loadingBackground).getAlpha() == 0 ? R.id.googleMap : R.id.loadingBackground);

            //Save transition ID on intent
            i.putExtra("DetailActivity_BackgroundTransition", transitionID);

            //Define shared elements to perform transition
            final ActivityOptions options = ActivityOptions
                    .makeSceneTransitionAnimation(this,
                            new Pair<>(viewRoot.findViewById(transitionID),
                                    getString(R.string.transition_drawable)),
                            new Pair<>(viewRoot.findViewById(R.id.imgTracker),
                                    getString(R.string.transition_icon)),
                            new Pair<>(viewRoot.findViewById(R.id.lblItemCount),
                                    getString(R.string.transition_title)),
                            new Pair<>(viewRoot.findViewById(R.id.txtTrackerModel),
                                    getString(R.string.transition_subtitle)));

            //Wait for loading animation to show
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run()
                {
                    //Start activity after 500ms
                    startActivityForResult(i, 0, options.toBundle());
                }
            }, 500);

        }
        else
        {
            //No support to shared transition, start activity without animation
            startActivityForResult(i, 0);
        }
    }


    @Override
    public void OnTrackerEdit(String tracker_id, Tracker tracker, View viewRoot)
    {
        // Go to the details page for the selected restaurant
        final Intent i = new Intent(this, RegisterActivity.class);

        // Put tracker data on intent
        i.putExtra("RegisterActivity_TrackerID", tracker_id);
        i.putExtra("RegisterActivity_TrackerName", tracker.getName());
        i.putExtra("RegisterActivity_TrackerDescription", tracker.getDescription());
        i.putExtra("RegisterActivity_TrackerIdentification", tracker.getIdentification());
        i.putExtra("RegisterActivity_TrackerModel", tracker.getModel());
        i.putExtra("RegisterActivity_TrackerColor", tracker.getBackgroundColor());
        i.putExtra("RegisterActivity_TrackerUpdateInterval", tracker.getUpdateInterval());
        i.putExtra("TrackerPosition", mRecyclerView.getChildAdapterPosition(viewRoot));

        //Start edit activity
        startActivityForResult(i, 0);
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
                //On click, load register activity
                Intent intent = new Intent(getApplicationContext(), RegisterActivity.class);
                startActivityForResult(intent, 0);
                return true;

            case R.id.action_refresh:
                // Refresh items
                mSwipeRefresh.setRefreshing(true);
                mAdapter.setQuery(mQuery);
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
