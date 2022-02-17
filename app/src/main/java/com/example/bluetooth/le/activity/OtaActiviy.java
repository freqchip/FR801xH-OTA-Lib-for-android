package com.example.bluetooth.le.activity;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.example.bluetooth.le.BluetoothLeClass;
import com.example.bluetooth.le.BluetoothLeClass.OnRecvDataListerner;
import com.example.bluetooth.le.AdapterManager;
import com.example.bluetooth.le.BluetoothApplication;
import com.example.bluetooth.le.BluetoothLeClass.OnWriteDataListener;
import com.example.bluetooth.le.LeDeviceListAdapter;
import com.example.bluetooth.le.R;
import com.example.bluetooth.le.WriterOperation;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.ParcelUuid;
import android.os.Parcelable;
import android.provider.Settings;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import static android.bluetooth.BluetoothGatt.GATT_SUCCESS;
import static android.content.ContentValues.TAG;

public class OtaActiviy extends Activity {
	private final static String UUID_SEND_DATA = "0000ff01-0000-1000-8000-00805f9b34fb";
	private final static String UUID_RECV_DATA = "0000ff02-0000-1000-8000-00805f9b34fb";
	private final static String UUID_SERVICE_DATA_H = "02f00000-0000-0000-0000-00000000fe00";
	private final static String UUID_SEND_DATA_H = "02f00000-0000-0000-0000-00000000ff01";
	private final static String UUID_RECV_DATA_H = "02f00000-0000-0000-0000-00000000ff02";
	private final static String UUID_DES = "00002902-0000-1000-8000-00805f9b34fb";
	private final static int OTA_CMD_NVDS_TYPE = 0;
	private final static int OTA_CMD_GET_STR_BASE = 1;
	private final static int OTA_CMD_PAGE_ERASE = 3;
	private final static int OTA_CMD_CHIP_ERASE = 4;
	private final static int OTA_CMD_WRITE_DATA = 5;
	private final static int OTA_CMD_READ_DATA = 6;
	private final static int OTA_CMD_WRITE_MEM = 7;
	private final static int OTA_CMD_READ_MEM = 8;
	private final static int OTA_CMD_REBOOT = 9;
	private final static int OTA_CMD_NULL = 10;
	private final static int DEVICE_8010 = 0;
	private final static int DEVICE_8010H = 1;
	public static final int RESULT_CODE = 1000;
	public static final String SEND_FILE_NAME = "sendFileName";
	private static final int REQUEST_CODE_ACCESS_COARSE_LOCATION = 1;
	private static final int REQUEST_LOCATION_PERMISSION = 2;

	private String filePath;
	private String sharepath = null;
	private BluetoothDevice btDev;
	private AdapterManager mAdapterManager;
	private BluetoothApplication mApplication;
	private BluetoothAdapter mBluetoothAdapter;
	private LeDeviceListAdapter mLeDeviceListAdapter;
	private BluetoothLeClass bleclass;


	private TextView precenttv;
	private View view;
	private Dialog mDialog;
	private Editor editor;
	private Button searchBn;
	private EditText _txtRead;
	private EditText pathet = null;

	private boolean writeStatus = false;

	private int sencondaddr = 0x14000;
	private int firstaddr = 0;
	private int recv_data;
	private int writePrecent;
	private int delay_num;
	private long leng;
	private byte[] recvValue = null;

