package com.yourtag.notifier.youtube;

import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

/**
 * Checks YouTube channel RSS feed for new videos.
 * Uses persistent memory instead of filesystem cache (more reliable on Railway).
 */
public class YouTubePoller {

    private static final String RSS_BASE_URL =
            "https://www.youtube.com/feeds/videos.xml?channel_id=";

    private final String channelId;
    private String lastVideoId = ""; // In-memory cache

    public YouTubePoller(String channelId) {
        this.channelId = channelId;
        // Try to load from file if it exists
        loadCacheFromFile();
    }

    /**
     * Checks YouTube RSS feed for new videos.
     *
     * @return New video URL if found, null otherwise
     */
    public String checkForNewVideo() {
        try {
            String feedUrl = RSS_BASE_URL + channelId;

            SyndFeedInput input = new SyndFeedInput();
            SyndFeed feed = input.build(new XmlReader(new URL(feedUrl)));

            List<SyndEntry> entries = feed.getEntries();
            if (entries == null || entries.isEmpty()) {
                System.out.println("[YouTube] Feed is empty.");
                return null;
            }

            // First entry = newest video
            SyndEntry latestEntry = entries.get(0);
            String latestUrl   = latestEntry.getLink();
            String latestId    = extractVideoId(latestUrl);
            String latestTitle = latestEntry.getTitle();

            System.out.println("[YouTube] Latest video ID: " + latestId);
            System.out.println("[YouTube] Cached video ID:  " + lastVideoId);

            if (!latestId.equals(lastVideoId) && !latestId.isEmpty()) {
                // New video detected!
                lastVideoId = latestId;
                saveCacheToFile(latestId); // Save for next restart
                System.out.println("[YouTube] ✅ New video found: " + latestTitle + " → " + latestUrl);
                return latestUrl;
            } else {
                System.out.println("[YouTube] No new videos. Latest: " + latestTitle);
            }

        } catch (Exception e) {
            System.err.println("[YouTube] ❌ Feed check error: " + e.getMessage());
            e.printStackTrace();
        }

        return null;
    }

    private String extractVideoId(String url) {
        if (url == null || url.isEmpty()) return "";
        if (url.contains("v="))        return url.split("v=")[1].split("&")[0];
        if (url.contains("youtu.be/")) return url.split("youtu.be/")[1].split("\\?")[0];
        return url;
    }

    /**
     * Load last video ID from file (if it exists from previous run)
     */
    private void loadCacheFromFile() {
        try {
            String cached = Files.readString(Path.of("/tmp/last_video.txt")).trim();
            if (!cached.isEmpty()) {
                lastVideoId = cached;
                System.out.println("[Cache] Loaded from file: " + lastVideoId);
            }
        } catch (IOException e) {
            System.out.println("[Cache] No cache file found (first run).");
        }
    }

    /**
     * Save last video ID to file (persistent storage)
     */
    private void saveCacheToFile(String videoId) {
        try {
            Files.writeString(Path.of("/tmp/last_video.txt"), videoId);
            System.out.println("[Cache] Saved to file: " + videoId);
        } catch (IOException e) {
            System.err.println("[Cache] Failed to save: " + e.getMessage());
        }
    }
}
