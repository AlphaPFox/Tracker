package br.gov.dpf.tracker.Entities;

import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.StyleSpan;

import java.util.Map;

public class NotificationMessage
{
    private int id;
    private String groupKey;
    private String title, content, expanded, coordinates;
    private Long datetime;

    public NotificationMessage(int notificationID, Map<String, String> data)
    {
        //Get data from FCM message payload
        this.id = notificationID;
        this.groupKey = data.get("id");
        this.title = data.get("title");
        this.content = data.get("content");
        this.expanded = data.get("expanded");
        this.coordinates = data.get("coordinates");
        this.datetime =  Long.valueOf(data.get("datetime"));
    }

    public int getNotificationID() { return id; }

    public String getGroupKey() { return groupKey; }

    public String getTitle() { return title; }

    public String getContent() { return content; }

    public String getCoordinates() { return coordinates; }

    public String getExpandedContent() { return expanded; }

    public Long getDatetime() { return datetime; }

    public Spannable getStyledMessage()
    {
        //Create styled text
        Spannable sb = new SpannableString(title + ": " + content);

        //Set title bold
        sb.setSpan(new StyleSpan(android.graphics.Typeface.BOLD), 0, title.length() + 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

        //Return styled message
        return sb;
    }
}
