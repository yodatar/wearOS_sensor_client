/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package sk.wearClient;

import android.content.Intent;
import android.util.Log;

import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.WearableListenerService;

import sk.wearClient.helpers.SampleMessage;

/**
 * Listens to DataItems and Messages from the local node.
 */
public class DataLayerListenerService extends WearableListenerService {
    private static final String TAG = "DataLayerService";

    // default
    public static String CLIENT_ID = "smartwatch";

    //
    public static final String START_ACTIVITY = "/start-activity";
    public static final String SET_CLIENT_ID = "/set-client-id";

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {

    }

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        Log.w(TAG, "onMessageReceived: " + messageEvent);

        // Check to see if the message is to start an activity
        if (messageEvent.getPath().equals(START_ACTIVITY)) {
            Intent startIntent = new Intent(this, MainActivity.class);
            startIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(startIntent);
        }

        // Renaming node
        if (messageEvent.getPath().equals(SET_CLIENT_ID)) {
            Log.d(TAG, "Data Changed for SET_CLIENT_ID");

            try {
                SampleMessage.CLIENT_ID = messageEvent.getData().toString();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}