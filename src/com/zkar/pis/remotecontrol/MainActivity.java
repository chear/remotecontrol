package com.zkar.pis.remotecontrol;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

public class MainActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// 启动服务
		Intent intent = new Intent(this, MyService.class);
		startService(intent);
		finish();
		Log.i("WatchDog", "remotecontrolMainActivity  onCreate");

		ContentResolver contentResolver = getContentResolver();
	}
}
