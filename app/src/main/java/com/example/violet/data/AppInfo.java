package com.example.violet.data;

import android.graphics.drawable.Drawable;

import java.util.Objects;

/**
 * Represents metadata of an installed launchable application.
 */
public class AppInfo {
    private final String packageName;
    private final String activityName;
    private final String label;
    private final Drawable icon;
    private final String sectionHeader;

    public AppInfo(String packageName, String activityName, String label, Drawable icon, String sectionHeader) {
        this.packageName = packageName;
        this.activityName = activityName;
        this.label = label;
        this.icon = icon;
        this.sectionHeader = sectionHeader;
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

    public Drawable getIcon() {
        return icon;
    }

    public String getSectionHeader() {
        return sectionHeader;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AppInfo appInfo = (AppInfo) o;
        return Objects.equals(packageName, appInfo.packageName) &&
                Objects.equals(activityName, appInfo.activityName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(packageName, activityName);
    }

    @Override
    public String toString() {
        return "AppInfo{" +
                "label='" + label + '\'' +
                ", packageName='" + packageName + '\'' +
                ", activityName='" + activityName + '\'' +
                ", sectionHeader='" + sectionHeader + '\'' +
                '}';
    }
}
