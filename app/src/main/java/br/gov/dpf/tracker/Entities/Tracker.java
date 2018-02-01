package br.gov.dpf.tracker.Entities;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.firebase.firestore.GeoPoint;

import java.util.Date;

import br.gov.dpf.tracker.Components.GeoPointParcelable;

@SuppressWarnings("unused")
public class Tracker implements Parcelable {

    private String mName;

    private String mDescription;

    private String mIdentification;

    private String mModel;

    private int mBatteryLevel;

    private int mSignalLevel;

    private int mUpdateInterval;

    private Date mLastUpdate;

    private GeoPointParcelable mLastCoordinate;

    private String mLastCoordinateType;

    private String mBackgroundColor;

    //Required by FireStore DB
    public Tracker() {

    }

    public String getName() {
        return mName;
    }

    public void setName(String mName) {
        this.mName = mName;
    }

    public String getDescription() {
        return mDescription;
    }

    public void setDescription(String mDescription) {
        this.mDescription = mDescription;
    }

    public String getIdentification() {
        return mIdentification;
    }

    public void setIdentification(String mIdentification) { this.mIdentification = mIdentification;  }

    public int getUpdateInterval() {
        return mUpdateInterval;
    }

    public void setUpdateInterval(int mUpdateInterval) {
        this.mUpdateInterval = mUpdateInterval;
    }

    public String getModel() {
        return mModel;
    }

    public void setModel(String mModel) {
        this.mModel = mModel;
    }

    public String getBackgroundColor() {
        return mBackgroundColor;
    }

    public void setBackgroundColor(String mBackgroundColor) { this.mBackgroundColor = mBackgroundColor;  }

    public int getBatteryLevel() {
        return mBatteryLevel;
    }

    public void setBatteryLevel(int mBatteryLevel) {
        this.mBatteryLevel = mBatteryLevel;
    }

    public int getSignalLevel() {
        return mSignalLevel;
    }

    public String getLastCoordinateType() {
        return mLastCoordinateType;
    }

    public void setLastCoordinateType(String mLastCoordinateType) { this.mLastCoordinateType = mLastCoordinateType;  }

    public void setSignalLevel(int mSignalLevel) {
        this.mSignalLevel = mSignalLevel;
    }

    public Date getLastUpdate() {
        return mLastUpdate;
    }

    public void setLastUpdate(Date mLastUpdate) {
        this.mLastUpdate = mLastUpdate;
    }

    public GeoPoint getLastCoordinate() {
        return mLastCoordinate;
    }

    public void setLastCoordinate(GeoPoint mLastCoordinate)
    {
        if(mLastCoordinate != null)
            this.mLastCoordinate = new GeoPointParcelable(mLastCoordinate.getLatitude(), mLastCoordinate.getLongitude());
    }

    protected Tracker(Parcel in) {
        mName = in.readString();
        mDescription = in.readString();
        mIdentification = in.readString();
        mModel = in.readString();
        mBatteryLevel = in.readInt();
        mSignalLevel = in.readInt();
        mUpdateInterval = in.readInt();
        long tmpMLastUpdate = in.readLong();
        mLastUpdate = tmpMLastUpdate != -1 ? new Date(tmpMLastUpdate) : null;
        mLastCoordinate = in.readParcelable(GeoPointParcelable.class.getClassLoader());
        mLastCoordinateType = in.readString();
        mBackgroundColor = in.readString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mName);
        dest.writeString(mDescription);
        dest.writeString(mIdentification);
        dest.writeString(mModel);
        dest.writeInt(mBatteryLevel);
        dest.writeInt(mSignalLevel);
        dest.writeInt(mUpdateInterval);
        dest.writeLong(mLastUpdate != null ? mLastUpdate.getTime() : -1L);
        dest.writeParcelable(mLastCoordinate, PARCELABLE_WRITE_RETURN_VALUE);
        dest.writeString(mLastCoordinateType);
        dest.writeString(mBackgroundColor);
    }

    public static final Parcelable.Creator<Tracker> CREATOR = new Parcelable.Creator<Tracker>() {
        @Override
        public Tracker createFromParcel(Parcel in) {
            return new Tracker(in);
        }

        @Override
        public Tracker[] newArray(int size) {
            return new Tracker[size];
        }
    };

    public String formatName() {
        if (mName.length() > 10)
            return mName;
        else
            return "Rastreador: " + mName;
    }

    public String formatSignalLevel() {
        if (mSignalLevel != 0)
            return String.valueOf(mSignalLevel) + "%";
        else
            return "N/D";
    }

    public String formatBatteryLevel() {
        if (mBatteryLevel != 0)
            return String.valueOf(mBatteryLevel) + "%";
        else
            return "N/D";
    }
}
