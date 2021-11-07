package com.scorchedcode.ReportBot;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.channel.ChannelDeleteEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRemoveEvent;
import net.dv8tion.jda.api.events.guild.member.update.GuildMemberUpdateNicknameEvent;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.events.message.MessageDeleteEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.MessageUpdateEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.ButtonStyle;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;

public class UserActivityListener extends ListenerAdapter {

    protected static HashMap<String, ReportManager.Report> pendingMap = new HashMap<>();

    @Override
    public void onGuildMemberUpdateNickname(@NotNull GuildMemberUpdateNicknameEvent event) {
        if(event.getNewNickname() != null)
            ReportDB.getInstance().serializeNickname(event.getUser().getId(), event.getNewNickname());
        EmbedBuilder eb = new EmbedBuilder();
        eb.setAuthor(event.getUser().getAsTag(), null, event.getUser().getAvatarUrl())
                .setTitle("Nickname Change")
                .setColor(Color.YELLOW)
                .setFooter("User ID: " + event.getUser().getId())
                .setTimestamp(OffsetDateTime.now())
                .addField("Old Nickname", (event.getOldNickname() == null) ? event.getUser().getName(): event.getOldNickname(), false)
                .addField("New Nickname", (event.getNewNickname() == null) ? event.getUser().getName(): event.getNewNickname(), false);
        event.getGuild().getTextChannelById(ReportBot.logRoomId).sendMessageEmbeds(eb.build()).queue();
    }

    @Override
    public void onGuildMemberJoin(@NotNull GuildMemberJoinEvent event) {
        event.getGuild().getTextChannelById(ReportBot.welcomeRoomId).sendMessage(ReportBot.getInstance().getWelcomeMsg().replaceAll("\\[u\\]", event.getMember().getAsMention())).queue();
    }

    @Override
    public void onGuildMemberRemove(@NotNull GuildMemberRemoveEvent event) {
        ReportBot.getInstance().logAction(event.getUser().getAsTag() + " has left.");
    }

    @Override
    public void onChannelDelete(@NotNull ChannelDeleteEvent event) {
        if (event.getChannelType() == ChannelType.GUILD_PRIVATE_THREAD) {
            ReportManager.Report rep = ReportManager.getInstance().getReport(event.getChannel().getName());
            if (rep != null && ReportBot.lockedCache.containsKey(rep.getReportedUser())) {
                Role[] roles = ReportBot.lockedCache.get(rep.getReportedUser());
                Member lockedMember = event.getJDA().getGuilds().get(0).retrieveMemberById(rep.getReportedUser()).complete();
                Role tempRole = lockedMember.getRoles().get(0);
                event.getJDA().getGuilds().get(0).removeRoleFromMember(lockedMember, lockedMember.getRoles().get(0)).complete();
                tempRole.delete().complete();
                ReportBot.lockedCache.remove(rep.getReportedUser());
                for (int x = 0; x < roles.length; x++)
                    event.getJDA().getGuilds().get(0).addRoleToMember(rep.getReportedUser(), roles[x]).complete();
                Message reportMessage = event.getJDA().getTextChannelById(ReportBot.reportRoomId).getHistoryAround(rep.getReportID(), 5).complete().getMessageById(rep.getReportID());
                reportMessage.editMessageComponents(ActionRow.of(reportMessage.getActionRows().get(1).getButtons().get(0).asEnabled(),
                                reportMessage.getActionRows().get(1).getButtons().get(1).asEnabled()),
                        ActionRow.of(reportMessage.getActionRows().get(2).getButtons().get(0).withStyle(ButtonStyle.SUCCESS).withLabel("Locked and completed by " + event.getJDA().getUserById(rep.getActionAdmin()).getAsTag()))).complete();
            }
        }
    }

