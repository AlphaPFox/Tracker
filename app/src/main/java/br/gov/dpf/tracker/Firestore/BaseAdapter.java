package br.gov.dpf.tracker.Firestore;

import android.app.Activity;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.util.Log;

import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

import br.gov.dpf.tracker.R;

public abstract class BaseAdapter<VH extends RecyclerView.ViewHolder>
        extends RecyclerView.Adapter<VH>
        implements EventListener<QuerySnapshot> {

    private static final String TAG = "BaseAdapter";

    private Activity mActivity;
    private Query mQuery;
    private ListenerRegistration mRegistration;

    public ArrayList<DocumentSnapshot> mSnapshots = new ArrayList<>();
    private ArrayList<DocumentSnapshot> mSnapshotsFilter;

    BaseAdapter(Activity activity, Query query)
    {
        mQuery = query;
        mActivity = activity;
    }

    @Override
    public void onEvent(QuerySnapshot documentSnapshots, FirebaseFirestoreException e) {
        if (e != null) {
            Log.w(TAG, "onEvent:error", e);
            onError(e);
            return;
        }

        // Dispatch the event
        Log.d(TAG, "onEvent:numChanges:" + documentSnapshots.getDocumentChanges().size());
        for (DocumentChange change : documentSnapshots.getDocumentChanges()) {
            switch (change.getType()) {
                case ADDED:
                    onDocumentAdded(change);
                    break;
                case MODIFIED:
                    onDocumentModified(change);
                    break;
                case REMOVED:
                    onDocumentRemoved(change);
                    break;
            }
        }
        onDataChanged();
    }

    public void startListening()
    {
        if (mQuery != null && mRegistration == null)
        {
            mRegistration = mQuery.addSnapshotListener(mActivity, this);
        }
    }

    public void stopListening() {
        if (mRegistration != null)
        {
            mRegistration.remove();
            mRegistration = null;
        }

        mSnapshotsFilter = null;
        mSnapshots.clear();
    }

    public void setQuery(Query query) {
        // Stop listening
        stopListening();

        // Clear existing data
        notifyDataSetChanged();

        // Listen to new query
        mQuery = query;
        startListening();
    }

    @Override
    public int getItemCount()
    {
        //Return item count from filtered array (if filter available, else return item count from default array)
        return mSnapshotsFilter != null ? mSnapshotsFilter.size() : mSnapshots.size();
    }

    DocumentSnapshot getSnapshot(int index)
    {
        //Return item from filtered array (if filter available, else return item from default array)
        return mSnapshotsFilter != null ? mSnapshotsFilter.get(index) : mSnapshots.get(index);
    }

    public void applyFilter(@NonNull ArrayList<String> fields, String query)
    {
        //Create a filtered result from original array
        mSnapshotsFilter = new ArrayList<>();

        //For each document
        for(DocumentSnapshot snapshot : mSnapshots)
        {
            //Check each filter
            for(String field : fields)
            {
                //If field in document contains value specified in filter
                if(snapshot.get(field).toString().toLowerCase().replaceAll(" ", "").contains(query.toLowerCase().replaceAll(" ", "")))
                {
                    //Add to filtered array
                    mSnapshotsFilter.add(snapshot);
                    break;
                }
            }
        }

        //Notify changes on dataset
        notifyDataSetChanged();
    }

    public void removeFilter()
    {
        //If filter previously available
        if(mSnapshotsFilter != null)
        {
            //Remove filtered array
            mSnapshotsFilter = null;

            //Update dataset
            notifyDataSetChanged();
        }
    }

    private void onDocumentAdded(DocumentChange change)
    {
        mSnapshots.add(change.getNewIndex(), change.getDocument());
        notifyDataSetChanged();
    }

    private void onDocumentModified(DocumentChange change) {
        if (change.getOldIndex() == change.getNewIndex()) {
            // Item changed but remained in same position
            mSnapshots.set(change.getOldIndex(), change.getDocument());
            notifyItemChanged(change.getOldIndex());
        } else {
            // Item changed and changed position
            mSnapshots.remove(change.getOldIndex());
            mSnapshots.add(change.getNewIndex(), change.getDocument());
            notifyItemMoved(change.getOldIndex(), change.getNewIndex());
        }
    }

    private void onDocumentRemoved(DocumentChange change) {
        mSnapshots.remove(change.getOldIndex());
        notifyDataSetChanged();
    }

    protected void onError(FirebaseFirestoreException e) {};

    protected void onDataChanged() {}

    String formatDateTime(Date datetime, boolean short_weekdays, boolean short_format)
    {
        //Check if received any update yet
        if(datetime == null)
        {
            //Default waiting message
            return mActivity.getString(R.string.txtWaitingTitle);
        }
        else
        {
            SimpleDateFormat dateFormat;

            Calendar date1 = Calendar.getInstance();
            date1.setTime(new Date());

            Calendar date2 = Calendar.getInstance();
            date2.setTime(datetime);

            if(date1.get(Calendar.YEAR) == date2.get(Calendar.YEAR) && date1.get(Calendar.MONTH) == date2.get(Calendar.MONTH))
            {
                if(date1.get(Calendar.DAY_OF_MONTH) == date2.get(Calendar.DAY_OF_MONTH))
                {
                    if(short_format)
                        dateFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
                    else
                        dateFormat = new SimpleDateFormat("'Hoje' - HH:mm", Locale.getDefault());
                }
                else if (date1.get(Calendar.DAY_OF_MONTH) - date2.get(Calendar.DAY_OF_MONTH) == 1)
                {
                    if(short_format)
                        return "Ontem";
                    else
                        dateFormat = new SimpleDateFormat("'Ontem' - HH:mm", Locale.getDefault());
                }
                else if (date1.get(Calendar.DAY_OF_MONTH) - date2.get(Calendar.DAY_OF_MONTH) < 7)
                {
                    if(short_format)
                        return mActivity.getResources().getStringArray(R.array.short_weekdays)[date2.get(Calendar.DAY_OF_WEEK) - 1];
                    else if(short_weekdays)
                        dateFormat = new SimpleDateFormat("'"+ mActivity.getResources().getStringArray(R.array.short_weekdays)[date2.get(Calendar.DAY_OF_WEEK) - 1] +"' - HH:mm", Locale.getDefault());
                    else
                        dateFormat = new SimpleDateFormat("'"+ mActivity.getResources().getStringArray(R.array.weekdays)[date2.get(Calendar.DAY_OF_WEEK) - 1] +"' - HH:mm", Locale.getDefault());
                }
                else
                {
                    if(short_format)
                        dateFormat = new SimpleDateFormat("dd/MM", Locale.getDefault());
                    else
                        dateFormat = new SimpleDateFormat("dd/MM/yyyy - HH:mm", Locale.getDefault());
                }
            }
            else
            {
                dateFormat = new SimpleDateFormat("dd/MM/yyyy - HH:mm", Locale.getDefault());
            }

            return dateFormat.format(datetime);
        }
    }

    String formatDateTime(Date datetime1, Date datetime2)
    {
        //Check if received any update yet
        if(datetime1 == null || datetime2 == null)
        {
            //Default waiting message
            return mActivity.getString(R.string.txtWaitingTitle);
        }
        else
        {
            Calendar date1 = Calendar.getInstance();
            date1.setTime(datetime1);

            Calendar date2 = Calendar.getInstance();
            date2.setTime(datetime2);

            //If two dates are in the same day
            if(date1.get(Calendar.DAY_OF_YEAR) == date2.get(Calendar.DAY_OF_YEAR) && date1.get(Calendar.YEAR) == date2.get(Calendar.YEAR))
            {
                //Use default value
                return formatDateTime(datetime1, false, false) + " até " + new SimpleDateFormat("HH:mm", Locale.getDefault()).format(datetime2);
            }
            else
            {
                //Get value from second date time
                String formatted_text = formatDateTime(datetime2, true, false);

                //If dates in same year
                if(date1.get(Calendar.YEAR) == date2.get(Calendar.YEAR))
                    formatted_text = formatted_text.replace("/" + String.valueOf(date1.get(Calendar.YEAR)), "");

                //Format text to adapt two dates representation
                return formatDateTime(datetime1, true, false) + " até " + formatted_text.substring(0,1).toLowerCase() + formatted_text.substring(1);
            }
        }
    }
}
