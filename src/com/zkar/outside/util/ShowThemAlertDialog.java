package com.zkar.outside.util;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Handler;
import android.os.Message;

public class ShowThemAlertDialog extends AlertDialog {
	private int FLAG_DISMISS = 1;
	 private boolean flag = true;

	protected ShowThemAlertDialog(Context context) {
		super(context);
	}
	
	@Override
	public void show() {
		super.show();
	}
	
	@Override
	public void dismiss() {
		super.dismiss();
		
	}

	private Thread mThread = new Thread() {
		@Override
		public void run() {
			super.run();
			while (flag) {
				try {
					Thread.sleep(2000);
					Message msg = mHandler.obtainMessage();
					msg.what = FLAG_DISMISS;
					mHandler.sendMessage(msg);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	};

	private Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			if (msg.what == FLAG_DISMISS)
				dismiss();
		}
	};
}
