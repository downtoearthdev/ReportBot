package com.scorchedcode.ReportBot;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;
import net.dv8tion.jda.api.events.interaction.SelectionMenuEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.Button;
import net.dv8tion.jda.api.interactions.components.ButtonStyle;
import net.dv8tion.jda.api.interactions.components.selections.SelectOption;
import net.dv8tion.jda.api.interactions.components.selections.SelectionMenu;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.util.ArrayList;
import java.util.UUID;

public class ReportListener extends ListenerAdapter {

    public ReportListener() {

    }

    @Override
    public void onButtonClick(@NotNull ButtonClickEvent event) {
        if (event.getButton().getId().contains("history")) {
            String user = event.getButton().getId().split(":")[1];
            if (user != null) {
                event.deferReply(true).queue();
                //event.replyEmbeds(eb.build()).setEphemeral(true).queue();
                event.getHook().editOriginalEmbeds(getHistory(user)).queue();
            }
        }
    }

    @Override
    public void onSelectionMenu(@NotNull SelectionMenuEvent event) {
        if (event.getComponent().getId().contains("menu:reportbot")) {
            ReportManager.Report report = ReportManager.getInstance().getReport(UUID.fromString(event.getComponent().getId().split(":")[2]));
            SelectOption option = event.getInteraction().getSelectedOptions().get(0);
            String choice = (event.getGuild().getMemberById(report.getReportedUser()) != null) ? option.getValue() : "complete";
            Message msg = event.getGuild().getTextChannelById(report.getChannelID()).getHistoryAround(report.getMessageID(), 5).complete().getMessageById(report.getMessageID());
            switch (choice) {
                case "warn":
                    event.getMessage().editMessageComponents(ActionRow.of(SelectionMenu.create("disabled").setRequiredRange(1,1).addOption("something", "something").setDisabled(true).build()), ActionRow.of(event.getMessage().getActionRows().get(1).getButtons().get(0).asDisabled(),
                                    event.getMessage().getActionRows().get(1).getButtons().get(1).asEnabled()),
                            ActionRow.of(event.getMessage().getActionRows().get(2).getButtons().get(0).withStyle(ButtonStyle.PRIMARY).withLabel("Pending warning by " + event.getUser().getAsTag()))).complete();
                    report.setResult(ReportAction.WARN, event.getMember().getId(), "PLACEHOLDER");
                    if(!(msg == null))
                        msg.delete().queue();
                    event.reply("Please respond with a warning message in chat!").setEphemeral(true).queue();
                    UserActivityListener.pendingMap.put(event.getUser().getId(), report);
                    return;
                case "ban":
                    event.getMessage().editMessageComponents(ActionRow.of(SelectionMenu.create("disabled").setRequiredRange(1,1).addOption("something", "something").setDisabled(true).build()), ActionRow.of(event.getMessage().getActionRows().get(1).getButtons().get(0).asEnabled(),
                                    event.getMessage().getActionRows().get(1).getButtons().get(1).asEnabled()),
                            ActionRow.of(event.getMessage().getActionRows().get(2).getButtons().get(0).withStyle(ButtonStyle.PRIMARY).withLabel("Pending ban by " + event.getUser().getAsTag()))).complete();
                    report.setResult(ReportAction.BAN, event.getMember().getId(), "PLACEHOLDER");
                    event.reply("Please respond with a reason for ban in chat!").setEphemeral(true).queue();
                    UserActivityListener.pendingMap.put(event.getUser().getId(), report);
                    return;
                case "delete":
                    if (!(msg == null))
                        msg.delete().queue();
                    event.editSelectionMenu(null).complete();
                    event.getMessage().editMessageComponents(ActionRow.of(event.getMessage().getActionRows().get(1).getButtons().get(0).asDisabled(),
                                    event.getMessage().getActionRows().get(1).getButtons().get(1).asEnabled()),
                            ActionRow.of(event.getMessage().getActionRows().get(2).getButtons().get(0).withStyle(ButtonStyle.SUCCESS).withLabel("Deleted by " + event.getUser().getAsTag()))).complete();
                    report.setResult(ReportAction.DELETE, event.getMember().getId(), "null");
                    break;
                case "lock":
                    event.getMessage().editMessageComponents(ActionRow.of(((SelectionMenu)event.getMessage().getActionRows().get(0).getComponents().get(0)).createCopy().build()), ActionRow.of(event.getMessage().getActionRows().get(1).getButtons().get(0).asEnabled(),
                                    event.getMessage().getActionRows().get(1).getButtons().get(1).asEnabled()),
                            ActionRow.of(event.getMessage().getActionRows().get(2).getButtons().get(0).withStyle(ButtonStyle.PRIMARY).withLabel(event.getGuild().getMemberById(report.getReportedUser()).getUser().getAsTag() + " currently locked by " + event.getUser().getAsTag()))).complete();
                    report.setResult(ReportAction.LOCK, event.getMember().getId(), null);
                    ReportBot.getInstance().lockUser(report);
                    break;
                case "complete":
                    event.editSelectionMenu(null).complete();
                    event.getMessage().editMessageComponents(ActionRow.of(event.getMessage().getActionRows().get(1).getButtons().get(0).asEnabled(),
                                    event.getMessage().getActionRows().get(1).getButtons().get(1).asEnabled()),
                            ActionRow.of(event.getMessage().getActionRows().get(2).getButtons().get(0).withStyle(ButtonStyle.SUCCESS).withLabel("Completed by " + event.getUser().getAsTag()))).complete();
                    report.setResult(ReportAction.COMPLETE, event.getMember().getId(), null);
                    break;
            }
            sendActionEmbed(report);
        }
    }

