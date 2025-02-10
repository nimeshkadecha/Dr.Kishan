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
import java.util.Iterator;
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

	private void uploadDataToFirebase() {
		DatabaseReference reference = FirebaseDatabase.getInstance().getReference();

		try {
			// ✅ Fetch stored JSON from SharedPreferences
			SharedPreferences prefs = getSharedPreferences("DrKishan", MODE_PRIVATE);
			String savedJson = prefs.getString("savedJson", "{}"); // Default: empty JSON
			JSONObject jsonObject = new JSONObject(savedJson);

			// ✅ Navigate to the correct path in SharedPreferences data
			if (!jsonObject.has(userName)) {
				Toast.makeText(this, "No data found for user!", Toast.LENGTH_SHORT).show();
				return;
			}

			JSONObject userObject = jsonObject.getJSONObject(userName);
			if (!userObject.has(productName)) return;
			JSONObject productObject = userObject.getJSONObject(productName);
			if (!productObject.has(stage)) return;
			JSONObject stageObject = productObject.getJSONObject(stage);
			if (!stageObject.has(subStage)) return;
			JSONObject subStageObject = stageObject.getJSONObject(subStage);

			// ✅ Fix `data` field: Convert existing messages to a valid JSON array
			JSONArray messagesArray = new JSONArray();
			if (subStageObject.has("data")) {
				Object dataObject = subStageObject.get("data");

				if (dataObject instanceof JSONObject) { // If `data` is wrapped in a `value` key
					JSONObject dataJsonObject = (JSONObject) dataObject;
					if (dataJsonObject.has("value")) {
						messagesArray = new JSONArray(dataJsonObject.getString("value"));
					}
				} else if (dataObject instanceof JSONArray) {
					messagesArray = (JSONArray) dataObject; // ✅ Already a JSONArray
				}
			}

			// ✅ Append all current RecyclerView messages with ORIGINAL QUANTITY
			messagesArray = new JSONArray();
			for (int i = 0; i < productMessages.size(); i++) {
				JSONObject messageObj = new JSONObject();
				messageObj.put("m", productMessages.get(i));

				// ✅ Store original quantity
				double originalQuantity = productQuantities.get(i) / amount;
				messageObj.put("q", originalQuantity);

				messageObj.put("qt", productUnits.get(i));
				messagesArray.put(messageObj);
			}
			subStageObject.put("data", messagesArray.toString()); // ✅ Store as JSON string

			// ✅ Ensure `count`, `countingValue`, `date`, and `interval` are updated correctly
			String[] keysToFix = {"count", "countingValue", "date", "interval"};
			for (String key : keysToFix) {
				if (subStageObject.has(key)) {
					Object valueObj = subStageObject.get(key);
					if (valueObj instanceof JSONObject) {
						JSONObject valueJson = (JSONObject) valueObj;
						if (valueJson.has("value")) {
							subStageObject.put(key, valueJson.get("value")); // ✅ Directly replace in `subStageObject`
						}
					}
				}
			}

			// ✅ Convert updated `subStageObject` to a Map for Firebase
			Map<String, Object> firebaseData = jsonToMapWithoutValueWrapper(subStageObject);

			Log.d("ENimesh", "FD: " + userName + " /" + productName + "/ " + stage + " /" + subStage + " /" + firebaseData);

			// ✅ Upload only the relevant `subStage` as a proper JSON object
			reference.child(userName)
											.child(productName)
											.child(stage)
											.child(subStage)
											.setValue(firebaseData)
											.addOnSuccessListener(aVoid ->
																			                      Toast.makeText(allinfo.this, "Data Synced to Firebase!", Toast.LENGTH_SHORT).show()
											                     )
											.addOnFailureListener(e -> Log.e("Firebase", "Error uploading data", e));

		} catch (JSONException e) {
			Log.e("Firebase", "JSON Processing Error", e);
		}
	}


	private Map<String, Object> jsonToMapWithoutValueWrapper(JSONObject jsonObject) throws JSONException {
		Map<String, Object> map = new HashMap<>();
		Iterator<String> keys = jsonObject.keys();

		while (keys.hasNext()) {
			String key = keys.next();
			Object value = jsonObject.get(key);

			if (value instanceof JSONObject) {
				map.put(key, jsonToMapWithoutValueWrapper((JSONObject) value)); // ✅ Convert nested JSON objects correctly
			} else if (value instanceof JSONArray) {
				map.put(key, value.toString()); // ✅ Store JSON array as a proper string (Firebase format)
			} else {
				map.put(key, value); // ✅ Store direct key-value pairs correctly
			}
		}
		return map;
	}


	// ✅ Convert JSONObject to Map<String, Object> WITHOUT "value" nesting
	private Map<String, Object> jsonToDirectMap(JSONObject jsonObject) throws JSONException {
		Map<String, Object> map = new HashMap<>();
		Iterator<String> keys = jsonObject.keys();

		while (keys.hasNext()) {
			String key = keys.next();
			Object value = jsonObject.get(key);

			if (value instanceof JSONObject) {
				map.put(key, jsonToDirectMap((JSONObject) value)); // ✅ Convert nested JSON objects
			} else if (value instanceof JSONArray) {
				map.put(key, value.toString()); // ✅ Store JSON array as a string (Firebase-compatible)
			} else {
				map.put(key, value); // ✅ Store direct key-value pairs correctly
			}
		}
		return map;
	}


	// ✅ Convert JSONObject to Map<String, Object>
	private Map<String, Object> jsonToMap(JSONObject jsonObject) throws JSONException {
		Map<String, Object> map = new HashMap<>();
		Iterator<String> keys = jsonObject.keys();

		while (keys.hasNext()) {
			String key = keys.next();
			Object value = jsonObject.get(key);

			if (value instanceof JSONObject) {
				map.put(key, jsonToMap((JSONObject) value)); // ✅ Convert nested JSON objects correctly
			} else if (value instanceof JSONArray) {
				map.put(key, value.toString()); // ✅ Store JSON array as a proper string
			} else {
				map.put(key, value); // ✅ Store direct key-value pairs without nesting
			}
		}
		return map;
	}


	// ✅ Convert JSONArray to List<Object>
	private List<Object> jsonArrayToList(JSONArray jsonArray) throws JSONException {
		List<Object> list = new ArrayList<>();
		for (int i = 0; i < jsonArray.length(); i++) {
			Object value = jsonArray.get(i);

			if (value instanceof JSONObject) {
				list.add(jsonToMap((JSONObject) value)); // Convert nested JSONObject
			} else if (value instanceof JSONArray) {
				list.add(jsonArrayToList((JSONArray) value)); // Convert nested JSONArray
			} else {
				list.add(value);
			}
		}
		return list;
	}


	// ✅ Add Data Locally in the correct format
	private void addDataLocally(String newMessage, int quantity, String unit) {
		try {
			SharedPreferences prefs = getSharedPreferences("DrKishan", MODE_PRIVATE);
			String savedJson = prefs.getString("savedJson", "{}"); // Default to empty JSON
			JSONObject jsonObject = new JSONObject(savedJson);

			// ✅ Navigate to correct structure
			if (!jsonObject.has("users1")) jsonObject.put("users1", new JSONObject());
			JSONObject usersObj = jsonObject.getJSONObject("users1");

			if (!usersObj.has(userName)) usersObj.put(userName, new JSONObject());
			JSONObject userObj = usersObj.getJSONObject(userName);

			if (!userObj.has(productName)) userObj.put(productName, new JSONObject());
			JSONObject productObj = userObj.getJSONObject(productName);

			if (!productObj.has(stage)) productObj.put(stage, new JSONObject());
			JSONObject stageObj = productObj.getJSONObject(stage);

			if (!stageObj.has(subStage)) stageObj.put(subStage, new JSONObject());
			JSONObject subStageObj = stageObj.getJSONObject(subStage);

			// ✅ Extract existing messages or create new
			JSONArray messagesArray;
			if (subStageObj.has("data")) {
				messagesArray = new JSONArray(subStageObj.getJSONObject("data").getString("value"));
			} else {
				messagesArray = new JSONArray();
			}

			// ✅ Append new message
			JSONObject newMessageObj = new JSONObject();
			newMessageObj.put("m", newMessage);
			newMessageObj.put("q", quantity);
			newMessageObj.put("qt", unit);
			messagesArray.put(newMessageObj);

			// ✅ Store updated messages in the correct format
			JSONObject formattedData = new JSONObject();
			formattedData.put("value", messagesArray.toString());

			subStageObj.put("data", formattedData);

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
