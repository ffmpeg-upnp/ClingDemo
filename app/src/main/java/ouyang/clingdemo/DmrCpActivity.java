package ouyang.clingdemo;

import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import ouyang.clingdemo.upnp.UpnpException;
import ouyang.clingdemo.upnp.UpnpManager;

public class DmrCpActivity extends AppCompatActivity {

    public static final String EXTRA_DEVICE_NAME = "deviceName";
    public static final String EXTRA_DEVICE_ID = "deviceId";

    private static final String TAG = "DmrCpActivity";
    private String deviceName;
    private String deviceId;
    private Handler handler = new Handler();
    private TextView log;
//    private int rate = 0;
//    private int volume = 10;
//    private long progress = 0;
//    private long duration = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dmr_cp);

        deviceName = this.getIntent().getStringExtra(EXTRA_DEVICE_NAME);

        deviceId = this.getIntent().getStringExtra(EXTRA_DEVICE_ID);
        if (deviceId == null) {
            Log.e(TAG, "device is null");
            finish();
        }

        initTitle();
        initLog();
    }

    private void initTitle() {
        this.setTitle(deviceName);
    }

    private void initLog() {
        log = (TextView) findViewById(R.id.log);
        log.setMovementMethod(new ScrollingMovementMethod());
    }

    private void addInfo(String info) {
        String newInfo;

        CharSequence oldInfo = log.getText();
        if (oldInfo.length() > 1024 * 10) {
            newInfo = String.format("%s\r\n", info);
        } else {
            newInfo = String.format("%s%s\r\n", oldInfo, info);
        }

        log.setText(newInfo);
    }

    private void showLog(String info) {
        Log.d(TAG, info);

        final String newLog;

        CharSequence oldLog = log.getText();
        if (oldLog.length() > 1024 * 10) {
            newLog = String.format("%s\r\n", info);
        } else {
            newLog = String.format("%s%s\r\n", oldLog, info);
        }

        this.handler.post(new Runnable() {
            @Override
            public void run() {
                log.setText(newLog);
            }
        });
    }

    public void onButtonConnect(View button) {
//        UpnpManager.getControlPoint().;
    }

    public void onButtonDisconnect(View button) {
    }

    public void onButtonPlay(View button) {
        String url = "http://www.baidu.com/img/bd_logo1.png";

        try {
            UpnpManager.getControlPoint().play(deviceId, url);
        } catch (UpnpException e) {
            e.printStackTrace();
        }
    }

    public void onButtonNext(View button) {
    }

    public void onButtonStop(View button) {
    }

    public void onButtonPlayPause(View button) {
    }

    public void onButtonSetVolume(View button) {
    }

    public void onButtonSetProgress(View button) {
    }

    public void onButtonGetProgress(View button) {
    }

    public void onButtonGetDuration(View button) {
    }

//    /**
//     * RemotePlayerEventHandler
//     */
//
//    @Override
//    public void onConnecting() {
//        showLog("onConnecting");
//    }
//
//    @Override
//    public void onConnected() {
//        showLog("onConnected");
//    }
//
//    @Override
//    public void onConnectFailed() {
//        showLog("onConnectFailed");
//    }
//
//    @Override
//    public void onDisconnecting() {
//        showLog("onDisconnecting");
//    }
//
//    @Override
//    public void onDisconnected() {
//        showLog("onDisconnected");
//    }
//
//    @Override
//    public void onLoading() {
//        showLog("onLoading");
//    }
//
//    @Override
//    public void onPlaying() {
//        showLog("onPlaying");
//    }
//
//    @Override
//    public void onPaused() {
//        showLog("onPaused");
//    }
//
//    @Override
//    public void onStopped() {
//        showLog("onStopped");
//    }
//
//    @Override
//    public void onVolumeChanged(int volume) {
//        showLog("onVolumeChanged: " + volume);
//    }
//
//    @Override
//    public void onMute(boolean isMute) {
//        showLog("onMute: " + isMute);
//    }
}
