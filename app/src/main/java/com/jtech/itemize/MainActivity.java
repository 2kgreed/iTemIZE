package com.jtech.itemize;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaScannerConnection;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.jtech.itemize.model.Item;
import com.jtech.itemize.utils.BiometricHelper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private Button btnAddItem, btnGenerateReport;
    private ItemAdapter adapter;
    private List<Item> itemList = new ArrayList<>();
    private FirebaseFirestore db;

    private static final int REQUEST_PERMISSION_CODE = 1001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize Firestore
        db = FirebaseFirestore.getInstance();

        // Bind UI elements
        recyclerView = findViewById(R.id.recyclerView);
        btnAddItem = findViewById(R.id.btnAddItem);
        btnGenerateReport = findViewById(R.id.btnGenerateReport);

        // Configure RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ItemAdapter(itemList, item -> {
            if (item.isSignedOut()) {
                authenticateAndReturnItem(item);
            } else {
                authenticateAndSignOutItem(item);
            }
        });
        recyclerView.setAdapter(adapter);

        // Add Item button logic
        btnAddItem.setOnClickListener(v -> startActivity(new Intent(this, AddItemActivity.class)));

        // Generate Report button logic
        btnGenerateReport.setOnClickListener(v -> generateReport());

        // Load items from Firestore in real-time
        loadItems();

        // Request for storage permissions
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_PERMISSION_CODE);
        }
    }

    /**
     * Fetch items from Firestore and update RecyclerView in real-time.
     */
    private void loadItems() {
        db.collection("items").addSnapshotListener((snapshot, e) -> {
            if (e != null) {
                Log.e("MainActivity", "Error listening to changes", e);
                return;
            }

            if (snapshot != null) {
                itemList.clear();
                for (com.google.firebase.firestore.DocumentSnapshot doc : snapshot.getDocuments()) {
                    Item item = doc.toObject(Item.class);
                    if (item != null) {
                        itemList.add(item);
                    }
                }
                adapter.notifyDataSetChanged();
            }
        });
    }

    private void addSampleItem() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Sample Item Data
        Map<String, Object> sampleItem = new HashMap<>();
        sampleItem.put("id", "1234");
        sampleItem.put("name", "Laptop");
        sampleItem.put("isSignedOut", true);

        // Sign-outs array
        List<Map<String, String>> signOuts = new ArrayList<>();
        Map<String, String> signOut1 = new HashMap<>();
        signOut1.put("signedOutBy", "John Doe");
        signOut1.put("signedOutFingerprint", "fingerprint_123");
        signOut1.put("signedOutAt", "2024-11-20T10:00:00Z");
        signOuts.add(signOut1);

        Map<String, String> signOut2 = new HashMap<>();
        signOut2.put("signedOutBy", "Jane Smith");
        signOut2.put("signedOutFingerprint", "fingerprint_456");
        signOut2.put("signedOutAt", "2024-11-21T08:30:00Z");
        signOuts.add(signOut2);

        sampleItem.put("signOuts", signOuts);
        sampleItem.put("returnedAt", "2024-11-21T10:00:00Z");

        // Add to Firestore
        db.collection("items").document("1234").set(sampleItem)
                .addOnSuccessListener(aVoid -> Log.d("Firestore", "Document successfully written!"))
                .addOnFailureListener(e -> Log.w("Firestore", "Error writing document", e));
    }


    /**
     * Authenticate and sign out an item.
     */
    private void authenticateAndSignOutItem(Item item) {
        // Prompt for user's name
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Enter Your Name");

        final EditText input = new EditText(this);
        input.setHint("Your Name");
        builder.setView(input);

        builder.setPositiveButton("OK", (dialog, which) -> {
            String userName = input.getText().toString().trim();

            if (userName.isEmpty()) {
                Toast.makeText(this, "Name is required to sign out an item!", Toast.LENGTH_SHORT).show();
            } else {
                BiometricHelper biometricHelper = new BiometricHelper(this, new BiometricHelper.AuthenticationCallback() {
                    @Override
                    public void onAuthenticationSuccess() {
                        String currentTime = getCurrentTimestamp(); // Get current timestamp
                        item.setSignedOut(true);
                        item.setSignedOutBy(userName);
                        item.setSignedOutAt(currentTime);

                        db.collection("items").document(item.getId()).set(item)
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(MainActivity.this, item.getName() + " signed out by " + userName, Toast.LENGTH_SHORT).show();
                                    loadItems(); // Refresh the list
                                })
                                .addOnFailureListener(e -> Toast.makeText(MainActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                    }

                    @Override
                    public void onAuthenticationFailure(String errorMessage) {
                        Toast.makeText(MainActivity.this, "Authentication failed: " + errorMessage, Toast.LENGTH_SHORT).show();
                    }
                });

                biometricHelper.authenticate("Sign Out Item", "Authenticate to sign out " + item.getName());
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    /**
     * Authenticate and return an item.
     */
    private void authenticateAndReturnItem(Item item) {
        BiometricHelper biometricHelper = new BiometricHelper(this, new BiometricHelper.AuthenticationCallback() {
            @Override
            public void onAuthenticationSuccess() {
                String currentTime = getCurrentTimestamp(); // Get current timestamp
                item.setSignedOut(false);
                item.setReturnedAt(currentTime);

                db.collection("items").document(item.getId()).set(item)
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(MainActivity.this, item.getName() + " returned successfully!", Toast.LENGTH_SHORT).show();
                            loadItems(); // Refresh the list
                        })
                        .addOnFailureListener(e -> Toast.makeText(MainActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onAuthenticationFailure(String errorMessage) {
                Toast.makeText(MainActivity.this, "Authentication failed: " + errorMessage, Toast.LENGTH_SHORT).show();
            }
        });

        biometricHelper.authenticate("Return Item", "Authenticate to return " + item.getName());
    }

    /**
     * Generate a CSV report of item activities.
     */
    private void generateReport() {
        db.collection("items").get()
                .addOnSuccessListener(snapshot -> {
                    StringBuilder csvContent = new StringBuilder();
                    csvContent.append("Item Name, Signed Out By, Signed Out At, Returned At\n");

                    for (com.google.firebase.firestore.DocumentSnapshot doc : snapshot.getDocuments()) {
                        Item item = doc.toObject(Item.class);
                        if (item != null) {
                            csvContent.append(item.getName()).append(",");
                            csvContent.append(item.getSignedOutBy() != null ? item.getSignedOutBy() : "N/A").append(",");
                            csvContent.append(item.getSignedOutAt() != null ? item.getSignedOutAt() : "N/A").append(",");
                            csvContent.append(item.getReturnedAt() != null ? item.getReturnedAt() : "N/A").append("\n");
                        }
                    }

                    saveCsvToDevice(csvContent.toString());
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Error fetching data: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    /**
     * Save the CSV content to a file on the device's Documents directory.
     */
    private void saveCsvToDevice(String csvData) {
        try {
            String fileName = "ItemReport_" + System.currentTimeMillis() + ".csv";
            File documentsDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "ItemReports");
            if (!documentsDir.exists()) {
                documentsDir.mkdirs();
            }

            File file = new File(documentsDir, fileName);
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(csvData.getBytes());
            fos.close();

            // Notify MediaStore
            MediaScannerConnection.scanFile(this, new String[]{file.getAbsolutePath()}, null, (path, uri) -> {
                Log.d("MainActivity", "File is now visible in media store: " + uri);
            });

            Toast.makeText(this, "Report saved: " + file.getAbsolutePath(), Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            Toast.makeText(this, "Error saving report: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Get the current timestamp in a compatible format.
     */
    public static String getCurrentTimestamp() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault());
        return sdf.format(new Date());
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permission granted", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
