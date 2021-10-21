package com.scorchedcode.ReportBot;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Emoji;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.Button;
import net.dv8tion.jda.api.interactions.components.ButtonStyle;
import net.dv8tion.jda.api.interactions.components.selections.SelectionMenu;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.json.JSONObject;

import javax.security.auth.login.LoginException;
import java.io.*;
import java.nio.file.Files;
import java.util.Calendar;

public class ReportBot {
    private static ReportBot instance;
    private String TOKEN;
    private String status;
    private String reportRoomId;
    private JDA api;

    private ReportBot() {

    }

    public static void main(String[] args) {
        ReportBot bot = ReportBot.getInstance();
        bot.initDiscordBot();
        bot.setStatus();
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
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (TOKEN == null || TOKEN.isEmpty() || status == null || status.isEmpty() || reportRoomId == null || reportRoomId.isEmpty())
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
        String reportingUsers = "";
        for(String user : rep.getReportingUsers())
            reportingUsers = reportingUsers + user + ",";
        EmbedBuilder eb = new EmbedBuilder();
        eb.setAuthor(reportMessage.getAuthor().getAsTag(), null, reportMessage.getAuthor().getAvatarUrl())
                .setTitle(null)
                .setFooter("Reported by " + reportingUsers.substring(0, reportingUsers.length()-1))
                .setTimestamp(reportMessage.getTimeCreated())
                .setDescription(reportMessage.getContentStripped());
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
                        Button.of(ButtonStyle.PRIMARY, "history:" + rep.getReportedUser().getUserID(), "View User History", Emoji.fromUnicode("\uD83D\uDCD6"))),
                ActionRow.of(Button.danger("complete", "Under Review").asDisabled())).complete();
        rep.setReportID(msgReport.getId());
    }

    public static ReportBot getInstance() {
        if (instance == null)
            instance = new ReportBot();
        return instance;
    }

    protected JDA getAPI() {
        return api;
    }
}
