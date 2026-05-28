package com.yourtag.notifier.youtube;

import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * YouTube kanalının RSS feed'ini periyodik olarak kontrol eder.
 * API key gerektirmez — herkese açık RSS endpoint kullanır.
 *
 * Railway notu: Cache dosyası /tmp dizinine yazılır.
 * Railway her deploy'da container'ı sıfırlar, bu yüzden /tmp kullanılır.
 * (Deploy anında en fazla 1 tekrar bildirim gönderebilir — normaldir.)
 */
public class YouTubePoller {

    private static final String RSS_BASE_URL =
            "https://www.youtube.com/feeds/videos.xml?channel_id=";

    // Railway container'ında yazılabilir geçici dizin
    private static final String CACHE_FILE = "/tmp/last_video.txt";

    private final String channelId;

    public YouTubePoller(String channelId) {
        this.channelId = channelId;
    }

    /**
     * YouTube RSS feed'ini çeker ve en son videoyu kontrol eder.
     *
     * @return Yeni video varsa URL'si, yoksa null
     */
    public String checkForNewVideo() {
        try {
            String feedUrl = RSS_BASE_URL + channelId;

            SyndFeedInput input = new SyndFeedInput();
            SyndFeed feed = input.build(new XmlReader(new URL(feedUrl)));

            List<SyndEntry> entries = feed.getEntries();
            if (entries == null || entries.isEmpty()) {
                System.out.println("[YouTube] Feed boş döndü.");
                return null;
            }

            // İlk entry = en yeni video
            SyndEntry latestEntry = entries.get(0);
            String latestUrl   = latestEntry.getLink();
            String latestId    = extractVideoId(latestUrl);
            String latestTitle = latestEntry.getTitle();

            String cachedId = readCache();

            if (!latestId.equals(cachedId)) {
                writeCache(latestId);
                System.out.println("[YouTube] ✅ Yeni video: " + latestTitle + " → " + latestUrl);
                return latestUrl;
            } else {
                System.out.println("[YouTube] Yeni video yok. Son: " + latestTitle);
            }

        } catch (Exception e) {
            System.err.println("[YouTube] ❌ Feed hatası: " + e.getMessage());
        }

        return null;
    }

    private String extractVideoId(String url) {
        if (url == null || url.isEmpty()) return "";
        if (url.contains("v="))        return url.split("v=")[1].split("&")[0];
        if (url.contains("youtu.be/")) return url.split("youtu.be/")[1].split("\\?")[0];
        return url;
    }

    private String readCache() {
        try {
            return Files.readString(Path.of(CACHE_FILE)).trim();
        } catch (IOException e) {
            return ""; // Dosya yok = ilk çalışma
        }
    }

    private void writeCache(String videoId) {
        try {
            Files.writeString(Path.of(CACHE_FILE), videoId);
        } catch (IOException e) {
            System.err.println("[Cache] Yazma hatası: " + e.getMessage());
        }
    }
}
