package br.gov.dpf.tracker;

import android.app.ActivityOptions;
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
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.transition.Fade;
import android.util.Log;
import android.util.Pair;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;

import br.gov.dpf.tracker.Components.GridAutoLayoutManager;
import br.gov.dpf.tracker.Entities.Tracker;
import br.gov.dpf.tracker.Firestore.TrackerAdapter;

public class MainActivity
        extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, TrackerAdapter.OnTrackerSelectedListener {

    private ActionBarDrawerToggle mDrawerToggle;
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

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        mDrawerToggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);

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
        mQuery = mFireStoreDB.collection("Tracker");

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
            if(intent.hasExtra("DetailActivity_TrackerPosition"))
            {
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mRecyclerView.scrollToPosition(intent.getIntExtra("DetailActivity_TrackerPosition", 0));
                    }
                }, 200);
            }
            else
            {
                //Load new tracker
                Tracker tracker = new Tracker(intent.getStringExtra("TrackerName"), intent.getStringExtra("TrackerModel"), intent.getStringExtra("TrackerPhone"), "N/D", "N/D", intent.getStringExtra("TrackerColor"));

                // Add a new document with a generated ID
                mFireStoreDB.collection("Tracker")
                        .add(tracker)
                        .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                            @Override
                            public void onSuccess(DocumentReference documentReference) {
                                Log.d("LOG", "DocumentSnapshot added with ID: " + documentReference.getId());
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Log.w("LOG", "Error adding document", e);
                            }
                        });
            }
        }
    }

    @Override
    public void OnTrackerSelected(String tracker_id, Tracker tracker, View viewRoot) {

        // Go to the details page for the selected restaurant
        final Intent i = new Intent(this, DetailActivity.class);

        // Put tracker data on intent
        i.putExtra("DetailActivity_TrackerID", tracker_id);
        i.putExtra("DetailActivity_TrackerName", "Rastreador: " + tracker.getName());
        i.putExtra("DetailActivity_TrackerModel", tracker.getModel());
        i.putExtra("DetailActivity_TrackerColor", tracker.getBackgroundColor());
        i.putExtra("DetailActivity_TrackerPosition", mRecyclerView.getChildAdapterPosition(viewRoot));
        i.putExtra("DetailActivity_TrackerLastUpdate", (tracker.getLastUpdate() == null ? "" : String.valueOf(tracker.getLastUpdate().getTime())));

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
    public boolean onNavigationItemSelected(MenuItem item) {
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

}
