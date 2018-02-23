package br.gov.dpf.tracker;

import android.app.Activity;
import android.app.ActivityOptions;
import android.app.SearchManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
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
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.transition.Fade;
import android.util.Pair;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;

import br.gov.dpf.tracker.Components.GridAutoLayoutManager;
import br.gov.dpf.tracker.Entities.Tracker;
import br.gov.dpf.tracker.Firestore.TrackerAdapter;

public class MainActivity
        extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, SwipeRefreshLayout.OnRefreshListener {

    //Components used on recycler view
    public GridAutoLayoutManager mRecyclerLayoutManager;
    private RecyclerView mRecyclerView;
    private View mEmptyView, mLoadingView;
    private TrackerAdapter mAdapter;
    private SwipeRefreshLayout mSwipeRefreshLayout;

    //Left navigation menu component
    private NavigationView navigationView;

    //Search view action bar menu
    private SearchView searchView;

    //Store current Firestore DB instance
    private FirebaseFirestore mFireStoreDB;

    //Get shared preferences
    public SharedPreferences sharedPreferences;

    //Define possible result operations
    public static int RESULT_ERROR = -1;
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

        //Initialize shared preferences
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        //Initialize navigation view
        navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        //Select on menu item representing user preferred map type
        updateNavigationMenu();

        //Get views to replace recycler view while loading or if empty
        mLoadingView = findViewById(R.id.vwLoadingCardView);
        mEmptyView = findViewById(R.id.vwEmptyCardView);

        //Get swipe refresh layout
        mSwipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        mSwipeRefreshLayout.setColorSchemeResources(R.color.colorPrimaryDark, R.color.colorPrimary, R.color.colorAccent);
        mSwipeRefreshLayout.setOnRefreshListener(this);

        //Initialize db instance
        FirebaseFirestore.setLoggingEnabled(true);
        mFireStoreDB = FirebaseFirestore.getInstance();

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

                //Cancel loading animation
                dismissLoading();
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
            startActivity(intent);
            }
        });
    }

    private void dismissLoading()
    {
        //Wait 500 ms before hiding loading indicator
        mSwipeRefreshLayout.postDelayed(new Runnable() {
            @Override
            public void run()
            {
                //Hide loading
                mSwipeRefreshLayout.setRefreshing(false);
            }
        }, 500);
    }

    @Override
    protected void onActivityResult (int requestCode, int resultCode, final Intent intent)
    {
        //Check if it is an activity to select notification sound
        if(requestCode == RingtoneManager.TYPE_NOTIFICATION && resultCode == Activity.RESULT_OK)
        {
            //Get user selection
            Uri uri = intent.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);

            //If valid selection
            if (uri != null)
            {
                //Save user preferences
                sharedPreferences.edit().putString("Notification_Sound", uri.toString()).apply();
            }
        }
        else if (resultCode == RESULT_ERROR)
        {
            // Show error message
            Snackbar.make(findViewById(android.R.id.content), "Erro ao executar operação", Snackbar.LENGTH_LONG).show();
        }
    }

    public void OnTrackerSelected(Tracker tracker, TrackerAdapter.ViewHolder holder) {

        // Go to the details page for the selected restaurant
        final Intent intent = new Intent(this, DetailActivity.class);

        // Put tracker data on intent
        intent.putExtra("Tracker", tracker);

        //If device supports shared element transition
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setEnterTransition(new Fade(Fade.IN));
            getWindow().setEnterTransition(new Fade(Fade.OUT));

            //Check if loading animation is still visible
            int transitionID = (holder.itemView.findViewById(R.id.loadingBackground).getAlpha() == 0 ? R.id.googleMap : R.id.loadingBackground);

            //Save transition ID on intent
            intent.putExtra("DetailActivity_BackgroundTransition", transitionID);

            //Define shared elements to perform transition
            final ActivityOptions options;

            // Get configuration status
            if(holder.lastConfiguration.getVisibility() == View.VISIBLE)
            {
                //Perform transition with configuration layout elements
                options = ActivityOptions
                        .makeSceneTransitionAnimation(this,
                                new Pair<>(holder.itemView.findViewById(transitionID),
                                        getString(R.string.transition_drawable)),
                                new Pair<>((View) holder.imageView,
                                        getString(R.string.transition_icon)),
                                new Pair<>((View) holder.txtTrackerName,
                                        getString(R.string.transition_title)),
                                new Pair<>((View) holder.txtTrackerModel,
                                        getString(R.string.transition_subtitle)),
                                new Pair<>((View) holder.txtConfigDescription,
                                        getString(R.string.transition_config_description)),
                                new Pair<>((View) holder.txtStatus,
                                        getString(R.string.transition_config_status)),
                                new Pair<>((View) holder.imgStatus,
                                        getString(R.string.transition_config_image)));
            }
            else
            {
                //Perform transition without configuration components
                options = ActivityOptions
                        .makeSceneTransitionAnimation(this,
                                new Pair<>(holder.itemView.findViewById(transitionID),
                                        getString(R.string.transition_drawable)),
                                new Pair<>((View) holder.imageView,
                                        getString(R.string.transition_icon)),
                                new Pair<>((View) holder.txtTrackerName,
                                        getString(R.string.transition_title)),
                                new Pair<>((View) holder.txtTrackerModel,
                                        getString(R.string.transition_subtitle)));
            }

            //Start activity with transition elements
            startActivity(intent, options.toBundle());
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

    public void OnTrackerEdit(final Tracker tracker, final TrackerAdapter.ViewHolder holder)
    {
        //Create a new popup menu
        PopupMenu popup = new PopupMenu(this, holder.imgEdit);

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

                switch (item.getItemId())
                {
                    case R.id.action_detail:

                        //Call method to open detail activity
                        OnTrackerSelected(tracker, holder);

                        //End method
                        return true;

                    case R.id.action_default_settings:

                        //Set intent to open DefaultSettingsActivity
                        intent.setClass(MainActivity.this, DefaultSettingsActivity.class);

                        //Start edit activity
                        startActivity(intent);

                        //End method
                        return true;

                    case R.id.action_notification_settings:

                        //Define request intent to update an existing tracker
                        intent.putExtra("UpdateNotifications", true);

                    case R.id.action_tracker_settings:

                        //Set intent to open DefaultSettingsActivity
                        intent.setClass(MainActivity.this, TrackerSettingsActivity.class);

                        //Start edit activity
                        startActivity(intent);

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

    public void OnTrackerFavorite(final Tracker tracker)
    {
        //Get opposite of current preference from user to fix this tracker on top of list
        final boolean favorite = !sharedPreferences.getBoolean("Favorite_" + tracker.getIdentification(), false);

        //Create confirmation dialog
        new AlertDialog.Builder(this, R.style.Theme_AppCompat_Light_Dialog_Alert)
                .setIcon((favorite ? R.drawable.ic_star_grey_24dp : R.drawable.ic_star_border_grey_24dp))
                .setTitle((favorite ? "Adicionar aos favoritos" : "Remover dos favoritos"))
                .setMessage((favorite ? "Deseja fixar este rastreador sempre no topo da lista?" : "Deseja retirar este rastreador do topo da lista?"))
                .setPositiveButton("Sim", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        //Get shared preferences editor
                        SharedPreferences.Editor editor = sharedPreferences.edit();


                        //On star image click, update user preference
                        editor.putBoolean("Favorite_" + tracker.getIdentification(), favorite);

                        //Save preferences change
                        editor.apply();

                        //If tracker is now a favorite
                        if(favorite)
                        {
                            //Scroll back to top
                            mRecyclerView.scrollToPosition(0);
                        }

                        //Update tracker list
                        onRefresh();
                    }
                })
                .setNegativeButton("Não", new DialogInterface.OnClickListener()
                {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
        .show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);

        // Associate searchable configuration with the SearchView
        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);

        // Get search view menu
        searchView = (SearchView) menu.findItem(R.id.menu_search).getActionView();

        // If menu available
        if(searchManager != null)
        {
            //Set search manager
            searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));

            //Set query hint
            searchView.setQueryHint(getString(R.string.menu_search_hint));

            //Set search view actions
            searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                @Override
                public boolean onQueryTextSubmit(String query) {
                    return true;
                }

                @Override
                public boolean onQueryTextChange(String newText)
                {
                    //If search is at least 3 chars
                    if(newText.length() >= 3)
                    {
                        //Create filter with fields to be searched
                        ArrayList<String> filter = new ArrayList<>();

                        //Define fields
                        filter.add("name");
                        filter.add("identification");
                        filter.add("model");

                        //Apply filter
                        mAdapter.applyFilter(filter, newText);
                    }
                    else
                    {
                        //No filter with less than 3 chars
                        mAdapter.removeFilter();
                    }
                    return false;
                }
            });
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public void onRefresh()
    {
        // Signal SwipeRefreshLayout to start the progress indicator
        mSwipeRefreshLayout.setRefreshing(true);

        //Stop current snapshot listener
        mAdapter.stopListening();

        //Disable local cache results
        mAdapter.disablePersistence();

        //Start listening again, but now without cached data
        mAdapter.startListening();
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
                startActivity(intent);

                // End method
                return true;

            case R.id.action_refresh:

                // Start update process
                onRefresh();

                //End method
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void updateNavigationMenu()
    {
        //Select user preferred map type
        switch (sharedPreferences.getInt("UserMapType", GoogleMap.MAP_TYPE_NORMAL))
        {
            case GoogleMap.MAP_TYPE_NORMAL:
                navigationView.setCheckedItem(R.id.map_default);
                break;
            case GoogleMap.MAP_TYPE_SATELLITE:
                navigationView.setCheckedItem(R.id.map_satellite);
                break;
            case GoogleMap.MAP_TYPE_TERRAIN:
                navigationView.setCheckedItem(R.id.map_terrain);
                break;
            case GoogleMap.MAP_TYPE_HYBRID:
                navigationView.setCheckedItem(R.id.map_hybrid);
                break;
        }

        //Get user preference on notification
        boolean notificationEnabled = sharedPreferences.getBoolean("Notification_Enabled", true);

        //Set corresponding menu item
        navigationView.getMenu().findItem(R.id.menu_notifications_disable)
                .setTitle(notificationEnabled ? "Desativar notificações" : "Reativar notificações")
                .setIcon(notificationEnabled ? R.drawable.ic_notifications_off_black_24dp : R.drawable.ic_notifications_black_24dp);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {

        if(item.getGroupId() == R.id.menu_map_layers)
        {
            //Get shared preferences
            SharedPreferences.Editor editor = sharedPreferences.edit();

            //Get selected map type
            switch (item.getItemId())
            {
                case R.id.map_default:
                    editor.putInt("UserMapType", GoogleMap.MAP_TYPE_NORMAL);
                    break;
                case R.id.map_satellite:
                    editor.putInt("UserMapType", GoogleMap.MAP_TYPE_SATELLITE);
                    break;
                case R.id.map_terrain:
                    editor.putInt("UserMapType", GoogleMap.MAP_TYPE_TERRAIN);
                    break;
                case R.id.map_hybrid:
                    editor.putInt("UserMapType", GoogleMap.MAP_TYPE_HYBRID);
                    break;
            }

            //Save user setting
            editor.apply();

            //Update map type on trackers currently displayed
            mAdapter.notifyDataSetChanged();
        }
        else if(item.getGroupId() == R.id.menu_notifications)
        {
            //Get selected map type
            switch (item.getItemId())
            {
                case R.id.menu_notification_sound:

                    //Get notification sound
                    Uri soundURI = Uri.parse(sharedPreferences.getString("Notification_Sound", RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION).toString()));

                    //Create a list of sound options
                    Intent intent = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
                    intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION);
                    intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Escolha o som da notificação");
                    intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, soundURI);

                    //Start external activity to user select notification sound
                    this.startActivityForResult(intent, RingtoneManager.TYPE_NOTIFICATION);
                    break;

                case R.id.menu_notification_vibrate:

                    //Vibrate options
                    final String[] items = {"Desligado","Padrão","Curto","Longo"};

                    //Create alert with options
                    new AlertDialog.Builder(this, R.style.Theme_AppCompat_Light_Dialog_Alert)
                            .setTitle("Opções de vibração")
                            .setSingleChoiceItems(items, sharedPreferences.getInt("Notification_Vibrate", 0), null)
                            .setPositiveButton("Confirmar", new DialogInterface.OnClickListener()
                            {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i)
                                {
                                    //Get user option
                                    int option = ((AlertDialog)dialogInterface).getListView().getCheckedItemPosition();

                                    //Save on shared preferences
                                    sharedPreferences.edit().putInt("Notification_Vibrate", option).apply();
                                }
                            })
                            .setNegativeButton("Cancelar", new DialogInterface.OnClickListener()
                            {
                                @Override
                                public void onClick(DialogInterface dialog, int id)
                                {
                                    //Close dialog
                                    dialog.dismiss();
                                }
                            }).create()
                    .show();
                    break;
                case R.id.menu_notifications_disable:

                    //Get current user preference
                    final boolean enabled = sharedPreferences.getBoolean("Notification_Enabled", true);

                    //Create alert with options
                    new AlertDialog.Builder(this, R.style.Theme_AppCompat_Light_Dialog_Alert)
                            .setTitle((enabled ? "Desativar notificações" : "Reativar notificações"))
                            .setIcon((enabled ? R.drawable.ic_notifications_off_black_24dp : R.drawable.ic_notifications_black_24dp))
                            .setMessage((enabled ? "Deseja desativar todas as notificações deste aplicativo?" : "Deseja reativar as notificações deste aplicativo?"))
                            .setPositiveButton((enabled ? "Desativar" : "Reativar"), new DialogInterface.OnClickListener()
                            {
                                @Override
                                public void onClick(DialogInterface dialog, int id)
                                {
                                    //Save user preference
                                    sharedPreferences.edit().putBoolean("Notification_Enabled", !enabled).apply();

                                    //Update navigation menu
                                    updateNavigationMenu();
                                }
                            })
                            .setNegativeButton("Cancelar", new DialogInterface.OnClickListener()
                            {
                                @Override
                                public void onClick(DialogInterface dialog, int id)
                                {
                                    //Close dialog
                                    dialog.dismiss();
                                }
                            }).create()
                    .show();
                    break;
            }
        }
        else
        {
            // Handle navigation view item clicks here.
            switch (item.getItemId())
            {
                case R.id.menu_add:

                    // Create an intent to open Settings activity
                    Intent intent = new Intent(getApplicationContext(), DefaultSettingsActivity.class);

                    // Define request intent to update an existing tracker
                    intent.putExtra("Request", REQUEST_INSERT);

                    // Start register activity and wait for result
                    startActivity(intent);
                    break;

                case R.id.menu_refresh:

                    // Start update process
                    onRefresh();
                    break;

                case R.id.menu_search:

                    // Request search from this activity
                    searchView.setIconified(false);
                    break;
            }
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
    protected void onResume()
    {
        //If search view on action bar was previously open
        if(searchView != null)
        {
            //Return to original state
            searchView.setQuery("", false);
            searchView.setIconified(true);
        }

        //If navigation menu is already loaded
        if(navigationView != null)
        {
            //Update user preference on map type
            updateNavigationMenu();
        }

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
