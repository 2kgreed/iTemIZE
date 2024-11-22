package com.jtech.itemize;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;
import com.jtech.itemize.model.Item;

public class AddItemActivity extends AppCompatActivity {

    private EditText editTextItemName; // Input field for item name
    private Button btnSave;            // Button to save item
    private FirebaseFirestore db;      // Firestore instance

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_item);

        // Initialize Firestore
        db = FirebaseFirestore.getInstance();

        // Bind UI elements
        editTextItemName = findViewById(R.id.editTextItemName);
        btnSave = findViewById(R.id.btnSave);

        // Save button click listener
        btnSave.setOnClickListener(v -> {
            String itemName = editTextItemName.getText().toString().trim();

            // Validate input
            if (!itemName.isEmpty()) {
                // Generate a unique ID for the new item
                String itemId = db.collection("items").document().getId();

                // Create a new Item object
                Item newItem = new Item(itemId, itemName);

                // Save the item to Firestore
                db.collection("items").document(itemId).set(newItem)
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(this, "Item added successfully!", Toast.LENGTH_SHORT).show();
                            finish(); // Return to the previous screen
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        });
            } else {
                // Show error if the item name is empty
                Toast.makeText(this, "Please enter an item name", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
