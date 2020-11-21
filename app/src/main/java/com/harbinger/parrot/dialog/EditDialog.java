package com.harbinger.parrot.dialog;/**
 * Created by Administrator on 2016/11/4.
 */

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.TextPaint;
import android.text.TextUtils;
import android.view.Display;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.harbinger.parrot.R;


/**
 * 作者：苏航 on 2016/11/4 11:08
 * 邮箱：772192594@qq.com
 */
public class EditDialog extends Dialog {
    private Context context;
    private TextView dialogTitle;
    private Button okBtn;
    private Button cancelBtn;
    private SpannableString title;
    private String okText, cancelText;
    private int titleColor, titleSize;
    private boolean titleLeft, titleBold;
    private String hint, inputText;
    private int inputType = -1;
    private EditText dialogEt;
    private OnEditDialogClickListener mListener;

    public EditDialog(Context context) {
        super(context);
        this.context = context;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(getLayoutId());
        init();

        Window window = this.getWindow();
        WindowManager.LayoutParams lp = window.getAttributes();
        WindowManager wm = ((Activity) context).getWindowManager();
        Display d = wm.getDefaultDisplay();
        // lp.height = (int) (d.getHeight() * 0.4);
        lp.width = (int) (d.getWidth() * 0.9);
        // window.setGravity(Gravity.LEFT | Gravity.TOP);
        window.setGravity(Gravity.CENTER);
//        window.getDecorView().setPadding(0, 0, 0, 0);
        // lp.x = 100;
        // lp.y = 100;
        // lp.height = 30;
        // lp.width = 20;
        window.setAttributes(lp);
    }

    protected int getLayoutId() {
        return R.layout.dialog_edit;
    }

    private void init() {
        dialogTitle = (TextView) findViewById(R.id.title_tv);
        okBtn = (Button) findViewById(R.id.ok_btn);
        cancelBtn = (Button) findViewById(R.id.cancel_btn);
        dialogEt = findViewById(R.id.dialog_et);

        dialogTitle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            }
        });
        cancelBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
                if (null != mListener) {
                    mListener.onCancelClick();
                }
            }
        });
        okBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
                if (null != mListener && !TextUtils.isEmpty(dialogEt.getText().toString())) {
                    mListener.onOkClick(dialogEt.getText().toString());
                }
            }
        });
    }

    @Override
    public void show() {
        super.show();
        if (TextUtils.isEmpty(title)) {
            dialogTitle.setVisibility(View.GONE);
        } else {
            dialogTitle.setVisibility(View.VISIBLE);
            dialogTitle.setText(title);
        }
        if (TextUtils.isEmpty(okText)) {
            okBtn.setVisibility(View.GONE);
        } else {
            okBtn.setVisibility(View.VISIBLE);
            okBtn.setText(okText);
        }
        if (!TextUtils.isEmpty(hint)) {
            dialogEt.setHint(hint);
        }
        if (!TextUtils.isEmpty(inputText)) {
            dialogEt.setText(inputText);
        }
        if (inputType != -1) {
            dialogEt.setInputType(inputType);
        }
        if (TextUtils.isEmpty(cancelText)) {
            cancelBtn.setVisibility(View.GONE);
        } else {
            cancelBtn.setVisibility(View.VISIBLE);
            cancelBtn.setText(cancelText);
        }
        if (titleColor > 0) {
            dialogTitle.setTextColor(titleColor);
        }
        if (titleSize > 0) {
            dialogTitle.setTextSize(titleSize);
        }
        if (titleLeft) {
            dialogTitle.setGravity(Gravity.LEFT);
        } else {
            dialogTitle.setGravity(Gravity.CENTER_HORIZONTAL);
        }
        if (titleBold) {
            TextPaint tp = dialogTitle.getPaint();
            tp.setFakeBoldText(true);
        }
        dialogEt.requestFocus();
    }


    public void setTitle(SpannableString title) {
        this.title = title;
    }

    public void setCancelText(String cancelText) {
        this.cancelText = cancelText;
    }

    public void setOkText(String okText) {
        this.okText = okText;
    }

    public void setTitleColor(int titleColor) {
        this.titleColor = titleColor;
    }

    public void setTitleSize(int titleSize) {
        this.titleSize = titleSize;
    }

    public void setTitleBold(boolean titleBold) {
        this.titleBold = titleBold;
    }

    public void setTitleLeft(boolean titleLeft) {
        this.titleLeft = titleLeft;
    }

    public void setListener(OnEditDialogClickListener listener) {
        mListener = listener;
    }

    public void setHint(String hint) {
        this.hint = hint;
    }

    public void setInputText(String inputText) {
        this.inputText = inputText;
    }

    public void setInputType(int inputType) {
        this.inputType = inputType;
    }

    public interface OnEditDialogClickListener {
        void onOkClick(String result);

        void onCancelClick();
    }
}
