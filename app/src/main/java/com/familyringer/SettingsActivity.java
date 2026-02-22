package com.familyringer;

import android.content.Intent;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.SeekBar;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.familyringer.databinding.ActivitySettingsBinding;
import org.json.JSONArray;
import org.json.JSONException;
import java.util.ArrayList;
import java.util.List;

public class SettingsActivity extends AppCompatActivity {

    private ActivitySettingsBinding binding;
    private SessionManager session;
    private List<String> alerts = new ArrayList<>();
    private AlertEditAdapter alertEditAdapter;
    private static final int RINGTONE_PICKER_REQUEST = 123;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySettingsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Settings");
        }

        session = new SessionManager(this);

        // Info
        binding.textGroupName.setText("Group: " + session.getGroupName());
        binding.textRole.setText("Role: " + session.getRole());
        binding.textName.setText("Name: " + session.getName());

        // Alert editor — parents only
        if (session.isParent()) {
            binding.cardAlerts.setVisibility(View.VISIBLE);
            loadAlerts();
            setupAlertEditor();
        } else {
            binding.cardAlerts.setVisibility(View.GONE);
        }

        // Sound + volume + duration — all users
        setupSoundPicker();
        setupVolume();
        setupDuration();

        binding.btnLeaveGroup.setOnClickListener(v -> {
            session.clear();
            Intent intent = new Intent(this, SetupWizardActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });
    }

    // ── Alert messages (parent only) ─────────────────────────────────────────

    private void loadAlerts() {
        alerts.clear();
        String json = session.getAlertsJson();
        if (json != null) {
            try {
                JSONArray arr = new JSONArray(json);
                for (int i = 0; i < arr.length(); i++) alerts.add(arr.getString(i));
                return;
            } catch (JSONException ignored) {}
        }
        alerts.add("🍽️ Dinner is ready!");
        alerts.add("🚗 Training time!");
        alerts.add("😴 Time for bed!");
        alerts.add("📚 Homework time!");
        alerts.add("🏠 Come home now!");
    }

    private void setupAlertEditor() {
        alertEditAdapter = new AlertEditAdapter(alerts, pos -> {
            alerts.remove(pos);
            alertEditAdapter.notifyItemRemoved(pos);
            saveAlerts();
        });
        binding.recyclerAlerts.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerAlerts.setAdapter(alertEditAdapter);

        binding.btnAddAlert.setOnClickListener(v -> {
            String text = binding.editNewAlert.getText().toString().trim();
            if (text.isEmpty()) return;
            alerts.add(text);
            alertEditAdapter.notifyItemInserted(alerts.size() - 1);
            binding.editNewAlert.setText("");
            saveAlerts();
        });
    }

    private void saveAlerts() {
        JSONArray arr = new JSONArray();
        for (String a : alerts) arr.put(a);
        session.saveAlerts(arr.toString());
    }

    // ── Sound picker (all users) ──────────────────────────────────────────────

    private void setupSoundPicker() {
        String savedUri = session.getAlertSoundUri();
        updateSoundLabel(savedUri);

        binding.btnPickSound.setOnClickListener(v -> {
            Intent intent = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION);
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Choose alert sound");
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false);
            String current = session.getAlertSoundUri();
            if (current != null) {
                intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, Uri.parse(current));
            } else {
                // Pre-select the default notification sound
                intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI,
                        RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION));
            }
            startActivityForResult(intent, RINGTONE_PICKER_REQUEST);
        });

        binding.btnResetSound.setOnClickListener(v -> {
            session.saveAlertSoundUri(null);
            updateSoundLabel(null);
            Toast.makeText(this, "Reset to default notification sound", Toast.LENGTH_SHORT).show();
        });
    }

    private void updateSoundLabel(String uri) {
        if (uri != null) {
            try {
                Ringtone ringtone = RingtoneManager.getRingtone(this, Uri.parse(uri));
                binding.textCurrentSound.setText("🔔 " + ringtone.getTitle(this));
            } catch (Exception e) {
                binding.textCurrentSound.setText("🔔 Custom sound");
            }
        } else {
            // Show default notification sound name
            try {
                Uri defaultUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                Ringtone ringtone = RingtoneManager.getRingtone(this, defaultUri);
                binding.textCurrentSound.setText("🔔 " + ringtone.getTitle(this) + " (default)");
            } catch (Exception e) {
                binding.textCurrentSound.setText("🔔 Default notification sound");
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RINGTONE_PICKER_REQUEST && resultCode == RESULT_OK && data != null) {
            Uri uri = data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
            if (uri != null) {
                session.saveAlertSoundUri(uri.toString());
                updateSoundLabel(uri.toString());
                Toast.makeText(this, "Sound updated!", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // ── Volume (all users) ────────────────────────────────────────────────────

    private void setupVolume() {
        int volume = session.getAlertVolume();
        binding.seekbarVolume.setMax(100);
        binding.seekbarVolume.setProgress(volume);
        binding.textVolumeValue.setText(volume + "%");

        binding.seekbarVolume.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                // Minimum 10% so it's always audible
                int clamped = Math.max(progress, 10);
                binding.textVolumeValue.setText(clamped + "%");
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                int clamped = Math.max(seekBar.getProgress(), 10);
                session.saveAlertVolume(clamped);
                Toast.makeText(SettingsActivity.this,
                        "Volume set to " + clamped + "%", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ── Duration (all users) ──────────────────────────────────────────────────

    private void setupDuration() {
        int duration = session.getAlertDuration();

        // Map duration seconds to spinner position
        // 0 = don't stop, 15, 30, 60, 120 seconds
        int[] durations = {0, 15, 30, 60, 120};
        String[] labels = {"Until dismissed", "15 seconds", "30 seconds", "1 minute", "2 minutes"};

        android.widget.ArrayAdapter<String> adapter = new android.widget.ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, labels);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerDuration.setAdapter(adapter);

        // Select current value
        int selectedIndex = 0;
        for (int i = 0; i < durations.length; i++) {
            if (durations[i] == duration) { selectedIndex = i; break; }
        }
        binding.spinnerDuration.setSelection(selectedIndex);

        binding.spinnerDuration.setOnItemSelectedListener(
                new android.widget.AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(android.widget.AdapterView<?> parent,
                                               View view, int position, long id) {
                        session.saveAlertDuration(durations[position]);
                    }
                    @Override
                    public void onNothingSelected(android.widget.AdapterView<?> parent) {}
                });
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}