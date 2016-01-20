package ouyang.clingdemo;

import android.content.Intent;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import java.util.ArrayList;

import com.miui.upnp.UpnpDevice;
import com.miui.upnp.UpnpDeviceListener;
import com.miui.upnp.UpnpException;
import com.miui.upnp.UpnpManager;

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
        try {
            UpnpManager.getControlPoint().startScan(this);
        } catch (UpnpException e) {
            e.printStackTrace();
        }
    }

    public void onButtonStop(View view) {
        try {
            UpnpManager.getControlPoint().stopScan();
        } catch (UpnpException e) {
            e.printStackTrace();
        }
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
