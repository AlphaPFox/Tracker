package br.gov.dpf.tracker.Entities;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.firebase.firestore.GeoPoint;

import java.util.Date;

@SuppressWarnings("unused")
public class Tracker implements Parcelable {

    private String mID;

    private String mName;

    private String mDescription;

    private String mIdentification;

    private String mModel;

    private int mBatteryLevel;

    private int mSignalLevel;

    private int mUpdateInterval;

    private Date mLastUpdate;

    private Date mLastCheck;

    private GeoPoint mLastCoordinate;

    private String mLastCoordinateType;

    private String mBackgroundColor;

    //Required by FireStore DB
    public Tracker() {

    }

    public String getID() {
        return mID;
    }

    public void setID(String mID) {
        this.mID = mID;
    }

    public String getName() {
        return mName;
    }

    public String getTitleName() {
        if (mName.length() > 10)
            return mName;
        else
            return "Rastreador: " + mName;
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

    public void setIdentification(String mIdentification) {
        this.mIdentification = mIdentification;
    }

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

    public void setBackgroundColor(String mBackgroundColor) {
        this.mBackgroundColor = mBackgroundColor;
    }

    public int getBatteryLevel() {
        return mBatteryLevel;
    }

    public String getStringBatteryLevel() {
        if (mBatteryLevel != 0)
            return String.valueOf(mBatteryLevel) + "%";
        else
            return "N/D";
    }


    public void setBatteryLevel(int mBatteryLevel) {
        this.mBatteryLevel = mBatteryLevel;
    }

    public int getSignalLevel() {
        return mSignalLevel;
    }

    public String getStringSignalLevel() {
        if (mSignalLevel != 0)
            return String.valueOf(mSignalLevel) + "%";
        else
            return "N/D";
    }

    public String getLastCoordinateType() {
        return mLastCoordinateType;
    }

    public void setLastCoordinateType(String mLastCoordinateType) {
        this.mLastCoordinateType = mLastCoordinateType;
    }

    public void setSignalLevel(int mSignalLevel) {
        this.mSignalLevel = mSignalLevel;
    }

    public Date getLastUpdate() {
        return mLastUpdate;
    }

    public void setLastUpdate(Date mLastUpdate) {
        this.mLastUpdate = mLastUpdate;
    }

    public Date getLastCheck() {
        return mLastCheck;
    }

    public void setLastCheck(Date mLastCheck) {
        this.mLastCheck = mLastCheck;
    }

    public GeoPoint getLastCoordinate() {
        return mLastCoordinate;
    }

    public void setLastCoordinate(GeoPoint mLastCoordinate) {
        this.mLastCoordinate = mLastCoordinate;
    }

    protected Tracker(Parcel in) {
        mID = in.readString();
        mName = in.readString();
        mDescription = in.readString();
        mIdentification = in.readString();
        mModel = in.readString();
        mBatteryLevel = in.readInt();
        mSignalLevel = in.readInt();
        mUpdateInterval = in.readInt();
        long tmpMLastUpdate = in.readLong();
        mLastUpdate = tmpMLastUpdate != -1 ? new Date(tmpMLastUpdate) : null;
        long tmpMLastCheck = in.readLong();
        mLastCheck = tmpMLastCheck != -1 ? new Date(tmpMLastCheck) : null;
        mLastCoordinateType = in.readString();
        mBackgroundColor = in.readString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mID);
        dest.writeString(mName);
        dest.writeString(mDescription);
        dest.writeString(mIdentification);
        dest.writeString(mModel);
        dest.writeInt(mBatteryLevel);
        dest.writeInt(mSignalLevel);
        dest.writeInt(mUpdateInterval);
        dest.writeLong(mLastUpdate != null ? mLastUpdate.getTime() : -1L);
        dest.writeLong(mLastCheck != null ? mLastCheck.getTime() : -1L);
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
}
