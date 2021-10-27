package com.scorchedcode.ReportBot;

import java.sql.*;
import java.util.ArrayList;
import java.util.UUID;

public class ReportDB {

    private static ReportDB instance = null;
    private static final String CREATE_DB = "CREATE DATABASE `gp_reports`;";
    private static final String CREATE_REPORT_TABLE = "CREATE TABLE `Reports` (`ReportID` varchar(36) NOT NULL,  `PostedID` text NOT NULL,  `MessageID` text NOT NULL,  `ChannelID` text NOT NULL,  `AdminID` text NOT NULL,  `ReportedUserID` text NOT NULL,  `Action` int NOT NULL,  `Reason` text NOT NULL, PRIMARY KEY (`ReportID`));";
    private static final String CREATE_MESSAGE_TABLE = "CREATE TABLE `MessageCache` ( `MessageID` VARCHAR(18) NOT NULL , `UserID` text NOT NULL , `Content` text NOT NULL , PRIMARY KEY (`MessageID`));";
    private ReportDB() {
        if (!hasDB())
            initDB();
        else
            readDB();
    }

    private void initDB() {
        Connection conn = null;
        Connection conntwo = null;
        Statement stmt = null;
        Statement stmttwo = null;
        try {
            conn = DriverManager.getConnection(ReportBot.sqlURL, ReportBot.dbUser, ReportBot.dbPassword);
            stmt = conn.createStatement();
            stmt.execute(CREATE_DB);
            conntwo = DriverManager.getConnection(ReportBot.sqlURL + "/gp_reports", ReportBot.dbUser, ReportBot.dbPassword);
            stmttwo = conntwo.createStatement();
            stmttwo.execute(CREATE_REPORT_TABLE);
            stmttwo.execute(CREATE_MESSAGE_TABLE);
            /*ResultSet rs = stmt.executeBatch();) {
            // Extract data from result set
            while (rs.next()) {
                // Retrieve by column name
                System.out.print("ID: " + rs.getInt("id"));
                System.out.print(", Age: " + rs.getInt("age"));
                System.out.print(", First: " + rs.getString("first"));
                System.out.println(", Last: " + rs.getString("last"));
            }*/
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            try {
                conn.close();
                stmt.close();
                conntwo.close();
                stmttwo.close();
                ReportBot.getInstance().logAction("Database initialized.");
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    private boolean hasDB() {
        Connection init = null;
        Statement query = null;
        try {
            init = DriverManager.getConnection(ReportBot.sqlURL, ReportBot.dbUser, ReportBot.dbPassword);
            query = init.createStatement();
            ResultSet rs = query.executeQuery("SHOW DATABASES;");
            while (rs.next()) {
                String dbs = rs.getString("Database");
                if (dbs.equals("gp_reports"))
                    return true;
            }
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        } finally {
            try {
                init.close();
                query.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    private void readDB() {
        Connection init = null;
        Statement query = null;
        try {
            init = DriverManager.getConnection(ReportBot.sqlURL + "/gp_reports", ReportBot.dbUser, ReportBot.dbPassword);
            String readTable = "SELECT * FROM `Reports`;";
            query = init.createStatement();
            ResultSet rs = query.executeQuery(readTable);
            while (rs.next()) {
                ReportManager.getInstance().loadFromPersistance(UUID.fromString(rs.getString("ReportID")),rs.getString("PostedID"),rs.getString("MessageID"),rs.getString("ChannelID"),
                        rs.getString("AdminID"),rs.getString("ReportedUserID"), ReportAction.values()[rs.getInt("Action")], rs.getString("Reason"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            try {
                init.close();
                query.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
            ReportBot.getInstance().logAction("Reports loaded from database.");
        }
    }


    public void serializeMessage(String msgID, String userID, String content) {
        Connection init = null;
        Statement query = null;
        try {
            init = DriverManager.getConnection(ReportBot.sqlURL + "/gp_reports", ReportBot.dbUser, ReportBot.dbPassword);
            String insert = "INSERT INTO `MessageCache` (`MessageID`, `UserID`, `Content`) VALUES ('" + msgID +
                    "', '" + userID + "', '" + content.replaceAll("'", "''") + "') ON DUPLICATE KEY UPDATE `UserID`"
                    + " = '" + userID + "', `Content` = '" + content.replaceAll("'", "''") + "';";
            query = init.createStatement();
            query.execute(insert);
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        } finally {
            try {
                init.close();
                query.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    public void serializeDB(DBSerializable rep) {
        Connection init = null;
        Statement query = null;
        try {
            init = DriverManager.getConnection(ReportBot.sqlURL + "/gp_reports", ReportBot.dbUser, ReportBot.dbPassword);
            if (rep instanceof ReportManager.Report) {
                String insert = "INSERT INTO `Reports` (`ReportID`, `PostedID`, `MessageID`, `ChannelID`, `AdminID`, `ReportedUserID`, `Action`, `Reason`) VALUES ('" + ((ReportManager.Report) rep).getId() +
                        "', '" + ((ReportManager.Report) rep).getReportID() + "', '" + ((ReportManager.Report) rep).getMessageID() + "', '" + ((ReportManager.Report) rep).getChannelID() + "', '" +
                        ((ReportManager.Report) rep).getActionAdmin() + "', '" + ((ReportManager.Report) rep).getReportedUser() + "', '" + ((ReportManager.Report) rep).getResultAction().ordinal() + "', '" + ((ReportManager.Report) rep).getResultReason().replaceAll("'", "''") + "') ON DUPLICATE KEY UPDATE `PostedID`"
                        + " = '" + ((ReportManager.Report) rep).getReportID() + "', `MessageID` = '" + ((ReportManager.Report) rep).getMessageID() + "', `ChannelID` = '" + ((ReportManager.Report) rep).getChannelID() + "', `AdminID` = '" +
                        ((ReportManager.Report) rep).getActionAdmin() + "', `ReportedUserID` = '" + ((ReportManager.Report) rep).getReportedUser() + "', `Action` = '" + ((ReportManager.Report) rep).getResultAction().ordinal() + "', `Reason` = '" + ((ReportManager.Report) rep).getResultReason().replaceAll("'", "''") + "';";
                query = init.createStatement();
                query.execute(insert);
            } else {

            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            try {
                init.close();
                query.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    public ArrayList<String> retrieveMessage(String msgID) {
        Connection init = null;
        Statement query = null;
        try {
            init = DriverManager.getConnection(ReportBot.sqlURL + "/gp_reports", ReportBot.dbUser, ReportBot.dbPassword);
            String readTable = "SELECT * FROM `MessageCache`;";
            query = init.createStatement();
            ResultSet rs = query.executeQuery(readTable);
            while (rs.next()) {
                String messageID = rs.getString("MessageID");
                if(msgID.equals(messageID)) {
                    ArrayList<String> foundData = new ArrayList<>();
                    foundData.add(messageID);
                    foundData.add(rs.getString("UserID"));
                    foundData.add(rs.getString("Content"));
                    return foundData;
                }
            }
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        } finally {
            try {
                init.close();
                query.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    public static ReportDB getInstance() {
        if (instance == null)
            instance = new ReportDB();
        return instance;
    }
}
