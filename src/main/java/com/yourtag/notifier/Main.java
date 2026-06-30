package com.yourtag.notifier;

import com.yourtag.notifier.config.Config;
import com.yourtag.notifier.discord.DiscordNotifier;
import com.yourtag.notifier.youtube.YouTubePoller;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Discord YouTube Notifier - Main entry point
 * Runs 24/7 on Railway Cloud
 */
public class Main {

    public static void main(String[] args) {
        System.out.println("========================================");
        System.out.println("   Discord YouTube Notifier v1.0.0");
        System.out.println("   Railway Cloud Edition");
        System.out.println("========================================");

        // 1. Load configuration
        Config config;
        try {
            config = Config.load();
            System.out.println("[Main] ✅ Config loaded.");
            System.out.println("[Main] YouTube Channel ID : " + config.youtube_channel_id);
            System.out.println("[Main] Discord Channel ID : " + config.discord_channel_id);
            System.out.println("[Main] Check Interval    : " + config.check_interval_minutes + " minutes");
        } catch (Exception e) {
            System.err.println("[Main] ❌ Config error: " + e.getMessage());
            System.exit(1);
            return;
        }

        // 2. Start Discord bot
        DiscordNotifier notifier;
        try {
            notifier = new DiscordNotifier(
                    config.discord_bot_token,
                    config.discord_channel_id
            );
        } catch (InterruptedException e) {
            System.err.println("[Main] ❌ Discord connection error: " + e.getMessage());
            System.exit(1);
            return;
        }

        // 3. Create YouTube poller
        YouTubePoller poller = new YouTubePoller(config.youtube_channel_id);

        // 4. Scheduler: check YouTube every N minutes
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

        scheduler.scheduleAtFixedRate(() -> {
            System.out.println("\n[Scheduler] ⏰ Checking YouTube...");

            try {
                String newVideoUrl = poller.checkForNewVideo();

                if (newVideoUrl != null) {
                    // New video found - notify Discord
                    notifier.sendNotification(config.notification_message, newVideoUrl);
                }
            } catch (Exception e) {
                System.err.println("[Scheduler] ❌ Error: " + e.getMessage());
                e.printStackTrace();
            }

        }, 0, config.check_interval_minutes, TimeUnit.MINUTES);

        System.out.println("[Main] ✅ Bot is running on Railway!");

        // 5. Shutdown hook for clean shutdown
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("[Main] Shutting down...");
            scheduler.shutdown();
            notifier.shutdown();
        }));
    }
}
