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

package com.example.bluetooth.le;

import android.annotation.SuppressLint;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;

import java.lang.reflect.Method;
import java.util.List;
import java.util.UUID;

import com.example.bluetooth.le.activity.BluetoothservicesListview;

import static android.bluetooth.BluetoothGatt.GATT_SUCCESS;

/**
 * Service for managing connection and data communication with a GATT server
 * hosted on a given Bluetooth LE device.
 */
public class BluetoothLeClass {
	private final static String TAG = BluetoothLeClass.class.getSimpleName();
	private BluetoothManager mBluetoothManager;
	private BluetoothAdapter mBluetoothAdapter;
	private String mBluetoothDeviceAddress;
	private BluetoothGatt mBluetoothGatt;
	public byte[] temp = null;
	private RecvFile rf;
	public boolean isDisconnected = true;
	public int mtuSize  = 247;
	private boolean gotoServiceDiscover = false;
	private int discoverCount = 5;
	public boolean mtuChange = false;
	public interface OnConnectListener {
		public void onConnect(BluetoothGatt gatt);
	}

	public interface OnConnectingListener {
		public void onConnecting(BluetoothGatt gatt);
	}

	public interface OnDisconnectListener {
		public void onDisconnect(BluetoothGatt gatt);
	}

	public interface OnServiceDiscoverListener {
		public void onServiceDiscover(BluetoothGatt gatt,int status);
	}
    public interface OnRecvDataListerner{
    	public void OnCharacteristicRecv(BluetoothGatt gatt,
				BluetoothGattCharacteristic characteristic);
    	
    }
	public interface OnDataAvailableListener {
		public void onCharacteristicRead(BluetoothGatt gatt,
				BluetoothGattCharacteristic characteristic, int status);

		public void onCharacteristicWrite(BluetoothGatt gatt,
				BluetoothGattCharacteristic characteristic);
	}
	public interface OnWriteDataListener{
		public void OnCharacteristicWrite(BluetoothGatt gatt,
				BluetoothGattCharacteristic characteristic, int status);
		
	}
	private OnConnectListener mOnConnectListener;
	private OnDisconnectListener mOnDisconnectListener;
	private OnServiceDiscoverListener mOnServiceDiscoverListener;
	private OnDataAvailableListener mOnDataAvailableListener;
	private OnRecvDataListerner mOnRecvDataListerner;
	private OnWriteDataListener mOnWriteDataListerner;
	private Context mContext;
	private OnConnectingListener mOnConnectingListener;

	public void setOnConnectListener(OnConnectListener l) {
		mOnConnectListener = l;
	}

	public void setOnConnectingListener(OnConnectingListener l) {
		mOnConnectingListener = l;

	}
	public void setOnWriteDataListener(OnWriteDataListener l){
		mOnWriteDataListerner = l;
		
	}
	public void setOnDisconnectListener(OnDisconnectListener l) {
		mOnDisconnectListener = l;
	}

	public void setOnServiceDiscoverListener(OnServiceDiscoverListener l) {
		mOnServiceDiscoverListener = l;
	}

	public void setOnDataAvailableListener(OnDataAvailableListener l) {
		mOnDataAvailableListener = l;
	}
    public void setOnRecvDataListener(OnRecvDataListerner l){
    	mOnRecvDataListerner = l;
    	
    }
	public BluetoothLeClass(Context c) {
		rf = new RecvFile();
		mContext = c;
	}

	void updatastata(String action) {
		Intent intent = new Intent(action);
		mContext.sendBroadcast(intent);

	}
	int j = 0;
	public int i = 0;
	public boolean requestMtu(int size) {
		if (mBluetoothGatt != null && Build.VERSION.SDK_INT >=Build.VERSION_CODES.LOLLIPOP) {
			return mBluetoothGatt.requestMtu(size);
		}
		return false;
	}
	// Implements callback methods for GATT events that the app cares about. For
	// example,
	// connection change and services discovered.
	private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
		@Override
		public void onConnectionStateChange(BluetoothGatt gatt, int status,
				int newState) {
			Log.i(TAG, "status: "+status +" newstate: "+newState );
			//if(status != 133) {
				if (newState == BluetoothProfile.STATE_CONNECTED) {
					//BluetoothGatt.GATT_SUCCESS
					Log.i(TAG, "Connected to GATT server.");
					if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
						gatt.requestConnectionPriority(BluetoothGatt.CONNECTION_PRIORITY_HIGH);
					}
					//mBluetoothGatt.readRemoteRssi();

					if (mOnConnectListener != null)
						mOnConnectListener.onConnect(gatt);
					isDisconnected = false;


				} else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
					if (mOnDisconnectListener != null)
						mOnDisconnectListener.onDisconnect(gatt);
					isDisconnected = true;
					gotoServiceDiscover= false;
					//discoverCount = 5;
					close();
					System.out.println("Disconnected from GATT server.");
					updatastata("state");
				} else if (newState == BluetoothProfile.STATE_CONNECTING) {
					if (mOnConnectingListener != null) {
						mOnConnectingListener.onConnecting(gatt);
					}

				}
