package com.smartxplorer.bestsystemlottery;

import static com.smartxplorer.bestsystemlottery.util.Utils.regexDataScan;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.media.SoundPool;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import com.journeyapps.barcodescanner.DecoratedBarcodeView;
import com.journeyapps.barcodescanner.Size;
import com.journeyapps.barcodescanner.camera.CameraSettings;

import java.util.Map;

public class ScanActivity extends AppCompatActivity implements View.OnClickListener {

    private DecoratedBarcodeView barcodeView;
    private SoundPool soundPool;
    private int beepSound;
    private ImageView btnFlash;
    private View laser;
    boolean flash = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_scan);

        barcodeView = findViewById(R.id.barcodeView);
        barcodeView.setStatusText(getString(R.string.statusTextBarcodeView));

        laser = findViewById(R.id.laser);

        btnFlash = findViewById(R.id.btnFlash);
        btnFlash.setOnClickListener(this);

        soundPool = new SoundPool.Builder().setMaxStreams(1).build();
        beepSound = soundPool.load(this, R.raw.beep, 1);

        ObjectAnimator animator = ObjectAnimator.ofFloat(laser, "translationY", -203f, 203f);
        animator.setDuration(1200);
        animator.setRepeatCount(ValueAnimator.INFINITE);
        animator.setRepeatMode(ValueAnimator.REVERSE);
        animator.start();

        setupScanner();
    }
    private void setupScanner() {

        barcodeView.decodeContinuous(result -> {

            if (result.getText() != null) {
                barcodeView.pause(); // stop scan
                // 🔥 BEEP
                soundPool.play(beepSound, 1, 1, 0, 0, 1);
                // 🔥 TRAITEMENT
                handleScan(result.getText());
            }
        });

        // 🔥 AUTO FOCUS RAPIDE
        CameraSettings settings = new CameraSettings();
        settings.setAutoFocusEnabled(true);
        settings.setContinuousFocusEnabled(true);

        barcodeView.getBarcodeView().setCameraSettings(settings);

        // 🔥 FPS + RAPIDITÉ
        barcodeView.getBarcodeView().setFramingRectSize(new Size(400, 400));
    }
    private void handleScan(String rawData) {

        // 🔥 EXTRACTION TICKET + DATE
        Map<String, String> data = regexDataScan(rawData);

        String ticket = data.get("ticket");
        String date = data.get("date");

//        Toast.makeText(getApplicationContext(), "Ticke " + ticket + " \nDate " + date, Toast.LENGTH_LONG).show();

        Intent intent = new Intent();
        intent.putExtra("ticket", ticket);
        intent.putExtra("date", date);

        setResult(RESULT_OK, intent);

        finish();

    }

    // 🔥 FLASH
    public void toggleFlash(View v) {
        flash = !flash;
//        barcodeView.setTorch(flash);
    }

    @Override
    protected void onResume() {
        super.onResume();
        barcodeView.resume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        barcodeView.pause();
    }

    @Override
    public void onClick(View v) {
        flash = !flash;
        if (flash) {
            barcodeView.setTorchOn();
            btnFlash.setImageResource(R.drawable.baseline_flash_on_24);
        } else {
            barcodeView.setTorchOff();
            btnFlash.setImageResource(R.drawable.baseline_flash_off_24);
        }
    }
}