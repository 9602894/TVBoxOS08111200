package com.github.tvbox.osc.bean;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Index;

/**
 * EPG节目单数据实体
 * 将EPG节目单持久化到SQLite，避免60M数据全部加载到内存导致OOM
 * 
 * 复合主键：channelName + date + index 确保唯一性
 */
@Entity(tableName = "epg_data", 
        primaryKeys = {"channelName", "date", "epgIndex"},
        indices = {
            @Index(value = {"channelName", "date"}),
            @Index(value = {"date"})
        })
public class EpgData {

    @NonNull
    public String channelName = "";

    @NonNull
    public String date = "";

    public String title = "";

    public String startTime = "";

    public String endTime = "";

    public long startDateTime = 0;

    public long endDateTime = 0;

    public int epgIndex = 0;

    public long updateTime = 0;

    public EpgData() {}

    public EpgData(String channelName, String date, String title, 
                   String startTime, String endTime, 
                   long startDateTime, long endDateTime, int epgIndex) {
        this.channelName = channelName;
        this.date = date;
        this.title = title;
        this.startTime = startTime;
        this.endTime = endTime;
        this.startDateTime = startDateTime;
        this.endDateTime = endDateTime;
        this.epgIndex = epgIndex;
        this.updateTime = System.currentTimeMillis();
    }
}
