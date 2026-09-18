package com.example.violet.data;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Objects;
import java.util.UUID;

/**
 * Represents an individual Start screen tile.
 */
public class Tile {
    private final String id;
    private final String packageName;
    private final String activityName;
    private String label;
    private TileSize size;
    private TileType type;
    private int orderIndex;

    public Tile(String packageName, String activityName, String label, TileSize size) {
        this(UUID.randomUUID().toString(), packageName, activityName, label, size, TileType.STATIC, 0);
    }

    public Tile(String id, String packageName, String activityName, String label, TileSize size, TileType type, int orderIndex) {
        this.id = id != null ? id : UUID.randomUUID().toString();
        this.packageName = packageName;
        this.activityName = activityName;
        this.label = label;
        this.size = size != null ? size : TileSize.WIDE;
        this.type = type != null ? type : TileType.STATIC;
        this.orderIndex = orderIndex;
    }

    public String getId() {
        return id;
    }

    public String getPackageName() {
        return packageName;
    }

    public String getActivityName() {
        return activityName;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public TileSize getSize() {
        return size;
    }

    public void setSize(TileSize size) {
        this.size = size != null ? size : TileSize.WIDE;
    }

    public TileType getType() {
        return type;
    }

    public void setType(TileType type) {
        this.type = type != null ? type : TileType.STATIC;
    }

    public int getOrderIndex() {
        return orderIndex;
    }

    public void setOrderIndex(int orderIndex) {
        this.orderIndex = orderIndex;
    }

    public JSONObject toJsonObject() {
        JSONObject json = new JSONObject();
        try {
            json.put("id", id);
            json.put("packageName", packageName);
            json.put("activityName", activityName);
            json.put("label", label);
            json.put("size", size.name());
            json.put("type", type.name());
            json.put("orderIndex", orderIndex);
        } catch (JSONException ignored) {
        }
        return json;
    }

    public static Tile fromJsonObject(JSONObject json) {
        if (json == null) return null;
        String id = json.optString("id", UUID.randomUUID().toString());
        String packageName = json.optString("packageName", "");
        String activityName = json.optString("activityName", "");
        String label = json.optString("label", "");
        TileSize size = TileSize.fromString(json.optString("size", "WIDE"));
        TileType type = "LIVE".equalsIgnoreCase(json.optString("type")) ? TileType.LIVE : TileType.STATIC;
        int orderIndex = json.optInt("orderIndex", 0);

        return new Tile(id, packageName, activityName, label, size, type, orderIndex);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Tile tile = (Tile) o;
        return Objects.equals(id, tile.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
