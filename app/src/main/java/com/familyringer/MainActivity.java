package com.familyringer;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.familyringer.databinding.ActivityMainBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import org.json.JSONArray;
import org.json.JSONException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private SessionManager session;
    private MemberAdapter memberAdapter;
    private AlertAdapter alertAdapter;
    private List<Member> members = new ArrayList<>();
    private List<String> alerts = new ArrayList<>();
    private ListenerRegistration membersListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.toolbar);

        session = new SessionManager(this);

        // First run → go to setup wizard
        if (!session.isSetupComplete()) {
            startActivity(new Intent(this, SetupWizardActivity.class));
            finish();
            return;
        }

        // Wait for anonymous auth before doing anything
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            FirebaseAuth.getInstance().signInAnonymously()
                .addOnSuccessListener(r -> init())
                .addOnFailureListener(e -> Toast.makeText(this,
                    "Auth failed: " + e.getMessage(), Toast.LENGTH_LONG).show());
        } else {
            init();
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(
                        new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 1);
            }
        }
    }

    private void init() {
        loadAlerts();

        if (session.isParent()) {
            showParentUI();
            listenToMembers();
        } else {
            showChildUI();
        }
    }

    // ── Parent UI ─────────────────────────────────────────────────────────────

    private void showParentUI() {
        binding.cardMembers.setVisibility(View.VISIBLE);
        binding.cardAlerts.setVisibility(View.VISIBLE);
        binding.cardChild.setVisibility(View.GONE);
        binding.toolbar.setSubtitle(session.getGroupName());

        memberAdapter = new MemberAdapter(members, member -> {
            member.selected = !member.selected;
            memberAdapter.notifyDataSetChanged();
        });
        binding.recyclerMembers.setLayoutManager(
            new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        binding.recyclerMembers.setAdapter(memberAdapter);

        alertAdapter = new AlertAdapter(alerts, this::sendAlert);
        binding.recyclerAlerts.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerAlerts.setAdapter(alertAdapter);
    }

    private void listenToMembers() {
        String groupId = session.getGroupId();
        membersListener = FirebaseFirestore.getInstance()
            .collection("groups").document(groupId).collection("members")
            .addSnapshotListener((snap, e) -> {
                if (e != null || snap == null) return;
                members.clear();
                String myUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
                for (var doc : snap.getDocuments()) {
                    // Don't show ourselves in the member list
                    if (doc.getId().equals(myUid)) continue;
                    members.add(new Member(
                        doc.getId(),
                        doc.getString("name"),
                        doc.getString("role"),
                        doc.getString("fcmToken")
                    ));
                }
                if (memberAdapter != null) memberAdapter.notifyDataSetChanged();
                binding.textNoMembers.setVisibility(members.isEmpty() ? View.VISIBLE : View.GONE);
            });
    }

    private void sendAlert(String message) {
        List<String> selectedIds = members.stream()
            .filter(m -> m.selected)
            .map(m -> m.uid)
            .collect(Collectors.toList());

        if (selectedIds.isEmpty()) {
            Toast.makeText(this, "Select at least one person first", Toast.LENGTH_SHORT).show();
            return;
        }

        binding.progressSend.setVisibility(View.VISIBLE);
        CloudFunctions.sendAlert(session.getGroupId(), selectedIds, message)
            .addOnSuccessListener(result -> {
                binding.progressSend.setVisibility(View.GONE);
                int sent = ((Number) result.get("sent")).intValue();
                Toast.makeText(this, "📣 Alert sent to " + sent + " device(s)!", Toast.LENGTH_SHORT).show();
            })
            .addOnFailureListener(e -> {
                binding.progressSend.setVisibility(View.GONE);
                Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
            });
    }

    // ── Child UI ──────────────────────────────────────────────────────────────

    private void showChildUI() {
        binding.cardMembers.setVisibility(View.GONE);
        binding.cardAlerts.setVisibility(View.GONE);
        binding.cardChild.setVisibility(View.VISIBLE);
        binding.textChildName.setText("👋 Hi, " + session.getName() + "!");
        binding.textChildGroup.setText("Group: " + session.getGroupName());
    }

    // ── Alerts ────────────────────────────────────────────────────────────────

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
        // Defaults
        alerts.add("🍽️ Dinner is ready!");
        alerts.add("🚗 Training time!");
        alerts.add("😴 Time for bed!");
        alerts.add("📚 Homework time!");
        alerts.add("🏠 Come home now!");
        saveAlerts();
    }

    private void saveAlerts() {
        JSONArray arr = new JSONArray();
        for (String a : alerts) arr.put(a);
        session.saveAlerts(arr.toString());
    }

    // ── Menu ──────────────────────────────────────────────────────────────────

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        }
        if (item.getItemId() == R.id.action_qr) {
            startActivity(new Intent(this, QrActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (membersListener != null) membersListener.remove();
    }
}
