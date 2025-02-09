package com.nimeshkadecha.drkishan;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.FirebaseApp;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.json.JSONArray;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class allinfo extends AppCompatActivity {

	private DatabaseReference reference;
	private RecyclerView recyclerView;
	private ProductDataAdapter adapter;
	private List<String> productDates, productMessages;
	private String productName, stage, level, mainDate, userName;
	private int interval;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		EdgeToEdge.enable(this);
		setContentView(R.layout.activity_all_info);

		// Get Intent Data
		productName = getIntent().getStringExtra("productName");
		userName = getIntent().getStringExtra("userName");
		stage = getIntent().getStringExtra("stage");
		level = getIntent().getStringExtra("level");
		mainDate = getIntent().getStringExtra("date");

		interval =Integer.parseInt(Objects.requireNonNull(getIntent().getStringExtra("days")));

		FirebaseApp.initializeApp(this);
		reference = FirebaseDatabase.getInstance().getReference(userName).child(productName).child(level).child(stage);

		// Setup RecyclerView
		recyclerView = findViewById(R.id.ProductListWithInfo);
		recyclerView.setLayoutManager(new LinearLayoutManager(this));
		productDates = new ArrayList<>();
		productMessages = new ArrayList<>();
		adapter = new ProductDataAdapter(allinfo.this,productDates, productMessages,reference,interval);
		recyclerView.setAdapter(adapter);

		fetchProducts();

		// Set Click Listener for Add Button
		Button btnAdd = findViewById(R.id.button);
		btnAdd.setOnClickListener(v -> showAddProductDialog());

		// Copy Button Functionality
		Button btnCopy = findViewById(R.id.button2_copy);
		btnCopy.setOnClickListener(v -> showCopyDialog());

	}

	private void showCopyDialog() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle("Customize Copy Format");

		// Inflate custom layout
		View view = LayoutInflater.from(this).inflate(R.layout.dialog_copy_format, null);
		EditText etHeader = view.findViewById(R.id.etHeader);
		EditText etFooter = view.findViewById(R.id.etFooter);

		// ✅ Load saved Header & Footer from SharedPreferences
		etHeader.setText(getSavedText("savedHeader"));
		etFooter.setText(getSavedText("savedFooter"));

		builder.setView(view);

		// ✅ Copy Button (Save & Copy)
		builder.setPositiveButton("Copy", (dialog, which) -> {
			String header = etHeader.getText().toString().trim();
			String footer = etFooter.getText().toString().trim();

			// ✅ Save Header & Footer
			saveText("savedHeader", header);
			saveText("savedFooter", footer);

			// ✅ Copy Data
			copyDataToClipboard(header, footer);
		});

		// ❌ Cancel Button
		builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());

		// Show Dialog
		builder.create().show();
	}

	private void saveText(String key, String value) {
		getSharedPreferences("MyAppPrefs", MODE_PRIVATE)
										.edit()
										.putString(key, value)
										.apply();
	}

	private String getSavedText(String key) {
		return getSharedPreferences("MyAppPrefs", MODE_PRIVATE)
										.getString(key, ""); // Default: empty string
	}




	private void copyDataToClipboard(String header, String footer) {
		if (productDates.isEmpty() || productMessages.isEmpty()) {
			Toast.makeText(this, "No data to copy!", Toast.LENGTH_SHORT).show();
			return;
		}

		StringBuilder copiedText = new StringBuilder();

		// ✅ Add Header if provided
		if (!header.isEmpty()) {
			copiedText.append(header).append("\n\n");
		}

		// ✅ Format Dates & Messages
		for (int i = 0; i < productDates.size(); i++) {
			copiedText.append("- *").append(productDates.get(i)).append("*\n");

			// Split message into lines and add "-" before each line
			String[] messageLines = productMessages.get(i).split("\n");
			for (String line : messageLines) {
				copiedText.append("- ").append(line).append("\n");
			}
			copiedText.append("\n");
		}

		// ✅ Add Footer if provided
		if (!footer.isEmpty()) {
			copiedText.append("\n").append(footer);
		}

		// ✅ Copy to Clipboard
		android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
		android.content.ClipData clip = android.content.ClipData.newPlainText("Product Data", copiedText.toString());
		clipboard.setPrimaryClip(clip);

		Toast.makeText(this, "Copied to Clipboard!", Toast.LENGTH_SHORT).show();
	}


	private void fetchProducts() {
		reference.addListenerForSingleValueEvent(new ValueEventListener() {
			@SuppressLint("NotifyDataSetChanged")
			@Override
			public void onDataChange(@NonNull DataSnapshot snapshot) {
				productMessages.clear();
				productDates.clear();

				if (snapshot.child("data").exists()) {
					try {
						JSONArray dataArray = new JSONArray(snapshot.child("data").getValue(String.class));
						SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
						Calendar calendar = Calendar.getInstance();
						calendar.setTime(sdf.parse(mainDate));

						for (int i = 0; i < dataArray.length(); i++) {
							productMessages.add(dataArray.getString(i));
							productDates.add(sdf.format(calendar.getTime()));
							calendar.add(Calendar.DAY_OF_MONTH, interval);
						}
					} catch (Exception e) {
						Log.e("Firebase", "Error parsing data", e);
					}
				}
				adapter.updateList(productDates, productMessages);
			}

			@Override
			public void onCancelled(@NonNull DatabaseError error) {
				Log.e("Firebase", "Error fetching data", error.toException());
			}
		});
	}

	private void showAddProductDialog() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle("Add Message");
		View view = LayoutInflater.from(this).inflate(R.layout.dialog_add_product_data, null);
		EditText etProductMessage = view.findViewById(R.id.etProductMessage);

		// Auto-set next available date
		EditText etProductDate = view.findViewById(R.id.etProductDate);
		SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
		Calendar calendar = Calendar.getInstance();
		try {
			if (!productDates.isEmpty()) {
				calendar.setTime(sdf.parse(productDates.get(productDates.size() - 1)));
			} else {
				calendar.setTime(sdf.parse(mainDate));
			}
			calendar.add(Calendar.DAY_OF_MONTH, interval);
		} catch (ParseException e) {
			e.printStackTrace();
		}
		etProductDate.setText(sdf.format(calendar.getTime()));
		builder.setView(view);

		builder.setPositiveButton("Save", (dialog, which) -> {
			String newMessage = etProductMessage.getText().toString().trim();
			if (newMessage.isEmpty()) {
				Toast.makeText(this, "Message cannot be empty!", Toast.LENGTH_SHORT).show();
				return;
			}
			addDataToFirebase(newMessage);
		});

		builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
		builder.create().show();
	}

	private void addDataToFirebase(String newMessage) {
		reference.addListenerForSingleValueEvent(new ValueEventListener() {
			@Override
			public void onDataChange(@NonNull DataSnapshot snapshot) {
				List<String> messages = new ArrayList<>();
				boolean isFirstItem = !snapshot.child("data").exists(); // Check if it's the first item

				if (!isFirstItem) {
					try {
						// ✅ Convert existing data to JSONArray
						String existingDataString = snapshot.child("data").getValue(String.class);
						JSONArray dataArray = new JSONArray(existingDataString);

						// ✅ Add existing messages to the list
						for (int i = 0; i < dataArray.length(); i++) {
							messages.add(dataArray.getString(i));
						}
					} catch (Exception e) {
						Log.e("Firebase", "Error parsing existing data", e);
					}
				}

				// ✅ Append the new message at the end
				messages.add(newMessage);

				// ✅ Convert updated list back to JSON format
				String updatedDataString = new JSONArray(messages).toString();

				// ✅ Prepare data for Firebase update
				Map<String, Object> updatedValues = new HashMap<>();
				updatedValues.put("data", updatedDataString);

				// ✅ If it's the first item, also update `date` and `interval`
				if (isFirstItem || messages.size() == 1) {
					updatedValues.put("date", mainDate);
					updatedValues.put("interval", interval);
				}

				// ✅ Update Firebase
				reference.updateChildren(updatedValues)
												.addOnSuccessListener(aVoid -> {
													Log.d("Firebase", "Data Added Successfully!");
													fetchProducts(); // Refresh UI
													Toast.makeText(allinfo.this, "Data Added Successfully!", Toast.LENGTH_SHORT).show();
												})
												.addOnFailureListener(e -> {
													Toast.makeText(allinfo.this, "Failed to add data!", Toast.LENGTH_SHORT).show();
													Log.e("Firebase", "Error adding data", e);
												});
			}

			@Override
			public void onCancelled(@NonNull DatabaseError error) {
				Log.e("Firebase", "Error reading database", error.toException());
			}
		});
	}

}