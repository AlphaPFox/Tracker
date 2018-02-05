package br.gov.dpf.tracker.Entities;

import java.util.Date;

public class Configuration
{
    private String mCommand;

    private String mCommandType;

    private String mStatus;

    private Date mDatetime;

    public static String COMMAND_TYPE_SMS = "SMS";
    public static String TCP_COMMAND = "TCP";

    public Configuration()
    {
    }

    public Configuration(String command, String commandType) {
        this.mCommand = command;
        this.mCommandType = commandType;
        this.mStatus = "REQUESTED";
        this.mDatetime = new Date();
    }

    public String getCommand() {
        return mCommand;
    }

    public String getCommandType() {
        return mCommandType;
    }

    public String getStatus() {
        return mStatus;
    }

    public Date getDatetime() {
        return mDatetime;
    }
}
