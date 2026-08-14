package com.github.tvbox.osc.ui.dialog;

import android.content.Context;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.github.tvbox.osc.R;
import com.github.tvbox.osc.base.BaseDialog;
import com.github.tvbox.osc.util.HawkConfig;
import com.orhanobut.hawk.Hawk;

import org.jetbrains.annotations.NotNull;

/**
 * 设置弹窗修复类（适配酷9反编译项目）
 * 修复：直播订阅、EPG订阅点击无响应
 * 使用项目已有键名：LIVE_API_URL、EPG_URL
 */
public class SettingDialog extends BaseDialog {

    private TextView tvLiveSub;
    private TextView tvEpgSub;
    private TextView tvLiveClear;
    private TextView tvEpgClear;

    public SettingDialog(@NonNull @NotNull Context context) {
        super(context);
        setContentView(R.layout.dialog_setting);
        initView();
        initData();
        initListener();
    }

    private void initView() {
        tvLiveSub = findViewById(R.id.tvLiveSub);
        tvEpgSub = findViewById(R.id.tvEpgSub);
        tvLiveClear = findViewById(R.id.tvLiveClear);
        tvEpgClear = findViewById(R.id.tvEpgClear);
    }

    private void initData() {
        // 使用项目已有键名回显
        String liveUrl = Hawk.get(HawkConfig.LIVE_API_URL, "");
        String epgUrl = Hawk.get(HawkConfig.EPG_URL, "");

        if (tvLiveSub != null) {
            tvLiveSub.setText(liveUrl.isEmpty() ? "点击设置直播订阅" : "直播源已配置");
        }
        if (tvEpgSub != null) {
            tvEpgSub.setText(epgUrl.isEmpty() ? "点击设置EPG订阅" : "EPG已配置");
        }
    }

    private void initListener() {
        // 直播订阅点击
        if (tvLiveSub != null) {
            tvLiveSub.setOnClickListener(v -> showLiveSubDialog());
        }

        // EPG订阅点击
        if (tvEpgSub != null) {
            tvEpgSub.setOnClickListener(v -> showEpgSubDialog());
        }

        // 清除直播订阅
        if (tvLiveClear != null) {
            tvLiveClear.setOnClickListener(v -> {
                Hawk.put(HawkConfig.LIVE_API_URL, "");
                Hawk.put(HawkConfig.LIVE_API_HISTORY, "");
                Toast.makeText(getContext(), "直播订阅已清除，重启生效", Toast.LENGTH_SHORT).show();
                initData();
            });
        }

        // 清除EPG订阅
        if (tvEpgClear != null) {
            tvEpgClear.setOnClickListener(v -> {
                Hawk.put(HawkConfig.EPG_URL, "");
                Hawk.put(HawkConfig.EPG_HISTORY, "");
                Toast.makeText(getContext(), "EPG订阅已清除", Toast.LENGTH_SHORT).show();
                initData();
            });
        }
    }

    /**
     * 弹出直播订阅输入框
     */
    private void showLiveSubDialog() {
        String current = Hawk.get(HawkConfig.LIVE_API_URL, "");
        new InputDialog(getContext())
                .setTitle("直播订阅地址")
                .setHint("请输入直播源订阅链接 (支持m3u/txt格式)...")
                .setDefaultText(current)
                .setOnConfirmListener(text -> {
                    if (text == null || text.trim().isEmpty()) {
                        Toast.makeText(getContext(), "地址不能为空", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    String url = text.trim();
                    Hawk.put(HawkConfig.LIVE_API_URL, url);
                    // 保存到历史记录
                    saveToHistory(HawkConfig.LIVE_API_HISTORY, url);
                    Toast.makeText(getContext(), "直播订阅已保存，重启播放生效", Toast.LENGTH_LONG).show();
                    initData();
                    notifyLiveRefresh();
                })
                .show();
    }

    /**
     * 弹出EPG订阅输入框
     */
    private void showEpgSubDialog() {
        String current = Hawk.get(HawkConfig.EPG_URL, "");
        new InputDialog(getContext())
                .setTitle("EPG节目单地址")
                .setHint("请输入EPG订阅链接 (XML格式)...")
                .setDefaultText(current)
                .setOnConfirmListener(text -> {
                    if (text == null || text.trim().isEmpty()) {
                        Toast.makeText(getContext(), "地址不能为空", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    String url = text.trim();
                    Hawk.put(HawkConfig.EPG_URL, url);
                    saveToHistory(HawkConfig.EPG_HISTORY, url);
                    Toast.makeText(getContext(), "EPG订阅已保存", Toast.LENGTH_LONG).show();
                    initData();
                    notifyEpgRefresh();
                })
                .show();
    }

    /**
     * 保存到历史记录
     */
    private void saveToHistory(String key, String url) {
        try {
            java.util.List<String> history = Hawk.get(key, new java.util.ArrayList<>());
            if (history == null) history = new java.util.ArrayList<>();
            // 去重并置顶
            history.remove(url);
            history.add(0, url);
            // 最多保留10条
            if (history.size() > 10) {
                history = history.subList(0, 10);
            }
            Hawk.put(key, history);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 通知直播页面刷新源
     */
    private void notifyLiveRefresh() {
        try {
            android.content.Intent intent = new android.content.Intent("com.github.tvbox.osc.LIVE_REFRESH");
            getContext().sendBroadcast(intent);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 通知EPG刷新
     */
    private void notifyEpgRefresh() {
        try {
            android.content.Intent intent = new android.content.Intent("com.github.tvbox.osc.EPG_REFRESH");
            getContext().sendBroadcast(intent);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
