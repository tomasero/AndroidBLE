package com.aby.ble.sample;
import android.Manifest;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;


import android.graphics.Color;
import android.graphics.PorterDuff;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.polidea.rxandroidble.RxBleClient;
import com.polidea.rxandroidble.exceptions.BleScanException;
import com.polidea.rxandroidble.scan.ScanFilter;
import com.polidea.rxandroidble.scan.ScanResult;
import com.polidea.rxandroidble.scan.ScanSettings;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;

import android.media.AudioManager;
import android.content.Context;
import android.media.MediaPlayer;
//import android.view.KeyEvent;

public class ScanActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_FINE_LOCATION = 1;
    @BindView(R.id.scan_toggle_btn)
    Button scanToggleButton;
    @BindView(R.id.scan_results)
    RecyclerView recyclerView;
    @BindView(R.id.networkActive)
    Switch nwActive ;
    @BindView(R.id.volume_seek)
    SeekBar volumeSeek ;
    @BindView(R.id.volume_val)
    TextView volumeVal ;
    @BindView(R.id.play)
    Button playPauseBut ;
    @BindView(R.id.forward)
    Button forwardBut ;
    @BindView(R.id.backward)
    Button backwardBut ;
    @BindView(R.id.check)
    TextView output ;
    @BindView(R.id.file)
    Button chooseFile ;
    @BindView(R.id.seek)
    SeekBar seek1 ;
    @BindView(R.id.read_output)
    TextView readOutputView;
    @BindView(R.id.read_hex_output)
    TextView readHexOutputView;
    @BindView(R.id.write_input)
    TextView writeInput;
    @BindView(R.id.read)
    Button readButton;
    @BindView(R.id.write)
    Button writeButton;
    @BindView(R.id.notify)
    Button notifyButton;

    private RxBleClient rxBleClient;
    private Subscription scanSubscription;
    private ScanResultsAdapter resultsAdapter;
    private boolean nwCheck;
    private AudioManager audioManager;
    MediaPlayer mp;
    private boolean isLoaded = false ;
    int seekTime = 0 ;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan);
        requestLocationPermissionIfNeeded();
        ButterKnife.bind(this);
        rxBleClient = MusicChills.getRxBleClient(this);
        configureResultList();
        nwCheck = false;
        nwActive.setChecked(false);
        audioManager = (AudioManager) getApplicationContext().getSystemService(Context.AUDIO_SERVICE);
//        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        volumeSeek.setMax(audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC));
        int currVol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        volumeSeek.setProgress(currVol);
        volumeVal.setText("Volume: " +Integer.toString(currVol));
        volumeSeek.setOnSeekBarChangeListener(volumeSeekListener);
        mp = new MediaPlayer();
        output.setText("Select File");
