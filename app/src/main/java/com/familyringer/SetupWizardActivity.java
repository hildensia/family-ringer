package com.familyringer;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.familyringer.databinding.ActivitySetupWizardBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.messaging.FirebaseMessaging;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;
import androidx.activity.result.ActivityResultLauncher;

public class SetupWizardActivity extends AppCompatActivity {

    private ActivitySetupWizardBinding binding;
    private SessionManager session;
    private String scannedGroupId = null;

    // QR scanner launcher
    private final ActivityResultLauncher<ScanOptions> qrLauncher =
        registerForActivityResult(new ScanContract(), result -> {
            if (result.getContents() != null) {
                scannedGroupId = result.getContents();
                binding.textScannedGroup.setText(getString(R.string.toast_group_scanned,
                        scannedGroupId.substring(0, 8)));
                binding.textScannedGroup.setVisibility(View.VISIBLE);
                binding.btnJoinConfirm.setVisibility(View.VISIBLE);
            }
        });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySetupWizardBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        session = new SessionManager(this);

        // Show step 1 — choose create or join
        showStep1();

        binding.btnCreate.setOnClickListener(v -> showCreateStep());
        binding.btnJoin.setOnClickListener(v -> showJoinStep());
        binding.btnScanQr.setOnClickListener(v -> scanQr());
        binding.btnCreateConfirm.setOnClickListener(v -> createGroup());
        binding.btnJoinConfirm.setOnClickListener(v -> joinGroup());
    }

    private void showStep1() {
        binding.layoutStep1.setVisibility(View.VISIBLE);
        binding.layoutCreate.setVisibility(View.GONE);
        binding.layoutJoin.setVisibility(View.GONE);
    }

    private void showCreateStep() {
        binding.layoutStep1.setVisibility(View.GONE);
        binding.layoutCreate.setVisibility(View.VISIBLE);
        binding.layoutJoin.setVisibility(View.GONE);
    }

    private void showJoinStep() {
        binding.layoutStep1.setVisibility(View.GONE);
        binding.layoutCreate.setVisibility(View.GONE);
        binding.layoutJoin.setVisibility(View.VISIBLE);
    }

    private void scanQr() {
        ScanOptions options = new ScanOptions();
        options.setPrompt(getString(R.string.scan_prompt));
        options.setBeepEnabled(true);
        options.setOrientationLocked(true);
        qrLauncher.launch(options);
    }

    private void createGroup() {
        String groupName = binding.editGroupName.getText().toString().trim();
        String creatorName = binding.editCreatorName.getText().toString().trim();

        if (groupName.isEmpty() || creatorName.isEmpty()) {
            Toast.makeText(this, getString(R.string.error_fill_fields), Toast.LENGTH_SHORT).show();
            return;
        }

        ensureAuthThen(() -> {
            setLoading(true);
            FirebaseMessaging.getInstance().getToken().addOnSuccessListener(fcmToken -> {
                CloudFunctions.createGroup(groupName, creatorName, fcmToken)
                    .addOnSuccessListener(groupId -> {
                        session.saveSession(groupId, groupName, "parent", creatorName);
                        setLoading(false);
                        // Go to QR display so other devices can join
                        Intent intent = new Intent(this, QrActivity.class);
                        intent.putExtra("first_setup", true);
                        startActivity(intent);
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        setLoading(false);
                        Toast.makeText(this, getString(R.string.error_generic, e.getMessage()), Toast.LENGTH_LONG).show();
                    });
            });
        });
    }

    private void joinGroup() {
        if (scannedGroupId == null) {
            Toast.makeText(this, getString(R.string.error_scan_first), Toast.LENGTH_SHORT).show();
            return;
        }

        String name = binding.editJoinName.getText().toString().trim();
        String role = binding.radioParent.isChecked() ? "parent" : "child";

        if (name.isEmpty()) {
            Toast.makeText(this, getString(R.string.error_enter_name), Toast.LENGTH_SHORT).show();
            return;
        }

        ensureAuthThen(() -> {
            setLoading(true);
            FirebaseMessaging.getInstance().getToken().addOnSuccessListener(fcmToken -> {
                CloudFunctions.joinGroup(scannedGroupId, name, role, fcmToken)
                    .addOnSuccessListener(result -> {
                        String groupName = (String) result.get("groupName");
                        session.saveSession(scannedGroupId, groupName, role, name);
                        setLoading(false);
                        startActivity(new Intent(this, MainActivity.class));
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        setLoading(false);
                        Toast.makeText(this, getString(R.string.error_generic, e.getMessage()), Toast.LENGTH_LONG).show();
                    });
            });
        });
    }

    private void ensureAuthThen(Runnable action) {
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            action.run();
        } else {
            FirebaseAuth.getInstance().signInAnonymously()
                .addOnSuccessListener(r -> action.run())
                .addOnFailureListener(e -> Toast.makeText(this,
                        getString(R.string.error_auth_failed, e.getMessage()),
                        Toast.LENGTH_LONG).show());
        }
    }

    private void setLoading(boolean loading) {
        binding.progressSetup.setVisibility(loading ? View.VISIBLE : View.GONE);
        binding.btnCreateConfirm.setEnabled(!loading);
        binding.btnJoinConfirm.setEnabled(!loading);
    }
}
