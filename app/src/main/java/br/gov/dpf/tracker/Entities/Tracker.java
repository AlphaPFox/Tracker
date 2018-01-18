package br.gov.dpf.tracker.Entities;

import com.google.firebase.firestore.GeoPoint;

import java.util.Date;

public class Tracker {

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
    public Tracker()
    {

    }

    public Tracker(String mName, String mDescription, String mModel, String mIdentification, int mUpdateInterval, String mBackgroundColor) {
        this.mName = mName;
        this.mModel = mModel;
        this.mDescription = mDescription;
        this.mIdentification = mIdentification;
        this.mBackgroundColor = mBackgroundColor;
        this.mLastUpdate = new Date();
        this.mUpdateInterval = mUpdateInterval;
    }

    public String getName() {
        return mName;
    }

    public String getTitleName()
    {
        if(mName.length() > 10)
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

    public int getUpdateInterval()
    {
        return mUpdateInterval;
    }

    public void setUpdateInterval(int mUpdateInterval)
    {
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
}
