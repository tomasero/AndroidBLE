package com.aby.ble.sample;

import android.Manifest;
import android.annotation.TargetApi;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.media.MediaPlayer;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.aby.ble.sample.util.HexString;
import com.polidea.rxandroidble.RxBleConnection;
import com.polidea.rxandroidble.RxBleDevice;
import com.polidea.rxandroidble.utils.ConnectionSharingAdapter;
import com.trello.rxlifecycle.components.support.RxAppCompatActivity;


import org.w3c.dom.Text;

import java.util.UUID;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.subjects.PublishSubject;

import static com.trello.rxlifecycle.android.ActivityEvent.PAUSE;

public class MediaActivity extends RxAppCompatActivity {

    @BindView(R.id.connect)
    Button connectButton;
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
    @BindView(R.id.output)
    TextView output ;
    @BindView(R.id.time)
    TextView time ;
    @BindView(R.id.file)
    Button chooseFile ;
    @BindView(R.id.play)
    Button playPause ;
    @BindView(R.id.seek)
    SeekBar seek1 ;
    @BindView(R.id.volume_seek)
    SeekBar volumeSeek ;
    @BindView(R.id.volume_val)
    TextView volumeVal ;
    @BindView(R.id.forward)
    Button forwardBut ;
    @BindView(R.id.backward)
    Button backwardBut ;

//    private UUID characteristicReadUuid = UUID.fromString("00002221-0000-1000-8000-00805f9b34fb");
//    private UUID characteristicUuid = UUID.fromString("00002222-0000-1000-8000-00805f9b34fb");
    private UUID characteristicReadUuid = UUID.fromString("6E400003-B5A3-F393-E0A9-E50E24DCCA9E");
    private UUID characteristicUuid = UUID.fromString("6E400002-B5A3-F393-E0A9-E50E24DCCA9E");
    private PublishSubject<Void> disconnectTriggerSubject = PublishSubject.create();
    private Observable<RxBleConnection> connectionObservable;
    private RxBleDevice bleDevice;
    MediaPlayer mp;
    private boolean isLoaded = false ;
    private AudioManager audioManager;
    int seekTime = 0 ;

    public long[] triggerTime = {-1 , -1 , -1 , -1 , -1 , -1} ;
    public int triggerCount = 0 ;
    byte[] data = {20,100,10,100,10,0};
    // Start (Flag), Strength , Duration(*100ms)
    byte[][] multipleTriggers = {{100,10},{100,10},{100,10},{100,10},{100,10},{100,10}};
    byte[][] multipleTriggersMot = {{100,10},{100,10},{100,10},{100,10},{100,10},{100,10}};
    byte[] multipleTriggersPh = {0,0,0,0,0,0};
    boolean triggerActive = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_media);
        ButterKnife.bind(this);
        String macAddress = getIntent().getStringExtra(DeviceActivity.EXTRA_MAC_ADDRESS);
        bleDevice = MusicChills.getRxBleClient(this).getBleDevice(macAddress);
        connectionObservable = prepareConnectionObservable();
        //noinspection ConstantConditions
        getSupportActionBar().setSubtitle(getString(R.string.mac_address, macAddress));
        output.setText("Select File");
        mp = new MediaPlayer();
//        if(!isConnected()) onConnectToggleClick();
////        mp.setAudioStreamType(AudioManager.STREAM_MUSIC);
//        while(!isConnected()) {}
//        onNotifyClick();

//        requestLocationPermissionIfNeeded();

        audioManager = (AudioManager) getApplicationContext().getSystemService(Context.AUDIO_SERVICE);
//        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        volumeSeek.setMax(audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC));
        int currVol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        volumeSeek.setProgress(currVol);
        volumeVal.setText("Volume: " +Integer.toString(currVol));
        volumeSeek.setOnSeekBarChangeListener(volumeSeekListener);
        mp = new MediaPlayer();
        output.setText("Select File");

    }

    private Observable<RxBleConnection> prepareConnectionObservable() {
        return bleDevice
                .establishConnection(false)
                .takeUntil(disconnectTriggerSubject)
                .compose(bindUntilEvent(PAUSE))
                .compose(new ConnectionSharingAdapter());
    }



    @OnClick(R.id.file)
    @TargetApi(Build.VERSION_CODES.M)
    public void chooseFile(){
        String state = bleDevice.getConnectionState().toString();
        Log.d("TOMAS", state);
//        Intent intent = new Intent(Intent.ACTION_PICK);
        Intent intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, 10);
