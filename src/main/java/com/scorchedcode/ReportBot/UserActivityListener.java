package com.scorchedcode.ReportBot;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.channel.ChannelDeleteEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRemoveEvent;
import net.dv8tion.jda.api.events.message.MessageDeleteEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.MessageUpdateEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class UserActivityListener extends ListenerAdapter {

    @Override
    public void onGuildMemberRemove(@NotNull GuildMemberRemoveEvent event) {
        ReportBot.getInstance().logAction(event.getMember().getAsMention() + " has left.");
    }

    @Override
    public void onChannelDelete(@NotNull ChannelDeleteEvent event) {
        if(event.getChannelType() == ChannelType.GUILD_PRIVATE_THREAD) {
            if(ReportBot.lockedCache.containsKey(event.getChannel().getName())) {
                Role[] roles = ReportBot.lockedCache.get(event.getChannel().getName());
                Member lockedMember = event.getJDA().getGuilds().get(0).getMemberById(event.getChannel().getName());
                Role tempRole = lockedMember.getRoles().get(0);
                event.getJDA().getGuilds().get(0).removeRoleFromMember(lockedMember, lockedMember.getRoles().get(0)).complete();
                tempRole.delete().complete();
                ReportBot.lockedCache.remove(event.getChannel().getName());
                for(int x = 0; x < roles.length; x++)
                    event.getJDA().getGuilds().get(0).addRoleToMember(event.getChannel().getName(), roles[x]).complete();
            }
        }
    }

    @Override
    public void onMessageDelete(@NotNull MessageDeleteEvent event) {
        String msgID = event.getMessageId();
        ArrayList<String> foundData = ReportDB.getInstance().retrieveMessage(msgID);
        if(foundData != null) {
            EmbedBuilder eb = new EmbedBuilder();
            eb.setAuthor(event.getGuild().getMemberById(foundData.get(1)).getUser().getAsTag(), null, event.getGuild().getMemberById(foundData.get(1)).getUser().getAvatarUrl())
                    .setTitle(null)
                    .setColor(Color.MAGENTA)
                    .setFooter("User ID: " + foundData.get(1))
                    .setTimestamp(LocalDateTime.now())
                    .addField("Content", foundData.get(2), true)
                    .setDescription("Message deleted in " + event.getChannel().getAsMention());
            event.getGuild().getTextChannelById(ReportBot.nonReportRoomId).sendMessageEmbeds(eb.build()).queue();
        }
    }

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        if(event.getMessage().getContentRaw().contains("discord.gg")) {
            if(!event.getMember().getId().equals(ReportBot.getInstance().getAPI().getSelfUser().getId())) {
                ReportBot.getInstance().logAction("User " + event.getMember().getAsMention() + " posted this message: " + event.getMessage().getContentRaw() + " which has been deleted due to the invitation link.");
                event.getMessage().delete().complete();
                return;
            }
        }
        if(!event.getMember().getId().equals(ReportBot.getInstance().getAPI().getSelfUser().getId())) {
            String contentMessage = "";
            if(event.getMessage().getAttachments().size() == 1) {
                List<Message.Attachment> files = event.getMessage().getAttachments();
                contentMessage = files.get(0).getUrl();
            }
            ReportDB.getInstance().serializeMessage(event.getMessageId(), event.getMember().getId(), contentMessage + " " + event.getMessage().getContentDisplay());
        }
    }

    @Override
    public void onMessageUpdate(@NotNull MessageUpdateEvent event) {
        if(!event.getMember().getId().equals(ReportBot.getInstance().getAPI().getSelfUser().getId())) {
            String msgID = event.getMessageId();
            String contentMessage = "";
            ArrayList<String> foundData = ReportDB.getInstance().retrieveMessage(msgID);
            if(foundData != null) {
                if(event.getMessage().getAttachments().size() == 1) {
                    List<Message.Attachment> files = event.getMessage().getAttachments();
                    contentMessage = files.get(0).getUrl();
                }
                EmbedBuilder eb = new EmbedBuilder();
                eb.setAuthor(event.getGuild().getMemberById(foundData.get(1)).getUser().getAsTag(), null, event.getGuild().getMemberById(foundData.get(1)).getUser().getAvatarUrl())
                        .setTitle(null)
                        .setColor(Color.YELLOW)
                        .setFooter("User ID: " + foundData.get(1))
                        .setTimestamp(LocalDateTime.now())
                        .addField("Old Message", foundData.get(2), false)
                        .addField("New Message", contentMessage + " " + event.getMessage().getContentDisplay(), false)
                        .setDescription("Message edited in " + event.getChannel().getAsMention() + " - [Jump to message](" + event.getMessage().getJumpUrl() + ")");
                event.getGuild().getTextChannelById(ReportBot.nonReportRoomId).sendMessageEmbeds(eb.build()).queue();
            }

            ReportDB.getInstance().serializeMessage(event.getMessageId(), event.getMember().getId(), contentMessage + " " +  event.getMessage().getContentDisplay());
        }
    }
}
