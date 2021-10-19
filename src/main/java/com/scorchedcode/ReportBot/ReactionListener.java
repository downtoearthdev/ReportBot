package com.scorchedcode.ReportBot;

import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

public class ReactionListener extends ListenerAdapter {

    public ReactionListener() {
    }

    @Override
    public void onMessageReactionAdd(@NotNull MessageReactionAddEvent event) {
        String warningAlias = event.getReactionEmote().getName();
        //event.getChannel().sendMessage("Emoji alias: " +  warningAlias).queue();
        if(!warningAlias.contains("warning"))
            event.retrieveMessage().complete().clearReactions().queue();
        else {

        }
    }
}
