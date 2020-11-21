package com.harbinger.parrot.dialog;

import android.content.Context;
import android.text.SpannableString;

public class EditDialogBuilder {
    private Context mContext;
    private EditDialog mDialog;

    public EditDialogBuilder(Context context) {
        this.mContext = context;
        mDialog = new EditDialog(context);
    }

    public EditDialogBuilder setTitle(SpannableString ss) {
        mDialog.setTitle(ss);
        return this;
    }

    public EditDialogBuilder setTitle(String ss) {
        mDialog.setTitle(new SpannableString(ss));
        return this;
    }

    public EditDialogBuilder setOkText(String s) {
        mDialog.setOkText(s);
        return this;
    }

    public EditDialogBuilder setCancelText(String s) {
        mDialog.setCancelText(s);
        return this;
    }

    public EditDialogBuilder setTitleColor(int color) {
        mDialog.setTitleColor(color);
        return this;
    }

    public EditDialogBuilder setTitleSize(int size) {
        mDialog.setTitleSize(size);
        return this;
    }

    public EditDialogBuilder setTitleLeft(boolean isLeft) {
        mDialog.setTitleLeft(isLeft);
        return this;
    }

    public EditDialogBuilder setTitleBold(boolean isBold) {
        mDialog.setTitleBold(isBold);
        return this;
    }

    public EditDialogBuilder setHint(String s) {
        mDialog.setHint(s);
        return this;
    }

    public EditDialogBuilder setInputText(String s) {
        mDialog.setInputText(s);
        return this;
    }

    public EditDialogBuilder setInputType(int type) {
        mDialog.setInputType(type);
        return this;
    }

    public EditDialogBuilder setEditDialogListener(EditDialog.OnEditDialogClickListener listener) {
        mDialog.setListener(listener);
        return this;
    }

    public EditDialog create() {
        return mDialog;
    }
}
