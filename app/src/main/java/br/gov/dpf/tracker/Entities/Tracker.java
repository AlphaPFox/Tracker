package br.gov.dpf.tracker.Entities;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.firebase.firestore.GeoPoint;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import br.gov.dpf.tracker.Components.GeoPointParcelable;

@SuppressWarnings("unused")
public class Tracker implements Parcelable {

    private String mName;

    private String mDescription;

    private String mIdentification;

    private String mModel;

    private String mBatteryLevel;

    private String mSignalLevel;

    private String mBackgroundColor;

    private Date mLastUpdate;

    private Map<String, Object> mLastCoordinate;

    private Map<String, Object> mLastConfiguration;

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

    public Date getLastUpdate() {
        return mLastUpdate;
    }

    public void setLastUpdate(Date mLastUpdate) {
        this.mLastUpdate = mLastUpdate;
    }

    public Map<String, Object> getLastCoordinate() { return mLastCoordinate; }

    public void setLastCoordinate(Map<String, Object> mLastCoordinate) { this.mLastCoordinate = mLastCoordinate; }

    public Map<String, Object> getLastConfiguration() { return mLastConfiguration; }

    public void setLastConfiguration(Map<String, Object> mLastConfiguration) { this.mLastConfiguration = mLastConfiguration; }

    protected Tracker(Parcel in) {
        mName = in.readString();
        mDescription = in.readString();
        mIdentification = in.readString();
        mModel = in.readString();
        mBatteryLevel = in.readString();
        mSignalLevel = in.readString();
        mBackgroundColor = in.readString();
        long tmpLastUpdate = in.readLong();
        mLastUpdate = tmpLastUpdate != -1 ? new Date(tmpLastUpdate) : null;
        long tmpLastCoordinate = in.readLong();

        //Check if last coordinate is available
        if(tmpLastCoordinate != -1)
        {
            //Get last coordinate data
            mLastCoordinate = new HashMap<>();
            mLastCoordinate.put("datetime", new Date(tmpLastCoordinate));
            mLastCoordinate.put("type", in.readString());
            mLastCoordinate.put("location", in.readParcelable(GeoPointParcelable.class.getClassLoader()));
        }
        else
        {
            //No coordinates available, read from parcel but ignore data
            in.readString();
            in.readParcelable(GeoPointParcelable.class.getClassLoader());
        }

        //Check if last configuration is available
        String tmpConfigurationStep = in.readString();

        //If configuration data available
        if(!tmpConfigurationStep.isEmpty())
        {
            mLastConfiguration = new HashMap<>();
            mLastConfiguration.put("step", tmpConfigurationStep);
            mLastConfiguration.put("pending", in.readInt());
            mLastConfiguration.put("description", in.readString());
            mLastConfiguration.put("status", in.readString());
            mLastConfiguration.put("progress", in.readString());
            mLastConfiguration.put("datetime", new Date(in.readLong()));
        }
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
        dest.writeLong(mLastUpdate != null ? mLastUpdate.getTime() : -1L);
        dest.writeLong(mLastCoordinate != null ? ((Date) mLastCoordinate.get("datetime")).getTime() : -1L);
        dest.writeString(mLastCoordinate != null ? mLastCoordinate.get("type").toString() : "");
        dest.writeParcelable(mLastCoordinate != null ? new GeoPointParcelable((GeoPoint) mLastCoordinate.get("location")) : null, 0);
        dest.writeString(mLastConfiguration != null ? mLastConfiguration.get("step").toString() : "");
        dest.writeInt(mLastConfiguration != null ? Integer.valueOf(mLastConfiguration.get("pending").toString()) : 0);
        dest.writeString(mLastConfiguration != null ? mLastConfiguration.get("description").toString() : "");
        dest.writeString(mLastConfiguration != null ? mLastConfiguration.get("status").toString() : "");
        dest.writeString(mLastConfiguration != null ? mLastConfiguration.get("progress").toString() : "");
        dest.writeLong(mLastConfiguration != null ? ((Date) mLastConfiguration.get("datetime")).getTime() : -1L);
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
