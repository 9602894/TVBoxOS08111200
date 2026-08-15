package com.github.tvbox.osc.util;

import com.github.tvbox.osc.data.EpgDatabaseManager;

/**
 * EPG工具类 - 数据库版
 * 
 * 修复：原版本将60M epg_data.json全部加载到HashMap，导致OOM和启动崩溃
 * 新版本：通过EpgDatabaseManager查询SQLite数据库，内存占用极低
 * 
 * 保持API兼容，LivePlayActivity等调用方无需修改
 */
public class EpgUtil {

    /**
     * 查询频道信息
     * @param channelName 频道名称
     * @return String[]{logoUrl, epgid} 或 null
     */
    public static String[] getEpgInfo(String channelName) {
        return EpgDatabaseManager.getInstance().getEpgInfo(channelName);
    }

    /**
     * 初始化（在Application中调用）
     */
    public static void init() {
        EpgDatabaseManager.getInstance().initAsync();
    }

    /**
     * 同步初始化（用于需要立即使用的情况）
     */
    public static void initSync() {
        EpgDatabaseManager.getInstance().initSync();
    }
}