//        startService(intent, )
        if(this.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
            ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},1);

        triggerCount = 0 ;
        //triggerTimeShow.setText("Triggers");
        if(mp.isPlaying()) mp.stop();
        Log.d("TOMAS", bleDevice.getConnectionState().toString());
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if(resultCode == RESULT_OK && requestCode == 10){
            Uri uriSound=data.getData();
            output.setText(uriSound.getEncodedPath());
            mp = MediaPlayer.create(this,uriSound);
            isLoaded = true ;
            time.setText("Duration " + milliSecondsToTimer(mp.getDuration()));
            //output.append("Hello");
            initSeek(mp.getDuration());
//            Log.d("TOMAS", bleDevice.getConnectionState().toString());
//            while(!isConnected()) {
//                onConnectToggleClick();
//            }
//            onNotifyClick();
            Log.d("TOMAS", bleDevice.getConnectionState().toString());
        }
    }

    private void initSeek(int x){
        seek1.setMax(x);
        seek1.setProgress(0);
        seek1.setOnSeekBarChangeListener(mSeekBar);
        seek1.getProgressDrawable().setColorFilter(Color.RED, PorterDuff.Mode.SRC_IN);
    }

    private SeekBar.OnSeekBarChangeListener mSeekBar = new SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            time.setText("" + milliSecondsToTimer((int) progress));
            seekTime = progress ;
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {}

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            if(mp.isPlaying()) mp.seekTo(seekTime);
        }
    };
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
                time.post(mUpdateTime);
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
            time.setText("" + milliSecondsToTimer(seekTime));
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
            time.setText("" + milliSecondsToTimer(seekTime));
        }
    }

    private Runnable mUpdateTime = new Runnable() {
        public void run() {
            int currentDuration;
            if (mp.isPlaying()) {
                currentDuration = mp.getCurrentPosition();
                seek1.setProgress(currentDuration);
                time.postDelayed(this, 900);
            }else {
                time.removeCallbacks(this);
            }
        }
    };

    @OnClick(R.id.connect)
    public void onConnectToggleClick() {

        if (isConnected()) {
            triggerDisconnect();
        } else {
            Log.d("TOMAS", "Connecting again");
            int counter = 0;
            while (!isConnected()) {
                connect();
//            while(!isConnected()) {}
            }
            onNotifyClick();
        }
    }

    private void connect() {
        connectionObservable
                .flatMap(RxBleConnection::discoverServices)
                .flatMap(rxBleDeviceServices -> rxBleDeviceServices.getCharacteristic(characteristicUuid))
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe(() -> connectButton.setText(R.string.connecting))
                .subscribe(
                        characteristic -> {
                            updateUI(characteristic);
                            Log.i(getClass().getSimpleName(), "Hey, connection has been established!");
                        },
                        this::onConnectionFailure,
                        this::onConnectionFinished
                );
    }

    @OnClick(R.id.read)
    public void onReadClick() {

        if (isConnected()) {
            connectionObservable
                    .flatMap(rxBleConnection -> rxBleConnection.readCharacteristic(characteristicReadUuid))
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(bytes -> {
                        readOutputView.setText(new String(bytes));
                        readHexOutputView.setText(HexString.bytesToHex(bytes));
                        writeInput.setText(HexString.bytesToHex(bytes));
                    }, this::onReadFailure);
        }
    }

    @OnClick(R.id.write)
    public void onWriteClick() {
        if (isConnected()) {
            connectionObservable
                    .flatMap(rxBleConnection -> rxBleConnection.writeCharacteristic(characteristicUuid, getInputBytes()))
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                            bytes -> onWriteSuccess(),
                            this::onWriteFailure
                    );
        }
    }

    public void sendData() {

        if (isConnected()) {
            connectionObservable
                    .flatMap(rxBleConnection -> rxBleConnection.writeCharacteristic(characteristicUuid, data ))
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                            bytes -> onWriteSuccess(),
                            this::onWriteFailure
                    );
        }
    }
    @OnClick(R.id.notify)
    public void onNotifyClick() {
        if (isConnected()) {
            connectionObservable
                    .flatMap(rxBleConnection -> rxBleConnection.setupNotification(characteristicReadUuid))
                    .doOnNext(notificationObservable -> runOnUiThread(this::notificationHasBeenSetUp))
                    .flatMap(notificationObservable -> notificationObservable)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(this::onNotificationReceived, this::onNotificationSetupFailure);
        }
    }

    private boolean isConnected() {
        return bleDevice.getConnectionState() == RxBleConnection.RxBleConnectionState.CONNECTED;
    }

    private void onConnectionFailure(Throwable throwable) {
        //noinspection ConstantConditions
        Snackbar.make(findViewById(R.id.main), "Connection error: " + throwable, Snackbar.LENGTH_SHORT).show();
        updateUI(null);
    }

    private void onConnectionFinished() {
        updateUI(null);
    }

    private void onReadFailure(Throwable throwable) {
        //noinspection ConstantConditions
        Snackbar.make(findViewById(R.id.main), "Read error: " + throwable, Snackbar.LENGTH_SHORT).show();
    }

    private void onWriteSuccess() {
        //noinspection ConstantConditions
        Snackbar.make(findViewById(R.id.main), "Write success", Snackbar.LENGTH_SHORT).show();
    }

    private void onWriteFailure(Throwable throwable) {
        //noinspection ConstantConditions
        Snackbar.make(findViewById(R.id.main), "Write error: " + throwable, Snackbar.LENGTH_SHORT).show();
    }
    double minVal = 999999999;
    double maxVal = -99999999;
    boolean initVal = false;
    double prevVal = 0;
    int delta = 0;
    int prevDelta = 0;
    private void onNotificationReceived(byte[] bytes) {
        //I is toration
        //G is tap
        String str = new String(bytes);
//        Log.d("CREATION", "onNotificationReceived");
        //noinspection ConstantConditions
        //Snackbar.make(findViewById(R.id.main), "Change: " + HexString.bytesToHex(bytes), Snackbar.LENGTH_SHORT).show();
//        output.setText(HexString.bytesToHex(bytes));
//        output.setText(str);
        Log.d("NOTIFICATION", HexString.bytesToHex(bytes));
        char cmd =  str.charAt(0);
//        Log.d("CREATION", cmd);
        if (cmd == 'G') {
            output.setText("TAP");
            onClickPlay();
        } else if (cmd == 'I') {
            output.setText("DATA");
            String[] vals = str.split(",");
            Log.d("DATA", vals[4]);
            output.setText(String.valueOf(vals[4]));
            try {
                double datum = Double.parseDouble(vals[4].trim());
                Log.d("DATUM", String.valueOf(datum));
                if (datum < minVal) {
                    minVal = datum;
                } else if (datum > maxVal) {
                    maxVal = datum;
                }

                if (!initVal) {
                    initVal = true;
                } else {
                    delta = (int) datum - (int) prevVal;
                    if (delta > 3000) { //Weird jump from negative to positive
                        prevVal = datum;
                        return;
                    }
                    prevDelta = (int) (delta*0.9) + (int) (prevDelta*0.1);
                    Log.d("DELTA", String.valueOf(delta));
                    Log.d("DELTA", String.valueOf(prevDelta));
                    Log.d("DELTA", "---");
                    int mapped = (int) delta/40;

                    if (mapped > 3) {
                        mapped = 3;
                    } else if (mapped < -3) {
                        mapped = -3;
                    }
                    Log.d("DELTA", String.valueOf(mapped));
                    Log.d("DELTA", "---");
                    volumeVal.setText("Volume: " +Integer.toString(mapped));
                    int currVol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, currVol + mapped, 0);
                    volumeSeek.setProgress(currVol);
                }
                prevVal = datum;
                Log.d("MAX", String.valueOf(maxVal));
                Log.d("MIN", String.valueOf(minVal));
            } catch (NumberFormatException nfe) {
                return;
            } catch (Exception e) {
                return;
            }

        } else {
            output.setText("");
        }
