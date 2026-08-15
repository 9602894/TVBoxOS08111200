package com.github.tvbox.osc.data;

import androidx.room.Database;
import androidx.room.RoomDatabase;

import com.github.tvbox.osc.bean.EpgChannel;
import com.github.tvbox.osc.cache.Cache;
import com.github.tvbox.osc.cache.CacheDao;
import com.github.tvbox.osc.cache.EpgChannelDao;
import com.github.tvbox.osc.cache.VodCollect;
import com.github.tvbox.osc.cache.VodCollectDao;
import com.github.tvbox.osc.cache.VodRecord;
import com.github.tvbox.osc.cache.VodRecordDao;

@Database(entities = {Cache.class, VodRecord.class, VodCollect.class, EpgChannel.class}, version = 2)
public abstract class AppDataBase extends RoomDatabase {
    public abstract CacheDao getCacheDao();
    public abstract VodRecordDao getVodRecordDao();
    public abstract VodCollectDao getVodCollectDao();
    public abstract EpgChannelDao getEpgChannelDao();
}
