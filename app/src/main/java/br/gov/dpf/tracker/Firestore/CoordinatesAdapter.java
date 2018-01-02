package br.gov.dpf.tracker.Firestore;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.Query;

import br.gov.dpf.tracker.DetailActivity;
import br.gov.dpf.tracker.Entities.Coordinates;
import br.gov.dpf.tracker.R;

public class CoordinatesAdapter extends BaseAdapter<CoordinatesAdapter.ViewHolder> {
    
    //Define interface to event listener
    public interface OnCoordinatesSelectedListener {

        //Fired when user click on a recycler view item
        void OnCoordinatesSelected(DocumentSnapshot Coordinates, View viewRoot);
    }

    //Linked activity
    public DetailActivity mActivity;

    //Constructor
    public CoordinatesAdapter(DetailActivity activity, Query query) {
        super(activity, query);

        mActivity = activity;
    }

    // Create new views (invoked by the layout manager)
    @Override
    public CoordinatesAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        LayoutInflater inflater = LayoutInflater.from(parent.getContext());

        // Inflate the custom layout
        return new CoordinatesAdapter.ViewHolder(inflater.inflate(R.layout.detail_recyler_item, parent, false));
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(final CoordinatesAdapter.ViewHolder holder, int position) {

        //Get Coordinates using index position
        final Coordinates coordinates = getSnapshot(position).toObject(Coordinates.class);

        // - replace the contents of the view with that element
        holder.txtNumberID.setText(String.valueOf(getItemCount() - position));
        holder.txtDatetime.setText(formatDateTime(coordinates.getDatetime()));
        holder.txtAddress.setText(coordinates.getAddress());

        //Background for coordinates number ID
        Drawable numberBackground;

        //Check coordinates count and current position
        if(getItemCount() == 1)
        {
            //If only one coordinate
            numberBackground = mActivity.getResources().getDrawable(R.drawable.dashed);
        }
        else if(position == 0)
        {
            //If top (most recent) coordinate
            numberBackground = mActivity.getResources().getDrawable(R.drawable.dashed_top);
        }
        else if(position == getItemCount() - 1)
        {
            //If bottom (last) coordinate
            numberBackground = mActivity.getResources().getDrawable(R.drawable.dashed_bottom);
        }
        else
        {
            //Else, coordinates in the middle
            numberBackground = mActivity.getResources().getDrawable(R.drawable.dashed_middle);
        }

        //Change color based on user selection
        numberBackground.setColorFilter(Color.parseColor(mActivity.getIntent().getStringExtra("DetailActivity_TrackerColor")), PorterDuff.Mode.SRC_ATOP);

        //Set background
        holder.txtNumberID.setBackground(numberBackground);

        // Image click listener
        holder.imgInfo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(mActivity, "INFO cliked", Toast.LENGTH_SHORT).show();
            }
        });

        // Click listener
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mActivity != null)
                {
                    mActivity.OnCoordinatesSelected(getSnapshot(holder.getAdapterPosition()), holder.itemView);
                }
            }
        });
    }

    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder
    public static class ViewHolder
            extends RecyclerView.ViewHolder {

        // each data item is just a string in this case
        TextView txtNumberID, txtDatetime, txtAddress;
        ImageView imgInfo;

        ViewHolder(View itemView) {
            // Stores the itemView in a public final member variable that can be used
            // to access the context from any ViewHolder instance.
            super(itemView);

            //Text fields
            txtNumberID = itemView.findViewById(R.id.txtNumberID);
            txtDatetime = itemView.findViewById(R.id.txtDatetime);
            txtAddress = itemView.findViewById(R.id.txtAddress);

            //Image field
            imgInfo = itemView.findViewById(R.id.imgInfo);
        }
    }
}
