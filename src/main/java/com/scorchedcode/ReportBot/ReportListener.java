package com.scorchedcode.ReportBot;

import net.dv8tion.jda.api.events.interaction.SelectionMenuEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.selections.SelectOption;
import org.jetbrains.annotations.NotNull;

public class ReportListener extends ListenerAdapter {

    @Override
    public void onSelectionMenu(@NotNull SelectionMenuEvent event) {
        SelectOption option = event.getInteraction().getSelectedOptions().get(0);
        switch (option.getLabel()) {
            case "warn":
                break;
            case "ban":
                break;
            case "delete":
                //Do something
                break;
            case "lock":
                break;
            case "complete":
                break;
        }
    }
}
