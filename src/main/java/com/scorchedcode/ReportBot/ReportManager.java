package com.scorchedcode.ReportBot;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.UUID;

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

    public void makeReport(String msgID, String channelID, String reportedUser, String userID) {
        getReport(msgID, channelID, reportedUser).addReportingUser(userID);
    }

    public Report getReport(UUID id) {
        for(Report report : reports) {
            if(report.getId().equals(id))
                    return report;
        }
        return null;
    }

    private Report getReport(String msgID, String channelID, String reportedUser) {
        for(Report report : reports) {
            if(report.getMessageID().equals(msgID))
                return report;
        }
        return new Report(msgID, channelID, reportedUser);
    }

    private void publishReport(Report report) {
        ReportBot.getInstance().generateReportAesthetic(report);
    }






    class Report {
        private String messageID;
        private String channelID;
        private String reportedUser;
        private UUID id;
        private HashSet<String> reportingUsers = new HashSet<String>();

        public Report(String msgID, String channelID, String reportedUser) {
            this.messageID = msgID;
            this.channelID = channelID;
            this.reportedUser = reportedUser;
            this.id = UUID.randomUUID();
            reports.add(this);
        }

        public UUID getId() {
            return id;
        }

        public String getReportedUser() {
            return reportedUser;
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
