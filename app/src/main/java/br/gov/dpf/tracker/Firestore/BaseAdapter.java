package br.gov.dpf.tracker.Firestore;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.widget.ImageView;

import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import br.gov.dpf.tracker.R;

import static com.google.firebase.firestore.FieldValue.delete;

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

    public String formatDateTime(Date datetime)
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

    public String formatTime(Date datetime)
    {
        SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());

        return dateFormat.format(datetime);
    }
}
