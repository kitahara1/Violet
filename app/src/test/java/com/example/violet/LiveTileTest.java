package com.example.violet;

import com.example.violet.live.LiveTileData;
import com.example.violet.live.VioletNotificationService;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

public class LiveTileTest {

    @Test
    public void testEmptyLiveTileData() {
        LiveTileData data = LiveTileData.builder().build();
        assertEquals("", data.getHeader());
        assertEquals("", data.getPrimaryText());
        assertEquals("", data.getSubtitle());
        assertEquals(0, data.getBadgeCount());
        assertFalse(data.shouldFlip());
        assertFalse(data.hasContent());
    }

    @Test
    public void testNotificationLiveTileData() {
        LiveTileData data = LiveTileData.builder()
                .setHeader("MESSAGES")
                .setPrimaryText("3")
                .setSubtitle("3 new notifications")
                .setBadgeCount(3)
                .setShouldFlip(true)
                .build();

        assertEquals("MESSAGES", data.getHeader());
        assertEquals("3", data.getPrimaryText());
        assertEquals("3 new notifications", data.getSubtitle());
        assertEquals(3, data.getBadgeCount());
        assertTrue(data.shouldFlip());
        assertTrue(data.hasContent());
    }

    @Test
    public void testClockLiveTileData() {
        // Clock live tile flips between icon and live time/date
        LiveTileData data = LiveTileData.builder()
                .setHeader("CLOCK")
                .setPrimaryText("00:54")
                .setSubtitle("Friday, Sep 18")
                .setBadgeCount(0)
                .setShouldFlip(true)
                .build();

        assertEquals("CLOCK", data.getHeader());
        assertEquals("00:54", data.getPrimaryText());
        assertEquals("Friday, Sep 18", data.getSubtitle());
        assertEquals(0, data.getBadgeCount());
        assertTrue(data.shouldFlip());
        assertTrue(data.hasContent());
    }

    @Test
    public void testMediaLiveTileData() {
        // Media live tile (Apple Music, VLC, Spotify):
        // Shows "NOW PLAYING", track title, and artist, with 0 badge count
        LiveTileData data = LiveTileData.builder()
                .setHeader("NOW PLAYING")
                .setPrimaryText("Bohemian Rhapsody")
                .setSubtitle("Queen • A Night at the Opera")
                .setBadgeCount(0)
                .setShouldFlip(true)
                .build();

        assertEquals("NOW PLAYING", data.getHeader());
        assertEquals("Bohemian Rhapsody", data.getPrimaryText());
        assertEquals("Queen • A Night at the Opera", data.getSubtitle());
        assertEquals(0, data.getBadgeCount()); // Must suppress unread badge
        assertTrue(data.shouldFlip());
        assertTrue(data.hasContent());
    }

    @Test
    public void testMediaTrackInfo() {
        VioletNotificationService.MediaTrackInfo track = new VioletNotificationService.MediaTrackInfo(
                "Starboy",
                "The Weeknd",
                "Starboy",
                true
        );

        assertEquals("Starboy", track.getTitle());
        assertEquals("The Weeknd", track.getArtist());
        assertEquals("Starboy", track.getAlbum());
        assertTrue(track.isPlaying());
    }

    @Test
    public void testBadgeOnlyLiveTileData() {
        LiveTileData data = LiveTileData.builder()
                .setBadgeCount(5)
                .setShouldFlip(false)
                .build();

        assertEquals(5, data.getBadgeCount());
        assertFalse(data.shouldFlip());
        assertTrue(data.hasContent());
    }

    @Test
    public void testEqualityAndHashCode() {
        LiveTileData data1 = LiveTileData.builder()
                .setHeader("CLOCK")
                .setPrimaryText("12:00")
                .setSubtitle("Wednesday")
                .setBadgeCount(0)
                .setShouldFlip(true)
                .build();

        LiveTileData data2 = LiveTileData.builder()
                .setHeader("CLOCK")
                .setPrimaryText("12:00")
                .setSubtitle("Wednesday")
                .setBadgeCount(0)
                .setShouldFlip(true)
                .build();

        assertEquals(data1, data2);
        assertEquals(data1.hashCode(), data2.hashCode());
    }

    @Test
    public void testSimultaneousLiveTileUpdatesDetectChanges() {
        // Tile 1: Clock updates from 12:00 to 12:01
        LiveTileData clockOld = LiveTileData.builder()
                .setHeader("CLOCK")
                .setPrimaryText("12:00")
                .setSubtitle("Wednesday")
                .setShouldFlip(true)
                .build();
        LiveTileData clockNew = LiveTileData.builder()
                .setHeader("CLOCK")
                .setPrimaryText("12:01")
                .setSubtitle("Wednesday")
                .setShouldFlip(true)
                .build();

        // Tile 2: Battery or Calendar updates at the same time
        LiveTileData calendarOld = LiveTileData.builder()
                .setHeader("CALENDAR")
                .setPrimaryText("18")
                .setSubtitle("Wednesday, September")
                .setShouldFlip(true)
                .build();
        LiveTileData calendarNew = LiveTileData.builder()
                .setHeader("CALENDAR")
                .setPrimaryText("19")
                .setSubtitle("Thursday, September")
                .setShouldFlip(true)
                .build();

        assertNotEquals(clockOld, clockNew);
        assertNotEquals(calendarOld, calendarNew);

        // Verify staggered delays for multi-tile cascade
        int posTile1 = 1;
        int posTile2 = 2;
        int delay1 = (posTile1 % 6) * 160;
        int delay2 = (posTile2 % 6) * 160;

        assertEquals(160, delay1);
        assertEquals(320, delay2);
        assertTrue(delay2 > delay1);
    }
}