	private Handler mHandler;
	private SharedPreferences sp;
	private InputStream input;
	private FileInputStream isfile = null;
	private LayoutInflater layoutinflater;
	//private String checkSum;
	//private int checkSumLength;
	//private List<BluetoothGattCharacteristic> gattCharacteristics;
	private WriterOperation woperation;
	private BluetoothGattCharacteristic mgattCharacteristic = null;
	private BluetoothGattDescriptor descriptor = null;
//	private BluetoothGattCharacteristic mHgattCharacteristic = null;
//	private BluetoothGattCharacteristic mledwritegattCharacteristic = null;
//	private BluetoothGattCharacteristic readgattCharacteristic = null;
//	private BluetoothGattCharacteristic ledreadgattCharacteristic = null;


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.file);
		viewinit();

		if (!getPackageManager().hasSystemFeature(
				PackageManager.FEATURE_BLUETOOTH_LE)) {
			Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT)
					.show();
			finish();
		}

		final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
		mBluetoothAdapter = bluetoothManager.getAdapter();

		// Checks if Bluetooth is supported on the device.
		if (mBluetoothAdapter == null) {
			Toast.makeText(this, R.string.error_bluetooth_not_supported,
					Toast.LENGTH_SHORT).show();
			finish();
			return;
		}

		if(!mBluetoothAdapter.isEnabled())
			mBluetoothAdapter.enable();

		if (Build.VERSION.SDK_INT >= 23) {//如果 API level 是大于等于 23(Android 6.0) 时
			if (PackageManager.PERMISSION_GRANTED != this.checkSelfPermission(
					Manifest.permission.ACCESS_COARSE_LOCATION)) {
				requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
						REQUEST_CODE_ACCESS_COARSE_LOCATION);
			}
		}

		if (Build.VERSION.SDK_INT >= 23 && !isLocationOpen(getApplicationContext())) {
			Intent enableLocate = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
			startActivityForResult(enableLocate, REQUEST_LOCATION_PERMISSION);
		}

		mHandler = new MyHandler();
		woperation = new WriterOperation();

		bleclass = new BluetoothLeClass(this);
		if (!bleclass.initialize()) {
			Log.e(TAG, "Unable to initialize Bluetooth");
			finish();
		}

		bleclass.setOnConnectListener(mOnConnect);
		bleclass.setOnDisconnectListener(mOnDisconnect);
		bleclass.setOnServiceDiscoverListener(mOnServiceDiscover);
		bleclass.setOnRecvDataListener(mOnRecvData);
		bleclass.setOnWriteDataListener(mOnWriteData);

		mLeDeviceListAdapter = new LeDeviceListAdapter(this);
		checkSharedPreferences();

		mApplication = BluetoothApplication.getInstance();
		mApplication.getTouchObject();
		mAdapterManager = new AdapterManager(this);
		mApplication.setAdapterManager(mAdapterManager);

		IntentFilter intent = new IntentFilter();
		intent.addAction(BluetoothDevice.ACTION_FOUND);
		registerReceiver(scanBleReceiver,intent);

	}
	void checkSharedPreferences(){
		sp = getSharedPreferences("config", Context.MODE_PRIVATE);
		editor = sp.edit();
		sharepath = sp.getString("path", "");
		pathet.setText(sharepath);
		if (sharepath.length() <= 0) {
			editor.putString("path", "");
			editor.commit();
		}
	}
	private BroadcastReceiver scanBleReceiver = new BroadcastReceiver(){
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();

			if(action!= null){
				if(action.equals(BluetoothDevice.ACTION_FOUND)){

					BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

					String name = device.getName();
					if(name != null) {
						Log.d(TAG, "scanBleReceiver："+name);
						mLeDeviceListAdapter.addDevice(device);
						mLeDeviceListAdapter.notifyDataSetChanged();

					}
				}

			}
		}
	};
	public static boolean isLocationOpen(final Context context){
		LocationManager manager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
		//gps定位
		boolean isGpsProvider = manager.isProviderEnabled(LocationManager.GPS_PROVIDER);
		//网络定位
		boolean isNetWorkProvider = manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
		return isGpsProvider|| isNetWorkProvider;
	}
	private static final int REQUEST_EXTERNAL_STORAGE = 1;
	private static String[] PERMISSIONS_STORAGE = {
			Manifest.permission.READ_EXTERNAL_STORAGE,
			Manifest.permission.WRITE_EXTERNAL_STORAGE,
			Manifest.permission.MOUNT_UNMOUNT_FILESYSTEMS};

	public boolean verifyStoragePermissions() {
		int permission = 0;
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			permission = checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE);
		}
		Log.d("TAG","false " +permission);
		if (permission != PackageManager.PERMISSION_GRANTED) {

			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
				requestPermissions( PERMISSIONS_STORAGE, REQUEST_EXTERNAL_STORAGE);
			}
			return false;
		}
		Log.d("TAG","true");
		return true;
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {

		if (requestCode == REQUEST_EXTERNAL_STORAGE) {
			if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
				;
			} else {
				Toast.makeText(this, "需要打开存储权限才可以OTA", Toast.LENGTH_SHORT).show();
			}
		}
		else {
			super.onRequestPermissionsResult(requestCode, permissions, grantResults);
		}
	}
	void viewinit(){
		searchBn = (Button) findViewById(R.id.searchbt);
		_txtRead = (EditText) findViewById(R.id.etShow);
		pathet = (EditText)findViewById(R.id.pathet);
		searchBn.setOnLongClickListener(new onLongClickImp());

//		TextView tipTv = findViewById(R.id.tvtitle);
//		tipTv.setText("Tips:" +"\r\n1.需打开位置和存储权限.\r\n2.搜索设备, 连接成功后开始升级.\r\n3.长按已连接,断开当前连接");
	}
	@Override
	protected void onDestroy() {
		bleclass.disconnect();
		bleclass.close();
		bleclass = null;
		Log.d("OTA","onDestroy");
		unregisterReceiver(scanBleReceiver);
		super.onDestroy();
	}

	private void showDisconnectDialog() {

		layoutinflater = LayoutInflater.from(OtaActiviy.this);
		view = layoutinflater.inflate(R.layout.loading_process_dialog_anim,null);

		precenttv = (TextView) view.findViewById(R.id.precenttv);
		mDialog = new Dialog(this, R.style.dialog);
		precenttv.setText("断线中...");
		// mDialog.setOnKeyListener(keyListener);
		mDialog.setCancelable(false);
		mDialog.setContentView(view);

		mDialog.show();
	}

	private void showConnectingDialog() {

		layoutinflater = LayoutInflater.from(OtaActiviy.this);
		view = layoutinflater.inflate(R.layout.loading_process_dialog_anim,null);

		precenttv = (TextView) view.findViewById(R.id.precenttv);
		mDialog = new Dialog(this, R.style.dialog);
		precenttv.setText("正在连接...");
		// mDialog.setOnKeyListener(keyListener);
		//mDialog.setCancelable(false);
		mDialog.setContentView(view);

		mDialog.show();
	}
	private void showDialog(){

		 layoutinflater = LayoutInflater.from(OtaActiviy.this);
		 view = layoutinflater.inflate(R.layout.loading_process_dialog_anim,null);

		precenttv = (TextView) view.findViewById(R.id.precenttv);
		mDialog = new Dialog(this, R.style.dialog);

		// mDialog.setOnKeyListener(keyListener);
		mDialog.setCancelable(false);
		mDialog.setContentView(view);

		mDialog.show();

	}
	private BluetoothLeClass.OnConnectListener mOnConnect = new BluetoothLeClass.OnConnectListener() {
		@Override
		public void onConnect(BluetoothGatt gatt) {
				//3秒超时 , 重连? myhandler.sendEmptyMessageDelayed(3,3000);
				gatt.discoverServices();
			}
	};
	private BluetoothLeClass.OnDisconnectListener mOnDisconnect = new BluetoothLeClass.OnDisconnectListener() {
		@Override
		public void onDisconnect(BluetoothGatt gatt) {
			mHandler.sendEmptyMessage(7);//断线
		}
	};
	private BluetoothLeClass.OnServiceDiscoverListener mOnServiceDiscover = new BluetoothLeClass.OnServiceDiscoverListener() {
		@Override
		public void onServiceDiscover(BluetoothGatt gatt, int status) {
			if(status == GATT_SUCCESS){
				UUID UUID_SERVICE_H = UUID.fromString(UUID_SERVICE_DATA_H);
				UUID UUID_SEND_H = UUID.fromString(UUID_SEND_DATA_H);
				UUID UUID_RECV_H = UUID.fromString(UUID_RECV_DATA_H);
				try{
						BluetoothGattCharacteristic gattCharacteristic = gatt.getService(UUID_SERVICE_H).getCharacteristic(UUID_SEND_H);
						Log.d(TAG,"GATT uuid:"+gattCharacteristic.getUuid());
						String uuidString = gattCharacteristic.getUuid().toString();
						if(uuidString.equals(UUID_SEND_DATA_H)){
							//setTitle("找到端口");
						mgattCharacteristic = gattCharacteristic;
						mHandler.sendEmptyMessage(5);//找到端口
						}
						gattCharacteristic = gatt.getService(UUID_SERVICE_H).getCharacteristic(UUID_RECV_H);
						uuidString = gattCharacteristic.getUuid().toString();
						if(uuidString.equals(UUID_RECV_DATA_H)){

						descriptor = gattCharacteristic.getDescriptor(UUID.fromString(UUID_DES));
						if (descriptor != null) {
							bleclass.setCharacteristicNotification(gattCharacteristic, true);

							descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
							bleclass.writeDescriptor(descriptor);
							mHandler.sendEmptyMessage(6);//使能成功
						}
					}
				} catch(Exception e) {
					mHandler.sendEmptyMessage(8);//未找到UUID
				}
			}
		}
	};
	private BluetoothLeClass.OnRecvDataListerner mOnRecvData = new OnRecvDataListerner() {

		@Override
		public void OnCharacteristicRecv(BluetoothGatt gatt,
				BluetoothGattCharacteristic characteristic) {
			recvValue = characteristic.getValue();
			setRecv_data(1);
		}
	};
	private BluetoothLeClass.OnWriteDataListener mOnWriteData = new OnWriteDataListener() {

		@Override
		public void OnCharacteristicWrite(BluetoothGatt gatt,
				BluetoothGattCharacteristic characteristic, int status) {
			    //System.out.println("status " + status);
				if(status == 0){
					writeStatus = true;
				}
		}

	};

	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == RESULT_CODE) {
			// 请求为 "选择文件"
			try {
				// 取得选择的文件名
				String sendFileName = data.getStringExtra(SEND_FILE_NAME);
				editor.putString("path", sendFileName);
				editor.commit();
				pathet.setText(sendFileName);
			} catch (Exception e) {

			}
		}

	}

	private class  onLongClickImp implements View.OnLongClickListener{
		@Override
		public boolean onLongClick(View v) {
			if(v.getId() == R.id.searchbt){
				String bnString = searchBn.getText().toString();
				if(bnString.equals("已连接")){
					bleclass.disconnect();
				}
			}
			return false;
		}
	}


	public void searchBnOnclick(View v) {
		//Log.d("search","searchBnOnclick");
		//Button searchBn = findViewById(R.id.searchbt);
		String bnString = searchBn.getText().toString();
		if(bnString.equals("搜索")){
			if(mBluetoothAdapter.isDiscovering())
				mBluetoothAdapter.cancelDiscovery();
			mLeDeviceListAdapter.clear();
			mLeDeviceListAdapter.notifyDataSetChanged();
			mBluetoothAdapter.startDiscovery();
			Set<BluetoothDevice> bondedDevice = mBluetoothAdapter.getBondedDevices();
			if(bondedDevice.size() > 0){
				for(BluetoothDevice device:bondedDevice) {
					mLeDeviceListAdapter.addDevice(device);
				}
				mLeDeviceListAdapter.notifyDataSetChanged();
			}
			AlertDialog.Builder builder=new AlertDialog.Builder(OtaActiviy.this);
			builder.setAdapter(mLeDeviceListAdapter, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialogInterface, int position) {

					btDev = mLeDeviceListAdapter.getDevice(position);

					if(btDev == null)return;

					if(mBluetoothAdapter.isDiscovering())
						mBluetoothAdapter.cancelDiscovery();
					bleclass.connect(btDev.getAddress());
					mHandler.sendEmptyMessage(4);//开始连接
					//mBluetoothGatt = btDev.connectGatt(MainActivity.this,false,mBluetoothGattCallback);
				}
			}).show();

		}
	}

	public void localBnOnclick(View v){
		if ( verifyStoragePermissions() ) {
			Intent intent = new Intent(OtaActiviy.this,
					SelectFileActivity.class);
			intent.putExtra("filepatch", pathet.getText().toString());
			startActivityForResult(intent, OtaActiviy.RESULT_CODE);
		}
	}

	public void updateBnOnclick(View v){
		//byte[] Buffer = new byte[4];
		if(bleclass.isDisconnected) {
			Toast.makeText(OtaActiviy.this, "未连接", Toast.LENGTH_SHORT).show();
			return;
		}
		filePath = pathet.getText().toString().trim();
		File file = new File(filePath);
		if (file.length() < 100) {
			Toast.makeText(OtaActiviy.this, "请选择有效的配置文件", Toast.LENGTH_SHORT).show();
			return;
		}
		bleclass.mtuChange = false;
		mHandler.sendEmptyMessage(3);
		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					doSendFileByBluetooth(filePath);
					//mHandler.sendEmptyMessage(1);
				} catch (FileNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}).start();
	}

	private int page_erase(int addr, long length, BluetoothGattCharacteristic mgattCharacteristic, BluetoothLeClass bleclass) {

		long count = length / 0x1000;
		if ((length % 0x1000) != 0) {
			count++;
		}
		for (int i = 0; i < count; i++) {
			while ( ! woperation.send_data(OTA_CMD_PAGE_ERASE, addr, null, 0, mgattCharacteristic, bleclass)) {
				try{
					Thread.sleep(50);
					Log.d("TAG","send_data error");
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			delay_num =0;
			while(!writeStatus) {
				delay_num++;
				if(delay_num % 8000 == 0) {
					Log.d("TAG","send_data once more");
					woperation.send_data(OTA_CMD_PAGE_ERASE, addr, null, 0, mgattCharacteristic, bleclass);
				}
			}
			while (getRecv_data() != 1);
			setRecv_data(0);
			addr += 0x1000;
		}
		return 0;
	}
	public int getRecv_data() {
		return recv_data;
	}

	public void setRecv_data(int recv_data) {
		this.recv_data = recv_data;
	}
    boolean checkDisconnect(){
    	if(bleclass != null && bleclass.isDisconnected){
    		mHandler.sendEmptyMessage(2);
    		return true;
    	}
    	return false;
    }
	@SuppressLint("HandlerLeak")
	private class MyHandler extends Handler {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
				case 0:
					mDialog.cancel();
					Toast.makeText(OtaActiviy.this, "写入成功",
							Toast.LENGTH_SHORT).show();
					_txtRead.setText("写入成功");
					break;
				case 1:
					precenttv.setText("写入.." + writePrecent + "%");
					break;
				case 2:
					mDialog.cancel();
					Toast.makeText(OtaActiviy.this, "连接断开",
							Toast.LENGTH_LONG).show();
					//finish();
					break;
				case 3:
					showDialog();
					break;
				case 4:
					showConnectingDialog();
					break;

				case 5:
				break;
				case 6:
					mDialog.cancel();

					searchBn.setText("已连接");
					_txtRead.setText("名称:"+btDev.getName() + "\r\n地址:"+btDev.getAddress());
					Toast.makeText(OtaActiviy.this, "连接成功", Toast.LENGTH_SHORT).show();
					break;
				case 7:
				{
					_txtRead.setText(null);
					mDialog.cancel();
					searchBn.setText("搜索");
					Toast.makeText(OtaActiviy.this, "断开连接",
							Toast.LENGTH_LONG).show();
				}
				break;

				case 8:
				{
					mDialog.cancel();
					//_txtRead.setText("未找到OTA所用UUID");
					Toast.makeText(OtaActiviy.this, "未找到OTA所用UUID", Toast.LENGTH_LONG).show();
					bleclass.disconnect();
				}
					break;
				default:
					break;
			}
		}
	}

	public void doSendFileByBluetooth(String filePath)
			throws FileNotFoundException {
		if (!filePath.equals(null)) {
			int read_count;
			int i = 0;
			int addr;
			int lastReadCount = 0;
			int packageSize = 235;//bleclass.mtuSize - 3; //235;
			int send_data_count = 0;
			int deviceType;

			File file = new File(filePath);// 成文件路径中获取文件
			isfile = new FileInputStream(file);
			leng = file.length();
			input = new BufferedInputStream(isfile);
			//crc 校验
			int fileCRC=0;
			try {
				fileCRC = getCRC32new(filePath);
			} catch (Exception e) {
				e.printStackTrace();
			}
			//Log.d("TAG CRC",Integer.toHexString(fileCRC));
			setRecv_data(0);
			woperation.send_data(OTA_CMD_NVDS_TYPE, 0, null, 0,
					mgattCharacteristic, bleclass);	
			while (getRecv_data() != 1){
				if(checkDisconnect()){
					return;
				}	
			}
			if ((woperation.bytetochar(recvValue) & 0x10) == 0) {
				deviceType = DEVICE_8010;
				bleclass.requestMtu(247);
			} else {
				deviceType = DEVICE_8010H;
				bleclass.requestMtu(512);
				
			}
			while(bleclass.mtuChange == false){
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				System.out.println("mtuChange " + bleclass.mtuChange);
			}
			packageSize = bleclass.mtuSize - 3 - 9;
			byte[] inputBuffer = new byte[packageSize];
			setRecv_data(0);
			woperation.send_data(OTA_CMD_GET_STR_BASE, 0, null, 0,
					mgattCharacteristic, bleclass);
			while (getRecv_data() != 1){
				if(checkDisconnect()){
					return;
				}	
			}
			if(deviceType == DEVICE_8010){
				if (woperation.bytetoint(recvValue) == firstaddr) {
					addr = sencondaddr;
				} else {
					addr = firstaddr;
				}
			}else if(deviceType == DEVICE_8010H){
				addr = woperation.bytetoint(recvValue);
			}else{
				return;
			}
			setRecv_data(0);
			page_erase(addr, leng, mgattCharacteristic, bleclass);
		
			try {
         		while (((read_count = input.read(inputBuffer, 0, packageSize)) != -1)) {
//    					woperation.send_data(OTA_CMD_WRITE_DATA, addr, inputBuffer,
//    							read_count, mgattCharacteristic, bleclass);
					//20201116 修改
					while ( ! woperation.send_data(OTA_CMD_WRITE_DATA, addr, inputBuffer,
							read_count, mgattCharacteristic, bleclass)) {
						try{
							Thread.sleep(50);
							Log.d("TAG","send_data error");
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
					delay_num =0;
					while(!writeStatus) {
						delay_num++;
						if(delay_num % 8000 == 0) {
							Log.d("TAG","send_data once more");
							woperation.send_data(OTA_CMD_WRITE_DATA, addr, inputBuffer, read_count, mgattCharacteristic, bleclass);
						}
					}
					writeStatus = false;
					//for(delay_num = 0;delay_num < 10000;delay_num++);
					addr += read_count;
					lastReadCount = read_count;
					send_data_count += read_count;
					//System.out.println("times" + i + " " + read_count);
					i ++;
					if(writePrecent != (int)(((float)send_data_count / leng) * 100)) {
						writePrecent = (int) (((float) send_data_count / leng) * 100);
						mHandler.sendEmptyMessage(1);
					}

					while (getRecv_data() != 1){
						if(checkDisconnect()){
							return;
						}
					}
					setRecv_data(0);
    		    }
         		while(woperation.bytetoint(recvValue) != (addr - lastReadCount)){
         			if(checkDisconnect()){
						return;
					}	
         		}

         		//woperation.send_data(OTA_CMD_REBOOT, 0, null, 0, mgattCharacteristic, bleclass);
         		//crc 校验
				woperation.send_data_long(OTA_CMD_REBOOT, fileCRC, null, leng, mgattCharacteristic, bleclass);
				mHandler.sendEmptyMessage(0);
			} catch (IOException e) {
				e.printStackTrace();
			}

		} else {
			Toast.makeText(getApplicationContext(), "请选择要发送的文件!",
					Toast.LENGTH_LONG).show();
		}
	}
	private  int Crc32CalByByte(int oldcrc, byte[] ptr,int offset, int len)
	{
		int crc = oldcrc;
		int i = offset;
		while(len-- != 0)
		{
			int high = crc/256; //取CRC高8位
			crc <<= 8;
			crc ^= crc_ta_8[(high^ptr[i])&0xff];
			crc &= 0xFFFFFFFF;
			i++;
		}
		return crc&0xFFFFFFFF;
	}
	public int getCRC32new(String fp) throws IOException {

		File file = new File(fp);// 成文件路径中获取文件
		FileInputStream isfile = null;
		try {
			isfile = new FileInputStream(file);

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		long leng = file.length();
		int read_count = 0;
		InputStream input = new BufferedInputStream(isfile);
		byte[] inputBuffer = new byte[256];
		int crcInit =0;
		int couts = 0;
		while (((read_count = input.read(inputBuffer, 0, 256)) != -1)) {
			if(couts != 0) {
				crcInit = Crc32CalByByte(crcInit, inputBuffer, 0, read_count);
			}
			couts++;
			//Log.d("TAG CRC", "count: "+ couts+ "  "+ " read: "+ read_count+ "  "+ Integer.toHexString(crcInit));
		}
		return  crcInit;
	}

	/* CRC 字节余式表 */
	private final int crc_ta_8[]= new int[]{
			0x00000000, 0x77073096, 0xee0e612c, 0x990951ba,
			0x076dc419, 0x706af48f, 0xe963a535, 0x9e6495a3, 0x0edb8832,
			0x79dcb8a4, 0xe0d5e91e, 0x97d2d988, 0x09b64c2b, 0x7eb17cbd,
			0xe7b82d07, 0x90bf1d91, 0x1db71064, 0x6ab020f2, 0xf3b97148,
			0x84be41de, 0x1adad47d, 0x6ddde4eb, 0xf4d4b551, 0x83d385c7,
			0x136c9856, 0x646ba8c0, 0xfd62f97a, 0x8a65c9ec, 0x14015c4f,
			0x63066cd9, 0xfa0f3d63, 0x8d080df5, 0x3b6e20c8, 0x4c69105e,
			0xd56041e4, 0xa2677172, 0x3c03e4d1, 0x4b04d447, 0xd20d85fd,
			0xa50ab56b, 0x35b5a8fa, 0x42b2986c, 0xdbbbc9d6, 0xacbcf940,
			0x32d86ce3, 0x45df5c75, 0xdcd60dcf, 0xabd13d59, 0x26d930ac,
			0x51de003a, 0xc8d75180, 0xbfd06116, 0x21b4f4b5, 0x56b3c423,
			0xcfba9599, 0xb8bda50f, 0x2802b89e, 0x5f058808, 0xc60cd9b2,
			0xb10be924, 0x2f6f7c87, 0x58684c11, 0xc1611dab, 0xb6662d3d,
			0x76dc4190, 0x01db7106, 0x98d220bc, 0xefd5102a, 0x71b18589,
			0x06b6b51f, 0x9fbfe4a5, 0xe8b8d433, 0x7807c9a2, 0x0f00f934,
			0x9609a88e, 0xe10e9818, 0x7f6a0dbb, 0x086d3d2d, 0x91646c97,
			0xe6635c01, 0x6b6b51f4, 0x1c6c6162, 0x856530d8, 0xf262004e,
			0x6c0695ed, 0x1b01a57b, 0x8208f4c1, 0xf50fc457, 0x65b0d9c6,
			0x12b7e950, 0x8bbeb8ea, 0xfcb9887c, 0x62dd1ddf, 0x15da2d49,
			0x8cd37cf3, 0xfbd44c65, 0x4db26158, 0x3ab551ce, 0xa3bc0074,
			0xd4bb30e2, 0x4adfa541, 0x3dd895d7, 0xa4d1c46d, 0xd3d6f4fb,
			0x4369e96a, 0x346ed9fc, 0xad678846, 0xda60b8d0, 0x44042d73,
			0x33031de5, 0xaa0a4c5f, 0xdd0d7cc9, 0x5005713c, 0x270241aa,
			0xbe0b1010, 0xc90c2086, 0x5768b525, 0x206f85b3, 0xb966d409,
			0xce61e49f, 0x5edef90e, 0x29d9c998, 0xb0d09822, 0xc7d7a8b4,
			0x59b33d17, 0x2eb40d81, 0xb7bd5c3b, 0xc0ba6cad, 0xedb88320,
			0x9abfb3b6, 0x03b6e20c, 0x74b1d29a, 0xead54739, 0x9dd277af,
			0x04db2615, 0x73dc1683, 0xe3630b12, 0x94643b84, 0x0d6d6a3e,
			0x7a6a5aa8, 0xe40ecf0b, 0x9309ff9d, 0x0a00ae27, 0x7d079eb1,
			0xf00f9344, 0x8708a3d2, 0x1e01f268, 0x6906c2fe, 0xf762575d,
			0x806567cb, 0x196c3671, 0x6e6b06e7, 0xfed41b76, 0x89d32be0,
			0x10da7a5a, 0x67dd4acc, 0xf9b9df6f, 0x8ebeeff9, 0x17b7be43,
			0x60b08ed5, 0xd6d6a3e8, 0xa1d1937e, 0x38d8c2c4, 0x4fdff252,
			0xd1bb67f1, 0xa6bc5767, 0x3fb506dd, 0x48b2364b, 0xd80d2bda,
			0xaf0a1b4c, 0x36034af6, 0x41047a60, 0xdf60efc3, 0xa867df55,
			0x316e8eef, 0x4669be79, 0xcb61b38c, 0xbc66831a, 0x256fd2a0,
			0x5268e236, 0xcc0c7795, 0xbb0b4703, 0x220216b9, 0x5505262f,
			0xc5ba3bbe, 0xb2bd0b28, 0x2bb45a92, 0x5cb36a04, 0xc2d7ffa7,
			0xb5d0cf31, 0x2cd99e8b, 0x5bdeae1d, 0x9b64c2b0, 0xec63f226,
			0x756aa39c, 0x026d930a, 0x9c0906a9, 0xeb0e363f, 0x72076785,
			0x05005713, 0x95bf4a82, 0xe2b87a14, 0x7bb12bae, 0x0cb61b38,
			0x92d28e9b, 0xe5d5be0d, 0x7cdcefb7, 0x0bdbdf21, 0x86d3d2d4,
			0xf1d4e242, 0x68ddb3f8, 0x1fda836e, 0x81be16cd, 0xf6b9265b,
			0x6fb077e1, 0x18b74777, 0x88085ae6, 0xff0f6a70, 0x66063bca,
			0x11010b5c, 0x8f659eff, 0xf862ae69, 0x616bffd3, 0x166ccf45,
			0xa00ae278, 0xd70dd2ee, 0x4e048354, 0x3903b3c2, 0xa7672661,
			0xd06016f7, 0x4969474d, 0x3e6e77db, 0xaed16a4a, 0xd9d65adc,
			0x40df0b66, 0x37d83bf0, 0xa9bcae53, 0xdebb9ec5, 0x47b2cf7f,
			0x30b5ffe9, 0xbdbdf21c, 0xcabac28a, 0x53b39330, 0x24b4a3a6,
			0xbad03605, 0xcdd70693, 0x54de5729, 0x23d967bf, 0xb3667a2e,
			0xc4614ab8, 0x5d681b02, 0x2a6f2b94, 0xb40bbe37, 0xc30c8ea1,
			0x5a05df1b, 0x2d02ef8d,
	};
	void sendOtaWriteMEM()
	{
		byte[] bf = {0x2d, (byte)0xe9, (byte)0xf0, 0x47, 0x06, 0x46, 0x0c, 0x46, 0x17, 0x46, 0x44, (byte)0xf6, (byte)0x99, 0x08,
				0x20, 0x20, (byte)0xff, (byte)0xf7, 0x1f, (byte)0xfb, (byte)0x81, 0x46, 0x00, (byte)0xf0, (byte)0xdd, (byte)0xf9, 0x00, (byte)0xf0, 0x1e, (byte)0xf8,
				0x00, 0x20, 0x00, (byte)0xf0, 0x35, (byte)0xfe, 0x0e, (byte)0xe0, (byte)0xb6, (byte)0xf5, (byte)0x80, 0x1f, 0x0b, (byte)0xd2, (byte)0xc6, (byte)0xf5,
				(byte)0x80, 0x15, (byte)0xa5, 0x42, 0x00, (byte)0xd3, 0x25, 0x46, 0x3a, 0x46, 0x29, 0x46, 0x30, 0x46, (byte)0xc0, 0x47,
				0x2e, 0x44, 0x2f, 0x44, 0x64, 0x1b, 0x00, 0x2c, (byte)0xee, (byte)0xd1, 0x01, 0x20, (byte)0xff, (byte)0xf7, 0x4e, (byte)0xfa,
				0x48, 0x46, (byte)0xff, (byte)0xf7, 0x04, (byte)0xfb, (byte)0xbd, (byte)0xe8, (byte)0xf0, (byte)0x87, 0x00, 0x00, 0x4e, 0x20, 0x03, 0x49,
				0x08, 0x70, 0x45, 0x20, 0x08, 0x70, 0x57, 0x20, 0x08, 0x70, 0x70, 0x47, 0x00, (byte)0x80, 0x05, 0x50, };
		woperation.send_data(OTA_CMD_WRITE_MEM, 0x20001676, bf, bf.length, mgattCharacteristic, bleclass);
	}

	void sendOtaWriteflashErase()
	{
//		byte[] bf2 = {0x70, (byte)0xb5, 0x0d, 0x46, 0x06, 0x46, 0x44, (byte)0xf2, 0x75, 0x74, 0x72, (byte)0xb6, 0x00, (byte)0xf0,
//				(byte)0x9e, (byte)0xf9, 0x00, 0x20, 0x00, (byte)0xf0, (byte)0xf8, (byte)0xfd, 0x29, 0x46, 0x30, 0x46, (byte)0xa0, 0x47, 0x00, 0x20,
//				0x00, (byte)0xf0, (byte)0xbc, (byte)0xfd, 0x01, 0x20, (byte)0xff, (byte)0xf7, 0x1d, (byte)0xfa, 0x62, (byte)0xb6, 0x70, (byte)0xbd, 0x00, 0x00, };
		byte[] bf2 = {0x70, (byte)0xb5, 0x0d, 0x46, 0x06, 0x46, 0x44, (byte)0xf2, 0x75, 0x74, 0x72, (byte)0xb6, 0x00, (byte)0xf0,
				(byte)0x9e, (byte)0xf9, 0x01, 0x20, 0x00, (byte)0xf0, (byte)0xf8, (byte)0xfd, 0x29, 0x46, 0x30, 0x46, (byte)0xa0, 0x47, 0x01, 0x20,
				(byte)0xff, (byte)0xf7, 0x20, (byte)0xfa, 0x62, (byte)0xb6, 0x70, (byte)0xbd};
		woperation.send_data(OTA_CMD_WRITE_MEM, 0x200016fe, bf2, bf2.length, mgattCharacteristic, bleclass);
	}

}
