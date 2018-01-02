package br.gov.dpf.tracker.Entities;

import com.google.firebase.firestore.GeoPoint;

import java.util.Date;

public class Tracker {

    private String mName;

    private String mPhoneNumber;

    private String mModel;

    private String mBatteryLevel;

    private String mSignalLevel;

    private Date mLastUpdate;

    private GeoPoint mLastCoordinate;

    private String mBackgroundColor;

    //Required by FireStore DB
    public Tracker()
    {

    }

    public Tracker(String mName, String mModel, String mPhoneNumber, String mBatteryLevel, String mSignalLevel, String mBackgroundColor) {
        this.mName = mName;
        this.mModel = mModel;
        this.mPhoneNumber = mPhoneNumber;
        this.mBatteryLevel = mBatteryLevel;
        this.mSignalLevel = mSignalLevel;
        this.mBackgroundColor = mBackgroundColor;
    }

    public String getName() {
        return mName;
    }

    public void setName(String mName) {
        this.mName = mName;
    }

    public String getPhoneNumber() {
        return mPhoneNumber;
    }

    public void setPhoneNumber(String mPhoneNumber) {
        this.mPhoneNumber = mPhoneNumber;
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

    public String getBatteryLevel() {
        return mBatteryLevel;
    }

    public void setBatteryLevel(String mBatteryLevel) {
        this.mBatteryLevel = mBatteryLevel;
    }

    public String getSignalLevel() {
        return mSignalLevel;
    }

    public void setSignalLevel(String mSignalLevel) {
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

    public void setLastCoordinate(GeoPoint mLastCoordinate) {
        this.mLastCoordinate = mLastCoordinate;
    }
}