    protected static void sendActionEmbed(ReportManager.Report report) {
        Color resolutionColor = Color.GRAY;
        String actionText = "";
        switch (report.getResultAction()) {
            case WARN:
                resolutionColor = new Color(127, 0, 100);
                actionText = "Warning issued";
                break;
            case DELETE:
                resolutionColor = new Color(127, 97, 0);
                actionText = "Message deleted";
                break;
            case COMPLETE:
                resolutionColor = Color.GREEN;
                actionText = "Report marked completed";
                break;
            case BAN:
                resolutionColor = Color.RED;
                actionText = "Ban issued";
                break;
            case LOCK:
                resolutionColor = Color.ORANGE;
                actionText = "User locked";
                break;
        }
        EmbedBuilder eb = new EmbedBuilder();
        eb.setTitle("Report System")
                .setColor(resolutionColor)
                .addField("Action on report: ", actionText, false);
        if (report.getResultAction() == ReportAction.BAN)
            eb.addField("Ban reason", report.getResultReason(), false);
        else if (report.getResultAction() == ReportAction.WARN)
            eb.addField("Warning reason", report.getResultReason(), false);
        eb.setFooter(ReportBot.getInstance().getAPI().getUserById(report.getActionAdmin()).getAsTag(), ReportBot.getInstance().getAPI().getUserById(report.getActionAdmin()).getAvatarUrl());
        ReportBot.getInstance().getAPI().getTextChannelById(ReportBot.logRoomId).sendMessageEmbeds(eb.build()).setActionRows(ActionRow.of(Button.of(ButtonStyle.LINK, ReportBot.getInstance().getAPI().getGuilds().get(0).getTextChannelById(ReportBot.reportRoomId).getHistoryAround(report.getReportID(), 5).complete().getMessageById(report.getReportID()).getJumpUrl(), "View Report"))).queue();
    }

    protected static MessageEmbed getHistory(String user) {
        ArrayList<Integer> reportCounts = ReportManager.getInstance().getReportCounts(user);
        ArrayList<ReportManager.Report> reports = ReportManager.getInstance().getReports(user);
        String description = "";
        for (ReportManager.Report rep : reports)
            description += (rep.getResultAction() != ReportAction.COMPLETE) ? "[Jump to report](" + ReportBot.createJumpUrl(ReportBot.reportRoomId, rep.getReportID()) + ")\n" .concat(rep.getResultAction() == ReportAction.UNKNOWN ? "Under Review" : rep.getResultAction().getFancyPosessive() + " " + ReportBot.getInstance().getAPI().getGuilds().get(0).getMemberById(rep.getActionAdmin()).getUser().getAsTag() + "\n") : "";
        EmbedBuilder eb = new EmbedBuilder();
        eb.setAuthor((ReportBot.getInstance().getAPI().getUserById(user) == null) ? "Banned User" : ReportBot.getInstance().getAPI().getUserById(user).getAsTag(), null, (ReportBot.getInstance().getAPI().getUserById(user) == null) ? "https://banner2.cleanpng.com/20180406/rie/kisspng-emoji-eggplant-vegetable-food-text-messaging-eggplant-5ac7d3d9c1cc65.3692644815230453377938.jpg" : ReportBot.getInstance().getAPI().getUserById(user).getAvatarUrl())
                .setTitle(null)
                .setColor(Color.cyan)
                .setFooter(reportCounts.get(0) + " warning(s), " + reportCounts.get(1) + " lock(s), " + reportCounts.get(2) + " delete(s), " + reportCounts.get(3) + " ban(s)")
                .setDescription(description);
        return eb.build();
    }
}
