package br.gov.dpf.tracker.Entities;

import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.StyleSpan;

import java.util.Map;

import br.gov.dpf.tracker.Components.ProgressNotification;

public class NotificationMessage
{
    private int id, progress;
    private String topic;
    private String groupKey;
    private String title, content, expanded, coordinates;
    private Long datetime;

    public ProgressNotification progressNotification;

    public NotificationMessage(int notificationID, Map<String, String> data, String topic)
    {
        //Get data from FCM message payload
        this.id = notificationID;
        this.topic = topic;
        this.groupKey = data.get("id");
        this.title = data.get("title");
        this.content = data.get("content");
        this.expanded = data.get("expanded");
        this.coordinates = data.get("coordinates");
        this.progress = parseInt(data.get("progress"));
        this.datetime =  parseDatetime(data.get("datetime"));
    }

    public int getNotificationID() { return id; }

    public String getGroupKey() { return groupKey; }

    public String getTitle() { return title; }

    public String getTopic() { return topic; }

    public void setTitle(String value) { title = value; }

    public String getContent() { return content; }

    public void setContent(String value) { content = value; }

    public String getCoordinates() { return coordinates; }

    public String getExpandedContent() { return expanded; }

    public Long getDatetime() { return datetime; }

    public int getProgress() { return progress; }

    public void setProgress(int value) { this.progress = value; }

    public Spannable getStyledMessage()
    {
        //Create styled text
        Spannable sb = new SpannableString(title + ": " + content);

        //Set title bold
        sb.setSpan(new StyleSpan(android.graphics.Typeface.BOLD), 0, title.length() + 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

        //Return styled message
        return sb;
    }

    private int parseInt(String value) {
        try
        {
            //Try parse int value
            return Integer.parseInt(value);
        }
        catch(NumberFormatException nfe)
        {
            // Exception, return
            return 0;
        }
    }

    private long parseDatetime(String value) {
        try
        {
            //Try parse string value
            return Long.valueOf(value);
        }
        catch(NumberFormatException nfe)
        {
            // Log exception.
            return 0;
        }
    }
}
