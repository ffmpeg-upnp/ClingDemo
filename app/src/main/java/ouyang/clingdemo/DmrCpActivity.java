package ouyang.clingdemo;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;

import java.util.Timer;
import java.util.TimerTask;

import ouyang.clingdemo.fileserver.FileServer;
import com.miui.upnp.MediaRenderer;
import com.miui.upnp.UpnpAVControlPoint;
import com.miui.upnp.UpnpException;
import com.miui.upnp.UpnpManager;

public class DmrCpActivity extends AppCompatActivity implements MediaRenderer.EventListener {

    public static final String EXTRA_DEVICE_NAME = "deviceName";
    public static final String EXTRA_DEVICE_ID = "deviceId";

    private static final String TAG = "DmrCpActivity";
    private FileServer fileServer;
    private String deviceName;
    private String deviceId;
    private int rate = 0;
    private int volume = 10;
    private int progress = 0;
    private int duration = 0;

    private int photoIndex = 0;
    private String photos[] = {
            "http://www.baidu.com/img/bd_logo1.png",
            "/sdcard/DCIM/wps/0.png",
            "/sdcard/DCIM/wps/1.png",
            "/sdcard/DCIM/wps/2.png",
            "/sdcard/DCIM/wps/3.png",
            "/sdcard/DCIM/wps/4.png",
            "/sdcard/DCIM/wps/5.png",
            "/sdcard/DCIM/wps/6.png",
            "/sdcard/DCIM/wps/7.png",
            "/sdcard/DCIM/wps/8.png",
            "/sdcard/DCIM/wps/9.png",
            "/sdcard/DCIM/wps/10.png",
            "/sdcard/DCIM/wps/11.png",
            "/sdcard/DCIM/wps/12.png",
            "/sdcard/DCIM/wps/13.png",
            "/sdcard/DCIM/wps/14.png",
            "/sdcard/DCIM/wps/15.png",
            "/sdcard/DCIM/wps/16.png",
            "/sdcard/DCIM/wps/17.png",
            "/sdcard/DCIM/wps/18.png",
            "/sdcard/DCIM/wps/19.png",
            "/sdcard/DCIM/wps/20.png",
            "/sdcard/DCIM/wps/21.png",
            "/sdcard/DCIM/wps/22.png",
            "/sdcard/DCIM/wps/23.png",
            "/sdcard/DCIM/wps/24.png",
    };

    private int musicIndex = 0;
    private String musics[] = {
            "/sdcard/Music/demo0.mp3",
            "/sdcard/Music/demo1.mp3",
            "/sdcard/Music/demo2.mp3",
            "/sdcard/Music/demo3.mp3",
            "/sdcard/Music/北京北京_汪峰.mp3",
            "/sdcard/Music/存在_汪峰.mp3",
            "/sdcard/Music/怒放的生命_汪峰.mp3",
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dmr_cp);

        fileServer = new FileServer();
        fileServer.start();

        deviceName = this.getIntent().getStringExtra(EXTRA_DEVICE_NAME);

        deviceId = this.getIntent().getStringExtra(EXTRA_DEVICE_ID);
        if (deviceId == null) {
            Log.e(TAG, "device is null");
            finish();
        }

