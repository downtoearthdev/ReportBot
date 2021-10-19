package com.scorchedcode.ReportBot;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.json.JSONObject;

import javax.security.auth.login.LoginException;
import java.io.*;
import java.nio.file.Files;

public class ReportBot {
    private static ReportBot instance;
    private String TOKEN;
    private String status;
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
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (TOKEN == null || TOKEN.isEmpty() || status == null || status.isEmpty())
            System.exit(0);
    }

    private void setStatus() {
        api.getPresence().setPresence(OnlineStatus.ONLINE, Activity.playing(status));
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
