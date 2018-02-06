package br.gov.dpf.tracker.Entities;

import android.graphics.Color;
import android.view.View;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class Configuration
{
    private String mName;

    private String mValue;

    private Map<String, Object> mStatus;

    private boolean mEnabled;

    public Configuration()
    {
    }

    public Configuration(String name, String value, boolean enabled)
    {
        mName = name;
        mValue = value;
        mEnabled = enabled;
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
}
