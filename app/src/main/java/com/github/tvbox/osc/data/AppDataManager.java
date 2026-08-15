package com.github.tvbox.osc.data;

import android.annotation.SuppressLint;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;

import androidx.annotation.NonNull;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.github.tvbox.osc.base.App;
import com.github.tvbox.osc.cache.EpgChannelDao;
import com.github.tvbox.osc.cache.EpgDataDao;
import com.github.tvbox.osc.util.FileUtils;

import java.io.File;
import java.io.IOException;

/**
 * 应用数据管理器 - 更新版
 * 
 * 更新内容：
 * 1. 数据库版本升级到2，新增epg_channel和epg_data表
 * 2. 添加MIGRATION_1_2迁移
 * 3. 添加getEpgChannelDao()和getEpgDataDao()方法
 */
public class AppDataManager {
    private static final int DB_FILE_VERSION = 3;
    private static final String DB_NAME = "tvbox";
    private static AppDataManager manager;
    private static AppDataBase dbInstance;

    private AppDataManager() {
    }

    public static void init() {
        if (manager == null) {
            synchronized (AppDataManager.class) {
                if (manager == null) {
                    manager = new AppDataManager();
                }
            }
        }
    }

    static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            // 创建EPG频道映射表
            database.execSQL(
                "CREATE TABLE IF NOT EXISTS `epg_channel` (" +
                "`name` TEXT NOT NULL, " +
                "`logo` TEXT, " +
                "`epgid` TEXT, " +
                "`aliases` TEXT, " +
                "`updateTime` INTEGER NOT NULL, " +
                "PRIMARY KEY(`name`))"
            );
            database.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_epg_channel_epgid` ON `epg_channel` (`epgid`)"
            );

            // 创建EPG节目单数据表
            database.execSQL(
                "CREATE TABLE IF NOT EXISTS `epg_data` (" +
                "`channelName` TEXT NOT NULL, " +
                "`date` TEXT NOT NULL, " +
                "`title` TEXT, " +
                "`startTime` TEXT, " +
                "`endTime` TEXT, " +
                "`startDateTime` INTEGER NOT NULL, " +
                "`endDateTime` INTEGER NOT NULL, " +
                "`epgIndex` INTEGER NOT NULL, " +
                "`updateTime` INTEGER NOT NULL, " +
                "PRIMARY KEY(`channelName`, `date`, `epgIndex`))"
            );
            database.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_epg_data_channelName_date` ON `epg_data` (`channelName`, `date`)"
            );
            database.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_epg_data_date` ON `epg_data` (`date`)"
            );
        }
    };

    static final Migration MIGRATION_2_3 = new Migration(2, 3) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            // 版本2到3的迁移（预留）
        }
    };

    static final Migration MIGRATION_1_2_OLD = new Migration(1, 2) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            try {
                database.execSQL("ALTER TABLE sourceState ADD COLUMN tidSort TEXT");
            } catch (SQLiteException e) {
                e.printStackTrace();
            }
        }
    };

    static final Migration MIGRATION_2_3_OLD = new Migration(2, 3) {
        @SuppressLint("Range")
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            database.execSQL("CREATE TABLE IF NOT EXISTS `vodRecordTmp` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `vodId` TEXT, `updateTime` INTEGER NOT NULL, `sourceKey` TEXT, `data` BLOB, `dataJson` TEXT, `testMigration` INTEGER NOT NULL)");
            Cursor cursor = database.query("SELECT * FROM vodRecord");
            int id;
            int vodId;
            long updateTime;
            String sourceKey;
            String dataJson;
            while (cursor.moveToNext()) {
                id = cursor.getInt(cursor.getColumnIndex("id"));
                vodId = cursor.getInt(cursor.getColumnIndex("vodId"));
                updateTime = cursor.getLong(cursor.getColumnIndex("updateTime"));
                sourceKey = cursor.getString(cursor.getColumnIndex("sourceKey"));
                dataJson = cursor.getString(cursor.getColumnIndex("dataJson"));
                database.execSQL("INSERT INTO vodRecordTmp (id, vodId, updateTime, sourceKey, dataJson, testMigration) VALUES" +
                        " ('" + id + "', '" + vodId + "', '" + updateTime + "', '" + sourceKey + "', '" + dataJson + "',0 )");
            }
            database.execSQL("DROP TABLE vodRecord");
            database.execSQL("ALTER TABLE vodRecordTmp RENAME TO vodRecord");
        }
    };

    static final Migration MIGRATION_3_4_OLD = new Migration(3, 4) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            try {
                database.execSQL("ALTER TABLE vodRecord ADD COLUMN dataJson TEXT");
            } catch (SQLiteException e) {
                e.printStackTrace();
            }
        }
    };

    static final Migration MIGRATION_4_5_OLD = new Migration(4, 5) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            try {
                database.execSQL("ALTER TABLE localSource ADD COLUMN type INTEGER NOT NULL DEFAULT 0");
            } catch (SQLiteException e) {
                e.printStackTrace();
            }
        }
    };

    static String dbPath() {
        return DB_NAME + ".v" + DB_FILE_VERSION + ".db";
    }

    public static AppDataBase get() {
        if (manager == null) {
            throw new RuntimeException("AppDataManager is no init");
        }
        if (dbInstance == null)
            dbInstance = Room.databaseBuilder(App.getInstance(), AppDataBase.class, dbPath())
                    .setJournalMode(RoomDatabase.JournalMode.TRUNCATE)
                    .addMigrations(MIGRATION_1_2)
                    .addMigrations(MIGRATION_2_3)
                    .addCallback(new RoomDatabase.Callback() {
                        @Override
                        public void onCreate(@NonNull SupportSQLiteDatabase db) {
                            super.onCreate(db);
                        }

                        @Override
                        public void onOpen(@NonNull SupportSQLiteDatabase db) {
                            super.onOpen(db);
                        }
                    }).allowMainThreadQueries()
                    .build();
        return dbInstance;
    }

    /**
     * 获取EPG频道映射DAO
     */
    public EpgChannelDao getEpgChannelDao() {
        AppDataBase db = get();
        return db != null ? db.getEpgChannelDao() : null;
    }

    /**
     * 获取EPG节目单DAO
     */
    public EpgDataDao getEpgDataDao() {
        AppDataBase db = get();
        return db != null ? db.getEpgDataDao() : null;
    }

    public static boolean backup(File path) throws IOException {
        if (dbInstance != null && dbInstance.isOpen()) {
            dbInstance.close();
        }
        File db = App.getInstance().getDatabasePath(dbPath());
        if (db.exists()) {
            FileUtils.copyFile(db, path);
            return true;
        } else {
            return false;
        }
    }

    public static boolean restore(File path) throws IOException {
        if (dbInstance != null && dbInstance.isOpen()) {
            dbInstance.close();
        }
        File db = App.getInstance().getDatabasePath(dbPath());
        if (db.exists()) {
            db.delete();
        }
        if (!db.getParentFile().exists())
            db.getParentFile().mkdirs();
        FileUtils.copyFile(path, db);
        return true;
    }
}
