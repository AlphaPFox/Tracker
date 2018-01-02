package br.gov.dpf.tracker;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.PorterDuffXfermode;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatTextView;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.transition.Transition;
import android.transition.TransitionListenerAdapter;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Dot;
import com.google.android.gms.maps.model.Gap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PatternItem;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.Query;
import com.google.maps.android.ui.IconGenerator;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;
import com.sothree.slidinguppanel.SlidingUpPanelLayout.PanelSlideListener;
import com.sothree.slidinguppanel.SlidingUpPanelLayout.PanelState;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import br.gov.dpf.tracker.Entities.Coordinates;
import br.gov.dpf.tracker.Entities.Tracker;
import br.gov.dpf.tracker.Firestore.CoordinatesAdapter;
import br.gov.dpf.tracker.Firestore.TrackerAdapter;
import de.hdodenhof.circleimageview.CircleImageView;

public class DetailActivity extends AppCompatActivity implements OnMapReadyCallback, CoordinatesAdapter.OnCoordinatesSelectedListener {

    private static final String TAG = "DetailActivity";

    private SlidingUpPanelLayout mLayout;

    private GoogleMap mMap;

    private LinearLayout mLoadingBackground;
    private RecyclerView mRecyclerView;

    private FirebaseFirestore mFireStoreDB;
    private CoordinatesAdapter mAdapter;
    private Query mQuery;

    private List<LatLng> positions;

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
        setContentView(R.layout.activity_detail);

        final Toolbar mToolbar = findViewById(R.id.main_toolbar);
        Intent i = getIntent();

        setSupportActionBar(mToolbar);

        getSupportActionBar().setDisplayShowTitleEnabled(false);

        mLayout = findViewById(R.id.sliding_layout);

        //Initialize db instance
        mFireStoreDB = FirebaseFirestore.getInstance();

        //Define db search query
        mQuery = mFireStoreDB.collection("Tracker/" + i.getStringExtra("DetailActivity_TrackerID") + "/Coordinates").orderBy("datetime", Query.Direction.DESCENDING);

        //if extra is not null
        if (i.hasExtra("DetailActivity_TrackerID"))
        {
            CircleImageView imgToolbarIcon = mToolbar.findViewById(R.id.imgToolbarIcon);
            TextView txtToolbarTitle = mToolbar.findViewById(R.id.txtToolbarTitle);
            TextView txtToolbarSubtitle = mToolbar.findViewById(R.id.txtToolbarSubtitle);
            ProgressBar progressBar = findViewById(R.id.progressBar);
            mLoadingBackground = findViewById(R.id.loadingBackground);

            imgToolbarIcon.setCircleBackgroundColor(Color.parseColor(i.getStringExtra("DetailActivity_TrackerColor")));
            progressBar.getIndeterminateDrawable().setColorFilter(imgToolbarIcon.getCircleBackgroundColor(), android.graphics.PorterDuff.Mode.SRC_IN);

            txtToolbarTitle.setText(i.getStringExtra("DetailActivity_TrackerName"));
            txtToolbarSubtitle.setText(i.getStringExtra("DetailActivity_TrackerModel"));

            //Check if device supports shared element transition
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
            {
                //Check if loading animation was visible on previous activity (MainActivity)
                if(i.getIntExtra("DetailActivity_BackgroundTransition", R.id.loadingBackground) == R.id.loadingBackground)
                {
                    //Perform transition with loading animation on this activity
                    mLoadingBackground.setTransitionName(getString(R.string.transition_drawable));
                }
                else
                {
                    //Else, hide loading animation
                    mLoadingBackground.setVisibility(View.GONE);

                    //Perform transition with map component
                    findViewById(R.id.map).setTransitionName(getString(R.string.transition_drawable));
                }

                //Components in toolbar for transition
                imgToolbarIcon.setTransitionName(getString(R.string.transition_icon));
                txtToolbarTitle.setTransitionName(getString(R.string.transition_title));
                txtToolbarSubtitle.setTransitionName(getString(R.string.transition_subtitle));
            }
        }

