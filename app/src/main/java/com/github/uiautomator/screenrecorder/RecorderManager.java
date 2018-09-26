package com.github.uiautomator.screenrecorder;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.media.MediaCodecInfo;
import android.media.projection.MediaProjection;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.StrictMode;
import android.support.annotation.RequiresApi;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import static android.Manifest.permission.RECORD_AUDIO;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;
import static com.github.uiautomator.screenrecorder.ScreenRecorder.VIDEO_AVC;
import static com.github.uiautomator.screenrecorder.ScreenRecorder.AUDIO_AAC;

public class RecorderManager {

    private Activity mContext;

    public ScreenRecorder mRecorder;
    private Notifications mNotifications;

    static final String ACTION_STOP = "com.github.uiautomator.screenrecorder.action.STOP";

    public RecorderManager(Activity mContext){
        this.mContext = mContext;
        mNotifications = new Notifications(mContext.getApplicationContext());
    }

    public void stopRecorder() {
        mNotifications.clear();
        if (mRecorder != null) {
            mRecorder.quit();
        }
        mRecorder = null;
        try {
            mContext.unregisterReceiver(mStopActionReceiver);
        } catch (Exception e) {

        }
    }

    private boolean hasPermissions() {
        PackageManager pm = mContext.getPackageManager();
        String packageName = mContext.getPackageName();
        int granted = pm.checkPermission(RECORD_AUDIO, packageName) | pm.checkPermission(WRITE_EXTERNAL_STORAGE, packageName);
        return granted == PackageManager.PERMISSION_GRANTED;
    }

    private VideoEncodeConfig createVideoConfig() {
        final String codec = "OMX.hisi.video.encoder.avc";
        if (codec == null) {
            // no selected codec ??
            return null;
        }
        // video size
        int[] selectedWithHeight = {720, 480};
        boolean isLandscape = false;
        int width = selectedWithHeight[isLandscape ? 0 : 1];
        int height = selectedWithHeight[isLandscape ? 1 : 0];
        int framerate = 15;
        int iframe = 1;
        int bitrate = 800000;
        MediaCodecInfo.CodecProfileLevel profileLevel = null;
        return new VideoEncodeConfig(width, height, bitrate,
                framerate, iframe, codec, VIDEO_AVC, profileLevel);
    }

    private AudioEncodeConfig createAudioConfig() {
        String codec = "OMX.google.aac.encoder";
        if (codec == null) {
            return null;
        }
        int bitrate = 80000;
        int samplerate = 44100;
        int channelCount = 1;
        int profile = 1;

        return new AudioEncodeConfig(codec, AUDIO_AAC, bitrate, samplerate, channelCount, profile);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public void recorderResult(MediaProjection mediaProjection){
        if (mediaProjection == null) {
            Log.e("@@", "media projection is null");
            return;
        }
        VideoEncodeConfig video = createVideoConfig();
        AudioEncodeConfig audio = createAudioConfig(); // audio can be null
        if (video == null) {
            Toast.makeText(mContext, "Create ScreenRecorder failure", Toast.LENGTH_SHORT).show();
            mediaProjection.stop();
            return;
        }
        File dir = getSavingDir();
        if (!dir.exists() && !dir.mkdirs()) {
            cancelRecorder();
            return;
        }
        SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US);
        final File file = new File(dir, "Screen-" + format.format(new Date())
                + "-" + video.width + "x" + video.height + ".mp4");
        Log.d("@@", "Create recorder with :" + video + " \n " + audio + "\n " + file);
        mRecorder = newRecorder(mediaProjection, video, audio, file);
        if (hasPermissions()) {
            startRecorder();
        } else {
            cancelRecorder();
        }
    }

    private static File getSavingDir() {
        return new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES),
                "ScreenCaptures");
    }

    private void cancelRecorder() {
        if (mRecorder == null) return;
        Toast.makeText(mContext, "Permission denied! Screen recorder is cancel", Toast.LENGTH_SHORT).show();
        stopRecorder();
    }

    private void startRecorder() {
        if (mRecorder == null) return;
        mRecorder.start();
        mContext.registerReceiver(mStopActionReceiver, new IntentFilter(ACTION_STOP));
        mContext.moveTaskToBack(true);
    }

    private BroadcastReceiver mStopActionReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            File file = new File(mRecorder.getSavedPath());
            if (ACTION_STOP.equals(intent.getAction())) {
                stopRecorder();
            }
            Toast.makeText(context, "Recorder stopped!\n Saved file " + file, Toast.LENGTH_LONG).show();
            StrictMode.VmPolicy vmPolicy = StrictMode.getVmPolicy();
            try {
                // disable detecting FileUriExposure on public file
                StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder().build());
                viewResult(file);
            } finally {
                StrictMode.setVmPolicy(vmPolicy);
            }
        }

        private void viewResult(File file) {
            Intent view = new Intent(Intent.ACTION_VIEW);
            view.addCategory(Intent.CATEGORY_DEFAULT);
            view.setDataAndType(Uri.fromFile(file), VIDEO_AVC);
            view.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            try {
                mContext.startActivity(view);
            } catch (ActivityNotFoundException e) {
                // no activity can open this video
            }
        }
    };

    private ScreenRecorder newRecorder(MediaProjection mediaProjection, VideoEncodeConfig video,
                                       AudioEncodeConfig audio, final File output) {
        ScreenRecorder r = new ScreenRecorder(video, audio,
                1, mediaProjection, output.getAbsolutePath());
        r.setCallback(new ScreenRecorder.Callback() {
            long startTime = 0;

            @Override
            public void onStop(Throwable error) {
                mContext.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        stopRecorder();
                    }
                });
                if (error != null) {
                    Toast.makeText(mContext, "Recorder error ! See logcat for more details", Toast.LENGTH_SHORT).show();
                    error.printStackTrace();
                    output.delete();
                } else {
                    Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
                            .addCategory(Intent.CATEGORY_DEFAULT)
                            .setData(Uri.fromFile(output));
                    mContext.sendBroadcast(intent);
                }
            }

            @Override
            public void onStart() {
                mNotifications.recording(0);
            }

            @Override
            public void onRecording(long presentationTimeUs) {
                if (startTime <= 0) {
                    startTime = presentationTimeUs;
                }
                long time = (presentationTimeUs - startTime) / 1000;
                mNotifications.recording(time);
            }
        });
        return r;
    }


}
