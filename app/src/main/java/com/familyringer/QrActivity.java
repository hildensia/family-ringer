package com.familyringer;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;
import com.familyringer.databinding.ActivityQrBinding;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;

/**
 * Shows a QR code containing the groupId.
 * Other devices scan this to join the group.
 */
public class QrActivity extends AppCompatActivity {

    private ActivityQrBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityQrBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        SessionManager session = new SessionManager(this);
        String groupId = session.getGroupId();
        boolean firstSetup = getIntent().getBooleanExtra("first_setup", false);

        if (firstSetup) {
            binding.textQrTitle.setText("Group created! 🎉");
            binding.textQrSubtitle.setText("Scan this QR code on each family member's phone to join \"" + session.getGroupName() + "\"");
            binding.btnDone.setText("Open Family Ringer →");
        } else {
            binding.textQrTitle.setText("Invite to group");
            binding.textQrSubtitle.setText("Scan this on another device to join \"" + session.getGroupName() + "\"");
            binding.btnDone.setText("Done");
        }

        binding.textGroupId.setText("Group: " + groupId);
        generateQr(groupId);

        binding.btnDone.setOnClickListener(v -> {
            if (firstSetup) {
                startActivity(new android.content.Intent(this, MainActivity.class));
            }
            finish();
        });
    }

    private void generateQr(String content) {
        try {
            int size = 600;
            BitMatrix matrix = new MultiFormatWriter().encode(
                content, BarcodeFormat.QR_CODE, size, size);

            Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565);
            for (int x = 0; x < size; x++) {
                for (int y = 0; y < size; y++) {
                    bitmap.setPixel(x, y, matrix.get(x, y) ? 0xFF000000 : 0xFFFFFFFF);
                }
            }
            binding.imageQr.setImageBitmap(bitmap);
        } catch (WriterException e) {
            binding.textQrSubtitle.setText("Error generating QR code");
        }
    }
}
