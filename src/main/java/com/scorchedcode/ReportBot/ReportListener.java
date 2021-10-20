package com.scorchedcode.ReportBot;

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
    public void onSelectionMenu(@NotNull SelectionMenuEvent event) {
        if(event.getComponent().getId().contains("menu:reportbot")) {
            //TODO - Could be null
            ReportManager.Report report = ReportManager.getInstance().getReport(UUID.fromString(event.getComponent().getId().split(":")[2]));
            String userName = report.getReportedUser();
            SelectOption option = event.getInteraction().getSelectedOptions().get(0);
            switch (option.getValue()) {
                case "warn":
                    event.reply("PLACEHOLDER: Warned " + userName).setEphemeral(true).queue();
                    break;
                case "ban":
                    event.getGuild().getMemberByTag(report.getReportedUser()).ban(0, "Violating the rules").complete();
                    event.editSelectionMenu(null).complete();
                    event.getMessage().editMessageComponents(ActionRow.of(event.getMessage().getActionRows().get(1).getButtons().get(0).asEnabled(),
                                    event.getMessage().getActionRows().get(1).getButtons().get(1).asEnabled()),
                            ActionRow.of(event.getMessage().getActionRows().get(2).getButtons().get(0).withStyle(ButtonStyle.SUCCESS).withLabel("Banned by " + event.getUser().getAsTag()))).complete();
                    break;
                case "delete":
                    event.getGuild().getTextChannelById(report.getChannelID()).getHistoryAround(report.getMessageID(), 5).complete().getMessageById(report.getMessageID()).delete().queue();
                    event.editSelectionMenu(null).complete();
                    event.getMessage().editMessageComponents(ActionRow.of(event.getMessage().getActionRows().get(1).getButtons().get(0).asDisabled(),
                            event.getMessage().getActionRows().get(1).getButtons().get(1).asEnabled()),
                            ActionRow.of(event.getMessage().getActionRows().get(2).getButtons().get(0).withStyle(ButtonStyle.SUCCESS).withLabel("Deleted by " + event.getUser().getAsTag()))).complete();
                    break;
                case "lock":
                    event.reply("PLACEHOLDER: Jailed " + userName).setEphemeral(true).queue();
                    break;
                case "complete":
                    event.editSelectionMenu(null).complete();
                    event.getMessage().editMessageComponents(ActionRow.of(event.getMessage().getActionRows().get(1).getButtons().get(0).asEnabled(),
                            event.getMessage().getActionRows().get(1).getButtons().get(1).asEnabled()),
                            ActionRow.of(event.getMessage().getActionRows().get(2).getButtons().get(0).withStyle(ButtonStyle.SUCCESS).withLabel("Completed by " + event.getUser().getAsTag()))).complete();
                    break;
            }
        }
    }
}
