package com.scorchedcode.ReportBot;

import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class ReportManager {

    private static ReportManager instance;
    private ArrayList<Report> reports = new ArrayList<>();
    //private ArrayList<ReportUser> users = new ArrayList<>();

    private ReportManager() {

    }


    public static ReportManager getInstance() {
        if(instance == null)
            instance = new ReportManager();
        return instance;
    }

    public void loadFromPersistance(UUID id, String postID, String msgID, String chanID, String modID, String reportedID, ReportAction action, String reason) {
       new Report(id, postID, msgID, chanID, modID, reportedID, action, reason);
    }

    public void makeReport(String msgID, String channelID, String reportedUser, String userID) {
        getReport(msgID, channelID, reportedUser).addReportingUser(userID);
    }

    public ArrayList<Integer> getReportCounts(String userID) {
        ArrayList<Integer> countList = new ArrayList<>();
        List<Report> userOwnReports = new ArrayList<Report>();
        Collections.addAll(userOwnReports, reports.stream().filter(own -> own.getReportedUser().equals(userID)).toArray(Report[]::new));
        countList.add(userOwnReports.stream().filter(warn -> warn.getResultAction() == ReportAction.WARN).toArray().length);
        countList.add(userOwnReports.stream().filter(lock -> lock.getResultAction() == ReportAction.LOCK).toArray().length);
        countList.add(userOwnReports.stream().filter(delete -> delete.getResultAction() == ReportAction.DELETE).toArray().length);
        countList.add(userOwnReports.stream().filter(ban -> ban.getResultAction() == ReportAction.BAN).toArray().length);
        countList.add(userOwnReports.stream().filter(complete -> complete.getResultAction() == ReportAction.COMPLETE).toArray().length);
        return countList;
    }

    public ArrayList<Report> getReports(String userID) {
        ArrayList<Report> reportList = new ArrayList<>();
        reportList.addAll(reports.stream().filter(rep -> rep.getReportedUser().equals(userID)).collect(Collectors.toList()));
        return reportList;
    }

    public Report getReport(UUID id) {
        for(Report report : reports) {
            if(report.getId().equals(id))
                    return report;
        }
        return null;
    }

    public Report getReport(String msgID) {
        return reports.stream().filter(msg -> msg.getMessageID().equals(msgID)).findAny().get();
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


    class Report implements DBSerializable{
        private String messageID;
        private String channelID;
        private String reportedUser;
        private String actionAdmin;
        private String reportID = null;
        private String banWarnReason;
        private ReportAction resultAction = ReportAction.UNKNOWN;
        private UUID id;
        private HashSet<String> reportingUsers = new HashSet<>();

        public Report(String msgID, String channelID, String reportedUser) {
            this.messageID = msgID;
            this.channelID = channelID;
            this.reportedUser = reportedUser;
            this.id = UUID.randomUUID();
            reports.add(this);
            serialize();
        }

        private Report(UUID id, String postID, String msgID, String chanID, String modID, String reportedID, ReportAction action, String reason) {
            this.id = id;
            this.reportID = postID;
            this.messageID = msgID;
            this.channelID = chanID;
            this.actionAdmin = modID;
            this.reportedUser = reportedID;
            this.resultAction = action;
            this.banWarnReason = reason;
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

        protected void setReportID(String id) {
            reportID = id;
            serialize();
        }
        protected String getReportID() {
            return reportID;
        }

        protected void setActionAdmin(String id) {
            actionAdmin = id;
            serialize();
        }

        protected String getActionAdmin() {
            return actionAdmin;
        }

        protected ReportAction getResultAction() {
            return resultAction;
        }

        protected String getResultReason() {
            return banWarnReason;
        }

        protected void setResult(ReportAction resultAction, String admin, String reason) {
            this.resultAction = resultAction;
            actionAdmin = admin;
            banWarnReason = reason;
            serialize();
        }

        public HashSet<String> getReportingUsers() {
            return reportingUsers;
        }

        public void addReportingUser(String userID) {
            reportingUsers.add(userID);
            if(reportingUsers.size() > 0 && reportID == null)
                publishReport(this);
        }

        public void serialize() {
            ReportDB.getInstance().serializeDB(this);
        }
    }
}
