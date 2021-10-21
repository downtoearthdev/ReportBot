package com.scorchedcode.ReportBot;

import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

public class ReactionListener extends ListenerAdapter {

    public ReactionListener() {
    }

    @Override
    public void onMessageReactionAdd(@NotNull MessageReactionAddEvent event) {
        String warningAlias = event.getReactionEmote().getAsReactionCode();
        //event.getChannel().sendMessage("Emoji alias: " +  warningAlias).queue();
        //event.getTextChannel().sendMessage(warningAlias).queue();
        if(warningAlias.contains("\u26A0")) {
            event.retrieveMessage().complete().clearReactions(event.getReactionEmote().getEmoji()).queue();
            ReportManager.getInstance().makeReport(event.getMessageId(), event.getTextChannel().getId(), event.retrieveMessage().complete().getAuthor().getId(), event.getUser().getAsTag());
        }
    }
}
