package com.yourtag.notifier.config;

/**
 * Railway'de çalışırken ayarlar config.json'dan DEĞİL,
 * Railway Dashboard'dan girilen Environment Variable'lardan okunur.
 *
 * Bu sayede token ve ID'leri kod içine gömmüş olmayız — güvenli!
 *
 * Railway Dashboard → Variables sekmesine şunları ekle:
 *   DISCORD_BOT_TOKEN      → Bot token'ın
 *   DISCORD_CHANNEL_ID     → Kanal ID'n
 *   YOUTUBE_CHANNEL_ID     → YouTube kanal ID'n
 *   CHECK_INTERVAL_MINUTES → Kontrol sıklığı (varsayılan: 5)
 *   NOTIFICATION_MESSAGE   → Bildirim mesajı (varsayılan: 🎥 Yeni video!)
 */
public class Config {

    public final String discord_bot_token;
    public final String discord_channel_id;
    public final String youtube_channel_id;
    public final int check_interval_minutes;
    public final String notification_message;

    private Config() {
        // Environment variable'lardan oku
        this.discord_bot_token     = requireEnv("DISCORD_BOT_TOKEN");
        this.discord_channel_id    = requireEnv("DISCORD_CHANNEL_ID");
        this.youtube_channel_id    = requireEnv("YOUTUBE_CHANNEL_ID");
        this.notification_message  = getEnvOrDefault("NOTIFICATION_MESSAGE", "🎥 Yeni video yayınlandı!");

        // Sayısal değer — parse hatasına karşı güvenli oku
        String intervalStr = getEnvOrDefault("CHECK_INTERVAL_MINUTES", "5");
        int interval;
        try {
            interval = Integer.parseInt(intervalStr);
        } catch (NumberFormatException e) {
            System.err.println("[Config] CHECK_INTERVAL_MINUTES geçersiz, varsayılan 5 dakika kullanılıyor.");
            interval = 5;
        }
        this.check_interval_minutes = interval;
    }

    /**
     * Config nesnesini oluşturur ve environment variable'ları doğrular.
     */
    public static Config load() {
        System.out.println("[Config] Environment variable'lar okunuyor...");
        Config config = new Config();
        System.out.println("[Config] ✅ Tüm ayarlar yüklendi.");
        System.out.println("[Config] YouTube Kanal ID : " + config.youtube_channel_id);
        System.out.println("[Config] Discord Kanal ID : " + config.discord_channel_id);
        System.out.println("[Config] Kontrol Aralığı  : " + config.check_interval_minutes + " dakika");
        return config;
    }

    /**
     * Zorunlu bir environment variable okur.
     * Tanımlanmamışsa uygulamayı durdurur.
     */
    private static String requireEnv(String key) {
        String value = System.getenv(key);
        if (value == null || value.isBlank()) {
            throw new RuntimeException(
                "[Config] ❌ Zorunlu environment variable eksik: " + key +
                "\nRailway Dashboard → Variables sekmesine ekle!"
            );
        }
        return value.trim();
    }

    /**
     * Opsiyonel environment variable okur.
     * Tanımlanmamışsa varsayılan değeri döner.
     */
    private static String getEnvOrDefault(String key, String defaultValue) {
        String value = System.getenv(key);
        return (value != null && !value.isBlank()) ? value.trim() : defaultValue;
    }
}
