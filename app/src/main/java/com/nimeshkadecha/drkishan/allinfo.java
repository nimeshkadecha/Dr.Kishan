package com.nimeshkadecha.drkishan;

import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public class allinfo extends AppCompatActivity {

	private RecyclerView recyclerView;
	private ProductDataAdapter adapter;
	private List<String> productDates, productMessages;
	private String productName, stage, subStage, mainDate, userName, unit;
	private double amount;
	private int interval;

	List<Double> productQuantities;
	List<String> productUnits;

	private JSONObject storedData; // Holds data from SharedPreferences
	private boolean dataChanged = false; // Flag to track changes

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		EdgeToEdge.enable(this);
		setContentView(R.layout.activity_all_info);

		// Get Intent Data
		productName = getIntent().getStringExtra("productName");
		userName = getIntent().getStringExtra("userName");
		stage = getIntent().getStringExtra("stage");
		subStage = getIntent().getStringExtra("subStage");
		mainDate = getIntent().getStringExtra("date");
		amount = Integer.parseInt(getIntent().getStringExtra("amount"));
		unit = getIntent().getStringExtra("unit");
		interval = Integer.parseInt(Objects.requireNonNull(getIntent().getStringExtra("days")));

		// Log SharedPreferences data for debugging
		SharedPreferences prefs = getSharedPreferences("DrKishan", MODE_PRIVATE);
		String savedJson = prefs.getString("savedJson", "{}"); // Default: empty JSON

		Log.d("SharedPreferences", "Raw JSON Data: " + savedJson);

		recyclerView = findViewById(R.id.ProductListWithInfo);
		recyclerView.setLayoutManager(new LinearLayoutManager(this));

		productDates = new ArrayList<>();
		productMessages = new ArrayList<>();
		productQuantities = new ArrayList<>();
		productUnits = new ArrayList<>();


		adapter = new ProductDataAdapter(this, productDates, productMessages, productQuantities, productUnits, amount, unit);
		recyclerView.setAdapter(adapter); // ✅ Now set adapter after loading data

		loadDataFromSharedPreferences(); // ✅ Load data first


		// Set Click Listener for Add Button
		Button btnAdd = findViewById(R.id.button);
		btnAdd.setOnClickListener(v -> showAddProductDialog());

		// Copy Button Functionality (No Changes)
		Button btnCopy = findViewById(R.id.button2_copy);
		btnCopy.setOnClickListener(v -> showCopyDialog());

		// Save Button - Checks for changes before uploading
		Button btnSave = findViewById(R.id.button_Upload);
		btnSave.setOnClickListener(v -> {
			if (dataChanged) {
				uploadDataToFirebase();
			} else {
				Toast.makeText(this, "No changes detected!", Toast.LENGTH_SHORT).show();
			}
		});
	}

	// ✅ Load Data from SharedPreferences
	private void loadDataFromSharedPreferences() {
		SharedPreferences prefs = getSharedPreferences("DrKishan", MODE_PRIVATE);
		String savedJson = prefs.getString("savedJson", "{}"); // Default: empty JSON

		Log.d("SharedPreferences", "Raw JSON Data: " + savedJson);

		try {
			JSONObject jsonObject = new JSONObject(savedJson);
			if (jsonObject.has("users1")) {
				JSONObject usersObj = jsonObject.getJSONObject("users1");
				if (usersObj.has(productName) && usersObj.getJSONObject(productName).has(stage)) {
					storedData = usersObj.getJSONObject(productName).getJSONObject(stage).getJSONObject(subStage);

					if (storedData.has("data")) {
						JSONObject dataObject = storedData.getJSONObject("data");
						JSONArray messagesArray = new JSONArray(dataObject.getString("value"));

						SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
						Calendar calendar = Calendar.getInstance();
						calendar.setTime(sdf.parse(mainDate));

						List<String> newProductDates = new ArrayList<>();
						List<String> newProductMessages = new ArrayList<>();
						List<Double> newProductQuantities = new ArrayList<>();
						List<String> newProductUnits = new ArrayList<>();

						for (int i = 0; i < messagesArray.length(); i++) {
							JSONObject obj = messagesArray.getJSONObject(i);
							newProductMessages.add(obj.getString("m"));
							newProductQuantities.add(obj.getDouble("q"));
							newProductUnits.add(obj.getString("qt"));
							newProductDates.add(sdf.format(calendar.getTime()));
							calendar.add(Calendar.DAY_OF_MONTH, interval);
						}

						// ✅ Ensure adapter is not null before updating list
						if (adapter != null) {
							runOnUiThread(() -> {
								if (adapter == null) {
									adapter = new ProductDataAdapter(this, newProductDates, newProductMessages, newProductQuantities, newProductUnits, amount, unit);
									recyclerView.setAdapter(adapter);
								} else {
									adapter.updateList(newProductDates, newProductMessages, newProductQuantities, newProductUnits);
									adapter.notifyDataSetChanged();
								}
							});

						} else {
							Log.e("AdapterUpdate", "Adapter is null, cannot update list");
						}
					}
				}
			}
		} catch (JSONException | ParseException e) {
			Log.e("SharedPreferences", "Error parsing JSON", e);
		}
	}


	// ✅ Show Dialog to Add Message
	// ✅ Show Dialog to Add Message with Unit Selection
	private void showAddProductDialog() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle("Add Message");

		// Inflate custom layout
		View view = LayoutInflater.from(this).inflate(R.layout.dialog_add_product_data, null);
		EditText etProductMessage = view.findViewById(R.id.etProductMessage);
		EditText etProductQuantity = view.findViewById(R.id.etProductQuantity);
		Spinner unitSpinner = view.findViewById(R.id.unitSpinner); // ✅ Add Unit Spinner

		// ✅ Populate Spinner with unit options
		ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
										this, R.array.unit_options, android.R.layout.simple_spinner_item);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		unitSpinner.setAdapter(adapter);

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
			String selectedUnit = unitSpinner.getSelectedItem().toString();
			int newQuantity;

			try {
				newQuantity = Integer.parseInt(etProductQuantity.getText().toString().trim());
			} catch (NumberFormatException e) {
				Toast.makeText(this, "Enter a valid quantity", Toast.LENGTH_SHORT).show();
				return;
			}

			if (newMessage.isEmpty()) {
				Toast.makeText(this, "Message cannot be empty!", Toast.LENGTH_SHORT).show();
				return;
			}

			addDataLocally(newMessage, newQuantity, selectedUnit);
		});

		builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
		builder.create().show();
	}


	// ✅ Copy Functionality (No Changes)
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

		builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
		builder.create().show();
	}

	private void saveText(String key, String value) {
		getSharedPreferences("DrKishan", MODE_PRIVATE).edit().putString(key, value).apply();
	}

	private String getSavedText(String key) {
		return getSharedPreferences("DrKishan", MODE_PRIVATE).getString(key, ""); // Default: empty string
	}

	// ✅ Upload Data to Firebase if changed
	private void uploadDataToFirebase() {
		DatabaseReference reference = FirebaseDatabase.getInstance().getReference(userName)
										.child(productName).child(stage).child(subStage);

		Map<String, Object> updatedValues = new HashMap<>();
		updatedValues.put("data", storedData.optString("data", "[]"));
		updatedValues.put("date", mainDate);
		updatedValues.put("interval", interval);

		reference.updateChildren(updatedValues)
										.addOnSuccessListener(aVoid -> {
											Toast.makeText(allinfo.this, "Data Synced to Firebase!", Toast.LENGTH_SHORT).show();
											dataChanged = false; // Reset flag
										})
										.addOnFailureListener(e -> Log.e("Firebase", "Error uploading data", e));
	}

	// ✅ Add Data Locally to SharedPreferences
	private void addDataLocally(String newMessage, int quantity, String unit) {
		try {
			SharedPreferences prefs = getSharedPreferences("DrKishan", MODE_PRIVATE);
			String savedJson = prefs.getString("savedJson", "{}"); // Default to empty JSON

			JSONObject jsonObject = new JSONObject(savedJson);

			// ✅ Navigate to the correct sub-stage
			if (!jsonObject.has("users1")) jsonObject.put("users1", new JSONObject());
			JSONObject usersObj = jsonObject.getJSONObject("users1");

			if (!usersObj.has(productName)) usersObj.put(productName, new JSONObject());
			JSONObject productObj = usersObj.getJSONObject(productName);

			if (!productObj.has(stage)) productObj.put(stage, new JSONObject());
			JSONObject stageObj = productObj.getJSONObject(stage);

			if (!stageObj.has(subStage)) stageObj.put(subStage, new JSONObject());
			JSONObject subStageObj = stageObj.getJSONObject(subStage);

			// ✅ Append new message with quantity & unit
			JSONArray messagesArray;
			if (subStageObj.has("data")) {
				messagesArray = new JSONArray(subStageObj.getJSONObject("data").getString("value"));
			} else {
				messagesArray = new JSONArray();
			}

			JSONObject newMessageObj = new JSONObject();
			newMessageObj.put("m", newMessage);
			newMessageObj.put("q", quantity);
			newMessageObj.put("qt", unit);
			messagesArray.put(newMessageObj);

			subStageObj.put("data", messagesArray.toString());

			// ✅ Save updated JSON to SharedPreferences
			prefs.edit().putString("savedJson", jsonObject.toString()).apply();

			// ✅ Update RecyclerView
			productMessages.add(newMessage);
			SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
			Calendar calendar = Calendar.getInstance();
			if (!productDates.isEmpty()) {
				try {
					calendar.setTime(sdf.parse(productDates.get(productDates.size() - 1)));
				} catch (ParseException e) {
					e.printStackTrace();
				}
			}
			calendar.add(Calendar.DAY_OF_MONTH, interval);
			productDates.add(sdf.format(calendar.getTime())); // Adds next date correctly

			double convertedQuantity = quantity * amount; // ✅ Multiply with amount
			productQuantities.add(convertedQuantity);

			productUnits.add(unit);

// ✅ Notify Adapter
			runOnUiThread(() -> {
				if (adapter != null) {
					adapter.notifyItemInserted(productMessages.size() - 1);
				}
			});

			dataChanged = true; // ✅ Mark data as changed

			Toast.makeText(this, "Data Added Locally!", Toast.LENGTH_SHORT).show();

		} catch (JSONException e) {
			Log.e("SharedPreferences", "Error updating JSON", e);
			Toast.makeText(this, "Failed to add data!", Toast.LENGTH_SHORT).show();
		}
	}

	// ✅ Copy Data to Clipboard
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

			// ✅ Split message into multiple lines and add "-" before each line
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

}
