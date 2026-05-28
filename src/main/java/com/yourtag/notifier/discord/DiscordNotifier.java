package com.yourtag.notifier.discord;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.requests.GatewayIntent;

import java.awt.Color;
import java.time.Instant;

/**
 * Connects to Discord via JDA and sends video notifications.
 */
public class DiscordNotifier {

    private final JDA jda;
    private final String channelId;

    public DiscordNotifier(String botToken, String channelId) throws InterruptedException {
        System.out.println("[Discord] Starting bot...");

        this.jda = JDABuilder.createDefault(botToken)
                .enableIntents(GatewayIntent.GUILD_MESSAGES)
                .build();

        this.jda.awaitReady();
        this.channelId = channelId;

        System.out.println("[Discord] ✅ Connected → " + jda.getSelfUser().getAsTag());
    }

    /**
     * Sends an embed notification to the Discord channel.
     */
    public void sendNotification(String message, String videoUrl) {
        TextChannel channel = jda.getTextChannelById(channelId);

        if (channel == null) {
            System.err.println("[Discord] ❌ Channel not found! ID: " + channelId);
            return;
        }

        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("📺 New Video Just Dropped!", videoUrl)
                .setDescription(message + "\n\n🔗 **Click to watch:** " + videoUrl)
                .setColor(Color.RED)
                .setTimestamp(Instant.now())
                .setFooter("YouTube Notifier Bot");

        channel.sendMessageEmbeds(embed.build()).queue(
                success -> System.out.println("[Discord] ✅ Notification sent!"),
                error   -> System.err.println("[Discord] ❌ Failed to send: " + error.getMessage())
        );
    }

    public void shutdown() {
        System.out.println("[Discord] Shutting down bot...");
        jda.shutdown();
    }
}
