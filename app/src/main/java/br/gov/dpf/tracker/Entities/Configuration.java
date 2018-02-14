package br.gov.dpf.tracker.Entities;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class Configuration
{
    //Define config execution priority
    public static int PRIORITY_MAX = 5;
    public static int PRIORITY_HIGH = 4;
    public static int PRIORITY_MEDIUM = 3;
    public static int PRIORITY_DEFAULT = 2;
    public static int PRIORITY_LOW = 1;
    public static int PRIORITY_MIN = 0;


    private String mName;

    private String mDescription;

    private String mValue;

    private Map<String, Object> mStatus;

    private boolean mEnabled;

    private int mPriority;

    public Configuration()
    {
    }

    public Configuration(String name, String description, String value, boolean enabled, int priority)
    {
        mName = name;
        mValue = value;
        mEnabled = enabled;
        mPriority = priority;
        mDescription = description;

        mStatus = new HashMap<>();
        mStatus.put("step", "REQUESTED");
        mStatus.put("description", "Status: Aguardando envio...");
        mStatus.put("datetime", new Date());
        mStatus.put("completed", false);
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

    public Map<String, Object> getStatus() {
        return mStatus;
    }

    public void setStatus(Map<String, Object> mStatus) {
        this.mStatus = mStatus;
    }

    public boolean isEnabled() {
        return mEnabled;
    }

    public void setEnabled(boolean mEnabled) {
        this.mEnabled = mEnabled;
    }

    public int getPriority() {
        return mPriority;
    }

    public void setPriority(int mPriority) {
        this.mPriority = mPriority;
    }

    public String getDescription() {
        return mDescription;
    }

    public void setDescription(String mDescription) {
        this.mDescription = mDescription;
    }
}
