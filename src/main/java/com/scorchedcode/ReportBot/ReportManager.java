package com.scorchedcode.ReportBot;

import java.util.ArrayList;
import java.util.HashSet;

public class ReportManager {

    private static ReportManager instance;
    private ArrayList<Report> reports = new ArrayList<>();

    private ReportManager() {

    }


    public static ReportManager getInstance() {
        if(instance == null)
            instance = new ReportManager();
        return instance;
    }

    public void makeReport(String msgID, String channelID, String userID) {
        getReport(msgID, channelID).addReportingUser(userID);
    }

    private Report getReport(String msgID, String channelID) {
        for(Report report : reports) {
            if(report.getMessageID().equals(msgID))
                return report;
        }
        return new Report(msgID, channelID);
    }

    private void publishReport(Report report) {
        ReportBot.getInstance().generateReportAesthetic(report);
    }






    class Report {
        private String messageID;
        private String channelID;
        private HashSet<String> reportingUsers = new HashSet<String>();

        public Report(String msgID, String channelID) {
            this.messageID = msgID;
            this.channelID = channelID;
        }

        public String getMessageID() {
            return messageID;
        }

        public String getChannelID() {
            return channelID;
        }

        public HashSet<String> getReportingUsers() {
            return reportingUsers;
        }

        public void addReportingUser(String userID) {
            reportingUsers.add(userID);
            if(reportingUsers.size() > 0)
                publishReport(this);
        }
    }
}