//        mp.setAudioStreamType(AudioManager.STREAM_MUSIC);
    }

    private SeekBar.OnSeekBarChangeListener volumeSeekListener = new SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onProgressChanged(SeekBar seekBar, int newVolume, boolean b) {
//            data[3] = (byte)progress ;
            volumeVal.setText("Volume: " +Integer.toString(newVolume));
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, newVolume, 0);
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {}

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {}
    };

    @OnClick(R.id.file)
    @TargetApi(Build.VERSION_CODES.M)
    public void chooseFile(){

        Intent intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, 10);
        if(this.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
            ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},1);
        if(mp.isPlaying()) mp.stop();
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if(resultCode == RESULT_OK && requestCode == 10){
            Uri uriSound=data.getData();
            output.setText(uriSound.getEncodedPath());
            mp = MediaPlayer.create(this,uriSound);
            isLoaded = true ;
            output.setText("Duration " + milliSecondsToTimer(mp.getDuration()));
            //output.append("Hello");
            initSeek(mp.getDuration());
        }
    }


    private void initSeek(int x){
        seek1.setMax(x);
        seek1.setProgress(0);
        seek1.setOnSeekBarChangeListener(mSeekBar);
        output.setText("Set Triggers");
        seek1.getProgressDrawable().setColorFilter(Color.RED, PorterDuff.Mode.SRC_IN);


    }

    private SeekBar.OnSeekBarChangeListener mSeekBar = new SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            output.setText("" + milliSecondsToTimer((int) progress));
            seekTime = progress ;
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {}

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            if(mp.isPlaying()) mp.seekTo(seekTime);
        }
    };


    @OnClick(R.id.play)
    public void onClickPlay() {
//        if(!isConnected()) onConnectToggleClick();
        if(isLoaded) {
            if(mp.isPlaying()) {
                mp.pause();
            }
            else {
                mp.start();
                seek1.getProgressDrawable().setColorFilter(Color.BLUE, PorterDuff.Mode.SRC_IN);
                seek1.setProgress(0);
                output.post(mUpdateTime);
            }
        }
        else
            Toast.makeText(this, "Choose A Song", Toast.LENGTH_SHORT).show();
    }

    private int stepSize = 10;

    @OnClick(R.id.forward)
    public void onSeekForward() {
        Log.d("CREATION", "forward");
        if(isLoaded) {
            Log.d("CREATION", Integer.toString(seekTime));
            seekTime = seekTime + stepSize * 1000;
            if(mp.isPlaying()) mp.seekTo(seekTime);
            output.setText("" + milliSecondsToTimer(seekTime));
        }
    }

    @OnClick(R.id.backward)
    public void onSeekBackward() {
        Log.d("CREATION", "backward");
        if(isLoaded) {
            Log.d("CREATION", "inside");
            if (seekTime >= stepSize * 1000) {
                seekTime -= stepSize * 1000;
            } else {
                seekTime = 0;
            }
            if(mp.isPlaying()) mp.seekTo(seekTime);
            output.setText("" + milliSecondsToTimer(seekTime));
        }
    }

    @OnClick(R.id.scan_toggle_btn)
    public void onScanToggleClick() {
        scanToggleButton.setText("Select your Device");
        if (isScanning()) {
            scanSubscription.unsubscribe();
        } else {
            scanSubscription = rxBleClient.scanBleDevices(
                    new ScanSettings.Builder()
                            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                            .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
                            .build(),
                    new ScanFilter.Builder()

                            // add custom filters if needed
//                            .setDeviceName("RFduino")
                            .setDeviceName("Button_UART")
                            .build()
            )
                    .observeOn(AndroidSchedulers.mainThread())
                    .doOnUnsubscribe(this::clearSubscription)
                    .subscribe(resultsAdapter::addScanResult, this::onScanFailure);
        }

        updateButtonUIState();
    }

    private void handleBleScanException(BleScanException bleScanException) {
        final String text;

        switch (bleScanException.getReason()) {
            case BleScanException.BLUETOOTH_NOT_AVAILABLE:
                text = "Bluetooth is not available";
                break;
            case BleScanException.BLUETOOTH_DISABLED:
                text = "Enable bluetooth and try again";
                break;
            case BleScanException.LOCATION_PERMISSION_MISSING:
                text = "On Android 6.0 location permission is required. Implement Runtime Permissions";
                break;
            case BleScanException.LOCATION_SERVICES_DISABLED:
                text = "Location services needs to be enabled on Android 6.0";
                break;
            case BleScanException.SCAN_FAILED_ALREADY_STARTED:
                text = "Scan with the same filters is already started";
                break;
            case BleScanException.SCAN_FAILED_APPLICATION_REGISTRATION_FAILED:
                text = "Failed to register application for bluetooth scan";
                break;
            case BleScanException.SCAN_FAILED_FEATURE_UNSUPPORTED:
                text = "Scan with specified parameters is not supported";
                break;
            case BleScanException.SCAN_FAILED_INTERNAL_ERROR:
                text = "Scan failed due to internal error";
                break;
            case BleScanException.SCAN_FAILED_OUT_OF_HARDWARE_RESOURCES:
                text = "Scan cannot start due to limited hardware resources";
                break;
            case BleScanException.UNDOCUMENTED_SCAN_THROTTLE:
                text = String.format(
                        Locale.getDefault(),
                        "Android 7+ does not allow more scans. Try in %d seconds",
                        secondsTill(bleScanException.getRetryDateSuggestion())
                );
                break;
            case BleScanException.UNKNOWN_ERROR_CODE:
            case BleScanException.BLUETOOTH_CANNOT_START:
            default:
                text = "Unable to start scanning";
                break;
        }
        Log.w("EXCEPTION", text, bleScanException);
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
    }

    private long secondsTill(Date retryDateSuggestion) {
        return TimeUnit.MILLISECONDS.toSeconds(retryDateSuggestion.getTime() - System.currentTimeMillis());
    }

    public void onPause(){
        super.onPause();
        if(mp.isPlaying())mp.stop();
    }

    private Runnable mUpdateTime = new Runnable() {
        public void run() {
            int currentDuration;
            if (mp.isPlaying()) {
                currentDuration = mp.getCurrentPosition();
                seek1.setProgress(currentDuration);
                output.postDelayed(this, 900);
            }else {
                output.removeCallbacks(this);
            }
        }
    };


    private void configureResultList() {
        recyclerView.setHasFixedSize(true);
        LinearLayoutManager recyclerLayoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(recyclerLayoutManager);
        resultsAdapter = new ScanResultsAdapter();
        recyclerView.setAdapter(resultsAdapter);
        resultsAdapter.setOnAdapterItemClickListener(view -> {
            final int childAdapterPosition = recyclerView.getChildAdapterPosition(view);
            final ScanResult itemAtPosition = resultsAdapter.getItemAtPosition(childAdapterPosition);
            onAdapterItemClick(itemAtPosition);
        });
    }

    private boolean isScanning() {
        return scanSubscription != null;
    }

    private void onAdapterItemClick(ScanResult scanResults) {
        final String macAddress = scanResults.getBleDevice().getMacAddress();
        final Intent intent = new Intent(this, MediaActivity.class);
        intent.putExtra(DeviceActivity.EXTRA_MAC_ADDRESS, macAddress);
        startActivity(intent);
    }

    private void onScanFailure(Throwable throwable) {

        if (throwable instanceof BleScanException) {
            handleBleScanException((BleScanException) throwable);
        }
    }

    private void clearSubscription() {
        scanSubscription = null;
        resultsAdapter.clearScanResults();
        updateButtonUIState();
    }

    private void updateButtonUIState() {
        scanToggleButton.setText(isScanning() ? R.string.stop_scan : R.string.start_scan);
    }

    @TargetApi(Build.VERSION_CODES.M)
    private void requestLocationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Android M Permission check 
            if (this.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("This app needs location access");
                builder.setMessage("Please grant location access so this app can scan for Bluetooth peripherals");
                builder.setPositiveButton(android.R.string.ok, null);
                builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    public void onDismiss(DialogInterface dialog) {
                        requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_REQUEST_FINE_LOCATION);
                    }
                });
                builder.show();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_FINE_LOCATION: {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                } else {
                    final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle("Bluetooth Scanning not available");
                    builder.setMessage("Since location access has not been granted, the app will not be able to scan for Bluetooth peripherals");
                    builder.setPositiveButton(android.R.string.ok, null);
                    builder.setOnDismissListener(new DialogInterface.OnDismissListener() {

                        @Override
                        public void onDismiss(DialogInterface dialog) {
                        }

                    });
                    builder.show();
                }
                break;
            }
            default:
                break;
        }
    }

    @OnClick(R.id.networkActive)
    public void onToggle(){
        nwCheck = nwActive.isChecked() ;

    }

    public  String milliSecondsToTimer(long milliseconds) {
        String finalTimerString = "";
        String secondsString = "";

        // Convert total duration into time
        int hours = (int) (milliseconds / (1000 * 60 * 60));
        int minutes = (int) (milliseconds % (1000 * 60 * 60)) / (1000 * 60);
        int seconds = (int) ((milliseconds % (1000 * 60 * 60)) % (1000 * 60) / 1000);
        // Add hours if there
        if (hours > 0) {
            finalTimerString = hours + ":";
        }

        // Prepending 0 to seconds if it is one digit
        if (seconds < 10) {
            secondsString = "0" + seconds;
        } else {
            secondsString = "" + seconds;
        }

        finalTimerString = finalTimerString + minutes + ":" + secondsString;

        // return timer string
        return finalTimerString;
    }

}