        // RecyclerView
        mAdapter = new CoordinatesAdapter(this, mQuery) {

            @Override
            protected void onDataChanged()
            {
                float scale = getApplicationContext().getResources().getDisplayMetrics().density;
                mMap.clear();

                //Get how many items in recycler view
                if(getItemCount() == 0)
                {
                    //No items to display, hide recycler view
                    mRecyclerView.setVisibility(View.GONE);

                    //Show empty message layout
                    findViewById(R.id.vwEmptyRecycler).setVisibility(View.VISIBLE);

                    //Set corresponding panel height
                    mLayout.setPanelHeight(getResources().getDimensionPixelSize(R.dimen.panel_empty_height));
                }
                else
                {
                    //Items available to display, show recycler view
                    mRecyclerView.setVisibility(View.VISIBLE);

                    //Hide empty message
                    findViewById(R.id.vwEmptyRecycler).setVisibility(View.GONE);

                    //Check how many items available
                    if(getItemCount() == 1)
                    {
                        //If only one item, set panel height to show it
                        mLayout.setPanelHeight(getResources().getDimensionPixelSize(R.dimen.panel_single_row_height));
                    }
                    else
                    {
                        //Else, show the first two items
                        mLayout.setPanelHeight(getResources().getDimensionPixelSize(R.dimen.panel_multiple_row_height));
                    }
                    positions = new ArrayList<>();

                    IconGenerator iconFactory = new IconGenerator(getApplicationContext());
                    iconFactory.setColor(Color.parseColor(getIntent().getStringExtra("DetailActivity_TrackerColor")));
                    iconFactory.setTextAppearance(R.style.Marker);

                    for(DocumentSnapshot snapshot : mSnapshots)
                    {
                        Coordinates coordinates = snapshot.toObject(Coordinates.class);

                        LatLng position = new LatLng(coordinates.getPosition().getLatitude(), coordinates.getPosition().getLongitude());

                        positions.add(position);

                        MarkerOptions markerOptions = new MarkerOptions().
                                icon(BitmapDescriptorFactory.fromBitmap(iconFactory.makeIcon(String.valueOf(mSnapshots.size() - positions.size() + 1)))).
                                position(position).
                                anchor(iconFactory.getAnchorU(), iconFactory.getAnchorV());

                        mMap.addMarker(markerOptions);
                    }

                    //Center map on most recent available position
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(positions.get(0), 14));

                    // Add a polyline to the map.
                    mMap.addPolyline((new PolylineOptions())
                            .width(5)
                            .addAll(positions)
                            .color(Color.parseColor(getIntent().getStringExtra("DetailActivity_TrackerColor"))))
                            .setPattern(Arrays.asList(new Dot(), new Gap(5)));

                    mMap.setOnMapLoadedCallback(new GoogleMap.OnMapLoadedCallback() {
                        @Override
                        public void onMapLoaded()
                        {
                            mLoadingBackground.animate().setDuration(500).alpha(0f);
                        }
                    });
                }
            }

            @Override
            protected void onError(FirebaseFirestoreException e) {

                // Show a snack bar on errors
                Snackbar.make(findViewById(android.R.id.content),
                        "Error: check logs for info.", Snackbar.LENGTH_LONG).show();
            }
        };

        // Lookup the recycler view in activity layout
        mRecyclerView = findViewById(R.id.DetailRecycler);

        // Attach the adapter to the recycler view to populate items
        mRecyclerView.setAdapter(mAdapter);

        // Set linear layout for orientation
        LinearLayoutManager layoutManager = new LinearLayoutManager(mRecyclerView.getContext());

        // Set layout manager to position the items
        mRecyclerView.setLayoutManager(layoutManager);

        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(mRecyclerView.getContext(), layoutManager.getOrientation());

        mRecyclerView.addItemDecoration(dividerItemDecoration);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }


    @Override
    public void OnCoordinatesSelected(DocumentSnapshot coordinates, View viewRoot) {

        // Go to the details page for the selected restaurant
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(positions.get(mAdapter.mSnapshots.indexOf(coordinates)), 14));
        mLayout.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED);

    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.detail, menu);
        MenuItem item = menu.findItem(R.id.action_toggle);
        if (mLayout != null) {
            if (mLayout.getPanelState() == PanelState.HIDDEN) {
                item.setTitle(R.string.action_show);
            } else {
                item.setTitle(R.string.action_hide);
            }
        }
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case R.id.action_toggle: {
                if (mLayout != null) {
                    if (mLayout.getPanelState() != PanelState.HIDDEN) {
                        mLayout.setPanelState(PanelState.HIDDEN);
                        item.setTitle(R.string.action_show);
                    } else {
                        mLayout.setPanelState(PanelState.COLLAPSED);
                        item.setTitle(R.string.action_hide);
                    }
                }
                return true;
            }
            case R.id.action_anchor: {
                if (mLayout != null) {
                    if (mLayout.getAnchorPoint() == 1.0f) {
                        mLayout.setAnchorPoint(0.5f);
                        mLayout.setPanelState(PanelState.ANCHORED);
                        item.setTitle(R.string.action_anchor_disable);
                    } else {
                        mLayout.setAnchorPoint(1.0f);
                        mLayout.setPanelState(PanelState.COLLAPSED);
                        item.setTitle(R.string.action_anchor_enable);
                    }
                }
                return true;
            }
            case R.id.action_add:
            {
                Coordinates coordinate = new Coordinates();

                coordinate.setPosition(new GeoPoint(-19.885762 + Math.random() * 0.1, -43.935820 + Math.random() * 0.1));
                coordinate.setAddress("Av. Mato Grosso, 1215, Teste, Av. Mato grosso, Naviraí/MS");
                coordinate.setDatetime(new Date((long) (System.currentTimeMillis() + Math.random() * 10000) ));

                mFireStoreDB.collection("Tracker/" + getIntent().getStringExtra("DetailActivity_TrackerID") + "/Coordinates")
                        .add(coordinate)
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

                mFireStoreDB.document("Tracker/" + getIntent().getStringExtra("DetailActivity_TrackerID"))
                        .update("lastCoordinate", coordinate.getPosition(),"lastUpdate", coordinate.getDatetime());
            }
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        if (mLayout != null &&
                (mLayout.getPanelState() == PanelState.EXPANDED)) {
            mLayout.setPanelState(PanelState.COLLAPSED);
        } else {
            setResult(RESULT_OK, getIntent());
            finish();
        }
    }
}
