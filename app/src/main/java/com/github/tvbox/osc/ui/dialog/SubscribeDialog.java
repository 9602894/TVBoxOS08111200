package com.github.tvbox.osc.ui.dialog;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.github.tvbox.osc.R;
import com.github.tvbox.osc.util.HawkConfig;
import com.orhanobut.hawk.Hawk;

import org.jetbrains.annotations.NotNull;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

public class SubscribeDialog extends BaseDialog {

    private boolean isLive;
    private LinearLayout llListContainer;
    private TextView tvTitle;
    private TextView tvQrUrl;
    private ImageView ivQrCode;
    private EditText etName;
    private EditText etUrl;
    private List<String> dataList;
    private OnSubscribeChangeListener listener;

    public SubscribeDialog(@NonNull @NotNull Context context, boolean isLive) {
        super(context);
        this.isLive = isLive;
        setContentView(R.layout.dialog_subscribe);
        initView();
        loadData();
    }

    private void initView() {
        tvTitle = findViewById(R.id.tvSubTitle);
        tvQrUrl = findViewById(R.id.tvQrUrl);
        ivQrCode = findViewById(R.id.ivQrCode);
        llListContainer = findViewById(R.id.llSubscribeList);
        etName = findViewById(R.id.etSubName);
        etUrl = findViewById(R.id.etSubUrl);

        tvTitle.setText(isLive ? "列表订阅" : "EPG订阅");

        String ip = getLocalIpAddress();
        String qrUrl = "http://" + ip + ":9978/";
        tvQrUrl.setText(qrUrl);

        ivQrCode.setOnClickListener(v -> {
            copyToClipboard(qrUrl);
            Toast.makeText(getContext(), "URL已复制: " + qrUrl, Toast.LENGTH_SHORT).show();
        });
        tvQrUrl.setOnClickListener(v -> {
            copyToClipboard(qrUrl);
            Toast.makeText(getContext(), "URL已复制", Toast.LENGTH_SHORT).show();
        });

        findViewById(R.id.btnSubConfirm).setOnClickListener(v -> onConfirm());
        findViewById(R.id.btnSubDelete).setOnClickListener(v -> onDelete());
    }

    private void loadData() {
        dataList = isLive ? getLiveUrls() : getEpgUrls();
        llListContainer.removeAllViews();
        for (int i = 0; i < dataList.size(); i++) {
            View itemView = LayoutInflater.from(getContext()).inflate(R.layout.item_subscribe, llListContainer, false);
            TextView tvName = itemView.findViewById(R.id.tvSubItemName);
            TextView tvCopy = itemView.findViewById(R.id.tvSubCopy);
            TextView tvDelete = itemView.findViewById(R.id.tvSubDelete);
            TextView tvUp = itemView.findViewById(R.id.tvSubUp);

            String url = dataList.get(i);
            tvName.setText(extractName(url));

            final int pos = i;
            itemView.setOnClickListener(v -> {
                etUrl.setText(url);
                etName.setText(extractName(url));
            });
            tvCopy.setOnClickListener(v -> {
                copyToClipboard(url);
                Toast.makeText(getContext(), "已复制到剪贴板", Toast.LENGTH_SHORT).show();
            });
            tvDelete.setOnClickListener(v -> {
                dataList.remove(pos);
                saveUrls(dataList);
                loadData();
                etName.setText("");
                etUrl.setText("");
                Toast.makeText(getContext(), "已删除", Toast.LENGTH_SHORT).show();
                notifyChanged();
            });
            tvUp.setOnClickListener(v -> {
                if (pos > 0) {
                    Collections.swap(dataList, pos, pos - 1);
                    saveUrls(dataList);
                    loadData();
                }
            });

            llListContainer.addView(itemView);
        }
    }

    private void onConfirm() {
        String url = etUrl.getText().toString().trim();
        if (TextUtils.isEmpty(url)) {
            Toast.makeText(getContext(), "地址不能为空", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!dataList.contains(url)) {
            dataList.add(url);
            saveUrls(dataList);
            loadData();
            etName.setText("");
            etUrl.setText("");
            Toast.makeText(getContext(), "已添加", Toast.LENGTH_SHORT).show();
            notifyChanged();
        } else {
            Toast.makeText(getContext(), "该地址已存在", Toast.LENGTH_SHORT).show();
        }
    }

    private void onDelete() {
        String url = etUrl.getText().toString().trim();
        if (TextUtils.isEmpty(url)) {
            Toast.makeText(getContext(), "请选择或输入要删除的地址", Toast.LENGTH_SHORT).show();
            return;
        }
        if (dataList.remove(url)) {
            saveUrls(dataList);
            loadData();
            etName.setText("");
            etUrl.setText("");
            Toast.makeText(getContext(), "已删除", Toast.LENGTH_SHORT).show();
            notifyChanged();
        } else {
            Toast.makeText(getContext(), "列表中不存在该地址", Toast.LENGTH_SHORT).show();
        }
    }

    private List<String> getLiveUrls() {
        List<String> list = Hawk.get(HawkConfig.LIVE_SUBSCRIBE_LIST, new ArrayList<>());
        return list != null ? new ArrayList<>(list) : new ArrayList<>();
    }

    private List<String> getEpgUrls() {
        List<String> list = Hawk.get(HawkConfig.EPG_SUBSCRIBE_LIST, new ArrayList<>());
        return list != null ? new ArrayList<>(list) : new ArrayList<>();
    }

    private void saveUrls(List<String> list) {
        if (isLive) {
            Hawk.put(HawkConfig.LIVE_SUBSCRIBE_LIST, list);
        } else {
            Hawk.put(HawkConfig.EPG_SUBSCRIBE_LIST, list);
        }
    }

    private String extractName(String url) {
        if (url == null) return "";
        int idx = url.lastIndexOf("/");
        if (idx >= 0 && idx < url.length() - 1) {
            String name = url.substring(idx + 1);
            if (name.contains("?")) name = name.substring(0, name.indexOf("?"));
            if (name.contains(".")) {
                String ext = name.substring(name.lastIndexOf(".") + 1);
                if (ext.length() <= 5) name = name.substring(0, name.lastIndexOf("."));
            }
            return name.isEmpty() ? url : name;
        }
        return url;
    }

    private void copyToClipboard(String text) {
        ClipboardManager clipboard = (ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("url", text);
        clipboard.setPrimaryClip(clip);
    }

    private String getLocalIpAddress() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface intf = interfaces.nextElement();
                Enumeration<InetAddress> addrs = intf.getInetAddresses();
                while (addrs.hasMoreElements()) {
                    InetAddress addr = addrs.nextElement();
                    if (!addr.isLoopbackAddress() && addr.getHostAddress().indexOf(':') < 0) {
                        return addr.getHostAddress();
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "127.0.0.1";
    }

    private void notifyChanged() {
        if (listener != null) listener.onChanged();
    }

    public void setOnSubscribeChangeListener(OnSubscribeChangeListener listener) {
        this.listener = listener;
    }

    public interface OnSubscribeChangeListener {
        void onChanged();
    }
}
