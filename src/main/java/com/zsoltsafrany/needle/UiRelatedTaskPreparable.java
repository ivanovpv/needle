package com.zsoltsafrany.needle;

import android.os.Handler;
import android.os.Looper;


public abstract class UiRelatedTaskPreparable<Result> extends UiRelatedTask<Result> implements Preparable {

    @Override
    public void prepare() {
    }
}
