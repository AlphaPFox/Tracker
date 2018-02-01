package br.gov.dpf.tracker.Components;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.firebase.firestore.GeoPoint;

public class GeoPointParcelable extends GeoPoint implements Parcelable
{
    public GeoPointParcelable(double v, double v1) {
        super(v, v1);
    }

    protected GeoPointParcelable(Parcel in) {
        super(in.readDouble(), in.readDouble());
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeDouble(getLatitude());
        dest.writeDouble(getLongitude());
    }

    public static final Parcelable.Creator<GeoPointParcelable> CREATOR = new Parcelable.Creator<GeoPointParcelable>() {
        @Override
        public GeoPointParcelable createFromParcel(Parcel in) {
            return new GeoPointParcelable(in);
        }

        @Override
        public GeoPointParcelable[] newArray(int size) {
            return new GeoPointParcelable[size];
        }
    };
}
