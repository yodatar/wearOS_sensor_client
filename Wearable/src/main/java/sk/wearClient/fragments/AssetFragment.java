/*
 * Copyright (C) 2015 The Android Open Source Project
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

package sk.wearClient.fragments;

import android.app.Fragment;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import sk.wearClient.R;


/**
 * A simple fragment that shows a (photo) asset received from the phone.
 */
public class AssetFragment extends Fragment {

    private TextView mTextView;
    private TextView messageId;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.asset_fragment, container, false);
        mTextView = view.findViewById(R.id.text);
        messageId = view.findViewById(R.id.messageId);

        return view;
    }

    public void setText(String s) {
        if(mTextView != null) {
            mTextView.setText(s);
        }
    }

    public void setMessageId(String id) {
        if(mTextView != null) {
            mTextView.setText(id);

        }
    }
}
