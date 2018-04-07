package sk.wearClient;


public interface AccelerometerListener {

     void onAccelerationChanged(float x, float y, float z, long time);

     void onShaking(float x, float y, float z, long time, long id);

     void finalizeSampleMessage(String msg);

}