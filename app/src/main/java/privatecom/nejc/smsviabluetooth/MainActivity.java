package privatecom.nejc.smsviabluetooth;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
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

public class MainActivity extends AppCompatActivity {
    ListView pairedDevicesListView;
    BluetoothAdapter bluetoothAdapter;
    private final static int REQUEST_ENABLE_BT = 1;
    private final static String NAME = "bluetooth_sms";
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

    }

    private class AcceptThread extends Thread {
        private final BluetoothServerSocket mmServerSocket;

        public AcceptThread() {
            // Use a temporary object that is later assigned to mmServerSocket
            // because mmServerSocket is final.
            BluetoothServerSocket tmp = null;
            try {
                // MY_UUID is the app's UUID string, also used by the client code.
                tmp = bluetoothAdapter.listenUsingRfcommWithServiceRecord(NAME, MY_UUID);
            } catch (IOException e) {
                Log.e(TAG, "Socket's listen() method failed", e);
            }
            mmServerSocket = tmp;
        }

        public void run() {
            BluetoothSocket socket = null;
            // Keep listening until exception occurs or a socket is returned.
            while (true) {
                try {
                    socket = mmServerSocket.accept();
                } catch (IOException e) {
                    Log.e(TAG, "Socket's accept() method failed", e);
                    break;
                }

                if (socket != null) {
                    // A connection was accepted. Perform work associated with
                    // the connection in a separate thread.
                    manageMyConnectedSocket(socket);
                    try {
                        mmServerSocket.close();
                    } catch (IOException e) {
                        Log.e(TAG, "Could not close the connect socket", e);
                    }
                    break;
                }
            }
        }

        // Closes the connect socket and causes the thread to finish.
        public void cancel() {
            try {
                mmServerSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "Could not close the connect socket", e);
            }
        }

        private void manageMyConnectedSocket(BluetoothSocket socket) {
            try (InputStream inputStream = socket.getInputStream()) {
//                todo: implement
                int read = inputStream.read();
            } catch (IOException e) {
                Log.e(TAG, "Could not close the connect socket", e);

            }
        }
    }

}
