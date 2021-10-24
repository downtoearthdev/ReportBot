package com.scorchedcode.ReportBot;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;
import net.dv8tion.jda.api.events.interaction.SelectionMenuEvent;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.Button;
import net.dv8tion.jda.api.interactions.components.ButtonStyle;
import net.dv8tion.jda.api.interactions.components.selections.SelectOption;
import org.jetbrains.annotations.NotNull;

import java.awt.Color;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.UUID;

public class ReportListener extends ListenerAdapter {

    public ReportListener() {

    }

    @Override
    public void onSlashCommand(@NotNull SlashCommandEvent event) {
        if (event.getName().equals("warn")) {
            //event.getTextChannel().sendMessage(event.getOption("input").getAsString()).complete();
            //event.reply("true").setEphemeral(true).complete();
        }
    }

    @Override
    public void onButtonClick(@NotNull ButtonClickEvent event) {
        if (event.getButton().getId().contains("history")) {
            String user = event.getButton().getId().split(":")[1];
            if (user != null) {
                ArrayList<Integer> reportCounts = ReportManager.getInstance().getReportCounts(user);
                ArrayList<ReportManager.Report> reports = ReportManager.getInstance().getReports(user);
                String description = "";
                for (ReportManager.Report rep : reports) {
                    /*OffsetDateTime time = event.getTextChannel().getHistoryAround(rep.getReportID(), 5).complete().getMessageById(rep.getReportID()).getTimeCreated();;
                    Period timeBetween = Period.between(OffsetDateTime.now().toLocalDate(), time.toLocalDate());
                    Duration hoursBetween = Duration.between(OffsetDateTime.now().toLocalTime(), time.toLocalTime());
                    String timeString = ((timeBetween.getYears() > 0) ? timeBetween.getYears() + " years " : " ").concat((timeBetween.getMonths() > 0) ? (timeBetween.getMonths() + " months ") : " ").concat(
                            (timeBetween.getDays() > 0) ? timeBetween.getDays() + " days " : "").concat(Math.floorDiv(hoursBetween.toMinutes(),60) + " hours " + Math.floorMod(hoursBetween.toMinutes(), 60) + " minutes ago.");*/
                    String timeString = event.getTextChannel().getHistoryAround(rep.getReportID(), 5).complete().getMessageById(rep.getReportID()).getTimeCreated().toLocalDateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
                    description += timeString + "\n" + rep.getResultAction().getFancyPosessive() + " " + event.getGuild().getMemberById(rep.getActionAdmin()).getUser().getAsTag();
                }
                EmbedBuilder eb = new EmbedBuilder();
                eb.setAuthor(event.getGuild().getMemberById(user).getUser().getAsTag(), null, event.getGuild().getMemberById(user).getUser().getAvatarUrl())
                        .setTitle(null)
                        .setColor(Color.cyan)
                        .setFooter(reportCounts.get(0) + " warning(s), " + reportCounts.get(1) + " lock(s), " + reportCounts.get(2) + " delete(s), " + reportCounts.get(3) + " ban(s)")
                        .setDescription(description);
                event.replyEmbeds(eb.build()).setEphemeral(true).queue();
            }
        }
    }

    @Override
    public void onSelectionMenu(@NotNull SelectionMenuEvent event) {
        if (event.getComponent().getId().contains("menu:reportbot")) {
            //TODO - Could be null
            ReportManager.Report report = ReportManager.getInstance().getReport(UUID.fromString(event.getComponent().getId().split(":")[2]));
            String userName = event.getGuild().getMemberById(report.getReportedUser()).getUser().getAsTag();
            SelectOption option = event.getInteraction().getSelectedOptions().get(0);
            switch (option.getValue()) {
                case "warn":
                    event.reply("PLACEHOLDER: Warned " + userName).setEphemeral(true).queue();
                    report.setResult(ReportAction.WARN, event.getMember().getId(), "PLACEHOLDER");
                    break;
                case "ban":
                    event.getGuild().getMemberById(report.getReportedUser()).ban(0, "Violating the rules").complete();
                    event.editSelectionMenu(null).complete();
                    event.getMessage().editMessageComponents(ActionRow.of(event.getMessage().getActionRows().get(1).getButtons().get(0).asEnabled(),
                                    event.getMessage().getActionRows().get(1).getButtons().get(1).asEnabled()),
                            ActionRow.of(event.getMessage().getActionRows().get(2).getButtons().get(0).withStyle(ButtonStyle.SUCCESS).withLabel("Banned by " + event.getUser().getAsTag()))).complete();
                    report.setResult(ReportAction.BAN, event.getMember().getId(), "PLACEHOLDER");
                    break;
                case "delete":
                    Message msg = event.getGuild().getTextChannelById(report.getChannelID()).getHistoryAround(report.getMessageID(), 5).complete().getMessageById(report.getMessageID());
                    if (!(msg == null))
                        msg.delete().queue();
                    event.editSelectionMenu(null).complete();
                    event.getMessage().editMessageComponents(ActionRow.of(event.getMessage().getActionRows().get(1).getButtons().get(0).asDisabled(),
                                    event.getMessage().getActionRows().get(1).getButtons().get(1).asEnabled()),
                            ActionRow.of(event.getMessage().getActionRows().get(2).getButtons().get(0).withStyle(ButtonStyle.SUCCESS).withLabel("Deleted by " + event.getUser().getAsTag()))).complete();
                    report.setResult(ReportAction.DELETE, event.getMember().getId(), "null");
                    break;
                case "lock":
                    event.editSelectionMenu(null).complete();
                    event.getMessage().editMessageComponents(ActionRow.of(event.getMessage().getActionRows().get(1).getButtons().get(0).asEnabled(),
                                    event.getMessage().getActionRows().get(1).getButtons().get(1).asEnabled()),
                            ActionRow.of(event.getMessage().getActionRows().get(2).getButtons().get(0).withStyle(ButtonStyle.SUCCESS).withLabel("Locked by " + event.getUser().getAsTag()))).complete();
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

    private void sendActionEmbed(ReportManager.Report report) {
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
        }
        EmbedBuilder eb = new EmbedBuilder();
        //eb.setAuthor(event.getGuild().getMemberById(foundData.get(1)).getUser().getAsTag(), null, event.getGuild().getMemberById(foundData.get(1)).getUser().getAvatarUrl())
        eb.setTitle("Report System")
                .setColor(resolutionColor)
                .addField("Action on report: ", actionText, false);
        if (report.getResultAction() == ReportAction.BAN)
            eb.addField("Ban reason", report.getResultReason(), false);
        else if (report.getResultAction() == ReportAction.WARN)
            eb.addField("Warning reason", report.getResultReason(), false);
        //eb.addField("By moderator: ", " ", false)
                eb.setFooter(ReportBot.getInstance().getAPI().getUserById(report.getActionAdmin()).getAsTag(), ReportBot.getInstance().getAPI().getUserById(report.getActionAdmin()).getAvatarUrl());
        ReportBot.getInstance().getAPI().getGuilds().get(0).getTextChannelById(ReportBot.nonReportRoomId).sendMessageEmbeds(eb.build()).setActionRows(ActionRow.of(Button.of(ButtonStyle.LINK, ReportBot.getInstance().getAPI().getGuilds().get(0).getTextChannelById(ReportBot.reportRoomId).getHistoryAround(report.getReportID(), 5).complete().getMessageById(report.getReportID()).getJumpUrl(), "View Report"))).queue();
    }
}
