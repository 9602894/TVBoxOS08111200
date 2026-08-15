package com.github.tvbox.osc.cache;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.github.tvbox.osc.bean.EpgData;

import java.util.List;

@Dao
public interface EpgDataDao {

    @Query("SELECT * FROM epg_data WHERE channelName = :channelName AND date = :date ORDER BY epgIndex ASC")
    List<EpgData> getByChannelAndDate(String channelName, String date);

    @Query("SELECT COUNT(*) FROM epg_data WHERE channelName = :channelName AND date = :date")
    int getCountByChannelAndDate(String channelName, String date);

    @Query("DELETE FROM epg_data WHERE channelName = :channelName AND date = :date")
    void deleteByChannelAndDate(String channelName, String date);

    @Query("DELETE FROM epg_data WHERE date < :expireDate")
    void deleteExpired(String expireDate);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<EpgData> dataList);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(EpgData data);

    @Query("DELETE FROM epg_data")
    void deleteAll();

    @Query("SELECT COUNT(*) FROM epg_data")
    int getTotalCount();
}
