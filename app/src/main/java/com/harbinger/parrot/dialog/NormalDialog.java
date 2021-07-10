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
import android.widget.TextView;

import com.harbinger.parrot.R;


/**
 * 作者：苏航 on 2016/11/4 11:08
 * 邮箱：772192594@qq.com
 */
public class NormalDialog extends Dialog {
    private Context context;
    private TextView dialogTitle;
    private TextView dialogMessage;
    private Button okBtn;
    private Button cancelBtn;
    private SpannableString title, message;
    private String okText, cancelText;
    private int messageColor, titleColor, titleSize, messageSize;
    private boolean titleLeft, messageLeft, titleBold;

    private OnDialogClickListener mOnDialogClickListener;

    public NormalDialog(Context context) {
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
        return R.layout.dialog_normal;
    }

    private void init() {
        dialogTitle = (TextView) findViewById(R.id.dialog_title);
        dialogMessage = (TextView) findViewById(R.id.dialog_message);
        okBtn = (Button) findViewById(R.id.ok_btn);
        cancelBtn = (Button) findViewById(R.id.cancel_btn);

        dialogTitle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            }
        });
        cancelBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
                if (null != mOnDialogClickListener) {
                    mOnDialogClickListener.onCancelClick();
                }
            }
        });
        okBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
                if (null != mOnDialogClickListener) {
                    mOnDialogClickListener.onOkClick();
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
        if (TextUtils.isEmpty(message)) {
            dialogMessage.setVisibility(View.GONE);
        } else {
            dialogMessage.setVisibility(View.VISIBLE);
            dialogMessage.setText(message);
        }
        if (TextUtils.isEmpty(okText)) {
            okBtn.setVisibility(View.GONE);
        } else {
            okBtn.setVisibility(View.VISIBLE);
            okBtn.setText(okText);
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
        if (messageColor > 0) {
            dialogMessage.setTextColor(messageColor);
        }
        if (titleSize > 0) {
            dialogTitle.setTextSize(titleSize);
        }
        if (messageSize > 0) {
            dialogMessage.setTextSize(messageSize);
        }
        if (titleLeft) {
            dialogTitle.setGravity(Gravity.LEFT);
        } else {
            dialogTitle.setGravity(Gravity.CENTER_HORIZONTAL);
        }
        if (messageLeft) {
            dialogMessage.setGravity(Gravity.LEFT);
        } else {
            dialogMessage.setGravity(Gravity.CENTER_HORIZONTAL);
        }
        if (titleBold) {
            TextPaint tp = dialogTitle.getPaint();
            tp.setFakeBoldText(true);
        }
    }

    public void setOnDialogClickListener(OnDialogClickListener onDialogClickListener) {
        mOnDialogClickListener = onDialogClickListener;
    }

    public void setTitle(SpannableString title) {
        this.title = title;
    }

    public void setMessage(SpannableString message) {
        this.message = message;
    }

    public void setCancelText(String cancelText) {
        this.cancelText = cancelText;
    }

    public void setOkText(String okText) {
        this.okText = okText;
    }

    public void setMessageColor(int messageColor) {
        this.messageColor = messageColor;
    }

    public void setTitleColor(int titleColor) {
        this.titleColor = titleColor;
    }

    public void setTitleSize(int titleSize) {
        this.titleSize = titleSize;
    }

    public void setMessageSize(int messageSize) {
        this.messageSize = messageSize;
    }

    public void setTitleBold(boolean titleBold) {
        this.titleBold = titleBold;
    }

    public void setMessageLeft(boolean messageLeft) {
        this.messageLeft = messageLeft;
    }

    public void setTitleLeft(boolean titleLeft) {
        this.titleLeft = titleLeft;
    }

    public interface OnDialogClickListener {
        void onOkClick();

        void onCancelClick();
    }
}
