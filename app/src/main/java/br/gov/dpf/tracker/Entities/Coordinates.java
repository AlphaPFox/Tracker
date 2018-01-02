package br.gov.dpf.tracker.Entities;

import com.google.firebase.firestore.GeoPoint;

import java.util.Date;

public class Coordinates
{
    private GeoPoint mPosition;

    private String mAddress;

    private String mBatteryLevel;

    private String mSignalLevel;

    private Date mDatetime;

    private String mSpeed;

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

    public String getSpeed() {
        return mSpeed;
    }

    public void setSpeed(String mSpeed) {
        this.mSpeed = mSpeed;
    }
}
