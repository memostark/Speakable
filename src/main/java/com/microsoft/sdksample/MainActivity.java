//
// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license.
//
// Microsoft Cognitive Services (formerly Project Oxford): https://www.microsoft.com/cognitive-services
//
// Microsoft Cognitive Services (formerly Project Oxford) GitHub:
// https://github.com/Microsoft/Cognitive-Speech-TTS
//
// Copyright (c) Microsoft Corporation
// All rights reserved.
//
// MIT License:
// Permission is hereby granted, free of charge, to any person obtaining
// a copy of this software and associated documentation files (the
// "Software"), to deal in the Software without restriction, including
// without limitation the rights to use, copy, modify, merge, publish,
// distribute, sublicense, and/or sell copies of the Software, and to
// permit persons to whom the Software is furnished to do so, subject to
// the following conditions:
//
// The above copyright notice and this permission notice shall be
// included in all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED ""AS IS"", WITHOUT WARRANTY OF ANY KIND,
// EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
// MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
// NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
// LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
// OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
// WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
//

package com.microsoft.sdksample;

import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.projection.MediaProjectionManager;
import android.os.Bundle;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v4.app.NotificationCompat;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;


public class MainActivity extends AppCompatActivity {
    // Note: Sign up at http://www.projectoxford.ai for the client credentials.
    protected static final int REQUEST_CODE_SCREEN_CAPTURE = 100;
    static final int notificationId = 10;
    private static final String CHANNEL_ID = "process_text_channel";
    static final String NORMAL_SERVICE = "startService";
    static final String NORMAL_NO_PERM_SERVICE = "getPermissionService";
    private String TAG=this.getClass().getSimpleName();

    private CustomTTS tts;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (getString(R.string.api_key).startsWith("Please")) {
            new AlertDialog.Builder(this)
                    .setTitle(getString(R.string.add_subscription_key_tip_title))
                    .setMessage(getString(R.string.add_subscription_key_tip))
                    .setCancelable(false)
                    .show();
        } else {


            if(tts == null) {
                tts = new CustomTTS(MainActivity.this);
            }

            // Use a string for speech.
            //m_syn.SpeakToAudio(getString(R.string.tts_text));

            // Use SSML for speech.
            //String text = "<speak version=\"1.0\" xmlns=\"http://www.w3.org/2001/10/synthesis\" xmlns:mstts=\"http://www.w3.org/2001/mstts\" xml:lang=\"en-US\"><voice xml:lang=\"en-US\" name=\"Microsoft Server Speech Text to Speech Voice (en-US, ZiraRUS)\">You can also use SSML markup for text to speech.</voice></speak>";
            //m_syn.SpeakSSMLToAudio(text);


            findViewById(R.id.stop_btn).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    // TO DO
                }
            });

            Button playBtn = (Button) findViewById(R.id.play_btn);

            //Toast.makeText(MainActivity.this, "Edittext Toast1:" + text, Toast.LENGTH_LONG).show();

            playBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    EditText mEdit   = (EditText)findViewById(R.id.tts_ev);
                    final String text = mEdit.getText().toString();
                    tts.determineLanguage(text);
                }
            });

            findViewById(R.id.startBubble_btn).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    final MediaProjectionManager manager
                            = (MediaProjectionManager)getSystemService(Context.MEDIA_PROJECTION_SERVICE);
                    final Intent permissionIntent = manager.createScreenCaptureIntent();
                    startActivityForResult(permissionIntent, REQUEST_CODE_SCREEN_CAPTURE);
                    //startService(new Intent(MainActivity.this, ScreenTextService.class));

                }
            });
        }
    }

    @Override
    protected void onDestroy() {
        if(tts != null) tts.finishTTS();
        super.onDestroy();

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_SCREEN_CAPTURE) {
            if (resultCode == AppCompatActivity.RESULT_OK) {
                final Intent intent = new Intent(this, ScreenTextService.class);
                intent.setAction(NORMAL_SERVICE);
                intent.putExtra(ScreenTextService.EXTRA_RESULT_CODE, resultCode);
                intent.putExtras(data);
                startService(intent);
                createNotification();
                this.finish();
            }
        }
    }

    private void createNotification(){
        Intent intentNormalStart = new Intent(this, ScreenTextService.class);
        intentNormalStart.setAction(MainActivity.NORMAL_SERVICE);
        PendingIntent pendingIntentNormal = PendingIntent.getService(this, 0, intentNormalStart, 0);

        Intent intentHide = new Intent(this, Receiver.class);
        PendingIntent pendingIntentHide = PendingIntent.getBroadcast(this, (int) System.currentTimeMillis(), intentHide, PendingIntent.FLAG_CANCEL_CURRENT);


        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("My notification")
                .setContentText("Much longer text that cannot fit one line...")
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText("Much longer text that cannot fit one line..."))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setOngoing(true)
                .setContentIntent(pendingIntentNormal)
                .addAction(R.drawable.ic_close, getString(R.string.close_notification),pendingIntentHide);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);

        // notificationId is a unique int for each notification that you must define
        notificationManager.notify(notificationId, mBuilder.build());
    }
}
