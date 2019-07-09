package privatecom.nejc.smsviabluetooth;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import privatecom.nejc.smsviabluetooth.domain.MessageConstants;

public class MainActivity extends AppCompatActivity {
    ListView pairedDevicesListView;
    BluetoothAdapter bluetoothAdapter;
    private final static int REQUEST_ENABLE_BT = 1;
    private final static String NAME = "123e4567-e89b-12d3-a456-556642440000";
    private final static UUID MY_UUID = UUID.fromString(NAME);
    private final static String TAG = "smsviabluetooth";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        pairedDevicesListView = findViewById(R.id.pairedDevicesList);
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        fillListView(null);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.my_menu, menu);
        return true;
    }

    public void fillListView(MenuItem item) {
        // todo: just for test (maybe other class?)
        List<String> devices = Arrays.asList("Device1", "Device2", "Device3");
        // List<String> devices = getConnectedDevices();
        ArrayAdapter<String> arrayAdapter =
                new ArrayAdapter<>(this, android.R.layout.simple_list_item_multiple_choice, devices);
        pairedDevicesListView.setAdapter(arrayAdapter);
    }

    private List<String> getConnectedDevices() {
        List<String> connectedDevices = new ArrayList<>();
        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();

        for (BluetoothDevice device : pairedDevices) {
//            device.getUuids()[0].getUuid();
            // todo: get what actually needed
            System.out.println(device.getName() + " " + device.getAddress());
            connectedDevices.add(device.getName());
        }
        return connectedDevices;
    }

    @SuppressLint("DefaultLocale")
    public void showSelected(View view) {
        getSelectedItem();
    }

    private String getSelectedItem() {
        String selectedDevice = null;
        SparseBooleanArray checkedItemPositions = pairedDevicesListView.getCheckedItemPositions();
        for (int i = 0; i < checkedItemPositions.size(); i++) {
            if (checkedItemPositions.valueAt(i)) {
                int deviceId = checkedItemPositions.keyAt(i);
                String toPrint = String.format("Item %d is selected (%s)", deviceId, pairedDevicesListView.getItemAtPosition(deviceId));
                selectedDevice = pairedDevicesListView.getItemAtPosition(deviceId).toString();
                System.out.println(toPrint);
                break;
            }
        }
        return selectedDevice;
    }

    public void startListening(View view) {
        String selectedItem = getSelectedItem();
        if (bluetoothAdapter == null) {
            System.out.println("Bluetooth is not supported by device!");
            Toast.makeText(MainActivity.this, "Bluetooth is not supported by device!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }

        Thread thread = new Thread() {
            public void run() {
                try {
                    BluetoothServerSocket socket = bluetoothAdapter.listenUsingRfcommWithServiceRecord("SMS_via_blue", MY_UUID);
                    Handler blueHandler = new BlueHandler();
                    MyBluetoothService myBluetoothService = new MyBluetoothService(blueHandler);
                    BluetoothSocket deviceSocket = socket.accept();
                    myBluetoothService.startListening(deviceSocket);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };
        thread.run();
    }

    public void sendSMS(String phoneNumber, String message) {
//      todo: validate phone number
        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.SEND_SMS)
                != PackageManager.PERMISSION_GRANTED) {
            // Permission is not granted
            // Ask for permision
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.SEND_SMS}, 2);
            sendSMS(phoneNumber, message);
        } else {
            // Permission has already been granted
            try {
                SmsManager smsManager = SmsManager.getDefault();
                ArrayList<String> divideMessage = smsManager.divideMessage(message);
                smsManager.sendMultipartTextMessage(phoneNumber, null, divideMessage, null, null);
            } catch (Exception e) {
                if (e.toString().contains(Manifest.permission.READ_PHONE_STATE) && ContextCompat
                        .checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_PHONE_STATE}, 3);
                    sendSMS(phoneNumber, message);
                }
            }
        }
    }

    private class BlueHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MessageConstants.MESSAGE_READ:
                    sendSMS("+38670701664", new String((byte[]) msg.obj));
//                    sendSMS("+38641692228", new String((byte[]) msg.obj));
                    break;
                case MessageConstants.MESSAGE_WRITE:
                    break;
                case MessageConstants.MESSAGE_TOAST:
                    break;
                default:
                    break;
            }
        }
    }
}
