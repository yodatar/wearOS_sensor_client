package sk.wearClient;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Vibrator;
import android.util.Log;

import java.text.BreakIterator;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import sk.wearClient.helpers.SampleMessage;

import static android.content.Context.VIBRATOR_SERVICE;

class AccelerometerManager {

    private static Context context = null;
    /**
     * Accuracy default configuration
     */
    private static float threshold = 0.5f; // accelerometer force threshold
    private static int interval = 38000;
    private static SampleMessage sampleMessage;

    private static boolean recording;
    private static int recordingCounter;

    public static float getThreshold() {
        return threshold;
    }

    public static void setThreshold(float threshold) {
        AccelerometerManager.threshold = threshold;
    }

    public static int getInterval() {
        return interval;
    }

    public static void setInterval(int interval) {
        AccelerometerManager.interval = interval;
    }

    private static Sensor sensor;
    private static SensorManager sensorManager;

    private static AccelerometerListener listener;

    /**
     * indicates whether or not Accelerometer Sensor is supported
     */
    private static Boolean supported;
    /**
     * indicates whether or not Accelerometer Sensor is running
     */
    private static boolean running = false;

    /**
     * Returns true if the manager is listening to orientation changes
     */
    public static boolean isListening() {
        return running;
    }

    /**
     * Unregisters listeners
     */
    public static void stopListening() {
        running = false;
        try {
            if (sensorManager != null && sensorEventListener != null) {
                sensorManager.unregisterListener(sensorEventListener);
            }
        } catch (Exception e) {
            Log.e("sensorManager", e.toString());

        }
    }

    /**
     * Returns true if at least one Accelerometer sensor is available
     */
    public static boolean isSupported(Context cntxt) {
        context = cntxt;
        if (supported == null) {
            if (context != null) {

                sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);

                // Get all sensors in device
                List<Sensor> sensors = sensorManager.getSensorList(Sensor.TYPE_ACCELEROMETER);

                supported = new Boolean(sensors.size() > 0);
            } else {
                supported = Boolean.FALSE;
            }
        }
        Log.w("Debug", "Accelerometer supported");

        return supported;
    }


    public static void startListening(AccelerometerListener accelerometerListener) {
        //configure(threshold, interval);
        sampleMessage = SampleMessage.getInstance();

        sensorManager = (SensorManager) context.
                getSystemService(Context.SENSOR_SERVICE);

        // Take all sensors in device
        List<Sensor> sensors = sensorManager.getSensorList(
                Sensor.TYPE_ACCELEROMETER);

        if (sensors.size() > 0) {

            sensor = sensors.get(0);

            // Register Accelerometer Listener
            running = sensorManager.registerListener(
                    sensorEventListener, sensor, interval);

            if (running) {
                Log.wtf("registerListener TYPE_ACCELEROMETER", "true");
            } else {

            }

            listener = accelerometerListener;
        }
    }

    private static long lastNow;
    private static SensorEventListener sensorEventListener = new SensorEventListener() {

        private long now = 0;
        private float x = 0;
        private float y = 0;
        private float z = 0;
        private float force = 0;

        final int indexInPatternToRepeat = -1;
        long[] vibrationPattern = {100,100};

        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }

        public void onSensorChanged(SensorEvent event) {
            final Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);

            if(event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {

                now = System.currentTimeMillis();
                //now = TimeUnit.NANOSECONDS.toMicros(event.timestamp);

                // SWITCHED Y & X !!!
                x = event.values[1];
                y = event.values[0];
                z = event.values[2];

                if (!recording) {

                    /**
                     *  Behold, the Algorithm!
                     */
                    if ((y < -7) && (z > -2 && z < 2) && (x > -3 && x < 3)) {

                        try {
                            force = Math.abs(x + y + z - sampleMessage.getLastX() - sampleMessage.getLastY() - sampleMessage.getLastZ());

                        } catch (Exception e) {}


                        /**
                         *  If threshold triggered
                         */
                        if (Float.compare(force, threshold) > 0) {
                            vibrator.vibrate(vibrationPattern, indexInPatternToRepeat);
                            startRecording();
                        }
                    }
                }

                /**
                 * recording for 2 seconds
                 *
                 */
                if (recording) {
                    listener.onShaking(x,y,z,now);
                    recordingCounter++;

                    if(recordingCounter > 75) {
                        stopRecording();
                    }
                }

                // put in history
                sampleMessage.insertValues(x,y,z);
                //MainActivity.messageId.setText(String.valueOf(now-lastNow));
                //lastNow = now;

            }
        }

        private void startRecording() {

            recording = true;
            sampleMessage.setMessageId(now);
            sampleMessage.setClientId();
        }

        private void stopRecording() {
            recording = false;
            recordingCounter = 0;

            Log.wtf("stopRecording()", sampleMessage.toString());

            listener.finalizeSampleMessage(sampleMessage.toString());
        }


    };

}