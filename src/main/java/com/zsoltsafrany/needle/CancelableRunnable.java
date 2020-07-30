package com.zsoltsafrany.needle;

public interface CancelableRunnable extends Runnable {

	void cancel();

	boolean isCanceled();
}
