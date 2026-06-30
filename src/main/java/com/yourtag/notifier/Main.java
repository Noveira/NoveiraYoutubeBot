package com.yourtag.notifier;

import com.yourtag.notifier.config.Config;
import com.yourtag.notifier.discord.DiscordNotifier;
import com.yourtag.notifier.youtube.YouTubePoller;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import java.security.cert.X509Certificate;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Discord YouTube Notifier - Ana giriş noktası
 * Railway üzerinde 7/24 çalışır
 */
public class Main {

    public static void main(String[] args) {
        System.out.println("========================================");
        System.out.println("   Discord YouTube Notifier v1.0.0");
        System.out.println("   Railway Cloud Edition");
        System.out.println("========================================");

        // SSL certificate doğrulaması geçici olarak devre dışı bırak
        // (YouTube'a bağlantı sorununu çözmek için)
        disableSslVerification();

        // 1. Config dosyasını oku
        Config config;
        try {
            config = Config.load();
            System.out.println("[Main] ✅ Config yüklendi.");
            System.out.println("[Main] YouTube Kanal ID : " + config.youtube_channel_id);
            System.out.println("[Main] Discord Kanal ID : " + config.discord_channel_id);
            System.out.println("[Main] Kontrol Aralığı  : " + config.check_interval_minutes + " dakika");
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

        // 4. Scheduler: her N dakikada bir YouTube kontrol et
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

        scheduler.scheduleAtFixedRate(() -> {
            System.out.println("\n[Scheduler] ⏰ YouTube kontrol ediliyor...");

            try {
                String newVideoUrl = poller.checkForNewVideo();

                if (newVideoUrl != null) {
                    // Yeni video bulundu — Discord'a bildir
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

    /**
     * SSL certificate doğrulamasını devre dışı bırak
     * (SADECE GELIŞTIRME/YOUTUBE SORUNU İÇİN)
     */
    private static void disableSslVerification() {
        try {
            SSLContext sc = SSLContext.getInstance("TLSv1.2");
            sc.init(null, new javax.net.ssl.TrustManager[]{
                    new javax.net.ssl.X509TrustManager() {
                        public void checkClientTrusted(X509Certificate[] chain, String authType) {}
                        public void checkServerTrusted(X509Certificate[] chain, String authType) {}
                        public X509Certificate[] getAcceptedIssuers() {
                            return new X509Certificate[0];
                        }
                    }
            }, new java.security.SecureRandom());

            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
            HttpsURLConnection.setDefaultHostnameVerifier((hostname, session) -> true);

            System.out.println("[Main] SSL verification devre dışı bırakıldı");
        } catch (Exception e) {
            System.err.println("[Main] SSL devre dışı bırakma hatası: " + e.getMessage());
        }
    }
}