//        triggerTimeShow.setText(HexString.bytesToHex(bytes));
//        Log.d("CREATION", HexString.bytesToHex(bytes));
//        Log.i(getClass().getSimpleName(), HexString.bytesToHex(bytes));
//        phVal.setText(HexString.bytesToHex(bytes));
//        if(isLoaded && mp.isPlaying())
      //  {
      //      long x = mp.getCurrentPosition() ;
      //  }
    }

    private void onNotificationSetupFailure(Throwable throwable) {
        //noinspection ConstantConditions
        Snackbar.make(findViewById(R.id.main), "Notifications error: " + throwable, Snackbar.LENGTH_SHORT).show();
    }

    private void notificationHasBeenSetUp() {
        //noinspection ConstantConditions
        Snackbar.make(findViewById(R.id.main), "Notifications has been set up", Snackbar.LENGTH_SHORT).show();
    }

    private void triggerDisconnect() {
        disconnectTriggerSubject.onNext(null);
    }

    /**
     * This method updates the UI to a proper state.
     * @param characteristic a nullable {@link BluetoothGattCharacteristic}. If it is null then UI is assuming a disconnected state.
     */
    private void updateUI(BluetoothGattCharacteristic characteristic) {
        connectButton.setText(characteristic != null ? R.string.disconnect : R.string.connect);
        //readButton.setEnabled(hasProperty(characteristic, BluetoothGattCharacteristic.PROPERTY_READ));
        readButton.setEnabled(isConnected());
        writeButton.setEnabled(hasProperty(characteristic, BluetoothGattCharacteristic.PROPERTY_WRITE));
        notifyButton.setEnabled(isConnected());
        chooseFile.setEnabled(true);
    }

    private boolean hasProperty(BluetoothGattCharacteristic characteristic, int property) {
        return characteristic != null && (characteristic.getProperties() & property) > 0;
    }

    private byte[] getInputBytes() {
        return HexString.hexToBytes(writeInput.getText().toString());
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
