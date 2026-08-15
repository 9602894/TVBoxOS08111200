package com.github.tvbox.osc.cache;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.github.tvbox.osc.bean.EpgChannel;

import java.util.List;

@Dao
public interface EpgChannelDao {

    @Query("SELECT * FROM epg_channel WHERE name = :name LIMIT 1")
    EpgChannel getByName(String name);

    @Query("SELECT * FROM epg_channel WHERE epgid = :epgid LIMIT 1")
    EpgChannel getByEpgId(String epgid);

    @Query("SELECT * FROM epg_channel")
    List<EpgChannel> getAll();

    @Query("SELECT COUNT(*) FROM epg_channel")
    int getCount();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<EpgChannel> channels);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(EpgChannel channel);

    @Query("DELETE FROM epg_channel")
    void deleteAll();
}
