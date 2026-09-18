package com.example.violet.ui.apps;

import com.example.violet.data.AppInfo;

/**
 * Composite item representing either a Section Header or an App Entry
 * in the Windows Phone alphabetized list.
 */
public class AppItem {
    public static final int TYPE_HEADER = 0;
    public static final int TYPE_APP = 1;

    private final int type;
    private final String headerTitle;
    private final AppInfo appInfo;

    public static AppItem createHeader(String headerTitle) {
        return new AppItem(TYPE_HEADER, headerTitle, null);
    }

    public static AppItem createApp(AppInfo appInfo) {
        return new AppItem(TYPE_APP, null, appInfo);
    }

    private AppItem(int type, String headerTitle, AppInfo appInfo) {
        this.type = type;
        this.headerTitle = headerTitle;
        this.appInfo = appInfo;
    }

    public int getType() {
        return type;
    }

    public String getHeaderTitle() {
        return headerTitle;
    }

    public AppInfo getAppInfo() {
        return appInfo;
    }
}
