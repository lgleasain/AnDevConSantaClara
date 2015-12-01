package com.polyglotprogramminginc.andevcon;

import android.app.Fragment;
import android.app.FragmentTransaction;
import android.bluetooth.BluetoothDevice;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.mbientlab.bletoolbox.scanner.BleScannerFragment;
import com.mbientlab.metawear.MetaWearBleService;
import com.mbientlab.metawear.MetaWearBoard;

import java.util.Locale;
import java.util.UUID;

public class MainActivity extends AppCompatActivity implements ServiceConnection, BleScannerFragment.ScannerCommunicationBus,
        DeviceConfirmationFragment.DeviceConfirmCallback{
    private MetaWearBleService.LocalBinder mwBinder;
    private ScannerFragment mwScannerFragment;
    private ThermistorFragment thermistorFragment;
    private MetaWearBoard mwBoard;
    private Menu menu;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        getApplicationContext().bindService(new Intent(this, MetaWearBleService.class),
                this, Context.BIND_AUTO_CREATE);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        this.menu = menu;
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_connect) {
                     if (mwScannerFragment == null) {
                        mwScannerFragment = new ScannerFragment();
                        mwScannerFragment.show(getFragmentManager(), "metawear_scanner_fragment");
                    } else {
                        mwScannerFragment.show(getFragmentManager(), "metawear_scanner_fragment");
                    }
            return true;
        }else if(id == R.id.action_temperature){
            thermistorFragment = new ThermistorFragment();
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.main_content, thermistorFragment).commit();
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        ///< Get a reference to the MetaWear service from the binder
        mwBinder = (MetaWearBleService.LocalBinder) service;
        Log.i("Main Activity", "Service Connected");
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        Log.i("Main Activity", "Service Disconnected");
    }

    /**
     * callbacks for Bluetooth device scan
     */
    @Override
    public void onDeviceSelected(BluetoothDevice device) {
        connect(device);
        Fragment metawearBlescannerPopup = getFragmentManager().findFragmentById(R.id.metawear_blescanner_popup_fragment);
        FragmentTransaction fragmentTransaction = getFragmentManager().beginTransaction();
        fragmentTransaction.remove(metawearBlescannerPopup);
        fragmentTransaction.commit();
        mwScannerFragment.dismiss();
        Toast.makeText(this, String.format(Locale.US, "Selected device: %s",
                device.getAddress()), Toast.LENGTH_LONG).show();
    }

    @Override
    public UUID[] getFilterServiceUuids() {
        ///< Only return MetaWear boards in the scan
        return new UUID[]{UUID.fromString("326a9000-85cb-9195-d9dd-464cfbbae75a")};
    }

    @Override
    public long getScanDuration() {
        ///< Scan for 10000ms (10 seconds)
        return 10000;
    }


    /**
     * Connection callbacks
     */
    private MetaWearBoard.ConnectionStateHandler connectionStateHandler = new MetaWearBoard.ConnectionStateHandler() {
        @Override
        public void connected() {
            Log.i("Metawear Controller", "Device Connected");
            runOnUiThread(new Runnable() {
                              @Override
                              public void run() {
                                  DeviceConfirmationFragment deviceConfirmationFragment = new DeviceConfirmationFragment();
                                  deviceConfirmationFragment.flashDeviceLight(mwBoard, getFragmentManager());

                                  Toast.makeText(getApplicationContext(), R.string.toast_connected, Toast.LENGTH_SHORT).show();
                              }
                          }
            );

        }

        @Override
        public void disconnected() {
            Log.i("Metawear Controler", "Device Disconnected");
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getApplicationContext(), R.string.toast_disconnected, Toast.LENGTH_SHORT).show();
                }
            });

        }
    };

    private void connect(BluetoothDevice metaWearDevice){
        mwBoard = mwBinder.getMetaWearBoard(metaWearDevice);
        mwBoard.setConnectionStateHandler(connectionStateHandler);
        mwBoard.connect();
    }

    public void pairDevice() {
    }

    public void dontPairDevice() {
        new CountDownTimer(2000, 1000) {

            public void onTick(long millisUntilFinished) {
            }

            public void onFinish() {
                mwBoard.disconnect();
            }
        }.start();

    }



}
