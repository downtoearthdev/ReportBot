package com.scorchedcode.ReportBot;

import net.dv8tion.jda.api.*;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.Button;
import net.dv8tion.jda.api.interactions.components.ButtonStyle;
import net.dv8tion.jda.api.interactions.components.selections.SelectionMenu;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.json.JSONObject;

import javax.security.auth.login.LoginException;
import java.awt.*;
import java.io.*;
import java.nio.file.Files;
import java.util.HashMap;

public class ReportBot {
    private static ReportBot instance;
    private String TOKEN;
    private String status;
    protected static String reportRoomId;
    private String logRoomId;
    protected static String nonReportRoomId;
    private String lockRoomId;
    protected static HashMap<String, Role[]> lockedCache = new HashMap<>();
    protected static String sqlURL;
    protected static String dbUser;
    protected static String dbPassword;
    private JDA api;

    private ReportBot() {

    }

    public static void main(String[] args) {
        ReportBot bot = ReportBot.getInstance();
        bot.initDiscordBot();
        bot.setStatus();
        ReportDB.getInstance();
    }


    private void initDiscordBot() {
        handleConfig();
        try {
            api = JDABuilder.create(TOKEN, GatewayIntent.DIRECT_MESSAGE_REACTIONS, GatewayIntent.DIRECT_MESSAGE_TYPING,
                    GatewayIntent.DIRECT_MESSAGES, GatewayIntent.GUILD_BANS, GatewayIntent.GUILD_EMOJIS, GatewayIntent.GUILD_INVITES,
                    GatewayIntent.GUILD_MEMBERS, GatewayIntent.GUILD_MESSAGES, GatewayIntent.GUILD_MESSAGE_REACTIONS, GatewayIntent.GUILD_MESSAGE_TYPING,
                    GatewayIntent.GUILD_VOICE_STATES, GatewayIntent.GUILD_WEBHOOKS).build().awaitReady();
        } catch (LoginException | IllegalArgumentException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        getAPI().addEventListener(new ReactionListener());
        getAPI().addEventListener(new ReportListener());
        getAPI().addEventListener(new UserActivityListener());
        initializeCommands();
    }

    private void handleConfig() {
        if (!new File("config.json").exists()) {
            try {
                InputStream is = ReportBot.class.getResourceAsStream("/config.json");
                File config = new File("config.json");
                FileWriter os = new FileWriter(config);
                while (is.available() > 0)
                    os.write(is.read());
                is.close();
                os.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        try {
            String contents = new String(Files.readAllBytes(new File("config.json").toPath()));
            JSONObject obj = new JSONObject(contents);
            TOKEN = obj.getString("token");
            status = obj.getString("status");
            reportRoomId = obj.getString("report-room");
            logRoomId = obj.getString("log-room");
            nonReportRoomId = obj.getString("non-report-log-room");
            lockRoomId = obj.getString("lock-channel");
            sqlURL = obj.getString("db-url");
            dbUser = obj.getString("db-user");
            dbPassword = obj.getString("db-password");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (TOKEN == null || TOKEN.isEmpty() || status == null || status.isEmpty() || reportRoomId == null || reportRoomId.isEmpty() || logRoomId == null || logRoomId.isEmpty() ||
                sqlURL == null || sqlURL.isEmpty() || dbUser == null || dbUser.isEmpty() || dbPassword == null || dbPassword.isEmpty() || nonReportRoomId == null || nonReportRoomId.isEmpty()
                || lockRoomId == null || lockRoomId.isEmpty())
            System.exit(0);
    }

    private void setStatus() {
        api.getPresence().setPresence(OnlineStatus.ONLINE, Activity.playing(status));
    }

    private void initializeCommands() {
        CommandData testCommand = new CommandData("history", "Displays discipline history for user");
        testCommand.addOption(OptionType.STRING, "input", "String to echo back", true);
        getAPI().getGuilds().get(0).upsertCommand(testCommand).complete();
    }

    public void generateReportAesthetic(ReportManager.Report rep) {
        TextChannel reportRoom = api.getTextChannelById(reportRoomId);
        Message reportMessage = api.getTextChannelById(rep.getChannelID()).getHistoryAround(rep.getMessageID(), 5).complete().getMessageById(rep.getMessageID());
        //reportRoom.sendMessage(reportMessage.getContentStripped()).complete();
        EmbedBuilder eb = new EmbedBuilder();
        eb.setAuthor(reportMessage.getAuthor().getAsTag(), null, reportMessage.getAuthor().getAvatarUrl())
                .setTitle(null)
                .setFooter(null)
                .setTimestamp(reportMessage.getTimeCreated())
                .setDescription(reportMessage.getContentDisplay());
        SelectionMenu actions = SelectionMenu.create("menu:reportbot:" + rep.getId())
                .setPlaceholder("Choose an action to perform for this report.")
                .setRequiredRange(1, 1)
                .addOption("Warn", "warn", "Warn " + reportMessage.getAuthor().getName() + " for Violation of Rules", Emoji.fromUnicode("\u26A0"))
                .addOption("Ban User", "ban", "Ban " + reportMessage.getAuthor().getName() + " for Violation of Rules", Emoji.fromUnicode("\u26D4"))
                .addOption("Delete Message", "delete", "Remove this message without further action", Emoji.fromUnicode("\u274C"))
                .addOption("Lock User", "lock", "Lock " + reportMessage.getAuthor().getName() + " and create private thread", Emoji.fromUnicode("\uD83D\uDD12"))
                .addOption("Completed", "complete", "Mark report as handled", Emoji.fromUnicode("\u2705"))
                .build();
        Message msgReport = reportRoom.sendMessageEmbeds(eb.build()).setActionRows(ActionRow.of(actions),
                ActionRow.of(Button.of(ButtonStyle.LINK, reportMessage.getJumpUrl(), "View Message", Emoji.fromUnicode("\uD83D\uDD0D")),
                        Button.of(ButtonStyle.PRIMARY, "history:" + rep.getReportedUser(), "View User History", Emoji.fromUnicode("\uD83D\uDCD6"))),
                ActionRow.of(Button.danger("complete", "Under Review").asDisabled())).complete();
        rep.setReportID(msgReport.getId());
        logAction(rep);
    }

    public static ReportBot getInstance() {
        if (instance == null)
            instance = new ReportBot();
        return instance;
    }

    protected void warnBanUser(String userId, String warning, boolean ban) {

    }

    protected void lockUser(ReportManager.Report report) {
        TextChannel lockChannel = getAPI().getTextChannelById(lockRoomId);
        Member lockedUser = lockChannel.getGuild().getMemberById(report.getReportedUser());
        Role tempRole = lockChannel.getGuild().createRole().setColor(Color.RED).setName(lockedUser.getEffectiveName()).complete();
        lockedCache.put(report.getReportedUser(), lockedUser.getRoles().toArray(new Role[0]));
        for(Role role : lockedUser.getRoles())
            lockChannel.getGuild().removeRoleFromMember(lockedUser, role).complete();
        lockChannel.createPermissionOverride(tempRole).setAllow(Permission.VIEW_CHANNEL, Permission.MESSAGE_SEND_IN_THREADS).setDeny(Permission.MESSAGE_HISTORY, Permission.MESSAGE_SEND).complete();
        lockChannel.getGuild().addRoleToMember(lockedUser, tempRole).complete();
        GuildThread lockThread = lockChannel.createThread(report.getReportedUser(), true).complete();
        lockThread.sendMessage(lockedUser.getAsMention()).queue();
    }

    protected void logAction(String string) {
        TextChannel logNonReportRoom = getAPI().getTextChannelById(nonReportRoomId);
        switch (string) {
            case "left":
                break;
            default:
                logNonReportRoom.sendMessage(string).queue();
        }

    }

    protected void logAction(ReportManager.Report report) {
        TextChannel logRoom = getAPI().getTextChannelById(logRoomId);
        String logAdmin = (report.getActionAdmin() != null) ? logRoom.getGuild().getMemberById(report.getActionAdmin()).getUser().getAsTag() : null;
        String outputText = (report.getActionAdmin() != null) ? "Report-System\nFor: " + logRoom.getGuild().getMemberById(report.getReportedUser()).getUser().getAsTag() + "\nFrom: " +
                logRoom.getGuild().getMemberById(report.getActionAdmin()).getUser().getAsTag() +"\nAction taken: "  : null;
        switch(report.getResultAction()) {
            case BAN:
                outputText+="Banned by " + logAdmin + "\nReason: " + report.getResultReason();
                break;
            case WARN:
                outputText+="Warned by " + logAdmin + "\nReason: " + report.getResultReason();
                break;
            case DELETE:
                outputText+="Message deleted by " + logAdmin;
                break;
            case COMPLETE:
                outputText+="Completed by " + logAdmin;
                break;
            case LOCK:
                outputText+="Private discussion thread created by " + logAdmin;
                break;
            case UNKNOWN:
                String reportingUsers = "";
                for(String user : report.getReportingUsers())
                    reportingUsers+=reportingUsers + logRoom.getGuild().getMemberById(user).getUser().getAsTag() + ",";
                outputText = "Report-System\n Report generated for: " + logRoom.getGuild().getMemberById(report.getReportedUser()).getUser().getAsTag() + "\nReported by: " +
                        reportingUsers.substring(0, reportingUsers.length()-1);
                break;
        }
        logRoom.sendMessage(outputText).queue();
    }

    protected JDA getAPI() {
        return api;
    }
}
