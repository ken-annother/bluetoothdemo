package com.tech.fy_android.bluetoothdemo.adapter;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.tech.fy_android.bluetoothdemo.R;

import java.util.List;

/**
 * @描述: 可连接列表的适配器
 * @项目名: ConnectableListBaseAdapter
 * @包名: com.tech.fy_android.bluetoothdemo.adapter
 * @类名: ConnectableListBaseAdapter
 * @作者: soongkun
 * @创建时间: 16-9-30 下午4:05
 */
public class ConnectableListBaseAdapter extends BaseAdapter {

    private final Context mContext;
    private final List<BluetoothDevice> mBondedDevicesAll;

    public ConnectableListBaseAdapter(Context context, List<BluetoothDevice> bondedDevicesAll) {
        this.mContext = context;
        this.mBondedDevicesAll = bondedDevicesAll;
    }

    @Override
    public int getCount() {
        return mBondedDevicesAll == null ? 0 : mBondedDevicesAll.size();
    }

    @Override
    public BluetoothDevice getItem(int position) {
        return mBondedDevicesAll.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        if (convertView == null) {
            convertView = View.inflate(mContext, R.layout.item_list, null);
        }

        BluetoothDevice item = getItem(position);
        String tmp = item.getName() + "   |   " + item.getAddress() + "   |   " + item.getBondState();
        ((TextView) convertView).setText(tmp);

        return convertView;
    }
}