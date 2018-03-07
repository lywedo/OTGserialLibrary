package com.lam.otg;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;

import com.usbserial.FTRusbSerialHelper;
import com.usbserial.util.HexDump;


/**
 * Created by Lam on 2018/3/1.
 */

public class SerialConsoleActivity extends AppCompatActivity {
    private final String TAG = SerialConsoleActivity.class.getSimpleName();

    private ScrollView mScrollView;
    private TextView mDumpTextView;
    private TextView mTitleTextView;
    private CheckBox chkDTR;
    private CheckBox chkRTS;
    private EditText mEditText;
    private Button mSendButton;

    FTRusbSerialHelper.ISerialConnectStatusListener mConnectStatusListener = new FTRusbSerialHelper.ISerialConnectStatusListener() {
        @Override
        public void noDevicesCallback() {
            mTitleTextView.setText("No serial device.");
        }

        @Override
        public void openDevicesErrCallback() {
            mTitleTextView.setText("Opening device failed");
        }

        @Override
        public void showStatus(String lable, boolean value) {
            showSerialStatus(mDumpTextView, lable, value);
        }

        @Override
        public void clossPort() {
            mTitleTextView.setText("Serial device: " + FTRusbSerialHelper.port.getClass().getSimpleName());
        }

    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.serial_console);
        mTitleTextView = (TextView) findViewById(R.id.demoTitle);
        mDumpTextView = (TextView) findViewById(R.id.consoleText);
        mScrollView = (ScrollView) findViewById(R.id.demoScroller);
        chkDTR = (CheckBox) findViewById(R.id.checkBoxDTR);
        chkRTS = (CheckBox) findViewById(R.id.checkBoxRTS);

        chkDTR.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
//                try {
//                    sPort.setDTR(isChecked);
//                }catch (IOException x){}
            }
        });

        chkRTS.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
//                try {
//                    sPort.setRTS(isChecked);
//                }catch (IOException x){}
            }
        });
        mEditText = (EditText) findViewById(R.id.et);
        mSendButton = (Button) findViewById(R.id.btn);
        mSendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                FTRusbSerialHelper.mSerialIoManager.writeAsync(mEditText.getText().toString().getBytes().length==0?new byte[]{0x01,0x02,0x03}:mEditText.getText().toString().getBytes());
            }
        });
        FTRusbSerialHelper.setSerialReceiveListener(new FTRusbSerialHelper.ISerialReceriveListener() {
            @Override
            public void onRunError(Exception e) {
                Log.d(TAG, "Runner stopped.");
            }

            @Override
            public void onNewData(final byte[] data) {
                SerialConsoleActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            SerialConsoleActivity.this.updateReceivedData(data);
                        }
                    });
            }
        });

    }
    private void updateReceivedData(byte[] data) {
        final String message = "Read " + data.length + " bytes: \n"
                + new String(data) + "\n\n";

        mDumpTextView.append(message);
        Log.d("message", HexDump.dumpHexString(data));
        mScrollView.smoothScrollTo(0, mDumpTextView.getBottom());
    }

    static void show(Context context) {
        final Intent intent = new Intent(context, SerialConsoleActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_NO_HISTORY);
        context.startActivity(intent);
    }


    @Override
    protected void onPause() {
        super.onPause();
        FTRusbSerialHelper.close();
        finish();
    }

    void showSerialStatus(TextView theTextView, String theLabel, boolean theValue){
        String msg = theLabel + ": " + (theValue ? "enabled" : "disabled") + "\n";
        theTextView.append(msg);
    }

    @Override
    protected void onResume() {
        super.onResume();
        FTRusbSerialHelper.beginConnect(115200, mConnectStatusListener);
    }
}
