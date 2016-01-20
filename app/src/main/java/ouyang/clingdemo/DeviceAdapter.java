package ouyang.clingdemo;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.List;

import com.miui.upnp.UpnpDevice;

public class DeviceAdapter extends ArrayAdapter<UpnpDevice> {

    private static final String TAG = "DeviceAdapter";
    private List<UpnpDevice> devices;
    private int resourceId;
    private LayoutInflater inflater;

    public DeviceAdapter(Context context, int resource, List<UpnpDevice> objects) {
        super(context, resource, objects);

        this.resourceId = resource;
        this.devices = objects;
        this.inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        UpnpDevice device = getItem(position);

        if (convertView == null) {
            convertView = inflater.inflate(resourceId, null);
        }

        TextView name = (TextView) convertView.findViewById(R.id.device_name);
        name.setText(device.getFriendlyName() + "(" + device.getDeviceIp() + ")");

        return convertView;
    }
}
