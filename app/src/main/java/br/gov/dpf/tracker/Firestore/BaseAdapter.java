package br.gov.dpf.tracker.Firestore;

import android.app.Activity;
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

import br.gov.dpf.tracker.R;

public abstract class BaseAdapter<VH extends RecyclerView.ViewHolder>
        extends RecyclerView.Adapter<VH>
        implements EventListener<QuerySnapshot> {

    private static final String TAG = "BaseAdapter";

    private Activity mActivity;
    private Query mQuery;
    private ListenerRegistration mRegistration;

    public ArrayList<DocumentSnapshot> mSnapshots = new ArrayList<>();

    public BaseAdapter(Activity activity, Query query) {

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

    public void startListening() {
        if (mQuery != null && mRegistration == null) {
            mRegistration = mQuery.addSnapshotListener(mActivity, this);
        }
    }

    public void stopListening() {
        if (mRegistration != null) {
            mRegistration.remove();
            mRegistration = null;
        }

        mSnapshots.clear();
        notifyDataSetChanged();
    }

    public void setQuery(Query query) {
        // Stop listening
        stopListening();

        // Clear existing data
        mSnapshots.clear();
        notifyDataSetChanged();

        // Listen to new query
        mQuery = query;
        startListening();
    }

    @Override
    public int getItemCount() {
        return mSnapshots.size();
    }

    DocumentSnapshot getSnapshot(int index) {
        return mSnapshots.get(index);
    }

    protected void onDocumentAdded(DocumentChange change) {
        mSnapshots.add(change.getNewIndex(), change.getDocument());
        notifyDataSetChanged();
    }

    protected void onDocumentModified(DocumentChange change) {
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

    protected void onDocumentRemoved(DocumentChange change) {
        mSnapshots.remove(change.getOldIndex());
        notifyDataSetChanged();
    }

    protected void onError(FirebaseFirestoreException e) {};

    protected void onDataChanged() {}

    public String formatDateTime(Date datetime, boolean short_weekdays)
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
                    dateFormat = new SimpleDateFormat("'Hoje' - HH:mm", Locale.getDefault());
                }
                else if (date1.get(Calendar.DAY_OF_MONTH) - date2.get(Calendar.DAY_OF_MONTH) == 1)
                {
                    dateFormat = new SimpleDateFormat("'Ontem' - HH:mm", Locale.getDefault());
                }
                else if (date1.get(Calendar.DAY_OF_MONTH) - date2.get(Calendar.DAY_OF_MONTH) < 7)
                {
                    if(short_weekdays)
                        dateFormat = new SimpleDateFormat("'"+ mActivity.getResources().getStringArray(R.array.short_weekdays)[date2.get(Calendar.DAY_OF_WEEK) - 1] +"' - HH:mm", Locale.getDefault());
                    else
                        dateFormat = new SimpleDateFormat("'"+ mActivity.getResources().getStringArray(R.array.weekdays)[date2.get(Calendar.DAY_OF_WEEK) - 1] +"' - HH:mm", Locale.getDefault());
                }
                else
                {
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

    public String formatDateTime(Date datetime1, Date datetime2)
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
                return formatDateTime(datetime1, false) + " até " + formatTime(datetime2);
            }
            else
            {
                //Get value from second date time
                String formatted_text = formatDateTime(datetime2, true);

                //If dates in same year
                if(date1.get(Calendar.YEAR) == date2.get(Calendar.YEAR))
                    formatted_text = formatted_text.replace("/" + String.valueOf(date1.get(Calendar.YEAR)), "");

                //Format text to adapt two dates representation
                return formatDateTime(datetime1, true) + " até " + formatted_text.substring(0,1).toLowerCase() + formatted_text.substring(1);
            }
        }
    }

    public String formatTime(Date datetime)
    {
        SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());

        return dateFormat.format(datetime);
    }
}
