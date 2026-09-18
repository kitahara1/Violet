package com.example.violet.data;

/**
 * Supported tile sizes on the Start screen.
 * Based on a 4-column base grid:
 * - SMALL:  1 column wide x 1 unit high (1x1 compact square)
 * - WIDE:   2 columns wide x 1 unit high (2x1 compact wide rectangle)
 * - MEDIUM: 2 columns wide x 2 units high (2x2 half-width square)
 * - LARGE:  4 columns wide x 2 units high (4x2 full-width banner)
 */
public enum TileSize {
    SMALL(1, 1),
    WIDE(2, 1),
    MEDIUM(2, 2),
    LARGE(4, 2);

    private final int spanSize;
    private final int rowSpan;

    TileSize(int spanSize, int rowSpan) {
        this.spanSize = spanSize;
        this.rowSpan = rowSpan;
    }

    public int getSpanSize() {
        return spanSize;
    }

    public int getRowSpan() {
        return rowSpan;
    }

    /**
     * Cycles through all 4 sizes: SMALL (1x1) -> WIDE (2x1) -> MEDIUM (2x2) -> LARGE (4x2) -> SMALL (1x1)
     */
    public TileSize next() {
        switch (this) {
            case SMALL:
                return WIDE;
            case WIDE:
                return MEDIUM;
            case MEDIUM:
                return LARGE;
            case LARGE:
            default:
                return SMALL;
        }
    }

    public static TileSize fromString(String name) {
        if (name == null) return WIDE;
        try {
            return TileSize.valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            return WIDE;
        }
    }
}