//			}else {
//				//close();
//				disconnect();
//				isDisconnected = true;
//				gotoServiceDiscover= false;
//				//discoverCount = 5;
//			}
		}

		@Override
		public void onServicesDiscovered(BluetoothGatt gatt, int status) {
			if (status == GATT_SUCCESS && mOnServiceDiscoverListener != null) {
				gotoServiceDiscover = true;
				mOnServiceDiscoverListener.onServiceDiscover(gatt,status );

				Log.d(TAG, "onServicesDiscovered : " + status);
			} else {
				Log.w(TAG, "discoverServices fail try again: " + status + gatt.discoverServices());
			}
		}

		@Override
		public void onCharacteristicRead(BluetoothGatt gatt,
				BluetoothGattCharacteristic characteristic, int status) {
			if (mOnDataAvailableListener != null)
				mOnDataAvailableListener.onCharacteristicRead(gatt,
						characteristic, status);
		}
	    public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
	    	if(mOnWriteDataListerner != null){
	    		mOnWriteDataListerner.OnCharacteristicWrite(gatt, characteristic, status);
	    	}
	    };
		@Override
		public void onCharacteristicChanged(BluetoothGatt gatt,
				BluetoothGattCharacteristic characteristic) {
			// System.out.println("onCharRead "
			// +Utils.bytesToHexString(characteristic.getValue()));
			// temp = characteristic.getValue();
			// i += temp.length;
			// System.out.println("i -->" + i++ );
			// System.out.println("temp -->" + Utils.bytesToHexString(temp));
			//updatastata("recvdata");
			if(mOnRecvDataListerner != null){	
				mOnRecvDataListerner.OnCharacteristicRecv(gatt, characteristic);
			}
			 if (mOnDataAvailableListener!=null)
			 mOnDataAvailableListener.onCharacteristicWrite(gatt,
			characteristic);
			// rf.savefile(temp, temp.length);
		}
		@Override
		public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
		super.onMtuChanged(gatt, mtu, status);
		    System.out.println("onMtuChanged "+mtu + " " + status);
			if (GATT_SUCCESS == status) {
				mtuSize = mtu;
				System.out.println("BleService"+"onMtuChanged success MTU = " + mtu);
			}else {
				mtuSize = 235;
			    Log.d("BleService", "onMtuChanged fail ");
			}
			mtuChange = true;
		}
	};

	/**
	 * Initializes a reference to the local Bluetooth adapter.
	 * 
	 * @return Return true if the initialization is successful.
	 */
	public boolean initialize() {
		// For API level 18 and above, get a reference to BluetoothAdapter
		// through
		// BluetoothManager.
		if (mBluetoothManager == null) {
			mBluetoothManager = (BluetoothManager) mContext
					.getSystemService(Context.BLUETOOTH_SERVICE);
			if (mBluetoothManager == null) {
				Log.e(TAG, "Unable to initialize BluetoothManager.");
				return false;
			}
		}

		mBluetoothAdapter = mBluetoothManager.getAdapter();
		if (mBluetoothAdapter == null) {
			Log.e(TAG, "Unable to obtain a BluetoothAdapter.");
			return false;
		}

		return true;
	}

	/**
	 * Connects to the GATT server hosted on the Bluetooth LE device.
	 * 
	 * @param address
	 *            The device address of the destination device.
	 * 
	 * @return Return true if the connection is initiated successfully. The
	 *         connection result is reported asynchronously through the
	 *         {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
	 *         callback.
	 */
	public boolean connect(final String address) {
		//System.out.println("addr " + address);
		if (mBluetoothAdapter == null || address == null) {
			Log.w(TAG,
					"BluetoothAdapter not initialized or unspecified address.");
			return false;
		}
		// Previously connected device. Try to reconnect.
//		if (mBluetoothDeviceAddress == address && mBluetoothGatt != null) {
//			//System.out.println("Trying to use an existing mBluetoothGatt for connection.");
//			if (mBluetoothGatt.connect()) {
//				return true;
//			} else {
//				return false;
//			}
//			
//		}
		//disconnect();
		final BluetoothDevice device = mBluetoothAdapter
				.getRemoteDevice(address);
		if (device == null) {
			Log.w(TAG, "Device not found.  Unable to connect.");
			return false;
		}
		// We want to directly connect to the device, so we are setting the
		// autoConnect
		// parameter to false.

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			mBluetoothGatt = device.connectGatt(mContext, false, mGattCallback,BluetoothDevice.TRANSPORT_LE); // 连接蓝牙设备
		}else {
			mBluetoothGatt = device.connectGatt(mContext, false, mGattCallback);
		}
		Log.d(TAG, "Trying to create a new connection.");
		mBluetoothDeviceAddress = address;
		if (mBluetoothGatt != null) {
			if (mBluetoothGatt.connect()) {
				return true;
			} else {
				return false;
			}
		}
		return true;
	}

	/**
	 * Disconnects an existing connection or cancel a pending connection. The
	 * disconnection result is reported asynchronously through the
	 * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
	 * callback.
	 */
	public void disconnect() {
		if (mBluetoothAdapter == null || mBluetoothGatt == null) {
			Log.d(TAG, "BluetoothAdapter not initialized");
			return;
		}
		//System.out.println("disconnect");
		mBluetoothGatt.disconnect();
		//mBluetoothGatt.close();
	}

	/**
	 * After using a given BLE device, the app must call this method to ensure
	 * resources are released properly.
	 */
	public void close() {
		if (mBluetoothGatt == null) {
			return;
		}
		mBluetoothGatt.close();
		mBluetoothGatt = null;
	}

	/**
	 * Request a read on a given {@code BluetoothGattCharacteristic}. The read
	 * result is reported asynchronously through the
	 * {@code BluetoothGattCallback#onCharacteristicRead(android.bluetooth.BluetoothGatt, android.bluetooth.BluetoothGattCharacteristic, int)}
	 * callback.
	 * 
	 * @param characteristic
	 *            The characteristic to read from.
	 */
	public void readCharacteristic(BluetoothGattCharacteristic characteristic) {
		if (mBluetoothAdapter == null || mBluetoothGatt == null) {
			Log.w(TAG, "BluetoothAdapter not initialized");
			return;
		}
		mBluetoothGatt.readCharacteristic(characteristic);
	}

	/**
	 * Enables or disables notification on a give characteristic.
	 * 
	 * @param characteristic
	 *            Characteristic to act on.
	 * @param enabled
	 *            If true, enable notification. False otherwise.
	 */
	public void setCharacteristicNotification(
			BluetoothGattCharacteristic characteristic, boolean enabled) {
		if (mBluetoothAdapter == null || mBluetoothGatt == null) {
			Log.w(TAG, "BluetoothAdapter not initialized");
			return;
		}
		mBluetoothGatt.setCharacteristicNotification(characteristic, enabled);
	}

	public boolean writeCharacteristic(BluetoothGattCharacteristic characteristic) {
		return mBluetoothGatt.writeCharacteristic(characteristic);
	}
	public void discoverService(){
		mBluetoothGatt.discoverServices();
	}
	public void writeDescriptor(BluetoothGattDescriptor gattDescriptor) {
		mBluetoothGatt.writeDescriptor(gattDescriptor);
	}
	/**
	 * Retrieves a list of supported GATT services on the connected device. This
	 * should be invoked only after {@code BluetoothGatt#discoverServices()}
	 * completes successfully.
	 * 
	 * @return A {@code List} of supported services.
	 */
	public List<BluetoothGattService> getSupportedGattServices() {
		if (mBluetoothGatt == null)
			return null;

		return mBluetoothGatt.getServices();
	}
}
