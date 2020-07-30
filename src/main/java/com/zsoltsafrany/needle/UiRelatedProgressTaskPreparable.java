package com.zsoltsafrany.needle;

public abstract class UiRelatedProgressTaskPreparable<Result, Progress> extends
        UiRelatedProgressTask<Result, Progress> implements Preparable {

    @Override
    public void prepare() {
    }
}