    @Override
    public void onMessageDelete(@NotNull MessageDeleteEvent event) {
        String msgID = event.getMessageId();
        ArrayList<String> foundData = ReportDB.getInstance().retrieveMessage(msgID);
        if (foundData != null) {
            EmbedBuilder eb = new EmbedBuilder();
            eb.setAuthor(event.getJDA().getUserById(foundData.get(1)).getAsTag(), null, event.getJDA().getUserById(foundData.get(1)).getAvatarUrl())
                    .setTitle(null)
                    .setColor(Color.MAGENTA)
                    .setFooter("User ID: " + foundData.get(1))
                    .setTimestamp(OffsetDateTime.now())
                    .addField("Content", foundData.get(2), true)
                    .setDescription("Message deleted in " + event.getChannel().getAsMention());
            event.getGuild().getTextChannelById(ReportBot.logRoomId).sendMessageEmbeds(eb.build()).queue();
        }
    }

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        if (event.getMessage().getContentRaw().contains("discord.gg")) {
            if (!event.getMember().getId().equals(ReportBot.getInstance().getAPI().getSelfUser().getId())) {
                ReportBot.getInstance().logAction("User " + event.getMember().getAsMention() + " posted this message: " + event.getMessage().getContentRaw() + " which has been deleted due to the invitation link.");
                event.getMessage().delete().complete();
                return;
            }
        }

        if (pendingMap != null && !pendingMap.isEmpty() && pendingMap.containsKey(event.getMember().getId()) && event.getChannel().getId().equals(ReportBot.reportRoomId)) {
            ReportManager.Report rep = pendingMap.get(event.getMember().getId());
            Message msg = event.getJDA().getTextChannelById(ReportBot.reportRoomId).getHistoryAround(rep.getReportID(), 5).complete().getMessageById(rep.getReportID());
            if (pendingMap.get(event.getMember().getId()).getResultAction() == ReportAction.BAN) {
                msg.editMessageComponents(ActionRow.of(msg.getActionRows().get(1).getButtons().get(0).asEnabled(),
                                msg.getActionRows().get(1).getButtons().get(1).asEnabled()),
                        ActionRow.of(msg.getActionRows().get(2).getButtons().get(0).withStyle(ButtonStyle.SUCCESS).withLabel("Banned by " + event.getMember().getUser().getAsTag() + " for: " + event.getMessage().getContentDisplay()))).complete();
                pendingMap.remove(event.getMember().getId());
                rep.setResult(ReportAction.BAN, event.getMember().getId(), event.getMessage().getContentDisplay());
                event.getMessage().delete().complete();
                ReportListener.sendActionEmbed(rep);
                warnBanUser(rep.getReportedUser(), event.getMessage().getContentDisplay(), true);
                return;
            } else {
                msg.editMessageComponents(ActionRow.of(msg.getActionRows().get(1).getButtons().get(0).asEnabled(),
                                msg.getActionRows().get(1).getButtons().get(1).asEnabled()),
                        ActionRow.of(msg.getActionRows().get(2).getButtons().get(0).withStyle(ButtonStyle.SUCCESS).withLabel("Warned by " + event.getMember().getUser().getAsTag() + " for: " + event.getMessage().getContentDisplay()))).complete();
                pendingMap.remove(event.getMember().getId());
                rep.setResult(ReportAction.WARN, event.getMember().getId(), event.getMessage().getContentDisplay());
                event.getMessage().delete().complete();
                ReportListener.sendActionEmbed(rep);
                warnBanUser(rep.getReportedUser(), event.getMessage().getContentDisplay(), false);
                return;
            }
        }

