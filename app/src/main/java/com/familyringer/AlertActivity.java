package com.familyringer;

import android.content.Intent;
import android.os.Bundle;
import android.view.WindowManager;
import androidx.appcompat.app.AppCompatActivity;
import com.familyringer.databinding.ActivityAlertBinding;

public class AlertActivity extends AppCompatActivity {

    private ActivityAlertBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Show over lock screen and turn on screen
        getWindow().addFlags(
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON |
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD |
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
        );

        binding = ActivityAlertBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        String message = getIntent().getStringExtra("message");
        if (message != null) {
            binding.textAlertMessage.setText(message);
        }

        binding.btnDismiss.setOnClickListener(v -> {
            // Stop alarm sound
            stopService(new Intent(this, AlarmService.class));
            finish();
        });
    }

    @Override
    public void onBackPressed() {
        // Prevent dismissing with back button — must tap Dismiss
    }
}
