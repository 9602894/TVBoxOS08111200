package com.github.tvbox.osc.util;

import android.content.res.AssetManager;
import android.text.TextUtils;

import com.github.tvbox.osc.base.App;
import com.github.tvbox.osc.bean.EpgChannel;
import com.github.tvbox.osc.cache.EpgChannelDao;
import com.github.tvbox.osc.data.AppDataManager;
import com.github.tvbox.osc.util.LOG;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

/**
 * EPG工具类 - 数据库版
 * 将epg_data.json导入SQLite，避免60M数据常驻内存导致OOM
 */
public class EpgUtil {
    private static final String TAG = "EpgUtil";
    private static final String EPG_DATA_JSON = "epg_data.json";
    private static final int BATCH_SIZE = 500;
    private static Hashtable<String, String[]> hsEpg = new Hashtable<>();
    private static volatile boolean loaded = false;

    public static void init() {
        new Thread(() -> loadEpgData()).start();
    }

    private static void loadEpgData() {
        try {
            EpgChannelDao dao = AppDataManager.get().getEpgChannelDao();
            if (dao == null) return;

            if (dao.getCount() > 0) {
                loaded = true;
                LOG.i(TAG + " EPG data already in DB, count=" + dao.getCount());
                return;
            }

            AssetManager am = App.getInstance().getAssets();
            BufferedReader br = new BufferedReader(new InputStreamReader(am.open(EPG_DATA_JSON), "UTF-8"));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
            br.close();

            if (sb.length() == 0) {
                LOG.e(TAG + " epg_data.json is empty");
                return;
            }

            JsonObject root = JsonParser.parseString(sb.toString()).getAsJsonObject();
            if (root == null || !root.has("epgs")) {
                LOG.e(TAG + " epg_data.json format error");
                return;
            }

            JsonArray epgs = root.getAsJsonArray("epgs");
            List<EpgChannel> batch = new ArrayList<>();
            int total = 0;

            for (JsonElement el : epgs) {
                if (el == null || !el.isJsonObject()) continue;
                JsonObject obj = el.getAsJsonObject();
                String name = getString(obj, "name");
                String logo = getString(obj, "logo");
                String epgid = getString(obj, "epgid");
                if (TextUtils.isEmpty(name)) continue;

                String[] names = name.split(",");
                for (String n : names) {
                    String trim = n.trim();
                    if (TextUtils.isEmpty(trim)) continue;
                    EpgChannel ch = new EpgChannel();
                    ch.name = trim;
                    ch.logo = logo;
                    ch.epgid = epgid;
                    ch.aliases = name;
                    ch.updateTime = System.currentTimeMillis();
                    batch.add(ch);
                }

                if (batch.size() >= BATCH_SIZE) {
                    dao.insertAll(batch);
                    total += batch.size();
                    batch.clear();
                }
            }
            if (!batch.isEmpty()) {
                dao.insertAll(batch);
                total += batch.size();
            }

            loaded = true;
            LOG.i(TAG + " EPG data imported, count=" + total);
        } catch (Exception e) {
            LOG.e(TAG + " load error: " + e.getMessage());
        }
    }

    public static String[] getEpgInfo(String channelName) {
        if (!loaded || TextUtils.isEmpty(channelName)) return null;
        try {
            EpgChannelDao dao = AppDataManager.get().getEpgChannelDao();
            if (dao == null) return null;
            EpgChannel ch = dao.getByName(channelName);
            if (ch == null) {
                String compact = channelName.replace("-", "").replace(" ", "").trim();
                if (!compact.equals(channelName)) ch = dao.getByName(compact);
            }
            if (ch != null) return new String[]{ch.logo, ch.epgid};
        } catch (Exception e) {
            LOG.e(TAG + " getEpgInfo error: " + e.getMessage());
        }
        return null;
    }

    private static String getString(JsonObject obj, String key) {
        if (obj == null || !obj.has(key) || obj.get(key).isJsonNull()) return "";
        try {
            return obj.get(key).getAsString();
        } catch (Exception e) {
            return "";
        }
    }
}