        if (!event.isWebhookMessage() && !event.getMember().getId().equals(ReportBot.getInstance().getAPI().getSelfUser().getId())) {
            String contentMessage = "";
            if (event.getMessage().getAttachments().size() == 1) {
                List<Message.Attachment> files = event.getMessage().getAttachments();
                contentMessage = files.get(0).getUrl();
            }
            ReportDB.getInstance().serializeMessage(event.getMessageId(), event.getMember().getId(), contentMessage + " " + event.getMessage().getContentDisplay());
        }
    }

    @Override
    public void onMessageUpdate(@NotNull MessageUpdateEvent event) {
        if (!event.getMember().getId().equals(ReportBot.getInstance().getAPI().getSelfUser().getId())) {
            String msgID = event.getMessageId();
            String contentMessage = "";
            ArrayList<String> foundData = ReportDB.getInstance().retrieveMessage(msgID);
            if (foundData != null) {
                if (event.getMessage().getAttachments().size() == 1) {
                    List<Message.Attachment> files = event.getMessage().getAttachments();
                    contentMessage = files.get(0).getUrl();
                }
                EmbedBuilder eb = new EmbedBuilder();
                eb.setAuthor(event.getJDA().getUserById(foundData.get(1)).getAsTag(), null, event.getJDA().getUserById(foundData.get(1)).getAvatarUrl())
                        .setTitle(null)
                        .setColor(Color.YELLOW)
                        .setFooter("User ID: " + foundData.get(1))
                        .setTimestamp(OffsetDateTime.now())
                        .addField("Old Message", foundData.get(2), false)
                        .addField("New Message", contentMessage + " " + event.getMessage().getContentDisplay(), false)
                        .setDescription("Message edited in " + event.getChannel().getAsMention() + " - [Jump to message](" + event.getMessage().getJumpUrl() + ")");
                event.getGuild().getTextChannelById(ReportBot.logRoomId).sendMessageEmbeds(eb.build()).queue();
            }

            ReportDB.getInstance().serializeMessage(event.getMessageId(), event.getMember().getId(), contentMessage + " " + event.getMessage().getContentDisplay());
        }
    }

    private void warnBanUser(String userId, String warning, boolean ban) {
        Member doomedMember = ReportBot.getInstance().getAPI().getGuilds().get(0).getMemberById(userId);
        if (ban) {
            String doomedMessage = "Hello, hope you're well. You are being banned from the r/GabbyPetito Discord Server for the following reason: " + warning + "\n" +
                    "You may appeal this ban. Please visit the google form here to request consideration: https://docs.google.com/forms/d/e/1FAIpQLSdlPqMjaHE6I_-Ch3-YnNwFVbUoMgVAboK2bQrj7Bi1nOKojg/viewform?usp=sf_link";
            try {
                PrivateChannel chan = doomedMember.getUser().openPrivateChannel().complete();
                chan.sendMessage(doomedMessage).complete();
                doomedMember.ban(7, warning).complete(); //Set deldays to 5 for production
            } catch (Exception e) {
                TextChannel tempChannel = ReportBot.getInstance().getAPI().getGuilds().get(0).getCategoryById("903490755128078346").createTextChannel(userId).complete();
                Role tempRole = doomedMember.getGuild().createRole().setColor(Color.GRAY).setName(userId).complete();
                tempChannel.createPermissionOverride(tempRole).setDeny(Permission.ALL_PERMISSIONS).complete();
                tempChannel.getManager().putRolePermissionOverride(tempRole.getIdLong(), Arrays.asList(Permission.VIEW_CHANNEL, Permission.MESSAGE_HISTORY), null).putRolePermissionOverride(tempChannel.getGuild().getPublicRole().getIdLong(), null, Arrays.asList(Permission.VIEW_CHANNEL)).complete();
                for (Role role : doomedMember.getRoles())
                    doomedMember.getGuild().removeRoleFromMember(doomedMember, role).complete();
                //tempChannel.createPermissionOverride(tempRole).setAllow(Permission.VIEW_CHANNEL).setDeny(Permission.MESSAGE_HISTORY, Permission.MESSAGE_SEND).complete();
                tempChannel.getGuild().addRoleToMember(doomedMember, tempRole).complete();
                tempChannel.sendMessage(doomedMessage + "\nYou are in a private channel because you have DMs disabled. The ban will commence in 5 minutes.").complete();
                Timer botTimer = new Timer();
                botTimer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        doomedMember.ban(7, warning).complete();
                        tempChannel.delete().complete();
                        tempRole.delete().complete();
                    }
                }, 300000L);
            }
        } else {
            try {
                PrivateChannel chan = doomedMember.getUser().openPrivateChannel().complete();
                chan.sendMessage("You have been warned on the r/Gabbypetito Discord for the following reason:\n" + warning).complete();
            } catch (Exception e) {

            }
        }
    }

    @Override
    public void onSlashCommand(@NotNull SlashCommandEvent event) {
        //if (ReportBot.getInstance().isMod(event.getMember().getId())) ;
        //{
            if (event.getName().equals("history")) {
                if (event.getOption("handle") != null) {
                    event.deferReply(true).queue();
                    //event.replyEmbeds(eb.build()).setEphemeral(true).queue();
                    event.getHook().editOriginalEmbeds(ReportListener.getHistory(event.getOption("handle").getAsUser().getId())).queue();
                }
            }
            else if(event.getName().equals("history-uid")) {
                if (event.getOption("userid") != null) {
                    event.deferReply(true).queue();
                    //event.replyEmbeds(eb.build()).setEphemeral(true).queue();
                    event.getHook().editOriginalEmbeds(ReportListener.getHistory(event.getOption("userid").getAsString())).queue();
                }
            }
            else if (event.getName().equals("ban")) {
                if (event.getOption("handle") != null && (event.getOption("reason") != null || !event.getOption("reason").getAsString().isEmpty())) {
                    warnBanUser(event.getOption("handle").getAsUser().getId(), event.getOption("reason").getAsString(), true);
                    Message log = ReportListener.sendActionEmbed(event.getMember().getId(), event.getOption("handle").getAsUser().getId(), ReportAction.BAN, event.getOption("reason").getAsString());
                    ReportDB.getInstance().serializeBanWarn(log.getId(), event.getMember().getId(), event.getOption("handle").getAsUser().getId(), ReportAction.BAN, event.getOption("reason").getAsString());
                    event.reply("User has been banned from the server.").setEphemeral(true).queue();
                }
            } else if (event.getName().equals("warn")) {
                if (event.getOption("handle") != null && (event.getOption("reason") != null || !event.getOption("reason").getAsString().isEmpty())) {
                    warnBanUser(event.getOption("handle").getAsUser().getId(), event.getOption("reason").getAsString(), false);
                    Message log = ReportListener.sendActionEmbed(event.getMember().getId(), event.getOption("handle").getAsUser().getId(), ReportAction.WARN, event.getOption("reason").getAsString());
                    ReportDB.getInstance().serializeBanWarn(log.getId(), event.getMember().getId(), event.getOption("handle").getAsUser().getId(), ReportAction.WARN, event.getOption("reason").getAsString());
                    event.reply("User has been warned. In the case the user's DMs aren't enabled, they will miss this warning.").setEphemeral(true).queue();
                }
            } else if (event.getName().equals("setwelcome")) {
                if (event.getOption("msg") != null && !event.getOption("msg").getAsString().isEmpty()) {
                    ReportBot.getInstance().setWelcomeMsg(event.getOption("msg").getAsString());
                    event.reply("Welcome message set!").setEphemeral(true).queue();
                }
            }
            else if(event.getName().equals("userinfo")) {
                if(event.getOption("handle") != null) {
                    EmbedBuilder eb = new EmbedBuilder();
                    User user = event.getOption("handle").getAsUser();
                    String aliases = ReportDB.getInstance().retrieveNicknames(user.getId());
                    String roleString = "";
                    for(Role role : event.getGuild().retrieveMemberById(user.getId()).complete().getRoles())
                        roleString+=role.getAsMention()+"\n";
                    eb.setAuthor(user.getAsTag(), null, null)
                            .setTitle(null)
                            .setThumbnail(user.getAvatarUrl())
                            .setColor(new Color(0,87,77))
                            .setDescription("ID: " + event.getOption("handle").getAsUser().getId() + "\n" +
                                    "Roles (" + event.getGuild().retrieveMemberById(user.getId()).complete().getRoles().size() + " total):" + roleString + "\n" +
                                    "Joined Server: " + event.getGuild().retrieveMemberById(user.getId()).complete().getTimeJoined().format(DateTimeFormatter.ofPattern("MM/dd/YYYY")) + "\n" +
                                    "Joined Discord: " + user.getTimeCreated().format(DateTimeFormatter.ofPattern("MM/dd/YYYY")) +
                                    ((aliases != null) ? "\nAliases:\n" + aliases : ""));
                    event.replyEmbeds(eb.build()).setEphemeral(true).queue();
                }
            }
        //}
    }

}
