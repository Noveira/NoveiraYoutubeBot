package com.yourtag.notifier.youtube;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Checks YouTube channel RSS feed using OkHttp.
 * Resolves SSL certificate issues with modern HTTP client.
 */
public class YouTubePoller {

    private static final String RSS_BASE_URL =
            "https://www.youtube.com/feeds/videos.xml?channel_id=";

    private static final String CACHE_FILE = "/tmp/last_video.txt";
    
    private final String channelId;
    private final OkHttpClient httpClient;
    private String lastVideoId = "";

    public YouTubePoller(String channelId) {
        this.channelId = channelId;
        // Create OkHttp client with modern TLS and certificate handling
        this.httpClient = new OkHttpClient.Builder()
                .build();
        loadCacheFromFile();
    }

    /**
     * Fetches and checks YouTube RSS feed using OkHttp.
     *
     * @return New video URL if found, null otherwise
     */
    public String checkForNewVideo() {
        try {
            String feedUrl = RSS_BASE_URL + channelId;
            System.out.println("[YouTube] Feed URL: " + feedUrl);

            // Make request with OkHttp
            Request request = new Request.Builder()
                    .url(feedUrl)
                    .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    System.err.println("[YouTube] ❌ HTTP Error: " + response.code());
                    return null;
                }

                String feedContent = response.body().string();
                System.out.println("[YouTube] Feed downloaded successfully (" + feedContent.length() + " bytes)");

                // Parse RSS feed from string
                SyndFeedInput input = new SyndFeedInput();
                SyndFeed feed = input.build(new XmlReader(
                        new ByteArrayInputStream(feedContent.getBytes(StandardCharsets.UTF_8))
                ));

                List<SyndEntry> entries = feed.getEntries();
                if (entries == null || entries.isEmpty()) {
                    System.out.println("[YouTube] Feed is empty.");
                    return null;
                }

                // Get latest video
                SyndEntry latestEntry = entries.get(0);
                String latestUrl = latestEntry.getLink();
                String latestId = extractVideoId(latestUrl);
                String latestTitle = latestEntry.getTitle();

                System.out.println("[YouTube] Latest video ID: " + latestId);
                System.out.println("[YouTube] Cached video ID:  " + lastVideoId);

                if (!latestId.equals(lastVideoId) && !latestId.isEmpty()) {
                    // New video detected!
                    lastVideoId = latestId;
                    saveCacheToFile(latestId);
                    System.out.println("[YouTube] ✅ New video found: " + latestTitle + " → " + latestUrl);
                    return latestUrl;
                } else {
                    System.out.println("[YouTube] No new videos. Latest: " + latestTitle);
                }

            }

        } catch (Exception e) {
            System.err.println("[YouTube] ❌ Feed check error: " + e.getMessage());
            e.printStackTrace();
        }

        return null;
    }

    /**
     * Extracts video ID from YouTube URL.
     */
    private String extractVideoId(String url) {
        if (url == null || url.isEmpty()) return "";
        if (url.contains("v="))        return url.split("v=")[1].split("&")[0];
        if (url.contains("youtu.be/")) return url.split("youtu.be/")[1].split("\\?")[0];
        return url;
    }

    /**
     * Load last video ID from cache file.
     */
    private void loadCacheFromFile() {
        try {
            String cached = Files.readString(Path.of(CACHE_FILE)).trim();
            if (!cached.isEmpty()) {
                lastVideoId = cached;
                System.out.println("[Cache] Loaded: " + lastVideoId);
            }
        } catch (Exception e) {
            System.out.println("[Cache] Cache file not found (first run).");
        }
    }

    /**
     * Save last video ID to cache file.
     */
    private void saveCacheToFile(String videoId) {
        try {
            Files.writeString(Path.of(CACHE_FILE), videoId);
            System.out.println("[Cache] Saved: " + videoId);
        } catch (Exception e) {
            System.err.println("[Cache] Write error: " + e.getMessage());
        }
    }
}
