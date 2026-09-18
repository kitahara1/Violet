package com.example.violet;

import com.example.violet.data.TileSize;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class TileSizeTest {

    @Test
    public void testTileSizeDimensions() {
        assertEquals(1, TileSize.SMALL.getSpanSize());
        assertEquals(1, TileSize.SMALL.getRowSpan());

        assertEquals(2, TileSize.WIDE.getSpanSize());
        assertEquals(1, TileSize.WIDE.getRowSpan());

        assertEquals(2, TileSize.MEDIUM.getSpanSize());
        assertEquals(2, TileSize.MEDIUM.getRowSpan());

        assertEquals(4, TileSize.LARGE.getSpanSize());
        assertEquals(2, TileSize.LARGE.getRowSpan());
    }

    @Test
    public void testTileSizeCycle() {
        assertEquals(TileSize.WIDE, TileSize.SMALL.next());
        assertEquals(TileSize.MEDIUM, TileSize.WIDE.next());
        assertEquals(TileSize.LARGE, TileSize.MEDIUM.next());
        assertEquals(TileSize.SMALL, TileSize.LARGE.next());
    }

    @Test
    public void testTileSizeFromString() {
        assertEquals(TileSize.SMALL, TileSize.fromString("small"));
        assertEquals(TileSize.WIDE, TileSize.fromString("WIDE"));
        assertEquals(TileSize.MEDIUM, TileSize.fromString("medium"));
        assertEquals(TileSize.LARGE, TileSize.fromString("LARGE"));

        // Fallback for null or unknown
        assertEquals(TileSize.WIDE, TileSize.fromString(null));
        assertEquals(TileSize.WIDE, TileSize.fromString("unknown"));
    }
}
