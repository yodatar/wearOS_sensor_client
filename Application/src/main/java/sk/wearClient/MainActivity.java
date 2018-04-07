

package sk.wearClient;


import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.WorkerThread;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.github.mikephil.charting.charts.LineChart;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.android.gms.wearable.CapabilityClient;
import com.google.android.gms.wearable.CapabilityInfo;
import com.google.android.gms.wearable.DataClient;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.MessageClient;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.Wearable;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;

import sk.wearClient.helpers.SampleMessage;
import sk.wearClient.helpers.ChartHelper;
import sk.wearClient.helpers.MqttHelper;


public class MainActivity extends Activity implements
        AccelerometerListener,
        DataClient.OnDataChangedListener,
        MessageClient.OnMessageReceivedListener,
        CapabilityClient.OnCapabilityChangedListener {

    private static final String TAG = "MainActivity";

    private static final int REQUEST_IMAGE_CAPTURE = 1;

    private static final String START_ACTIVITY_PATH = "/start-activity";
    private static final String COUNT_PATH = "/count";
    private static final String COUNT_KEY = "count";

    private ListView mDataItemList;

    private View mStartActivityBtn;

    private DataItemAdapter mDataItemListAdapter;

    // Send DataItems.
    private ScheduledExecutorService mGeneratorExecutor;
    private ScheduledFuture<?> mDataItemGeneratorFuture;






    MqttHelper mqttHelper;
    ChartHelper mChart;
    LineChart chart;
    private static Context context = null;

    sk.wearClient.helpers.SampleMessage sampleMessage;

    TextView dataReceived;
    TextView messageId;

    ToggleButton accelerationToggle;
    ToggleButton mqttToggle;
    SeekBar sampleRateSeek;
    SeekBar thresholdSeek;
    EditText ipAddress;
    EditText clientId;

    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");




    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.w(TAG, "onCreate");

        setContentView(R.layout.main_activity);
        setupViews();

        // Stores DataItems received by the local broadcaster or from the paired watch.
        mDataItemListAdapter = new DataItemAdapter(this, android.R.layout.simple_list_item_1);
        mDataItemList.setAdapter(mDataItemListAdapter);

        //mGeneratorExecutor = new ScheduledThreadPoolExecutor(1);

        final AccelerometerListener accelerometerListener = (AccelerometerListener) this;


        ipAddress = (EditText) findViewById(R.id.ipField);
        dataReceived = (TextView) findViewById(R.id.dataReceived);
        messageId = (TextView) findViewById(R.id.messageId);
        clientId = (EditText) findViewById(R.id.clientId);


        chart = (LineChart) findViewById(R.id.chart);

        accelerationToggle = (ToggleButton) findViewById(R.id.accelerationToggle);
        mqttToggle = (ToggleButton) findViewById(R.id.mqttToggle);

        sampleRateSeek = (SeekBar) findViewById(R.id.sampleRateSeek);
        thresholdSeek = (SeekBar) findViewById(R.id.thresholdSeek);

        sampleMessage = new SampleMessage();

        mqttToggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    if(mqttHelper == null || ! mqttHelper.mqttAndroidClient.isConnected()) {
                        startMqtt();
                    } else {
                        Log.w("mqtt", "Already Connected");
                        mqttToggle.setChecked(true);
                    }
                } else {
                    closeMqtt();
                }
            }
        });


        accelerationToggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    AccelerometerManager.startListening(accelerometerListener);
                } else {
                    AccelerometerManager.stopListening();
                }
            }
        });

        sampleRateSeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
              @Override
              public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                  AccelerometerManager.setInterval(i*1000);
                  //AccelerometerManager.stopListening();
                  //AccelerometerManager.startListening(accelerometerListener);
              }
              @Override
              public void onStartTrackingTouch(SeekBar seekBar) {   }
              @Override
              public void onStopTrackingTouch(SeekBar seekBar) {   }
            }
        );

        thresholdSeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
             @Override
             public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                 AccelerometerManager.setThreshold(i);
             }
             @Override
             public void onStartTrackingTouch(SeekBar seekBar) {   }
             @Override
             public void onStopTrackingTouch(SeekBar seekBar) {   }
         }
        );


        mChart = new ChartHelper(chart);
        // fake init value
        mChart.addEntryToChart(0f,0f,0f,new Date());

    }

    @Override
    public void onResume() {
        super.onResume();
       /* mDataItemGeneratorFuture = mGeneratorExecutor.scheduleWithFixedDelay(
                new DataItemGenerator(), 1, 5, TimeUnit.SECONDS);
*/
        mStartActivityBtn.setEnabled(true);

        // Instantiates clients without member variables, as clients are inexpensive to create and
        // won't lose their listeners. (They are cached and shared between GoogleApi instances.)
        Wearable.getDataClient(this).addListener(this);
        Wearable.getMessageClient(this).addListener(this);
        Wearable.getCapabilityClient(this)
                .addListener(
                        this, Uri.parse("wear://"), CapabilityClient.FILTER_REACHABLE);


        if (AccelerometerManager.isSupported(this)) {
            AccelerometerManager.startListening(this);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        //mDataItemGeneratorFuture.cancel(true /* mayInterruptIfRunning */);

        Wearable.getDataClient(this).removeListener(this);
        Wearable.getMessageClient(this).removeListener(this);
        Wearable.getCapabilityClient(this).removeListener(this);
        //Check device supported Accelerometer sensor or not
        if (AccelerometerManager.isListening()) {

            //Start Accelerometer Listening
            AccelerometerManager.stopListening();

            Toast.makeText(this, "onStop Accelerometer Stopped", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

    }




    /**
     *
     * Communication with paired smartwatch
     */
    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {
        Log.w(TAG, "onDataChanged: " + dataEvents);

        /*for (DataEvent event : dataEvents) {
            if (event.getType() == DataEvent.TYPE_CHANGED) {
                mDataItemListAdapter.add(
                        new Event("DataItem Changed", event.getDataItem().toString()));
            } else if (event.getType() == DataEvent.TYPE_DELETED) {
                mDataItemListAdapter.add(
                        new Event("DataItem Deleted", event.getDataItem().toString()));
            }
        }*/
    }

    @Override
    public void onMessageReceived(final MessageEvent messageEvent) {
        String msg = "";
        try {
            msg = new String(messageEvent.getData(), "UTF-8");
            Log.w(TAG, "onMessageReceived() "+ messageEvent.getPath() + " " + msg);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        mDataItemListAdapter.add(new Event(messageEvent.getPath(), msg.substring(0,43) ));

        finalizeSampleMessage(msg);
    }

    @Override
    public void onCapabilityChanged(final CapabilityInfo capabilityInfo) {
        Log.w(TAG, "onCapabilityChanged: " + capabilityInfo);

        mDataItemListAdapter.add(new Event("onCapabilityChanged", capabilityInfo.toString()));
    }

    /**
     * Sets up UI components and their callback handlers.
     */
    private void setupViews() {
        mDataItemList = (ListView) findViewById(R.id.data_item_list);
        mStartActivityBtn = findViewById(R.id.start_wearable_activity);
    }



    /**
     * Sends an RPC to start a fullscreen Activity on the wearable.
     */
    public void onStartWearableActivityClick(View view) {
        Log.w(TAG, "Generating RPC");

        // Trigger an AsyncTask that will query for a list of connected nodes and send a
        // "start-activity" message to each connected node.
        new StartWearableActivityTask().execute();
    }



    @WorkerThread
    private void sendStartActivityMessage(String node) {

        Task<Integer> sendMessageTask =
                Wearable.getMessageClient(this).sendMessage(node, START_ACTIVITY_PATH, new byte[0]);

        try {
            // Block on a task and get the result synchronously (because this is on a background
            // thread).
            Integer result = Tasks.await(sendMessageTask);
            Log.w(TAG, "Message sent: " + result);

        } catch (ExecutionException exception) {
            Log.w(TAG, "Task failed: " + exception);

        } catch (InterruptedException exception) {
            Log.e(TAG, "Interrupt occurred: " + exception);
        }
    }



/*
private void sendPhoto(Asset asset) {

        PutDataMapRequest dataMap = PutDataMapRequest.create(IMAGE_PATH);
        dataMap.getDataMap().putAsset(IMAGE_KEY, asset);
        dataMap.getDataMap().putLong("time", new Date().getTime());
        PutDataRequest request = dataMap.asPutDataRequest();
        request.setUrgent();

        Task<DataItem> dataItemTask = Wearable.getDataClient(this).putDataItem(request);

        dataItemTask.addOnSuccessListener(new OnSuccessListener<DataItem>() {
            @Override
            public void onSuccess(DataItem dataItem) {
                Log.w(TAG, "Sending image was successful: " + dataItem);
            }
        });

    }
*/
    @WorkerThread
    private Collection<String> getNodes() {
        HashSet<String> results = new HashSet<>();

        Task<List<Node>> nodeListTask =
                Wearable.getNodeClient(getApplicationContext()).getConnectedNodes();

        try {
            // Block on a task and get the result synchronously (because this is on a background
            // thread).
            List<Node> nodes = Tasks.await(nodeListTask);

            for (Node node : nodes) {
                results.add(node.getId());

            }

        } catch (ExecutionException exception) {
            Log.e(TAG, "Task failed: " + exception);

        } catch (InterruptedException exception) {
            Log.e(TAG, "Interrupt occurred: " + exception);
        }

        return results;
    }

    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {

    }


    /**
     * A View Adapter for presenting the Event objects in a list
     */
    private static class DataItemAdapter extends ArrayAdapter<Event> {

        private final Context mContext;

        public DataItemAdapter(Context context, int unusedResource) {
            super(context, unusedResource);
            mContext = context;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder;
            if (convertView == null) {
                holder = new ViewHolder();
                LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(
                        Context.LAYOUT_INFLATER_SERVICE);
                convertView = inflater.inflate(android.R.layout.two_line_list_item, null);
                convertView.setTag(holder);
                holder.text1 = (TextView) convertView.findViewById(android.R.id.text1);
                holder.text2 = (TextView) convertView.findViewById(android.R.id.text2);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }
            Event event = getItem(position);
            holder.text1.setText(event.title);
            holder.text2.setText(event.text);
            return convertView;
        }

        private class ViewHolder {
            TextView text1;
            TextView text2;
        }
    }

    private class Event {

        String title;
        String text;

        public Event(String title, String text) {
            this.title = title;
            this.text = text;
        }
    }

    private class StartWearableActivityTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... args) {
            Collection<String> nodes = getNodes();
            for (String node : nodes) {
                sendStartActivityMessage(node);
            }
            return null;
        }
    }

    /**
     * Generates a DataItem based on an incrementing count.
     */
  /*  private class DataItemGenerator implements Runnable {

        private int count = 0;

        @Override
        public void run() {
            PutDataMapRequest putDataMapRequest = PutDataMapRequest.create(COUNT_PATH);
            putDataMapRequest.getDataMap().putInt(COUNT_KEY, count++);

            PutDataRequest request = putDataMapRequest.asPutDataRequest();
            request.setUrgent();

            Log.w(TAG, "Generating DataItem: " + request);

            Task<DataItem> dataItemTask =
                    Wearable.getDataClient(getApplicationContext()).putDataItem(request);

            try {
                // Block on a task and get the result synchronously (because this is on a background
                // thread).
                DataItem dataItem = Tasks.await(dataItemTask);

                Log.w(TAG, "DataItem saved: " + dataItem);

            } catch (ExecutionException exception) {
                Log.e(TAG, "Task failed: " + exception);

            } catch (InterruptedException exception) {
                Log.e(TAG, "Interrupt occurred: " + exception);
            }
        }
    }*/




    @Override
    public void onAccelerationChanged(float x, float y, float z, long time) {
        /*
        Date timestamp = new Date(time/1000000);
        float[] record = {x,y,z};
        mChart.addEntryToChart(record, timestamp);
        */
    }

    public void onShaking(float x, float y, float z, long time, long messageId) {
        Date timestamp = new Date(time);

        mChart.addEntryToChart(x,y,z, timestamp);

        // TODO: create separate helper
        sampleMessage.setValues(x,y,z, time, messageId, clientId.getText().toString());
    }

    public void finalizeSampleMessage(String msg) {
        if (mqttHelper != null && mqttHelper.mqttAndroidClient.isConnected()) {
            // publish sample
            if(msg != null) {
                mqttHelper.publishMessage(msg, null);
            } else {
                mqttHelper.publishMessage(sampleMessage.toString(), String.valueOf(sampleMessage.getId()));

                // display on UI
                messageId.setText(sdf.format(new Date(sampleMessage.getId())));

                // flush current sample
                sampleMessage.clear();
            }

        }
    }





    /**
     *  MQTT CONNECT
     */
    private void startMqtt() {

        mqttHelper = new MqttHelper(getApplicationContext(), ipAddress.getText().toString(), mqttToggle, clientId.getText().toString());

        mqttHelper.mqttAndroidClient.setCallback(new MqttCallbackExtended() {
            @Override
            public void connectComplete(boolean b, String s) {
                Log.w("connectComplete", "Connected: " + s);
                mqttToggle.setChecked(true);
                dataReceived.setText("MQTT: Connected");
            }

            @Override
            public void connectionLost(Throwable throwable) {
                Log.w("connectionLost", throwable);
                mqttToggle.setChecked(false);
            }

            @Override
            public void messageArrived(String topic, MqttMessage mqttMessage) throws Exception {
                Log.w("messageArrived", mqttMessage.toString());
                Toast.makeText(context, "messageArrived",Toast.LENGTH_LONG).show();
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {
                Log.w("deliveryComplete", iMqttDeliveryToken.toString());

            }
        });

    }

    private void closeMqtt() {
        if (mqttHelper != null && mqttHelper.mqttAndroidClient.isConnected()) {
            Log.w("mqtt", "Closing Connection");
            try {
                mqttHelper.mqttAndroidClient.disconnect();
                dataReceived.setText("MQTT: Disconnected");
            } catch (MqttException e) {
                e.printStackTrace();
            }
        }
    }


}