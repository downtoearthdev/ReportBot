package com.scorchedcode.ReportBot;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.UUID;

public class ReportManager {

    private static ReportManager instance;
    private ArrayList<Report> reports = new ArrayList<>();
    private ArrayList<ReportUser> users = new ArrayList<>();

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

    public ReportUser getReportUser(String id) {
        return users.stream().filter(user -> id.equals(user.getUserID())).findAny().orElse(null);
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
        private ReportUser reportedUser;
        private String reportID;
        private UUID id;
        private HashSet<String> reportingUsers = new HashSet<>();

        public Report(String msgID, String channelID, String reportedUser) {
            this.messageID = msgID;
            this.channelID = channelID;
            this.reportedUser = new ReportUser(reportedUser);
            this.id = UUID.randomUUID();
            reports.add(this);
        }

        public UUID getId() {
            return id;
        }

        public ReportUser getReportedUser() {
            return reportedUser;
        }

        public String getMessageID() {
            return messageID;
        }

        public String getChannelID() {
            return channelID;
        }

        protected void setReportID(String id) {
            reportID = id;
        }

        public HashSet<String> getReportingUsers() {
            return reportingUsers;
        }

        public void addReportingUser(String userID) {
            reportingUsers.add(userID);
            if(reportingUsers.size() > 0 && reportID == null)
                publishReport(this);
        }
    }

    class ReportUser {
        private int bans = 0;
        private int warns = 0;
        private int locks = 0;
        private int deletes = 0;
        private String id;

        ReportUser(String id) {
            this.id = id;
            users.add(this);
        }

        public String getUserID() {
            return id;
        }

        public int getBans() {
            return bans;
        }

        public void addBan() {
            bans++;
        }

        public int getWarns() {
            return warns;
        }

        public void addWarn() {
            warns++;
        }

        public int getLocks() {
            return locks;
        }

        public void addLock() {
            locks++;
        }

        public int getDeletes() {
            return deletes;
        }

        public void addDelete() {
            deletes++;
        }
    }
}
