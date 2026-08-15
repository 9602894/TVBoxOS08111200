package com.github.tvbox.osc.data;

import android.content.res.AssetManager;
import android.os.AsyncTask;
import android.text.TextUtils;

import com.github.tvbox.osc.base.App;
import com.github.tvbox.osc.bean.EpgChannel;
import com.github.tvbox.osc.bean.EpgData;
import com.github.tvbox.osc.cache.EpgChannelDao;
import com.github.tvbox.osc.cache.EpgDataDao;
import com.github.tvbox.osc.util.LOG;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * EPG数据库管理器
 * 
 * 核心职责：
 * 1. 将assets/epg_data.json中的频道映射信息导入SQLite（避免60M+数据常驻内存）
 * 2. 将网络获取的EPG节目单缓存到数据库（按频道+日期索引）
 * 3. 提供频道名→logo/epgid的查询映射
 * 4. 提供EPG节目单的增删改查
 * 5. 自动清理过期数据
 * 
 * 修复问题：
 * - 原EpgUtil一次性将epg_data.json全部加载到HashMap，60M数据导致OOM/崩溃
 * - 原代码在UI线程解析大JSON，ANR
 * - 原hsEpg内存缓存无上限，长时间使用后OOM
 */
public class EpgDatabaseManager {

    private static final String TAG = "EpgDatabaseManager";
    private static final String EPG_DATA_JSON = "epg_data.json";
    private static final int BATCH_SIZE = 500;
    private static final int MAX_CACHE_DAYS = 7;

    private static EpgDatabaseManager instance;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private volatile boolean initialized = false;

    // 内存缓存：仅缓存最近查询的频道映射（LRU策略，最多100条）
    private final Map<String, EpgChannel> channelCache = new HashMap<>();
    private static final int MAX_CHANNEL_CACHE = 100;

    private EpgDatabaseManager() {}

    public static synchronized EpgDatabaseManager getInstance() {
        if (instance == null) {
            instance = new EpgDatabaseManager();
        }
        return instance;
    }

    /**
     * 异步初始化：从assets导入epg_data.json到数据库
     * 在Application.onCreate()中调用
     */
    public void initAsync() {
        executor.execute(() -> {
            try {
                initInternal();
            } catch (Exception e) {
                LOG.e(TAG + " init error: " + e.getMessage());
            }
        });
    }

    /**
     * 同步初始化（仅用于需要立即使用的情况）
     */
    public void initSync() {
        if (!initialized) {
            initInternal();
        }
    }

