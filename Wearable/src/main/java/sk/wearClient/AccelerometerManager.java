package sk.wearClient;

import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Vibrator;
import android.util.Log;

import com.google.android.gms.wearable.Wearable;

import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import sk.wearClient.fragments.AssetFragment;
import sk.wearClient.helpers.SampleMessage;

import static android.content.Context.SENSOR_SERVICE;
import static android.content.Context.VIBRATOR_SERVICE;
import static android.hardware.SensorManager.SENSOR_DELAY_UI;

public class AccelerometerManager {

    private SensorManager sensorManager;
    private List<Sensor> accSensors;
    private SensorEventListener listener;
    private Sensor accSensor;
    private SampleMessage sampleMessage;


    private final int interval = 2000000000; // sample delay
    private final float threshold = 2f; // accelerometer force threshold

    private long now = 0;
    private float x = 0;
    private float y = 0;
    private float z = 0;

    private float force = 0;

    // vibrations
    private final int indexInPatternToRepeat = -1;
    private long[] vibrationPattern = {100,100};
    private int recordingCounter;
    private boolean recording;
    private long lastNow;
    private int retarder;


    void registerSensor(MainActivity mainActivity) {

        sensorManager = (SensorManager)mainActivity.getSystemService(SENSOR_SERVICE);

        // singleton sample
        sampleMessage = SampleMessage.getInstance();

        //write out to the log all the accSensors the device has.
        /*accSensors = sensorManager.getSensorList(Sensor.TYPE_ALL);
        if (accSensors.size() < 1) {
            Log.wtf(TAG,"No accSensors returned from getSensorList");
        }
        Sensor[] sensorArray = accSensors.toArray(new Sensor[accSensors.size()]);
        for (int i = 0; i < sensorArray.length; i++) {
            Log.d(TAG,"Found accSensor " + i + " " + sensorArray[i].toString());
        }*/


        if (sensorManager == null)
            sensorManager = (SensorManager)mainActivity.getSystemService(SENSOR_SERVICE);

        accSensors = sensorManager.getSensorList(Sensor.TYPE_ACCELEROMETER);

        final Vibrator vibrator = (Vibrator)mainActivity.getSystemService(VIBRATOR_SERVICE);

        if(accSensors.size() > 0) {
            accSensor = accSensors.get(0);
        }

        listener = new SensorEventListener() {

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) { }

            @Override
            public void onSensorChanged(SensorEvent event) {
                if(event.sensor.getType() == Sensor.TYPE_ACCELEROMETER && retarder == 0) {

                    now = System.currentTimeMillis();
                    //now = TimeUnit.NANOSECONDS.toMicros(event.timestamp);
                    x = event.values[0];
                    y = event.values[1];
                    z = event.values[2];

                    if (!recording) {

                        /**
                         *  Behold, the Algorithm!
                         */
                        if ((y < -7) && (z > -2 && z < 2) && (x > -3 && x < 3)) {

                            force = Math.abs(x + y + z - sampleMessage.getLastX() - sampleMessage.getLastY() - sampleMessage.getLastZ());

                            /**
                             *  If threshold triggered
                             */
                            if (Float.compare(force, threshold) > 0) {
                                startRecording();
                            }
                        }
                    }

                    /**
                     * recording for 2 seconds
                     *
                     */
                    if (recording) {
                        recordingCounter++;

                        if(recordingCounter > 80) {
                            stopRecording();
                        }
                    }

                    // put in history
                    sampleMessage.insertValues(x,y,z);

                     String msg = " x: " + String.valueOf(x) +
                        "\n y: " + String.valueOf(y) +
                        "\n z: " + String.valueOf(z) +
                        "\n T: " + String.valueOf(now-lastNow);
                    MainActivity.getmAssetFragment().setText(msg);

                    lastNow = now;
                }
                retarder++;
                if(retarder > 3) retarder = 0;
            }

            private void startRecording() {
                MainActivity.getmAssetFragment().setBackground(Color.GREEN);
                vibrator.vibrate(vibrationPattern, indexInPatternToRepeat);

                recording = true;
                sampleMessage.setMessageId(now);
                sampleMessage.setClientId();
            }

            private void stopRecording() {
                recording = false;
                MainActivity.getmAssetFragment().setBackground(Color.BLACK);
                recordingCounter = 0;

                Log.wtf("stopRecording()", sampleMessage.toString());

                MainActivity.sendSample(sampleMessage.toString(), sampleMessage.getMessageId());
            }
        };

        sensorManager.registerListener(listener, accSensor, interval);
    }

    void unregisterSensor() {
        if (sensorManager != null && listener != null) {
            sensorManager.unregisterListener(listener);
        }
        //clean up and release memory.
        sensorManager = null;
        listener = null;
    }
}
