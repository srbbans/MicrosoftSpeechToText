package com.example.voicerecongnition;

import static android.Manifest.permission.INTERNET;
import static android.Manifest.permission.READ_EXTERNAL_STORAGE;
import static android.Manifest.permission.RECORD_AUDIO;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.microsoft.cognitiveservices.speech.CancellationDetails;
import com.microsoft.cognitiveservices.speech.ResultReason;
import com.microsoft.cognitiveservices.speech.SpeechConfig;
import com.microsoft.cognitiveservices.speech.SpeechRecognitionResult;
import com.microsoft.cognitiveservices.speech.SpeechRecognizer;
import com.microsoft.cognitiveservices.speech.audio.AudioConfig;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class MainActivity extends AppCompatActivity {

    // Replace below with your own subscription key
    private static final String SpeechSubscriptionKey = "yourKeyHere";
    // Replace below with your own service region (e.g., "westus").
    private static final String SpeechRegion = "centralindia";

    private View btnSpeak;
    private TextView output;

    private MicrophoneStream microphoneStream;
    private SpeechConfig speechConfig;

    private static final ExecutorService s_executorService;

    static {
        s_executorService = Executors.newCachedThreadPool();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        btnSpeak = findViewById(R.id.btnSpeak);
        output = findViewById(R.id.tvOutput);

        setupPermissions();

        // if config is not get succeeded, don't set the listener.
        if (!setupConfig()) return;

        btnSpeak.setOnClickListener(view -> {
            final String logTag = "VoiceProcessDemo";

            disableButtons();

            clearTextBox();

            try {
                // final AudioConfig audioInput = AudioConfig.fromDefaultMicrophoneInput();
                final AudioConfig audioInput = AudioConfig.fromStreamInput(createMicrophoneStream());
                final SpeechRecognizer reco = new SpeechRecognizer(speechConfig, audioInput);

                final Future<SpeechRecognitionResult> task = reco.recognizeOnceAsync();
                setOnTaskCompletedListener(task, result -> {
                    String resultText = result.getText();
                    if (result.getReason() != ResultReason.RecognizedSpeech) {
                        String errorDetails = (result.getReason() == ResultReason.Canceled) ? CancellationDetails.fromResult(result).getErrorDetails() : "";
                        resultText = "Recognition failed with " + result.getReason() + ". Did you enter your subscription?" + System.lineSeparator() + errorDetails;
                    }
                    reco.close();
                    Log.i(logTag, "Recognizer returned: " + resultText);
                    setRecognizedText(resultText);
                    enableButtons();
                });
            } catch (Exception ex) {
                ex.printStackTrace();
                displayException(ex);
            }
        });
    }

    private MicrophoneStream createMicrophoneStream() {
        if (microphoneStream != null) {
            microphoneStream.close();
            microphoneStream = null;
        }
        microphoneStream = new MicrophoneStream();
        return microphoneStream;
    }


    private void setupPermissions() {
        try {
            // a unique number within the application to allow
            // correlating permission request responses with the request.
            int permissionRequestId = 5;
            // Request permissions needed for speech recognition
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{RECORD_AUDIO, INTERNET, READ_EXTERNAL_STORAGE}, permissionRequestId);
        } catch (Exception ex) {
            String excpMessage = "Could not initialize: " + ex.toString();
            Log.e("SpeechSDK", excpMessage);
            output.setText(excpMessage);
        }
    }

    private boolean setupConfig() {
        // create config
        try {
            speechConfig = SpeechConfig.fromSubscription(SpeechSubscriptionKey, SpeechRegion);
            return true;
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
            displayException(ex);
            return false;
        }
    }

    private void displayException(Exception ex) {
        String text = ex.getMessage() + System.lineSeparator() + TextUtils.join(System.lineSeparator(), ex.getStackTrace());
        output.setText(text);
    }

    private void clearTextBox() {
        AppendTextLine("Listening...");
    }

    private void setRecognizedText(final String s) {
        if (s == null) return;
        // Process your text here


        AppendTextLine(s);
    }

    private void AppendTextLine(final String s) {
        MainActivity.this.runOnUiThread(() -> output.setText(s));
    }

    private void disableButtons() {
        runOnUiThread(() -> btnSpeak.setEnabled(false));
    }

    private void enableButtons() {
        runOnUiThread(() -> btnSpeak.setEnabled(true));
    }

    private <T> void setOnTaskCompletedListener(Future<T> task, OnTaskCompletedListener<T> listener) {
        s_executorService.submit(() -> {
            T result = task.get();
            listener.onCompleted(result);
            return null;
        });
    }

    private interface OnTaskCompletedListener<T> {
        void onCompleted(T taskResult);
    }

}