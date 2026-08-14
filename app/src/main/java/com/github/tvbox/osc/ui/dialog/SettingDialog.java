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
 * 酷9设置弹窗修复类
 * 修复内容：直播订阅、EPG订阅点击无响应
 * 替换路径：app/src/main/java/com/github/tvbox/osc/ui/dialog/SettingDialog.java
 * 或根据反编译结构替换对应的设置弹窗类
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
        // 回显当前配置
        String liveUrl = Hawk.get(HawkConfig.LIVE_URL, "");
        String epgUrl = Hawk.get(HawkConfig.EPG_URL, "");

        if (tvLiveSub != null) {
            tvLiveSub.setText(liveUrl.isEmpty() ? "点击设置直播订阅" : "直播源已配置");
        }
        if (tvEpgSub != null) {
            tvEpgSub.setText(epgUrl.isEmpty() ? "点击设置EPG订阅" : "EPG已配置");
        }
    }

    private void initListener() {
        // ========== 直播订阅点击修复 ==========
        if (tvLiveSub != null) {
            tvLiveSub.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showLiveSubDialog();
                }
            });
        }

        // ========== EPG订阅点击修复 ==========
        if (tvEpgSub != null) {
            tvEpgSub.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showEpgSubDialog();
                }
            });
        }

        // 清除按钮
        if (tvLiveClear != null) {
            tvLiveClear.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Hawk.put(HawkConfig.LIVE_URL, "");
                    Toast.makeText(getContext(), "直播订阅已清除", Toast.LENGTH_SHORT).show();
                    initData();
                }
            });
        }

        if (tvEpgClear != null) {
            tvEpgClear.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Hawk.put(HawkConfig.EPG_URL, "");
                    Toast.makeText(getContext(), "EPG订阅已清除", Toast.LENGTH_SHORT).show();
                    initData();
                }
            });
        }
    }

    /**
     * 弹出直播订阅输入框
     */
    private void showLiveSubDialog() {
        String current = Hawk.get(HawkConfig.LIVE_URL, "");
        new InputDialog(getContext())
                .setTitle("直播订阅地址")
                .setHint("请输入直播源订阅链接...")
                .setDefaultText(current)
                .setOnConfirmListener(new InputDialog.OnConfirmListener() {
                    @Override
                    public void onConfirm(String text) {
                        if (text == null || text.trim().isEmpty()) {
                            Toast.makeText(getContext(), "地址不能为空", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        Hawk.put(HawkConfig.LIVE_URL, text.trim());
                        Toast.makeText(getContext(), "直播订阅已保存，重启生效", Toast.LENGTH_LONG).show();
                        initData();
                        // 通知LivePlayActivity刷新源
                        notifyLiveRefresh();
                    }
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
                .setOnConfirmListener(new InputDialog.OnConfirmListener() {
                    @Override
                    public void onConfirm(String text) {
                        if (text == null || text.trim().isEmpty()) {
                            Toast.makeText(getContext(), "地址不能为空", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        Hawk.put(HawkConfig.EPG_URL, text.trim());
                        Toast.makeText(getContext(), "EPG订阅已保存", Toast.LENGTH_LONG).show();
                        initData();
                        // 通知EPG加载
                        notifyEpgRefresh();
                    }
                })
                .show();
    }

    /**
     * 通知直播页面刷新源
     */
    private void notifyLiveRefresh() {
        try {
            // 发送广播通知LivePlayActivity重新加载直播源
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
