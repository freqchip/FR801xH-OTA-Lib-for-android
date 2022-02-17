/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.bluetooth.le.activity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.app.ListActivity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewStub.OnInflateListener;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.view.animation.TranslateAnimation;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.example.bluetooth.le.BluetoothLeClass;
import com.example.bluetooth.le.BluetoothLeClass.OnConnectListener;
import com.example.bluetooth.le.BluetoothLeClass.OnConnectingListener;
import com.example.bluetooth.le.BluetoothLeClass.OnDisconnectListener;
import com.example.bluetooth.le.LeDeviceListAdapter;
import com.example.bluetooth.le.R;
import com.example.bluetooth.le.Utils;
import com.example.bluetooth.le.BluetoothLeClass.OnDataAvailableListener;
import com.example.bluetooth.le.BluetoothLeClass.OnServiceDiscoverListener;
import com.example.bluetooth.le.R.string;

/**
 * Activity for scanning and displaying available Bluetooth LE devices.
 */
public class DeviceScanActivity extends Activity {
	private final static String TAG = DeviceScanActivity.class.getSimpleName();
	private final static String UUID_KEY_DATA = "0000ff01-0000-1000-8000-00805f9b34fb";
	public static List<BluetoothGattService> gattlist;
	private LeDeviceListAdapter mLeDeviceListAdapter;
	/** 搜索BLE终端 */
	private BluetoothAdapter mBluetoothAdapter;
	/** 读写BLE终端 */
	public static BluetoothLeClass mBLE;
	private boolean mScanning;
	private Handler mHandler;
	// Stops scanning after 10 seconds.
	private static final long SCAN_PERIOD = 10000;
	private ImageView searchiv = null;
	private ImageView myiv = null;
	private ViewGroup line2 = null;
	private Dialog dialog;
	private int width, heigh;
	private float density;
	private int screenwidth, screenheigh;
	private ListView blelv = null;
	private TextView mytv = null;
	private ImageView search_img = null;
	private int linheigth;
	private boolean connectfailed = false;
	private boolean connect = true;
	private static String bleaddr;
	private boolean isconnect = false;
	private boolean timeout = false;
	private boolean clickable = true;
	private int connectTime = 0;

	private static final int REQUEST_CODE_ACCESS_COARSE_LOCATION = 1;
	public static final int REQUEST_LOCATION_PERMISSION = 2;
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// getActionBar().setTitle(R.string.title_devices);
		setContentView(R.layout.activity_main);
		mHandler = new Handler();
		initlayout();
//		 Intent intent = new Intent(this,OtaActiviy.class);
//		 startActivity(intent);
		// Use this check to determine whether BLE is supported on the device.
		// Then you can
		// selectively disable BLE-related features.
		if (!getPackageManager().hasSystemFeature(
				PackageManager.FEATURE_BLUETOOTH_LE)) {
			Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT)
					.show();
			finish();
		}

		// Initializes a Bluetooth adapter. For API level 18 and above, get a
		// reference to
		// BluetoothAdapter through BluetoothManager.
		final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
		mBluetoothAdapter = bluetoothManager.getAdapter();

		// Checks if Bluetooth is supported on the device.
		if (mBluetoothAdapter == null) {
			Toast.makeText(this, R.string.error_bluetooth_not_supported,
					Toast.LENGTH_SHORT).show();
			finish();
			return;
		}
		// Intent intent = new
		// Intent(DeviceScanActivity.this,BleViewPage.class);
		// startActivity(intent);
		// 开启蓝牙
		if(!mBluetoothAdapter.isEnabled())
			mBluetoothAdapter.enable();

		if (Build.VERSION.SDK_INT >= 23) {//如果 API level 是大于等于 23(Android 6.0) 时
			//判断是否具有权限
			if (PackageManager.PERMISSION_GRANTED != this.checkSelfPermission(
					Manifest.permission.ACCESS_COARSE_LOCATION)) {
				//判断是否需要向用户解释为什么需要申请该权限
				/*if (this.shouldShowRequestPermissionRationale(
						Manifest.permission.ACCESS_COARSE_LOCATION)) {
					Toast.makeText(this, "需要打开位置权限才可以搜索到Ble设备。", Toast.LENGTH_SHORT).show();
				}*/
				//请求权限
				this.requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
						REQUEST_CODE_ACCESS_COARSE_LOCATION);
			}
		}

		//开启位置服务，支持获取ble蓝牙扫描结果
		if (Build.VERSION.SDK_INT >= 23 && !isLocationOpen(getApplicationContext())) {
			Intent enableLocate = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
			startActivityForResult(enableLocate, REQUEST_LOCATION_PERMISSION);
		}
		mBLE = new BluetoothLeClass(this);
		if (!mBLE.initialize()) {
			Log.e(TAG, "Unable to initialize Bluetooth");
			finish();
		}
		// 发现BLE终端的Service时回调
		mBLE.setOnServiceDiscoverListener(mOnServiceDiscover);
		// 收到BLE终端数据交互的事件
		mBLE.setOnDataAvailableListener(mOnDataAvailable);
		mBLE.setOnDisconnectListener(mOnDisconnect);
		mBLE.setOnConnectingListener(mOnConnecting);
		mBLE.setOnConnectListener(mOnConnect);
