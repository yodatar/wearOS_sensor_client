package sk.wearClient;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

import java.util.List;

class AccelerometerManager {

    private static Context context = null;
    /**
     * Accuracy default configuration
     */
    private static float threshold = 5f; // accelerometer force threshold
    private static int interval = 20000;


    //
    private static boolean recording;



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
    // you could use an OrientationListener array instead
// if you plans to use more than one listener
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
                List<Sensor> sensors = sensorManager.getSensorList(
                        Sensor.TYPE_ACCELEROMETER);

                supported = new Boolean(sensors.size() > 0);
            } else {
                supported = Boolean.FALSE;
            }
        }
        Log.w("Debug", "Accelerometer supported");

        return supported;
    }

    /**
     * Configure the listener for shaking
     *
     * @param threshold minimum acceleration variation for considering shaking
     * @param interval  minimum interval between to shake events
     */
    public static void configure(float threshold, int interval) {
        AccelerometerManager.threshold = threshold;
        AccelerometerManager.interval = interval;
    }

    /**
     * Registers a listener and start listening
     *
     * @param accelerometerListener callback for accelerometer events
     */
    public static void startListening(AccelerometerListener accelerometerListener) {
        //configure(threshold, interval);

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

            }

            listener = accelerometerListener;
        }
    }

    private static SensorEventListener sensorEventListener = new SensorEventListener() {

        private long now = 0;
        private long timeDiff = 0;
        private long lastUpdate = 0;
        private long lastShake = 0;

        private float x = 0;
        private float y = 0;
        private float z = 0;
        private float lastX = 0;
        private float lastY = 0;
        private float lastZ = 0;
        private float force = 0;

        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }

        public void onSensorChanged(SensorEvent event) {
            // use the event timestamp as reference
            // so the manager precision won't depends
            // on the AccelerometerListener implementation
            // processing time

            //now = event.timestamp;
            now = System.currentTimeMillis();

            x = event.values[0];
            y = event.values[1];
            z = event.values[2];

            // if not interesting in shake events
            // just remove the whole if then else block
            if (lastUpdate == 0) {
                lastUpdate = now;
                lastShake = now;
                lastX = x;
                lastY = y;
                lastZ = z;
            } else {
                timeDiff = now - lastUpdate;

                if (timeDiff > 0) {

                    force = Math.abs(x + y + z - lastX - lastY - lastZ);

                    // If threshold triggered
                    if (Float.compare(force, threshold) > 0) {

                        if (!recording) {
                            // trigger shake event
                            Log.w("Debug", "timeDiff: " + (now - lastShake));
                            lastShake = now;
                            recording = true;

                        } else {
                            //Toast.makeText(context, "No Motion detected",Toast.LENGTH_SHORT).show();
                        }
                    }

                    Long shakeDiff= (now - lastShake); // 1000000;

                    // in milliseconds
                    // recording for 2 seconds
                    if (shakeDiff > 3000 && recording) {
                        recording = false;
                        //
                        listener.recordingStopped();
                    }

                    if (recording) {
                        listener.onShaking(x, y, z, now, lastShake);
                    }

                    lastX = x;
                    lastY = y;
                    lastZ = z;
                    lastUpdate = now;

                } else {
                    //Toast.makeText(context, "No Motion detected", Toast.LENGTH_SHORT).show();
                    //Log.w("Debug", "No Motion detected");

                }

                //listener.onAccelerationChanged(x, y, z, now);

            }
// trigger change event
        }
    };
}