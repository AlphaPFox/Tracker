package br.gov.dpf.tracker.Entities;

import com.google.firebase.firestore.GeoPoint;

import java.util.Date;

public class Coordinates
{
    private GeoPoint mPosition;

    private String mCellID;

    private String mAddress;

    private String mBatteryLevel;

    private String mSignalLevel;

    private String mSpeed;

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

    public String getSpeed() {

        return mSpeed;
    }

    public void setSpeed(String mSpeed) {

        this.mSpeed = mSpeed;
    }

    public String getCellID() {
        return mCellID;
    }

    public void setCellID(String mCellID) {
        this.mCellID = mCellID;
    }

    public String getStringBatteryLevel()
    {
        if(mBatteryLevel != null)
            return mBatteryLevel;
        else
            return "N/D";
    }
    public String getStringSignalLevel()
    {
        if(mSignalLevel != null)
            return mSignalLevel;
        else
            return "N/D";
    }
}
