package br.gov.dpf.tracker.Entities;

import com.google.firebase.firestore.GeoPoint;

import java.util.Date;

public class Coordinates
{
    private GeoPoint mPosition;

    private String mCellID;

    private String mAddress;

    private int mBatteryLevel;

    private int mSignalLevel;

    private float mSpeed;

    private Date mDatetime;

    private Date mLastDatetime;

    //Required by FireStore DB
    public Coordinates()
    {

    }

    public GeoPoint getPosition() {
        return mPosition;
    }

    public void setPosition(GeoPoint mPosition) {
        this.mPosition = mPosition;
    }

    public String getAddress() {
        return mAddress;
    }

    public void setAddress(String mAddress) {
        this.mAddress = mAddress;
    }

    public int getBatteryLevel() {

        return mBatteryLevel;
    }

    public void setBatteryLevel(int mBatteryLevel) {

        this.mBatteryLevel = mBatteryLevel;
    }

    public int getSignalLevel() {

        return mSignalLevel;
    }

    public void setSignalLevel(int mSignalLevel) {

        this.mSignalLevel = mSignalLevel;
    }

    public Date getDatetime() {
        return mDatetime;
    }

    public void setDatetime(Date mDatetime) {

        this.mDatetime = mDatetime;
    }

    public Date getLastDatetime() {
        return mLastDatetime;
    }

    public void setLastDatetime(Date mLastDatetime) {
        this.mLastDatetime = mLastDatetime;
    }

    public float getSpeed() {

        return mSpeed;
    }

    public void setSpeed(float mSpeed) {

        this.mSpeed = mSpeed;
    }

    public String getCellID() {
        return mCellID;
    }

    public void setCellID(String mCellID) {
        this.mCellID = mCellID;
    }
}
