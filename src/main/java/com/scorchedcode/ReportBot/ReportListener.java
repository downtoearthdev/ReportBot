package com.scorchedcode.ReportBot;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;
import net.dv8tion.jda.api.events.interaction.SelectionMenuEvent;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.ButtonStyle;
import net.dv8tion.jda.api.interactions.components.selections.SelectOption;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class ReportListener extends ListenerAdapter {

    public ReportListener() {

    }

    @Override
    public void onSlashCommand(@NotNull SlashCommandEvent event) {
        if(event.getName().equals("test")) {
            event.getTextChannel().sendMessage(event.getOption("input").getAsString()).complete();
            event.reply("true").setEphemeral(true).complete();
        }
    }

    @Override
    public void onButtonClick(@NotNull ButtonClickEvent event) {
        if(event.getButton().getId().contains("history")) {
            ReportManager.ReportUser user = ReportManager.getInstance().getReportUser(event.getButton().getId().split(":")[1]);
            if(user != null) {
                EmbedBuilder eb = new EmbedBuilder();
                eb.setAuthor(event.getGuild().getMemberById(user.getUserID()).getUser().getAsTag(), null, event.getGuild().getMemberById(user.getUserID()).getUser().getAvatarUrl())
                        .setTitle(null)
                        .setFooter(user.getWarns() + " warning(s), " + user.getLocks() + " lock(s), " + user.getDeletes() + " delete(s), " + user.getBans() + " ban(s)")
                        .setDescription("PLACEHOLDER");
                event.replyEmbeds(eb.build()).setEphemeral(true).queue();
            }
        }
    }

    @Override
    public void onSelectionMenu(@NotNull SelectionMenuEvent event) {
        if(event.getComponent().getId().contains("menu:reportbot")) {
            //TODO - Could be null
            ReportManager.Report report = ReportManager.getInstance().getReport(UUID.fromString(event.getComponent().getId().split(":")[2]));
            String userName = event.getGuild().getMemberById(report.getReportedUser().getUserID()).getUser().getAsTag();
            SelectOption option = event.getInteraction().getSelectedOptions().get(0);
            switch (option.getValue()) {
                case "warn":
                    event.reply("PLACEHOLDER: Warned " + userName).setEphemeral(true).queue();
                    report.setResult(ReportAction.WARN, event.getMember().getId(), "PLACEHOLDER");
                    ReportBot.getInstance().logAction(report);
                    report.getReportedUser().addWarn();
                    break;
                case "ban":
                    event.getGuild().getMemberById(report.getReportedUser().getUserID()).ban(0, "Violating the rules").complete();
                    event.editSelectionMenu(null).complete();
                    event.getMessage().editMessageComponents(ActionRow.of(event.getMessage().getActionRows().get(1).getButtons().get(0).asEnabled(),
                                    event.getMessage().getActionRows().get(1).getButtons().get(1).asEnabled()),
                            ActionRow.of(event.getMessage().getActionRows().get(2).getButtons().get(0).withStyle(ButtonStyle.SUCCESS).withLabel("Banned by " + event.getUser().getAsTag()))).complete();
                    report.setResult(ReportAction.BAN, event.getMember().getId(), "PLACEHOLDER");
                    ReportBot.getInstance().logAction(report);
                    report.getReportedUser().addBan();
                    break;
                case "delete":
                    event.getGuild().getTextChannelById(report.getChannelID()).getHistoryAround(report.getMessageID(), 5).complete().getMessageById(report.getMessageID()).delete().queue();
                    event.editSelectionMenu(null).complete();
                    event.getMessage().editMessageComponents(ActionRow.of(event.getMessage().getActionRows().get(1).getButtons().get(0).asDisabled(),
                            event.getMessage().getActionRows().get(1).getButtons().get(1).asEnabled()),
                            ActionRow.of(event.getMessage().getActionRows().get(2).getButtons().get(0).withStyle(ButtonStyle.SUCCESS).withLabel("Deleted by " + event.getUser().getAsTag()))).complete();
                    report.setResult(ReportAction.DELETE, event.getMember().getId(), null);
                    ReportBot.getInstance().logAction(report);
                    report.getReportedUser().addDelete();
                    break;
                case "lock":
                    event.reply("PLACEHOLDER: Jailed " + userName).setEphemeral(true).queue();
                    report.setResult(ReportAction.LOCK, event.getMember().getId(), null);
                    ReportBot.getInstance().logAction(report);
                    report.getReportedUser().addLock();
                    break;
                case "complete":
                    event.editSelectionMenu(null).complete();
                    event.getMessage().editMessageComponents(ActionRow.of(event.getMessage().getActionRows().get(1).getButtons().get(0).asEnabled(),
                            event.getMessage().getActionRows().get(1).getButtons().get(1).asEnabled()),
                            ActionRow.of(event.getMessage().getActionRows().get(2).getButtons().get(0).withStyle(ButtonStyle.SUCCESS).withLabel("Completed by " + event.getUser().getAsTag()))).complete();
                    ReportBot.getInstance().logAction(report);
                    report.setResult(ReportAction.COMPLETE, event.getMember().getId(), null);
                    break;
            }
            report.setActionAdmin(event.getMember().getUser().getAsTag());
        }
    }
}
