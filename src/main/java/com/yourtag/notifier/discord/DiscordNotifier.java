package com.yourtag.notifier.discord;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.requests.GatewayIntent;

import java.awt.Color;
import java.time.Instant;

/**
 * JDA kullanarak Discord'a bağlanır ve bildirim gönderir.
 */
public class DiscordNotifier {

    private final JDA jda;
    private final String channelId;

    public DiscordNotifier(String botToken, String channelId) throws InterruptedException {
        System.out.println("[Discord] Bot başlatılıyor...");

        this.jda = JDABuilder.createDefault(botToken)
                .enableIntents(GatewayIntent.GUILD_MESSAGES)
                .build();

        this.jda.awaitReady();
        this.channelId = channelId;

        System.out.println("[Discord] ✅ Bağlandı → " + jda.getSelfUser().getAsTag());
    }

    /**
     * Discord kanalına embed bildirim gönderir.
     */
    public void sendNotification(String message, String videoUrl) {
        TextChannel channel = jda.getTextChannelById(channelId);

        if (channel == null) {
            System.err.println("[Discord] ❌ Kanal bulunamadı! ID: " + channelId);
            return;
        }

        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("📺 Yeni Video Yayınlandı!", videoUrl)
                .setDescription(message + "\n\n🔗 **İzlemek için tıkla:** " + videoUrl)
                .setColor(Color.RED)
                .setTimestamp(Instant.now())
                .setFooter("YouTube Notifier Bot");

        channel.sendMessageEmbeds(embed.build()).queue(
                success -> System.out.println("[Discord] ✅ Bildirim gönderildi!"),
                error   -> System.err.println("[Discord] ❌ Gönderim hatası: " + error.getMessage())
        );
    }

    public void shutdown() {
        System.out.println("[Discord] Bot kapatılıyor...");
        jda.shutdown();
    }
}