//		ConnectingDialog connectingDialog = new ConnectingDialog(this);
//		Window dialogWindow = connectingDialog.getWindow();
//		WindowManager m = this.getWindowManager();
//		Display d = m.getDefaultDisplay();
//		WindowManager.LayoutParams p = dialogWindow.getAttributes(); // 获取对话框当前的参数值
//		p.x = (int) (d.getHeight() * 0.2);
//		p.y = (int) (d.getHeight() * 0.6);
//        p.height = (int) (d.getHeight() * 0.2); // 高度设置为屏幕的0.6，根据实际情况调整
//        p.width = (int) (d.getWidth() * 0.6); // 宽度设置为屏幕的0.65，根据实际情况调整
//        dialogWindow.setAttributes(p);
//		connectingDialog.show();
		dialog = new Dialog(this,R.style.dialog);  
        dialog.setContentView(R.layout.connetingdiglog); 
	}



	@Override
	public void onRequestPermissionsResult(int requestCode,  String[] permissions,  int[] grantResults) {
		if (requestCode == REQUEST_CODE_ACCESS_COARSE_LOCATION) {
			if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
				//用户允许改权限，0表示允许，-1表示拒绝 PERMISSION_GRANTED = 0， PERMISSION_DENIED = -1
				//permission was granted, yay! Do the contacts-related task you need to do.
				//这里进行授权被允许的处理
				//Toast.makeText(this, "位置权限已打开", Toast.LENGTH_SHORT).show();
			} else {
				//permission denied, boo! Disable the functionality that depends on this permission.
				//这里进行权限被拒绝的处理
				Toast.makeText(this, "需要打开位置权限才可以搜索到Ble设备。", Toast.LENGTH_SHORT).show();
			}
		}else {
			super.onRequestPermissionsResult(requestCode, permissions, grantResults);
		}
	}
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == REQUEST_LOCATION_PERMISSION) {
			if (isLocationOpen(getApplicationContext())) {
				//Log.("fang", " request location permission success");
				//Android6.0需要动态申请权限
				//Toast.makeText(this, "定位服务已打开", Toast.LENGTH_SHORT).show();
				/*if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
						!= PackageManager.PERMISSION_GRANTED) {
					//请求权限
					ActivityCompat.requestPermissions(this,
							new String[]{Manifest.permission.ACCESS_COARSE_LOCATION,
									Manifest.permission.ACCESS_FINE_LOCATION},
							IntentCons.REQUEST_LOCATION_PERMISSION);
					if (ActivityCompat.shouldShowRequestPermissionRationale(this,
							Manifest.permission.ACCESS_COARSE_LOCATION)) {
						//判断是否需要解释
						DialogUtils.shortT(getApplicationContext(), "需要蓝牙权限");
					}
				}*/

			} else {
				//若未开启位置信息功能，则退出该应用
				Toast.makeText(this, "需要打开定位服务才可以搜索到Ble设备", Toast.LENGTH_SHORT).show();
			}
		}

		super.onActivityResult(requestCode, resultCode, data);

	}
	/**
	 *判断位置信息是否开启
	 * @param context
	 * @return
	 */
	public static boolean isLocationOpen(final Context context){
		LocationManager manager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
		//gps定位
		boolean isGpsProvider = manager.isProviderEnabled(LocationManager.GPS_PROVIDER);
		//网络定位
		boolean isNetWorkProvider = manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
		return isGpsProvider|| isNetWorkProvider;
	}
	void initlayout() {

		DisplayMetrics dm = getResources().getDisplayMetrics();
		width = dm.widthPixels;
		heigh = dm.heightPixels;
		density = dm.density;
		screenwidth = (int) (width / density);
		screenheigh = (int) (heigh / density);
		searchiv = (ImageView) findViewById(R.id.searchiv);
		myiv = (ImageView) findViewById(R.id.myiv);
		line2 = (ViewGroup) findViewById(R.id.line2);
		blelv = (ListView) findViewById(R.id.blelv);
		mytv = (TextView) findViewById(R.id.mytv);
		search_img = (ImageView) findViewById(R.id.search_img);

		roanimation = new RotateAnimation(0f, 359f, Animation.RELATIVE_TO_SELF,
				0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
		LinearInterpolator lir = new LinearInterpolator();
		roanimation.setInterpolator(lir);
		roanimation.setDuration(1000);
		roanimation.setRepeatCount(-1);
		myiv.startAnimation(roanimation);
		RotateAnimation roanimationsearch = new RotateAnimation(0f, 359f,
				Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF,
				0.5f);
		roanimationsearch.setInterpolator(new LinearInterpolator());
		roanimationsearch.setDuration(2000);
		roanimationsearch.setRepeatCount(-1);
		searchiv.startAnimation(roanimationsearch);
		line2.setVisibility(View.INVISIBLE);
		new Thread(new Runnable() {

			@Override
			public void run() {
				try {
					Thread.sleep(5000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				Message message = new Message();
				myhandler.sendEmptyMessage(1);

			}
		}).start();
	}

	private int dp2px(int dpValue) {
		return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
				dpValue, getResources().getDisplayMetrics());
	}

	private int sp2px(int spValue) {
		return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP,
				spValue, getResources().getDisplayMetrics());
	}

	@SuppressLint("HandlerLeak")
	Handler myhandler = new Handler() {
		public void handleMessage(android.os.Message msg) {
			switch (msg.what) {
			case 1:
				line2.setVisibility(View.VISIBLE);
				int linwidth = line2.getWidth();
				linheigth = line2.getHeight();

				searchiv.clearAnimation();
				searchiv.setVisibility(View.GONE);
				mytv.setVisibility(View.GONE);
				//System.out.println("heigh　" + screenheigh + " " + linheigth
				//		+ " " + screenwidth);
				RelativeLayout.LayoutParams lp = (android.widget.RelativeLayout.LayoutParams) line2
						.getLayoutParams();
				// lp.setMargins(0, screenheigh,screenwidth, screenheigh +
				// linheigth);
				lp.setMargins(0, -1, 0, -(linheigth));
				line2.requestLayout();
				TranslateAnimation trananumation = new TranslateAnimation(
						Animation.RELATIVE_TO_SELF, 0f,
						Animation.RELATIVE_TO_SELF, 0f,
						Animation.RELATIVE_TO_SELF, 0f,
						Animation.RELATIVE_TO_SELF, -1f);
				trananumation.setDuration(1000);
				trananumation.setFillAfter(true);
				line2.startAnimation(trananumation);
				trananumation
						.setAnimationListener(new Animation.AnimationListener() {

							@Override
							public void onAnimationStart(Animation animation) {
								// TODO Auto-generated method stub

							}

							@Override
							public void onAnimationRepeat(Animation animation) {
								// TODO Auto-generated method stub

							}

							@Override
							public void onAnimationEnd(Animation animation) {
								line2.clearAnimation();
								search_img
										.setOnClickListener(new OnClickListenerimp());
								RelativeLayout.LayoutParams lp = (android.widget.RelativeLayout.LayoutParams) line2
										.getLayoutParams();
								// lp.setMargins(0, -1,0, -linheigth);
								lp.setMargins(0, 0, 0, 0);
								line2.setLayoutParams(lp);
							}
						});
				break;
			case 2:
				System.out.println("con " + connectfailed);
				if (connectfailed == false) {
					connect = false;
					clickable = true;
					dialog.dismiss();
					Intent intent = new Intent(DeviceScanActivity.this,
							OtaActiviy.class);
					startActivity(intent);
				} else {
					System.out.println("addrsss " + bleaddr);
					if (connect) {
						connectTime ++;
						if(connectTime < 2){
							
							mBLE.connect(bleaddr);
							timeout = false;
						}else{
							connectTime = 0;
							dialog.dismiss();
						}
					}
				}
				
				connectfailed = false;
				break;

				case 3:
				{
					Log.d(TAG,"discoverService again");
					mBLE.connect(bleaddr);
				}
					break;

			}

		};

	};

	@Override
	protected void onResume() {
		super.onResume();

		// Initializes list view adapter.
		mLeDeviceListAdapter = new LeDeviceListAdapter(this);
		blelv.setAdapter(mLeDeviceListAdapter);
		blelv.setOnItemClickListener(new OnItemClickListenerimp());

		myiv.startAnimation(roanimation);
		// setListAdapter(mLeDeviceListAdapter);
		scanLeDevice(true);
	}

	@Override
	protected void onPause() {
		super.onPause();
		scanLeDevice(false);
		// mLeDeviceListAdapter.clear();
		// mBLE.disconnect();
	}

	class OnClickListenerimp implements OnClickListener {

		@Override
		public void onClick(View v) {
			scanLeDevice(true);
			//System.out.println("onclick");
		}

	}

	class OnItemClickListenerimp implements OnItemClickListener {

		@Override
		public void onItemClick(AdapterView<?> arg0, View v, int position,
				long arg3) {
			//if(clickable == false) return;
			clickable = false;
			final BluetoothDevice device = mLeDeviceListAdapter
					.getDevice(position);
			if (device == null)
				return;
			if (mScanning) {
				mBluetoothAdapter.stopLeScan(mLeScanCallback);
				mScanning = false;
			}
			//blelv.setItemsCanFocus(false);
			dialog.show();
			connectfailed = false;
			connect = true;
			timeout = false;
			bleaddr = device.getAddress();
			mBLE.connect(bleaddr);

		}

	}

	private void scanLeDevice(final boolean enable) {
		if (enable) {
			// Stops scanning after a pre-defined scan period.
			mHandler.postDelayed(new Runnable() {
				@Override
				public void run() {
					mScanning = false;
					mBluetoothAdapter.stopLeScan(mLeScanCallback);
					invalidateOptionsMenu();
				}
			}, SCAN_PERIOD);
			myiv.setImageResource(R.drawable.searchrotate);
			mScanning = true;
			mBluetoothAdapter.startLeScan(mLeScanCallback);
			//mBluetoothAdapter.startDiscovery()
		} else {
			mScanning = false;
			myiv.clearAnimation();
			myiv.setImageResource(R.drawable.stopsearch);
			mBluetoothAdapter.stopLeScan(mLeScanCallback);
		}
		invalidateOptionsMenu();
	}

	/**
	 * 搜索到BLE终端服务的事件
	 */
	private BluetoothLeClass.OnServiceDiscoverListener mOnServiceDiscover = new OnServiceDiscoverListener() {

		@Override
		public void onServiceDiscover(BluetoothGatt gatt,int status) {
			displayGattServices(mBLE.getSupportedGattServices());
		}

	};

	void read_data(String action) {
		Intent intent = new Intent(action);
		this.sendBroadcast(intent);

	}

	/**
	 * 收到BLE终端数据交互的事件
	 */
	private BluetoothLeClass.OnDataAvailableListener mOnDataAvailable = new OnDataAvailableListener() {

		/**
		 * BLE终端数据被读的事件
		 */
		@Override
		public void onCharacteristicRead(BluetoothGatt gatt,
				BluetoothGattCharacteristic characteristic, int status) {
			if (status == BluetoothGatt.GATT_SUCCESS) {
				read_data("leddata");
				//System.out.println("onCharRead " + gatt.getDevice().getName()
				//		+ " read " + characteristic.getUuid().toString()
					//	+ " -> "
					//	+ Utils.bytesToHexString(characteristic.getValue()));
			}

		}

		/**
		 * 收到BLE终端写入数据回调
		 */
		@Override
		public void onCharacteristicWrite(BluetoothGatt gatt,
				BluetoothGattCharacteristic characteristic) {
			// baseaddr = characteristic.getValue();
			// rf.savefile(baseaddr, baseaddr.length);
			read_data("ledack");
			// _txtRead.append(count++ +" " + '\n');
		}
	};
	private BluetoothLeClass.OnConnectingListener mOnConnecting = new OnConnectingListener() {

		@Override
		public void onConnecting(BluetoothGatt gatt) {
			isconnect = true;
			System.out.println("connecting");
			blelv.setEnabled(false);
		}

	};
	private BluetoothLeClass.OnConnectListener mOnConnect = new OnConnectListener() {
		@Override
		public void onConnect(BluetoothGatt gatt) {
			isconnect = false;
			gatt.discoverServices();

			myhandler.sendEmptyMessageDelayed(3,3000);
		}


	};
	private BluetoothLeClass.OnDisconnectListener mOnDisconnect = new OnDisconnectListener() {

		@Override
		public void onDisconnect(BluetoothGatt gatt) {
			connectfailed = true;
			isconnect = false;
		}

	};
	// Device scan callback.
	private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {

		@Override
		public void onLeScan(final BluetoothDevice device, int rssi,
				byte[] scanRecord) {
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					mLeDeviceListAdapter.addDevice(device);
					mLeDeviceListAdapter.notifyDataSetChanged();
				}
			});
		}
	};
	private RotateAnimation roanimation;

	private void displayGattServices(List<BluetoothGattService> gattServices) {
		if ((gattServices == null) || (timeout == true))
			return;
		new Thread(new Runnable() {

			@Override
			public void run() {
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				myhandler.removeMessages(3);
				//Message message = new Message();
				myhandler.sendEmptyMessage(2);

			}
		}).start();
		timeout = true;
		connectfailed = false;
		
		gattlist = gattServices;
		//Intent intent = new Intent(DeviceScanActivity.this,
		//		LedctrActivity.class);
		//startActivity(intent);
		/*
		 * for (BluetoothGattService gattService : gattServices) {
		 * //-----Service的字段信息-----// int type = gattService.getType();
		 * System.out.println("-->service type:"+Utils.getServiceType(type));
		 * System
		 * .out.println("-->includedServices size:"+gattService.getIncludedServices
		 * ().size());
		 * System.out.println("-->service uuid:"+gattService.getUuid());
		 * 
		 * //-----Characteristics的字段信息-----// List<BluetoothGattCharacteristic>
		 * gattCharacteristics =gattService.getCharacteristics(); for (final
		 * BluetoothGattCharacteristic gattCharacteristic: gattCharacteristics)
		 * { System.out.println("---->char uuid:"+gattCharacteristic.getUuid());
		 * 
		 * int permission = gattCharacteristic.getPermissions();
		 * System.out.println
		 * ("---->char permission:"+Utils.getCharPermission(permission));
		 * 
		 * int property = gattCharacteristic.getProperties();
		 * System.out.println(
		 * "---->char property:"+Utils.getCharPropertie(property));
		 * 
		 * byte[] data = gattCharacteristic.getValue(); if (data != null &&
		 * data.length > 0) { System.out.println("---->char value:"+new
		 * String(data)); }
		 * 
		 * //UUID_KEY_DATA是可以跟蓝牙模块串口通信的Characteristic
		 * if(gattCharacteristic.getUuid().toString().equals(UUID_KEY_DATA)){
		 * //测试读取当前Characteristic数据，会触发mOnDataAvailable.onCharacteristicRead()
		 * mHandler.postDelayed(new Runnable() {
		 * 
		 * @Override public void run() {
		 * mBLE.readCharacteristic(gattCharacteristic); } }, 500);
		 * 
		 * //接受Characteristic被写的通知,收到蓝牙模块的数据后会触发mOnDataAvailable.
		 * onCharacteristicWrite()
		 * mBLE.setCharacteristicNotification(gattCharacteristic, true);
		 * //设置数据内容 gattCharacteristic.setValue("send data->"); //往蓝牙模块写入数据
		 * mBLE.writeCharacteristic(gattCharacteristic); }
		 * 
		 * //-----Descriptors的字段信息-----// List<BluetoothGattDescriptor>
		 * gattDescriptors = gattCharacteristic.getDescriptors(); for
		 * (BluetoothGattDescriptor gattDescriptor : gattDescriptors) {
		 * System.out.println("-------->desc uuid:" + gattDescriptor.getUuid());
		 * int descPermission = gattDescriptor.getPermissions();
		 * System.out.println("-------->desc permission:"+
		 * Utils.getDescPermission(descPermission));
		 * 
		 * byte[] desData = gattDescriptor.getValue(); if (desData != null &&
		 * desData.length > 0) { System.out.println("-------->desc value:"+ new
		 * String(desData)); } } } }
		 */

	}
}