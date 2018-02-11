package br.gov.dpf.tracker.Entities;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.Map;

@SuppressWarnings("unused")
public class Tracker implements Parcelable {

    private String mName;

    private String mDescription;

    private String mIdentification;

    private String mModel;

    private String mBatteryLevel;

    private String mSignalLevel;

    private String mBackgroundColor;

    private Map<String, Object> mLastUpdate;

    private Map<String, Object> mLastCoordinate;

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

    public String getBatteryLevel()
    {
        if(mBatteryLevel == null)
            return "N/D";
        else
            return mBatteryLevel;
    }

    public void setBatteryLevel(String mBatteryLevel) { this.mBatteryLevel = mBatteryLevel; }

    public String getSignalLevel()
    {
        if(mSignalLevel == null)
            return "N/D";
        else
            return mSignalLevel;
    }

    public void setSignalLevel(String mSignalLevel) { this.mSignalLevel = mSignalLevel; }

    public Map<String, Object> getLastCoordinate() { return mLastCoordinate; }

    public void setLastCoordinate(Map<String, Object> mLastUpdate) { this.mLastCoordinate = mLastUpdate; }

    protected Tracker(Parcel in) {
        mName = in.readString();
        mDescription = in.readString();
        mIdentification = in.readString();
        mModel = in.readString();
        mBatteryLevel = in.readString();
        mSignalLevel = in.readString();
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
        dest.writeString(mBatteryLevel);
        dest.writeString(mSignalLevel);
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

    public String formatTrackerModel()
    {
        switch (mModel)
        {
            case "tk102b":
                return "TK 102B";
            case "spot":
                return "SPOT Trace";
            case "pt39":
                return "PT-39";
            case "st940":
                return "ST-940";
            case "gt02":
                return "GT-02";
            default:
                return "Modelo desconhecido";
        }
    }
}
