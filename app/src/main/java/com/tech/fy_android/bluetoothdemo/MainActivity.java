package com.tech.fy_android.bluetoothdemo;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import com.tech.fy_android.bluetoothdemo.adapter.ConnectableListBaseAdapter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    final String SPP_UUID = "00001101-0000-1000-8000-00805F9B34FB";
    UUID uuid = UUID.fromString(SPP_UUID);


    private static final int MY_PERMISSION_REQUEST_CONSTANT = 1;
    private Button mOpen1;
    private BluetoothAdapter mDefaultAdapter;
    private Toast mToast;
    private Button search;

    //蓝牙已经开启可用
    private boolean mBTAvialuable;
    private ListView lv;
    private List<BluetoothDevice> mBondedDevicesAll;

    private ConnectableListBaseAdapter mAdapter;
    private SearchBTAction mSearchBTAction;
    private Button enablevisable;
    private Button startServer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        lv = (ListView) findViewById(R.id.lv);
        mOpen1 = (Button) findViewById(R.id.open);
        search = (Button) findViewById(R.id.search);
        enablevisable = (Button) findViewById(R.id.enablevisable);
        startServer = (Button) findViewById(R.id.startServer);

        mBondedDevicesAll = new ArrayList<>();
        mAdapter = new ConnectableListBaseAdapter(this, mBondedDevicesAll);
        lv.setAdapter(mAdapter);

        initRuntime();

        initListner();
    }


    class MyArrayAdapter extends ArrayAdapter<BluetoothDevice> {

        public MyArrayAdapter(Context context, int resource, List<BluetoothDevice> objects) {
            super(context, resource, objects);
        }


        @NonNull
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            return super.getView(position, convertView, parent);
        }
    }


    private void initRuntime() {
        if (Build.VERSION.SDK_INT >= 6.0) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, MY_PERMISSION_REQUEST_CONSTANT);
        }
    }


    /**
     * 注册搜索广播
     */
    @Override
    protected void onStart() {

        if (mSearchBTAction == null) {
            mSearchBTAction = new SearchBTAction();
        }

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothDevice.ACTION_FOUND);
        intentFilter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        intentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        intentFilter.addAction(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED);
        intentFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);

        registerReceiver(mSearchBTAction, intentFilter);


        super.onStart();
    }


    @Override
    protected void onDestroy() {

        if (mDefaultAdapter != null && mDefaultAdapter.isDiscovering()) {      //如果适配器正在扫描,记得取消
            mDefaultAdapter.cancelDiscovery();
        }

        if (mSearchBTAction != null) {
            unregisterReceiver(mSearchBTAction);
        }

        super.onDestroy();
    }

    private void initListner() {
        mOpen1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openBluetooth();
            }
        });

        search.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                searchNewDevices();
            }
        });

        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                showToast("第" + position + "位的设备要连接了");
                paringBT(position);
            }
        });

        enablevisable.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setEnableVisual();
            }
        });

        startServer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startServer();
            }
        });

    }

    /**
     * 开启服务器
     */
    private void startServer() {
        try {
            BluetoothServerSocket bluetoothServerSocket = mDefaultAdapter.listenUsingRfcommWithServiceRecord(mDefaultAdapter.getName(), uuid);
            bluetoothServerSocket.accept();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 使设备可见
     */
    private void setEnableVisual() {
        Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
        startActivity(discoverableIntent);
    }


    /**
     * 开启蓝牙
     */
    private void openBluetooth() {
        mDefaultAdapter = BluetoothAdapter.getDefaultAdapter();

        if (mDefaultAdapter == null) {
            showToast("没有可用的蓝牙设备");
        } else {
            showToast("有可用的蓝牙设备");

            if (!mDefaultAdapter.isEnabled()) {       //蓝牙如果没有打开
                Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);     //开启蓝牙
                // 设置蓝牙可见性，最多300秒
                intent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
                startActivityForResult(intent, RESULT_FIRST_USER);
            } else {
                showToast("蓝牙已经开启");
                findConnectDevice();
            }


        }
    }


    private void showToast(String str) {
        if (mToast == null) {
            mToast = Toast.makeText(this, "", Toast.LENGTH_SHORT);
        }

        mToast.setText(str);
        mToast.show();
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        switch (resultCode) {
            case RESULT_OK:
                showToast("开启蓝牙成功");
                findConnectDevice();

                break;
            case RESULT_CANCELED:
                showToast("开启蓝牙失败");
                break;
        }

        super.onActivityResult(requestCode, resultCode, data);
    }


    /**
     * 搜索可以匹配的蓝牙设备
     */
    private void findConnectDevice() {
        mBTAvialuable = true;
        //查看是否有已经配对的设备
        showHadAlradeyConncectDevices();
    }

    /**
     * 搜索新的设备
     */
    private void searchNewDevices() {
        if (!mBTAvialuable || mDefaultAdapter == null) {
            return;
        }

        Log.d("花开", "开始搜索新的设备");
        mDefaultAdapter.startDiscovery();
    }


    /**
     * 获取已经绑定的蓝牙设备
     */
    private void showHadAlradeyConncectDevices() {

        Log.d("花开", "开始获取已绑定的蓝牙设备");


        Set<BluetoothDevice> bondedDevices = mDefaultAdapter.getBondedDevices();


        mBondedDevicesAll.clear();

        if (bondedDevices.size() == 0) {
            Log.d("花开", "没有已经配对的蓝牙设备");
            return;
        }

        for (BluetoothDevice de : bondedDevices) {
            mBondedDevicesAll.add(de);
            String tmp = de.getName() + "   |   " + de.getAddress() + "   |   " + de.getBondState();
            Log.d("花开 |已找到的配对的蓝牙设备有", tmp);
//            mBondedDevicesAll.add(de);
        }

//        mAdapter.notifyDataSetChanged();

    }


    /**
     * 搜索的广播接收者
     */
    class SearchBTAction extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            Log.d("花开", action);

            switch (action) {
                case BluetoothDevice.ACTION_FOUND: //找到了设备
                    BluetoothDevice extra = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                    String tmp = extra.getName() + "   |   " + extra.getAddress() + "   |   " + extra.getBondState();
                    Log.d("花开", "搜索到--" + tmp);

                    if (extra.getBondState() == BluetoothDevice.BOND_BONDED || compareIsSame(extra)) {        //已经是绑定的的了
                        Log.d("花开", "已经存在");
                    } else {
                        Log.d("花开", "不存在,正加入列表");
                        mBondedDevicesAll.add(extra);
                        mAdapter.notifyDataSetChanged();
                    }

                    break;

                case BluetoothAdapter.ACTION_DISCOVERY_FINISHED:    //搜索结束了
                    showToast("搜索结束了");
                    Log.d("花开", "搜索结束了");
                    break;
            }

        }
    }

    /**
     * 设备是否相同
     *
     * @param extra
     * @return
     */
    private boolean compareIsSame(BluetoothDevice extra) {
        for (BluetoothDevice dev : mBondedDevicesAll) {
            if (dev.getAddress().equals(extra.getAddress())) {
                return true;
            }
        }

        return false;
    }


    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {

        switch (requestCode) {
            case MY_PERMISSION_REQUEST_CONSTANT: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    //permission granted!
                }
                return;
            }
        }
    }


    private void paringBT(int index) {

        if (mDefaultAdapter.isDiscovering()) {
            mDefaultAdapter.cancelDiscovery();
        }

        //得到该设备的绑定状态
        BluetoothDevice bluetoothDevice = mBondedDevicesAll.get(index);
        int bondState = bluetoothDevice.getBondState();

        switch (bondState) {
            case BluetoothDevice.BOND_NONE:
                showToast("未绑定");

                // 配对
                try {
                    Method createBondMethod = BluetoothDevice.class.getMethod("createBond");
                    createBondMethod.invoke(bluetoothDevice);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;

            case BluetoothDevice.BOND_BONDING:
                showToast("绑定中");
                break;

            case BluetoothDevice.BOND_BONDED:
                showToast("已绑定");

                connect(bluetoothDevice);

                break;


        }
    }


    //客户端链接
    private void connect(BluetoothDevice btDev) {
        try {
            BluetoothSocket btSocket = btDev.createRfcommSocketToServiceRecord(uuid);
            Log.d("BlueToothTestActivity", "开始连接...");

            run(btSocket);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void run(final BluetoothSocket btSocket) {

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    btSocket.connect();

                    InputStream inputStream = btSocket.getInputStream();

                    String string = ConvertToString(inputStream);

                    Log.d("花开", "收到的字符串是" + string);

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();


    }


    public static String ConvertToString(InputStream inputStream) {
        InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
        BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
        StringBuilder result = new StringBuilder();
        String line = null;
        try {
            while (!(line = bufferedReader.readLine()).equals("")) {
                result.append(line + "\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                inputStreamReader.close();
                inputStream.close();
                bufferedReader.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return result.toString();
    }


}
