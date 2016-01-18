package ouyang.clingdemo;

import android.content.Intent;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import java.util.ArrayList;

import ouyang.clingdemo.upnp.UpnpDevice;
import ouyang.clingdemo.upnp.UpnpDeviceListener;
import ouyang.clingdemo.upnp.UpnpManager;

public class MainActivity extends AppCompatActivity implements UpnpDeviceListener {

    private static final String TAG = "MainActivity";
    private DeviceAdapter deviceAdapter;
    private Handler handler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        UpnpManager.getInstance().initialize();

        handler = new Handler();
        deviceAdapter = new DeviceAdapter(this, R.layout.item_device, new ArrayList<UpnpDevice>());
        ListView deviceListView = (ListView) findViewById(R.id.deviceList);
        deviceListView.setAdapter(deviceAdapter);
        deviceListView.setOnItemClickListener(onItemClickListener);
    }

    public void onButtonStart(View view) {
        UpnpManager.getControlPoint().startScan(this);
    }

    public void onButtonStop(View view) {
        UpnpManager.getControlPoint().stopScan();
        deviceAdapter.clear();
    }

    @Override
    public void onDeviceFound(final UpnpDevice device) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                deviceAdapter.add(device);
            }
        });
    }

    @Override
    public void onDeviceLost(final UpnpDevice device) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                deviceAdapter.remove(device);
            }
        });
    }

    private AdapterView.OnItemClickListener onItemClickListener = new AdapterView.OnItemClickListener() {

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            UpnpDevice device = deviceAdapter.getItem(position);
            if (device != null) {
                Intent intent;

                if (device.getDeviceType().equals("MediaRenderer")) {
                    intent = new Intent(view.getContext(), DmrCpActivity.class);
                    intent.putExtra(DmrCpActivity.EXTRA_DEVICE_NAME, device.getFriendlyName());
                    intent.putExtra(DmrCpActivity.EXTRA_DEVICE_ID, device.getDeviceId());
                    startActivity(intent);
                }
            }
        }
    };
}
