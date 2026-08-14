package com.github.tvbox.osc.ui.dialog;

import android.content.Context;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.github.tvbox.osc.R;
import com.github.tvbox.osc.base.BaseDialog;

import org.jetbrains.annotations.NotNull;

/**
 * 通用输入对话框
 */
public class InputDialog extends BaseDialog {

    private TextView tvTitle;
    private EditText etInput;
    private TextView tvConfirm;
    private TextView tvCancel;
    private OnConfirmListener listener;

    public InputDialog(@NonNull @NotNull Context context) {
        super(context);
        setContentView(R.layout.dialog_input);
        initView();
        initListener();
    }

    private void initView() {
        tvTitle = findViewById(R.id.tvTitle);
        etInput = findViewById(R.id.etInput);
        tvConfirm = findViewById(R.id.tvConfirm);
        tvCancel = findViewById(R.id.tvCancel);
    }

    private void initListener() {
        tvConfirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (listener != null) {
                    listener.onConfirm(etInput.getText().toString().trim());
                }
                dismiss();
            }
        });

        tvCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });
    }

    public InputDialog setTitle(String title) {
        if (tvTitle != null) tvTitle.setText(title);
        return this;
    }

    public InputDialog setHint(String hint) {
        if (etInput != null) etInput.setHint(hint);
        return this;
    }

    public InputDialog setDefaultText(String text) {
        if (etInput != null && text != null) {
            etInput.setText(text);
            etInput.setSelection(text.length());
        }
        return this;
    }

    public InputDialog setOnConfirmListener(OnConfirmListener listener) {
        this.listener = listener;
        return this;
    }

    public interface OnConfirmListener {
        void onConfirm(String text);
    }
}
