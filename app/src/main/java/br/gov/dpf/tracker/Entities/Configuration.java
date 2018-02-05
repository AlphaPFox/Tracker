package br.gov.dpf.tracker.Entities;

import java.util.Date;

public class Configuration
{
    private String mName;

    private String mValue;

    private String mStatus;

    private boolean mEnabled;

    private Date mDatetime;

    public Configuration()
    {
    }

    public Configuration(String name, String value, boolean enabled)
    {
        mName = name;
        mValue = value;
        mEnabled = enabled;
    }

    public String getName() {
        return mName;
    }

    public void setName(String mName) {
        this.mName = mName;
    }

    public String getValue() {
        return mValue;
    }

    public void setValue(String mValue) {
        this.mValue = mValue;
    }

    public String getStatus() {
        return mStatus;
    }

    public void setStatus(String mStatus) {
        this.mStatus = mStatus;
    }

    public boolean isEnabled() {
        return mEnabled;
    }

    public void setEnabled(boolean mEnabled) {
        this.mEnabled = mEnabled;
    }

    public Date getDatetime() {
        return mDatetime;
    }

    public void setDatetime(Date mDatetime) {
        this.mDatetime = mDatetime;
    }
}
