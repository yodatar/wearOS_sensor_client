package com.example.android.wearable.wearClient.helpers;

import android.content.Context;
import android.util.Log;
import android.widget.ToggleButton;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;


public class MqttHelper {

    public MqttAndroidClient mqttAndroidClient;
    private String hostAddress;
    //final static String clientId = "smartphone pali";


    public MqttHelper(Context context, String ipAddress, ToggleButton mqttToggle, String clientId) {

        this.hostAddress = "ws://" + ipAddress + ":4000";

        mqttAndroidClient = new MqttAndroidClient(context, hostAddress, clientId);

        mqttAndroidClient.setCallback(new MqttCallbackExtended() {

            @Override
            public void connectComplete(boolean b, String s) {
                Log.w("mqtt", s);
            }

            @Override
            public void connectionLost(Throwable throwable) {
                Log.w("Mqtt", "Connection Lost: " + throwable.toString());
            }

            @Override
            public void messageArrived(String topic, MqttMessage mqttMessage) throws Exception {
                Log.w("Mqtt", mqttMessage.toString());
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {

            }
        });
        connect(mqttToggle);
    }

    public void setCallback(MqttCallbackExtended callback) {
        mqttAndroidClient.setCallback(callback);
    }

    private void connect(final ToggleButton mqttToggle) {
        Log.w("Mqtt", "Connect attempt");

        MqttConnectOptions mqttConnectOptions = new MqttConnectOptions();
        mqttConnectOptions.setAutomaticReconnect(true);
        mqttConnectOptions.setCleanSession(true);
        mqttConnectOptions.setKeepAliveInterval(30);

        // mqttConnectOptions.setUserName(username);
        //  mqttConnectOptions.setPassword(password.toCharArray());

        try {
            mqttAndroidClient.connect(mqttConnectOptions, null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    Log.w("Mqtt", "Connect successful");

            /*        DisconnectedBufferOptions disconnectedBufferOptions = new DisconnectedBufferOptions();
                    disconnectedBufferOptions.setBufferEnabled(true);
                    disconnectedBufferOptions.setBufferSize(100);
                    disconnectedBufferOptions.setPersistBuffer(false);
                    disconnectedBufferOptions.setDeleteOldestMessages(false);
                    mqttAndroidClient.setBufferOpts(disconnectedBufferOptions);*/

                    subscribeToTopic();

                    mqttToggle.setChecked(true);

                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    Log.w("Mqtt", "Connect Failed: " + hostAddress + " ::: " + exception.toString());
                    exception.printStackTrace();

                    mqttToggle.setChecked(false);

                }
            });


        } catch (MqttException ex) {
            ex.printStackTrace();
        }
    }


    private void subscribeToTopic() {
        try {
            mqttAndroidClient.subscribe("match", 0, null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) { Log.w("Mqtt", "Subscribed: match/*"); }
                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) { Log.w("Mqtt", "Subscribed fail!"); }
            });

        } catch (MqttException ex) {
            System.err.println("Exception whilst subscribing");
            //ex.printStackTrace();
        }
    }

    public void publishMessage(String payload, String id) {
        Log.w("Mqtt", "Message Published. payload: " + payload);

        try {
            MqttMessage message = new MqttMessage();
            message.setPayload(payload.getBytes());
            message.setId(Integer.parseInt(id.substring(0,9)));
            mqttAndroidClient.publish("data/acc", message);
            //Log.w("Mqtt", "Message Published. Id: " + id);
//            if(!mqttAndroidClient.isConnected()){
//                addToHistory(mqttAndroidClient.getBufferedMessageCount() + " messages in buffer.");
//            }
        } catch (MqttException e) {
            System.err.println("Error Publishing: " + e.getMessage());
            //e.printStackTrace();
        }
    }
}
