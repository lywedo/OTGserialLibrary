package com.usbserial;

import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;

import com.usbserial.driver.UsbSerialDriver;
import com.usbserial.driver.UsbSerialPort;
import com.usbserial.driver.UsbSerialProber;
import com.usbserial.util.SerialInputOutputManager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by Lam on 2018/3/6.
 */

public class FTRusbSerialHelper {
    static UsbManager mUsbManager;
    static IfindDriversListerner mFindDriversListener;
    static ISerialReceriveListener mSerialReceiveListener;
    public static UsbSerialPort port;
    public static SerialInputOutputManager mSerialIoManager;
    final static ExecutorService mExecutor = Executors.newSingleThreadExecutor();
    private static final int MESSAGE_REFRESH = 101;
    private static final long REFRESH_TIMEOUT_MILLIS = 5000;

    private static final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_REFRESH:
                    FTRusbSerialHelper.beginFindDriversTask();
                    mHandler.sendEmptyMessageDelayed(MESSAGE_REFRESH, REFRESH_TIMEOUT_MILLIS);
                    break;
                default:
                    super.handleMessage(msg);
                    break;
            }
        }

    };

    public static void init(UsbManager manager){
        mUsbManager = manager;
    }

    public static void setUsbSerialPort(UsbSerialPort serialPortport){
        port = serialPortport;
    }
    public static void setFindDriversListener(IfindDriversListerner listener){
        mFindDriversListener = listener;
    }
    public static void setSerialReceiveListener(ISerialReceriveListener listener){
        mSerialReceiveListener = listener;
    }

    static SerialInputOutputManager.Listener mSerialInputOutputLinsener = new SerialInputOutputManager.Listener() {
        @Override
        public void onNewData(byte[] data) {
            mSerialReceiveListener.onNewData(data);
        }

        @Override
        public void onRunError(Exception e) {
            mSerialReceiveListener.onRunError(e);
        }
    };

    public static void beginFindDrivers(){
        mHandler.sendEmptyMessage(MESSAGE_REFRESH);
    }
    public static void stopFindDrivers(){
        mHandler.removeMessages(MESSAGE_REFRESH);
    }

    private static void beginFindDriversTask(){
        new AsyncTask<Void, Void, List<UsbSerialPort>>() {
            @Override
            protected List<UsbSerialPort> doInBackground(Void... params) {
                SystemClock.sleep(1000);

                final List<UsbSerialDriver> drivers =
                        UsbSerialProber.getDefaultProber().findAllDrivers(mUsbManager);

                final List<UsbSerialPort> result = new ArrayList<UsbSerialPort>();
                for (final UsbSerialDriver driver : drivers) {
                    final List<UsbSerialPort> ports = driver.getPorts();
                    result.addAll(ports);
                }

                return result;
            }

            @Override
            protected void onPostExecute(List<UsbSerialPort> result) {
                mFindDriversListener.driversCallback(result);
            }

        }.execute((Void) null);
    }

    public static void beginConnect(int bitRate, ISerialConnectStatusListener listener){
        if (port == null) {
            listener.noDevicesCallback();
        } else {

            UsbDeviceConnection connection = mUsbManager.openDevice(port.getDriver().getDevice());
            if (connection == null) {
                listener.openDevicesErrCallback();
                return;
            }

            try {
                port.open(connection);
                port.setParameters(bitRate, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE);
                listener.showStatus("CD  - Carrier Detect", port.getCD());
                listener.showStatus("CTS - Clear To Send", port.getCTS());
                listener.showStatus("DSR - Data Set Ready", port.getDSR());
                listener.showStatus("DTR - Data Terminal Ready", port.getDTR());
                listener.showStatus("RI  - Ring Indicator", port.getRI());
                listener.showStatus("RTS - Request To Send", port.getRTS());



            } catch (IOException e) {
                listener.openDevicesErrCallback();
                try {
                    port.close();
                } catch (IOException e2) {
                    // Ignore.
                }
                port = null;
                return;
            }
            listener.clossPort();
        }
        stopIoManager();
        startIoManager();
    }

    private static void stopIoManager() {
        if (mSerialIoManager != null) {
            mSerialIoManager.stop();
            mSerialIoManager = null;
        }
    }

    private static void startIoManager() {
        if (FTRusbSerialHelper.port != null) {
            mSerialIoManager = new SerialInputOutputManager(FTRusbSerialHelper.port, mSerialInputOutputLinsener);
            mExecutor.submit(mSerialIoManager);
        }
    }

    public static void close(){
        stopIoManager();
        if (port != null) {
            try {
                port.close();
            } catch (IOException e) {
                // Ignore.
            }
            port = null;
        }
    }

    public interface IfindDriversListerner{
        void driversCallback(List<UsbSerialPort> result);
    }

    public interface ISerialConnectStatusListener{
        void noDevicesCallback();
        void openDevicesErrCallback();
        void showStatus(String lable, boolean value);
        void clossPort();
    }

    public interface ISerialReceriveListener{
        void onRunError(Exception e);
        void onNewData(final byte[] data);
    }

}