        initTitle();
    }

    @Override
    protected void onDestroy() {
        fileServer.stop();

        super.onDestroy();
    }

    private void initTitle() {
        this.setTitle(deviceName);
    }

    public void onButtonConnect(View button) {
        try {
            UpnpManager.getControlPoint().connect(deviceId, this);
        } catch (UpnpException e) {
            e.printStackTrace();
        }
    }

    public void onButtonDisconnect(View button) {
        try {
            UpnpManager.getControlPoint().disconnect();
        } catch (UpnpException e) {
            e.printStackTrace();
        }
    }

    private void doPlay() {
        try {
            String ip = UpnpManager.getControlPoint().getDeviceIp(deviceId);
            String url = fileServer.getHttpUrl(musics[musicIndex], ip);
            String title = "";
            // String extra = " {\"sid\":\"21190\",\"song\":\"随它吧\",\"artist\":\"姚贝娜\"}";
            String extra = " {\"sid\":\"21190\",\"song\":\"随它吧\",\"artist\":\"刘德华\"}";
            UpnpManager.getControlPoint().play(url, title, extra);

            musicIndex = (musicIndex + 1) % musics.length;
        } catch (UpnpException e) {
            e.printStackTrace();
        }
    }

    public void onButtonPlay(View button) {
        doPlay();
    }

    public void onButtonStop(View button) {
        playing = false;

        try {
            UpnpManager.getControlPoint().stop();
        } catch (UpnpException e) {
            e.printStackTrace();
        }
    }

    public void onButtonPlayPause(View button) {
        try {
            rate = (rate == 1) ? 0 : 1;
            UpnpManager.getControlPoint().setPlaybackRate(rate);
        } catch (UpnpException e) {
            e.printStackTrace();
        }
    }

    public void onButtonSetVolume(View button) {
        try {
            UpnpManager.getControlPoint().setVolume(volume);
        } catch (UpnpException e) {
            e.printStackTrace();
        }
    }

    public void onButtonGetVolume(View button) {
        try {
            UpnpManager.getControlPoint().getVolume(new UpnpAVControlPoint.GetVolumeHandler() {
                @Override
                public void onSucceed(int volume) {
                    Log.d(TAG, String.format("%d", volume));
                }

                @Override
                public void onFailed(String m) {

                }
            });
        } catch (UpnpException e) {
            e.printStackTrace();
        }
    }

    public void onButtonSetProgress(View button) {
        try {
            progress = 100;
            UpnpManager.getControlPoint().setPlaybackProgress(progress);
        } catch (UpnpException e) {
            e.printStackTrace();
        }
    }

    public void onButtonGetPositionInfo(View button) {
        getDurationLoop();
    }

    public void onButtonShowPhoto(View button) {
        try {
            String url = getNextPhoto();
            UpnpManager.getControlPoint().play(url);
        } catch (UpnpException e) {
            e.printStackTrace();
        }
    }

    public void onButtonStartSlideshow(View button) {
        int duration = 2000;
        boolean isRecycle = false;

        startSlideTask(duration, isRecycle);
    }

    public void onButtonStopSlideshow(View button) {
        stopSlideTask();
    }

    @Override
    public void onVolumeChanged(int newVolume) {
        Log.e(TAG, "onVolumeChanged: " + newVolume);
    }

    @Override
    public void onLoading() {
        Log.e(TAG, "onLoading");
    }

    @Override
    public void onPlaying() {
        Log.e(TAG, "onPlaying");
        playing = true;
        getDurationLoop();
    }

    @Override
    public void onNoMediaPresent_Stopped() {
        Log.e(TAG, "onNoMediaPresent_Stopped");
        playing = false;

        doPlay();
    }

    @Override
    public void onStopped() {
        Log.e(TAG, "onStopped");
        playing = false;
    }

    @Override
    public void onPaused() {
        Log.e(TAG, "onPaused");
    }

    private String getNextPhoto() {
        String url = null;

        try {
            String ip = UpnpManager.getControlPoint().getDeviceIp(deviceId);
            String nextPhoto = photos[photoIndex];
            Log.e(TAG, "next photo: " + nextPhoto);
            url = fileServer.getHttpUrl(nextPhoto, ip);
            photoIndex = (photoIndex + 1) % photos.length;
        } catch (UpnpException e) {
            e.printStackTrace();
        }

        return url;
    }

    private Timer mSlideTimer = null;
    private Object mSlideTimerLock = new Object();
    private static final int MIN_SLIDESHOW_TASK_INTERNAL = 2 * 1000;

    private boolean startSlideTask(int duration, boolean isRecyle) {
        synchronized (mSlideTimerLock) {
            if (mSlideTimer == null) {
                mSlideTimer = new Timer();

                if (duration < MIN_SLIDESHOW_TASK_INTERNAL) {
                    duration = MIN_SLIDESHOW_TASK_INTERNAL;
                }

                mSlideTimer.schedule(new SlideTask(isRecyle), 0, duration);
                return true;
            }
        }

        return false;
    }

    public boolean stopSlideTask() {
        synchronized (mSlideTimerLock) {
            if (mSlideTimer != null) {
                mSlideTimer.cancel();
                mSlideTimer = null;
                return true;
            }
        }

        return false;
    }

    private class SlideTask extends TimerTask {
        private boolean isRecycle = false;

        public SlideTask(boolean isRecycle) {
            this.isRecycle = isRecycle;
        }

        @Override
        public void run() {
            try {
                if (! UpnpManager.getControlPoint().isConnected()) {
                    stopSlideTask();
                    return;
                }
            } catch (UpnpException e) {
                e.printStackTrace();
            }

            String url = getNextPhoto();
            if (url == null) {
                stopSlideTask();
                return;
            }

            try {
                UpnpManager.getControlPoint().play(url);
            } catch (UpnpException e) {
                e.printStackTrace();
            }
        }
    }

    private boolean playing = false;

    private void getDurationLoop() {
        try {
            UpnpManager.getControlPoint().getPositionInfo(new UpnpAVControlPoint.GetPositionInfoHandler() {
                @Override
                public void onSucceed(long elapsed, long duration) {
                    Log.d(TAG, String.format("%d,%d", elapsed, duration));

                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    if (playing) {
                        getDurationLoop();
                    }
                }

                @Override
                public void onFailed(String m) {
                }
            });
        } catch (UpnpException e) {
            e.printStackTrace();
        }
    }
}