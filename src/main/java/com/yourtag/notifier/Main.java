package com.yourtag.notifier;

import com.yourtag.notifier.config.Config;
import com.yourtag.notifier.discord.DiscordNotifier;
import com.yourtag.notifier.youtube.YouTubePoller;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Discord YouTube Notifier - Ana giriş noktası
 * Railway üzerinde 7/24 çalışacak şekilde yapılandırılmıştır.
 */
public class Main {

    public static void main(String[] args) {
        System.out.println("========================================");
        System.out.println("   Discord YouTube Notifier v1.0.0");
        System.out.println("   Railway Cloud Edition");
        System.out.println("========================================");

        // 1. Environment variable'lardan config yükle
        Config config;
        try {
            config = Config.load();
        } catch (Exception e) {
            System.err.println("[Main] ❌ Config hatası: " + e.getMessage());
            System.exit(1);
            return;
        }

        // 2. Discord bot'u başlat
        DiscordNotifier notifier;
        try {
            notifier = new DiscordNotifier(
                    config.discord_bot_token,
                    config.discord_channel_id
            );
        } catch (InterruptedException e) {
            System.err.println("[Main] ❌ Discord bağlantı hatası: " + e.getMessage());
            System.exit(1);
            return;
        }

        // 3. YouTube poller oluştur
        YouTubePoller poller = new YouTubePoller(config.youtube_channel_id);

        // 4. Scheduler: her N dakikada YouTube kontrol et
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

        scheduler.scheduleAtFixedRate(() -> {
            System.out.println("\n[Scheduler] ⏰ YouTube kontrol ediliyor...");
            try {
                String newVideoUrl = poller.checkForNewVideo();
                if (newVideoUrl != null) {
                    notifier.sendNotification(config.notification_message, newVideoUrl);
                }
            } catch (Exception e) {
                System.err.println("[Scheduler] ❌ Hata: " + e.getMessage());
            }
        }, 0, config.check_interval_minutes, TimeUnit.MINUTES);

        System.out.println("[Main] ✅ Bot Railway'de çalışıyor!");

        // 5. Kapanma sinyali gelince temiz kapat
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("[Main] Kapatılıyor...");
            scheduler.shutdown();
            notifier.shutdown();
        }));
    }
}
