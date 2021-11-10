package com.scorchedcode.ReportBot;

import net.dv8tion.jda.api.*;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.exceptions.ParsingException;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.privileges.CommandPrivilege;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.Button;
import net.dv8tion.jda.api.interactions.components.ButtonStyle;
import net.dv8tion.jda.api.interactions.components.selections.SelectionMenu;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import org.json.JSONObject;

import javax.security.auth.login.LoginException;
import java.awt.*;
import java.io.*;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.logging.Logger;

public class ReportBot {
    private static ReportBot instance;
    private String TOKEN;
    private String status;
    protected static String reportRoomId;
    protected static String logRoomId;
    protected static String welcomeRoomId;
    private String lockRoomId;
    private String modId;
    protected static HashMap<String, Role[]> lockedCache = new HashMap<>();
    protected static String sqlURL;
    protected static String dbUser;
    protected static String dbPassword;
    private String welcomeMsg = "Welcome to the server!";
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
            api = JDABuilder.createDefault(TOKEN, GatewayIntent.DIRECT_MESSAGE_REACTIONS, GatewayIntent.DIRECT_MESSAGE_TYPING,
                    GatewayIntent.DIRECT_MESSAGES, GatewayIntent.GUILD_BANS, GatewayIntent.GUILD_MEMBERS, GatewayIntent.GUILD_MESSAGES, GatewayIntent.GUILD_MESSAGE_REACTIONS, GatewayIntent.GUILD_WEBHOOKS)
                    .build().awaitReady();
        } catch (LoginException | IllegalArgumentException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ParsingException e) {
            e.printStackTrace();
            System.exit(0);
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
            modId = obj.getString("mod-role-id");
            lockRoomId = obj.getString("lock-channel");
            welcomeRoomId = obj.getString("welcome-channel");
            sqlURL = obj.getString("db-url");
            dbUser = obj.getString("db-user");
            dbPassword = obj.getString("db-password");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (TOKEN == null || TOKEN.isEmpty() || status == null || status.isEmpty() || reportRoomId == null || reportRoomId.isEmpty() || logRoomId == null || logRoomId.isEmpty() ||
                sqlURL == null || sqlURL.isEmpty() || dbUser == null || dbUser.isEmpty() || dbPassword == null || dbPassword.isEmpty() || lockRoomId == null || lockRoomId.isEmpty() ||
            welcomeRoomId == null || welcomeRoomId.isEmpty() || modId == null || modId.isEmpty())
            System.exit(0);
    }

    private void setStatus() {
        api.getPresence().setPresence(OnlineStatus.ONLINE, Activity.playing(status));
    }

    private void initializeCommands() {
        CommandData testCommand = new CommandData("history", "Displays discipline history for user");
        testCommand.addOption(OptionType.USER, "handle", "User's Discord handle to search", true);
        CommandData uidHistoryCommand = new CommandData("history-uid", "Displays discipline history for user");
        uidHistoryCommand.addOption(OptionType.STRING, "userid", "User's Discord user ID to search", true);
        CommandData banCommand = new CommandData("ban", "Ban a user, removing the last 7 days of post history");
        banCommand.addOption(OptionType.USER, "handle", "User's Discord handle", true);
        banCommand.addOption(OptionType.STRING, "reason", "Reason provided for ban", true);
        CommandData warnCommand = new CommandData("warn", "Warn a user, sends a DM from the bot with a reason");
        warnCommand.addOption(OptionType.USER, "handle", "User's Discord handle to search", true);
        warnCommand.addOption(OptionType.STRING, "reason", "Reason provided for warning", true);
        CommandData welcomeCommand = new CommandData("setwelcome", "Set the welcome message sent to the welcome channel when users join");
        welcomeCommand.addOption(OptionType.STRING, "msg", "Message to be displayed on join. Use [u] to insert the user's @handle", true);
        CommandData infoCommand = new CommandData("userinfo", "Displays information on user");
        infoCommand.addOption(OptionType.USER, "handle", "User's Discord handle to search", true);
        getAPI().getGuilds().get(0).upsertCommand(testCommand).complete().updatePrivileges(getAPI().getGuilds().get(0), CommandPrivilege.disable(getAPI().getGuilds().get(0).getPublicRole()), CommandPrivilege.enableRole(modId)).complete();
        getAPI().getGuilds().get(0).upsertCommand(banCommand).complete().updatePrivileges(getAPI().getGuilds().get(0), CommandPrivilege.disable(getAPI().getGuilds().get(0).getPublicRole()), CommandPrivilege.enableRole(modId)).complete();
        getAPI().getGuilds().get(0).upsertCommand(warnCommand).complete().updatePrivileges(getAPI().getGuilds().get(0), CommandPrivilege.disable(getAPI().getGuilds().get(0).getPublicRole()), CommandPrivilege.enableRole(modId)).complete();
        getAPI().getGuilds().get(0).upsertCommand(welcomeCommand).complete().updatePrivileges(getAPI().getGuilds().get(0), CommandPrivilege.disable(getAPI().getGuilds().get(0).getPublicRole()), CommandPrivilege.enableRole(modId)).complete();
        getAPI().getGuilds().get(0).upsertCommand(infoCommand).complete().updatePrivileges(getAPI().getGuilds().get(0), CommandPrivilege.disable(getAPI().getGuilds().get(0).getPublicRole()), CommandPrivilege.enableRole(modId)).complete();
        getAPI().getGuilds().get(0).upsertCommand(uidHistoryCommand).complete().updatePrivileges(getAPI().getGuilds().get(0), CommandPrivilege.disable(getAPI().getGuilds().get(0).getPublicRole()), CommandPrivilege.enableRole(modId)).complete();
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
                .setDescription((reportMessage.getAttachments().size() == 1) ? (reportMessage.getAttachments().get(0).getUrl().concat(" ").concat(reportMessage.getContentDisplay())) : reportMessage.getContentDisplay());
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
        String[] reportingUsers = rep.getReportingUsers().toArray(new String[0]);
        String repUsers = "";
        for(int x = 0; x < reportingUsers.length; x++)
            repUsers+=getAPI().retrieveUserById(reportingUsers[x]).complete().getAsMention();
        EmbedBuilder logEB = new EmbedBuilder();
        logEB.setTitle("Report System")
                .setColor(Color.CYAN)
                .setDescription("Report Created for " + getAPI().getUserById(rep.getReportedUser()).getAsMention() + " \nReported by:\n" + repUsers);
                //.setDescription("Report Created for " + getAPI().getUserById(rep.getReportedUser()).getAsMention() + " \nReported by:\n" + getAPI().getUserById(reportingUsers[0]).getAsMention() + "\n" +
                        //getAPI().getUserById(reportingUsers[1]).getAsMention() + "\n" + getAPI().getUserById(reportingUsers[2]).getAsMention());
        getAPI().getTextChannelById(logRoomId).sendMessageEmbeds(logEB.build()).setActionRows(ActionRow.of(Button.of(ButtonStyle.LINK, getAPI().getTextChannelById(reportRoomId).getHistoryAround(rep.getReportID(), 5).complete().getMessageById(rep.getReportID()).getJumpUrl(), "View Report"))).queue();

    }

    public static ReportBot getInstance() {
        if (instance == null)
            instance = new ReportBot();
        return instance;
    }

    protected void lockUser(ReportManager.Report report) {
        TextChannel lockChannel = getAPI().getTextChannelById(lockRoomId);
        Member lockedUser = lockChannel.getGuild().retrieveMemberById(report.getReportedUser()).complete();
        Role tempRole = lockChannel.getGuild().createRole().setColor(Color.RED).setName(lockedUser.getEffectiveName()).complete();
        lockedCache.put(report.getReportedUser(), lockedUser.getRoles().toArray(new Role[0]));
        for(Role role : lockedUser.getRoles())
            lockChannel.getGuild().removeRoleFromMember(lockedUser, role).complete();
        lockChannel.createPermissionOverride(tempRole).setAllow(Permission.VIEW_CHANNEL, Permission.MESSAGE_SEND_IN_THREADS).setDeny(Permission.MESSAGE_HISTORY, Permission.MESSAGE_SEND).complete();
        lockChannel.getGuild().addRoleToMember(lockedUser, tempRole).complete();
        ThreadChannel lockThread = lockChannel.createThreadChannel(report.getMessageID(), true).complete();
        lockThread.sendMessage(lockedUser.getAsMention() + " " + getAPI().getRoleById(modId).getAsMention()).queue();
    }

    protected void logAction(String string) {
        TextChannel logNonReportRoom = getAPI().getTextChannelById(logRoomId);
        switch (string) {
            case "left":
                break;
            default:
                logNonReportRoom.sendMessage(string).queue();
        }

    }

    protected void setWelcomeMsg(String msg) {
        welcomeMsg = msg;
    }

    protected String getWelcomeMsg() {
        return welcomeMsg;
    }

    protected boolean isMod(String userID) {
        return getAPI().getGuilds().get(0).retrieveMemberById(userID).complete().getRoles().stream().anyMatch(id -> id.getId().equals(modId)) || getAPI().getGuilds().get(0).getMemberById(userID).isOwner();
    }

    protected JDA getAPI() {
        return api;
    }

    public static String createJumpUrl(String channelID, String messageID) {
        return "https://discordapp.com/channels/" + getInstance().getAPI().getGuilds().get(0).getId() + "/" + channelID + "/" + messageID;
    }
}
