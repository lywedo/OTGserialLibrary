package com.lam.otg;

import android.content.Context;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.TwoLineListItem;

import com.usbserial.FTRusbSerialHelper;
import com.usbserial.driver.UsbSerialDriver;
import com.usbserial.driver.UsbSerialPort;
import com.usbserial.driver.UsbSerialProber;
import com.usbserial.util.HexDump;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private final String TAG = MainActivity.class.getSimpleName();
    private UsbManager mUsbManager;
    private ListView mListView;
    private TextView mProgressBarTitle;
    private List<UsbSerialPort> mEntries = new ArrayList<UsbSerialPort>();
    private ArrayAdapter<UsbSerialPort> mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mUsbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
        FTRusbSerialHelper.init(mUsbManager);
        mListView = (ListView) findViewById(R.id.deviceList);
        mProgressBarTitle = (TextView) findViewById(R.id.progressBarTitle);
        mAdapter = new ArrayAdapter<UsbSerialPort>(this,
                android.R.layout.simple_expandable_list_item_2, mEntries) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                final TwoLineListItem row;
                if (convertView == null){
                    final LayoutInflater inflater =
                            (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                    row = (TwoLineListItem) inflater.inflate(android.R.layout.simple_list_item_2, null);
                } else {
                    row = (TwoLineListItem) convertView;
                }

                final UsbSerialPort port = mEntries.get(position);
                final UsbSerialDriver driver = port.getDriver();
                final UsbDevice device = driver.getDevice();

                final String title = String.format("Vendor %s Product %s",
                        HexDump.toHexString((short) device.getVendorId()),
                        HexDump.toHexString((short) device.getProductId()));
                row.getText1().setText(title);

                final String subtitle = driver.getClass().getSimpleName();
                row.getText2().setText(subtitle);

                return row;
            }

        };
        mListView.setAdapter(mAdapter);
        mListView.setOnItemClickListener(new ListView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Log.d(TAG, "Pressed item " + position);
                if (position >= mEntries.size()) {
                    Log.w(TAG, "Illegal position.");
                    return;
                }

                final UsbSerialPort port = mEntries.get(position);
                FTRusbSerialHelper.setUsbSerialPort(port);
                showConsoleActivity();
            }
        });
        FTRusbSerialHelper.setFindDriversListener(mFindDriversListener);
    }
    FTRusbSerialHelper.IfindDriversListerner mFindDriversListener = new FTRusbSerialHelper.IfindDriversListerner() {
        @Override
        public void driversCallback(List<UsbSerialPort> result) {
            mEntries.clear();
            mEntries.addAll(result);
            mAdapter.notifyDataSetChanged();
            mProgressBarTitle.setText(
                    String.format("%s device(s) found",Integer.valueOf(mEntries.size())));
            Log.d("RESULT", "Done refreshing, " + mEntries.size() + " entries found.");
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        FTRusbSerialHelper.beginFindDrivers();
    }

    @Override
    protected void onPause() {
        super.onPause();
        FTRusbSerialHelper.stopFindDrivers();
    }
    private void showConsoleActivity() {
        SerialConsoleActivity.show(this);
    }
}
