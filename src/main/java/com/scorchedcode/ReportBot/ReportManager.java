package com.scorchedcode.ReportBot;

import java.util.ArrayList;

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

    private void makeReport(String msgID) {

    }

    private Report getReport(String msgID) {
        for(Report report : reports) {
            if(report.getMessageID().equals(msgID))
                return report;
        }
        return new Report(msgID);
    }






    class Report {
        private String messageID;
        private ArrayList<String> reportingUsers = new ArrayList<>();
        private int timesreported = 0;

        public Report(String msgID) {
            this.messageID = msgID;
        }

        public String getMessageID() {
            return messageID;
        }

        public void addReportingUser(String userID) {
            reportingUsers.add(userID);
        }
    }
}
