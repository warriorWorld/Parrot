package com.harbinger.parrot.dialog;

import android.content.Context;
import android.text.SpannableString;

public class NormalDialogBuilder {
    private Context mContext;
    private NormalDialog mDialog;

    public NormalDialogBuilder(Context context) {
        this.mContext = context;
        mDialog = new NormalDialog(context);
    }

    public NormalDialogBuilder setTitle(SpannableString ss) {
        mDialog.setTitle(ss);
        return this;
    }

    public NormalDialogBuilder setTitle(String ss) {
        mDialog.setTitle(new SpannableString(ss));
        return this;
    }

    public NormalDialogBuilder setMessage(SpannableString ss) {
        mDialog.setMessage(ss);
        return this;
    }

    public NormalDialogBuilder setMessage(String ss) {
        mDialog.setMessage(new SpannableString(ss));
        return this;
    }

    public NormalDialogBuilder setOkText(String s) {
        mDialog.setOkText(s);
        return this;
    }

    public NormalDialogBuilder setCancelText(String s) {
        mDialog.setCancelText(s);
        return this;
    }

    public NormalDialogBuilder setTitleColor(int color) {
        mDialog.setTitleColor(color);
        return this;
    }

    public NormalDialogBuilder setMessageColor(int color) {
        mDialog.setMessageColor(color);
        return this;
    }

    public NormalDialogBuilder setTitleSize(int size) {
        mDialog.setTitleSize(size);
        return this;
    }

    public NormalDialogBuilder setMessageSize(int size) {
        mDialog.setMessageSize(size);
        return this;
    }

    public NormalDialogBuilder setTitleLeft(boolean isLeft) {
        mDialog.setTitleLeft(isLeft);
        return this;
    }

    public NormalDialogBuilder setMessageLeft(boolean isLeft) {
        mDialog.setMessageLeft(isLeft);
        return this;
    }

    public NormalDialogBuilder setTitleBold(boolean isBold) {
        mDialog.setTitleBold(isBold);
        return this;
    }

    public NormalDialogBuilder setOnDialogClickListener(NormalDialog.OnDialogClickListener listener) {
        mDialog.setOnDialogClickListener(listener);
        return this;
    }

    public NormalDialog create() {
        return mDialog;
    }
}