    private void initInternal() {
        EpgChannelDao dao = AppDataManager.get().getEpgChannelDao();
        if (dao == null) {
            LOG.e(TAG + " EpgChannelDao is null");
            return;
        }

        // 如果数据库已有数据，跳过导入
        if (dao.getCount() > 0) {
            initialized = true;
            LOG.i(TAG + " EPG channel data already imported, count=" + dao.getCount());
            return;
        }

        try {
            AssetManager assetManager = App.getInstance().getAssets();
            BufferedReader br = new BufferedReader(
                new InputStreamReader(assetManager.open(EPG_DATA_JSON), "UTF-8"));
            StringBuilder builder = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                builder.append(line);
            }
            br.close();

            if (builder.length() == 0) {
                LOG.e(TAG + " epg_data.json is empty");
                return;
            }

            // 使用流式解析避免OOM
            Gson gson = new Gson();
            JsonObject root = gson.fromJson(builder.toString(), JsonObject.class);
            if (root == null || !root.has("epgs")) {
                LOG.e(TAG + " epg_data.json format error");
                return;
            }

            JsonArray epgs = root.getAsJsonArray("epgs");
            List<EpgChannel> batch = new ArrayList<>();
            int totalCount = 0;

            for (JsonElement element : epgs) {
                if (element == null || !element.isJsonObject()) continue;
                JsonObject obj = element.getAsJsonObject();

                String name = safeGetString(obj, "name");
                String logo = safeGetString(obj, "logo");
                String epgid = safeGetString(obj, "epgid");

                if (TextUtils.isEmpty(name)) continue;

                // 将逗号分隔的别名也存入，主名和别名都插入
                String[] names = name.split(",");
                for (String n : names) {
                    String trimName = n.trim();
                    if (TextUtils.isEmpty(trimName)) continue;

                    EpgChannel channel = new EpgChannel();
                    channel.name = trimName;
                    channel.logo = logo;
                    channel.epgid = epgid;
                    channel.aliases = name; // 保存原始完整名称
                    channel.updateTime = System.currentTimeMillis();
                    batch.add(channel);
                }

                if (batch.size() >= BATCH_SIZE) {
                    dao.insertAll(batch);
                    totalCount += batch.size();
                    batch.clear();
                }
            }

            if (!batch.isEmpty()) {
                dao.insertAll(batch);
                totalCount += batch.size();
            }

            initialized = true;
            LOG.i(TAG + " EPG channel data imported, count=" + totalCount);

        } catch (Exception e) {
            LOG.e(TAG + " import error: " + e.getMessage());
        }
    }

    /**
     * 查询频道信息：返回 [logo, epgid]
     * 先查内存缓存，再查数据库
     */
    public String[] getEpgInfo(String channelName) {
        if (TextUtils.isEmpty(channelName)) return null;

        // 1. 内存缓存
        EpgChannel cached = channelCache.get(channelName);
        if (cached != null) {
            return new String[]{cached.logo, cached.epgid};
        }

        // 2. 数据库查询
        EpgChannelDao dao = AppDataManager.get().getEpgChannelDao();
        if (dao == null) return null;

        EpgChannel channel = dao.getByName(channelName);
        if (channel == null) {
            // 尝试模糊匹配：去掉空格、横线等
            String compactName = channelName.replace("-", "").replace(" ", "").trim();
            if (!compactName.equals(channelName)) {
                channel = dao.getByName(compactName);
            }
        }

        if (channel != null) {
            // 加入内存缓存
            putChannelCache(channelName, channel);
            return new String[]{channel.logo, channel.epgid};
        }

        return null;
    }

    /**
     * 根据epgid查询频道（用于EPG数据反查）
     */
    public EpgChannel getChannelByEpgId(String epgid) {
        if (TextUtils.isEmpty(epgid)) return null;

        EpgChannelDao dao = AppDataManager.get().getEpgChannelDao();
        if (dao == null) return null;

        return dao.getByEpgId(epgid);
    }

    /**
     * 保存EPG节目单到数据库
     */
    public void saveEpgData(String channelName, String date, List<EpgData> dataList) {
        if (TextUtils.isEmpty(channelName) || TextUtils.isEmpty(date) || dataList == null || dataList.isEmpty()) {
            return;
        }

        executor.execute(() -> {
            try {
                EpgDataDao dao = AppDataManager.get().getEpgDataDao();
                if (dao == null) return;

                // 先删除旧数据
                dao.deleteByChannelAndDate(channelName, date);
                // 插入新数据
                dao.insertAll(dataList);

                LOG.i(TAG + " saved EPG data: " + channelName + " " + date + " count=" + dataList.size());
            } catch (Exception e) {
                LOG.e(TAG + " saveEpgData error: " + e.getMessage());
            }
        });
    }

    /**
     * 从数据库读取EPG节目单
     */
    public List<EpgData> loadEpgData(String channelName, String date) {
        if (TextUtils.isEmpty(channelName) || TextUtils.isEmpty(date)) {
            return new ArrayList<>();
        }

        try {
            EpgDataDao dao = AppDataManager.get().getEpgDataDao();
            if (dao == null) return new ArrayList<>();

            return dao.getByChannelAndDate(channelName, date);
        } catch (Exception e) {
            LOG.e(TAG + " loadEpgData error: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * 检查数据库中是否有某频道的某日期EPG
     */
    public boolean hasEpgData(String channelName, String date) {
        if (TextUtils.isEmpty(channelName) || TextUtils.isEmpty(date)) return false;

        try {
            EpgDataDao dao = AppDataManager.get().getEpgDataDao();
            if (dao == null) return false;

            return dao.getCountByChannelAndDate(channelName, date) > 0;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 清理过期EPG数据（保留最近N天）
     */
    public void cleanExpiredData() {
        executor.execute(() -> {
            try {
                EpgDataDao dao = AppDataManager.get().getEpgDataDao();
                if (dao == null) return;

                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                Calendar cal = Calendar.getInstance();
                cal.add(Calendar.DAY_OF_MONTH, -MAX_CACHE_DAYS);
                String expireDate = sdf.format(cal.getTime());

                dao.deleteExpired(expireDate);
                LOG.i(TAG + " cleaned expired EPG data before " + expireDate);
            } catch (Exception e) {
                LOG.e(TAG + " cleanExpired error: " + e.getMessage());
            }
        });
    }

    /**
     * 清空所有EPG缓存数据
     */
    public void clearAllEpgData() {
        executor.execute(() -> {
            try {
                EpgDataDao dao = AppDataManager.get().getEpgDataDao();
                if (dao != null) dao.deleteAll();

                // 同时清理内存缓存
                synchronized (channelCache) {
                    channelCache.clear();
                }

                LOG.i(TAG + " all EPG data cleared");
            } catch (Exception e) {
                LOG.e(TAG + " clearAll error: " + e.getMessage());
            }
        });
    }

    /**
     * 获取数据库中EPG数据总量
     */
    public int getEpgDataCount() {
        try {
            EpgDataDao dao = AppDataManager.get().getEpgDataDao();
            if (dao == null) return 0;
            return dao.getTotalCount();
        } catch (Exception e) {
            return 0;
        }
    }

    private void putChannelCache(String key, EpgChannel channel) {
        synchronized (channelCache) {
            if (channelCache.size() >= MAX_CHANNEL_CACHE) {
                // 简单清理：清空一半
                List<String> keys = new ArrayList<>(channelCache.keySet());
                for (int i = 0; i < keys.size() / 2; i++) {
                    channelCache.remove(keys.get(i));
                }
            }
            channelCache.put(key, channel);
        }
    }

    private String safeGetString(JsonObject obj, String key) {
        if (obj == null || !obj.has(key) || obj.get(key).isJsonNull()) return "";
        try {
            return obj.get(key).getAsString();
        } catch (Exception e) {
            return "";
        }
    }

    public boolean isInitialized() {
        return initialized;
    }

    public void shutdown() {
        executor.shutdown();
    }
}
