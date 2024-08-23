package com.example.piecemealapplication;

import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import android.icu.text.DecimalFormat;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.HorizontalScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.ContextCompat;
import java.util.LinkedList;
import java.util.Queue;

import be.tarsos.dsp.*;
import be.tarsos.dsp.io.android.AudioDispatcherFactory;
import be.tarsos.dsp.pitch.PitchDetectionHandler;
import be.tarsos.dsp.pitch.PitchDetectionResult;
import be.tarsos.dsp.pitch.PitchProcessor;

public class MainActivity extends AppCompatActivity {
    HorizontalScrollView noteLayout;
    TextView currentHz, targetHz, targetNote;
    DecimalFormat df2;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        //create the launcher that will handle the response for a permission request
        ActivityResultLauncher<String> requestPermissionLauncher =
                registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                    if (!isGranted)
                        Toast.makeText(getApplicationContext(), "Need to accept permission for app to work correctly.", Toast.LENGTH_SHORT).show();
                    else {
                        realOnCreate();
                    }
                });

        //check if permission is already obtained
        if (ContextCompat.checkSelfPermission(
                this, "android.permission.RECORD_AUDIO") ==
                PackageManager.PERMISSION_GRANTED) {
            realOnCreate();
        } else { //ask the user to grant permission
            // You can directly ask for the permission.
            // The registered ActivityResultCallback gets the result of this request.
            requestPermissionLauncher.launch("android.permission.RECORD_AUDIO");
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    public void realOnCreate() {
        //get local resources from xml into memory
        final String[] tuningValues = getResources().getStringArray(R.array.tuningVals);
        String[] tuningNames = getResources().getStringArray(R.array.tunings);

        // Disable Scrolling for HorizontalScrollView by setting up an OnTouchListener to do nothing
        noteLayout = findViewById(R.id.notes);
        noteLayout.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return true;
            }
        });

        //inits
        df2 = new DecimalFormat();
        df2.setMaximumFractionDigits(2);
        df2.setMinimumFractionDigits(2);
        Queue<Double> lastNotes = new LinkedList<>();

        AudioDispatcher dispatcher = AudioDispatcherFactory.fromDefaultMicrophone(22050, 1024, 0);
        dispatcher.addAudioProcessor(new PitchProcessor(PitchProcessor.PitchEstimationAlgorithm.FFT_YIN, 22050.0f, 1024, new PitchDetectionHandler() {
            public void handlePitch(PitchDetectionResult pitchDetectionResult, AudioEvent audioEvent) {
                final double doublePitch = (double)pitchDetectionResult.getPitch();
                MainActivity.this.runOnUiThread(new Runnable() {
                    public void run() {
                        if (doublePitch != -1.0d) {
                            lastNotes.add(doublePitch);
                            double lastNotesAvg = getLastNotesAvg();

                            if(checkNotesWithinAverage(1)){
                                MainActivity.this.scrollToNote(lastNotesAvg);
                            }

                            if(lastNotes.size() >= 4) {
                                lastNotes.remove();
                            }
                        }
                    }

                    public boolean checkNotesWithinAverage(double range){
                        for(Double pitch: lastNotes){
                            if(Math.abs(getLastNotesAvg()-pitch) > range)
                            {
                                return false;
                            }
                        }
                        return true;
                    }

                    public double getLastNotesAvg(){
                        int count = 0;
                        double avg = 0.0d;
                        for(Double pitch: lastNotes){
                            avg+=pitch;
                            count++;
                        }
                        return avg/count;
                    }
                });
            }
        }));
        new Thread(dispatcher, "Audio Thread").start();

        currentHz = findViewById(R.id.currentHz);
        targetHz = findViewById(R.id.targetHz);
        targetNote = findViewById(R.id.targetNote);

        //create objects
        TextView tuning = findViewById(R.id.tuning);
        Spinner spinner = findViewById(R.id.spinner);

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                tuning.setText(tuningValues[position]);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        spinner.setAdapter(new ArrayAdapter<>(this, R.layout.spinner_item, tuningNames));
    }

    public void scrollToNote(double hz) {
        //set current hz
        currentHz.setText(df2.format(hz) + " Hz");

        //calculate where to scroll to
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int offset = displayMetrics.widthPixels / 2;

        double notesAway = 12 * (Math.log(hz / 16.35) / Math.log(2));

        setCurrentHzValues(notesAway);

        //dp = dp from C0 + dp of a letter * notesAway in one octave
        double dp = 700 + 200 * (notesAway % 12);
        int px = (int) (dp * displayMetrics.density);
        noteLayout.smoothScrollTo(px - offset, 0);
    }

    public void setCurrentHzValues(double notesAway) {
        double betweenNotesAway = notesAway - ((int) notesAway);

        //set the target notes int
        int targetNoteInt;
        if (betweenNotesAway > .5)
            targetNoteInt = ((int) notesAway) + 1;
        else
            targetNoteInt = ((int) notesAway);

        //find target Hz based on equation
        double targetHzDouble = 16.35 * Math.pow(2, (double) targetNoteInt / 12);

        //find target Note based on target int
        String targetHzString;
        switch (targetNoteInt % 12) {
            default:
                targetHzString = "E";
                break;
            case 0:
                targetHzString = "C";
                break;
            case 1:
                targetHzString = "D♭";
                break;
            case 2:
                targetHzString = "D";
                break;
            case 3:
                targetHzString = "E♭";
                break;
            case 4:
                targetHzString = "E";
                break;
            case 5:
                targetHzString = "F";
                break;
            case 6:
                targetHzString = "G♭";
                break;
            case 7:
                targetHzString = "G";
                break;
            case 8:
                targetHzString = "A♭";
                break;
            case 9:
                targetHzString = "A";
                break;
            case 10:
                targetHzString = "B♭";
                break;
            case 11:
                targetHzString = "B";
                break;
        }

        //set targetHz
        targetHz.setText(df2.format(targetHzDouble) + " Hz");
        targetNote.setText(targetHzString);
    }
}