package com.nimeshkadecha.drkishan.pages;

import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.nimeshkadecha.drkishan.Helper.ProductDataAdapter;
import com.nimeshkadecha.drkishan.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.MessageFormat;
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

public class _6_ShowAllMessages extends AppCompatActivity {

	private RecyclerView recyclerView;
	private ProductDataAdapter adapter;
	private List<String> productDates, productMessages;
	private String productName, stage, subStage, mainDate, userName, unit;
	private double amount;
	private int interval;

	List<Double> productQuantities;
	List<String> productUnits;
	private JSONObject storedData; // Holds data from SharedPreferences

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		EdgeToEdge.enable(this);
		setContentView(R.layout.activity_6_show_all_messages);

		Log.d("ENimesh","_6_ShowAllMessages");

		// Get Intent Data
		productName = getIntent().getStringExtra("productName");
		userName = getIntent().getStringExtra("userName");
		stage = getIntent().getStringExtra("stage");
		subStage = getIntent().getStringExtra("subStage");
		mainDate = getIntent().getStringExtra("date");
		amount = Double.parseDouble(getIntent().getStringExtra("amount"));
		unit = getIntent().getStringExtra("unit");
		interval = Integer.parseInt(Objects.requireNonNull(getIntent().getStringExtra("days")));

		// ✅ setting header
		TextView header = findViewById(R.id.textView_Header);
		header.setText(MessageFormat.format("FP > {0} > {1} > {2}", productName, stage, subStage));
		header.setTextSize(20f);
		HorizontalScrollView scrollView = findViewById(R.id.horizontalScrollView);
		scrollView.post(() -> scrollView.smoothScrollTo(header.getWidth(), 0));

		recyclerView = findViewById(R.id.ProductListWithInfo);
		recyclerView.setLayoutManager(new LinearLayoutManager(this));

		productDates = new ArrayList<>();
		productMessages = new ArrayList<>();
		productQuantities = new ArrayList<>();
		productUnits = new ArrayList<>();

		adapter = new ProductDataAdapter(this, productDates, productMessages, productQuantities, productUnits,
		                                 amount, unit, userName, productName, stage, subStage);

		recyclerView.setAdapter(adapter); // ✅ Set adapter after loading data

		loadDataFromSharedPreferences(); // ✅ Load data first

		// Set Click Listener for Add Button
		Button btnAdd = findViewById(R.id.button);
		btnAdd.setOnClickListener(v -> showAddProductDialog());

		// Copy Button Functionality
		Button btnCopy = findViewById(R.id.button2_copy);
		btnCopy.setOnClickListener(v -> showCopyDialog());

		// ✅ Save Button - Always Upload Data to Firebase
		Button btnSave = findViewById(R.id.button_Upload);
		btnSave.setOnClickListener(v -> uploadDataToFirebase()); // ✅ No check, always upload
	}

	// ✅ Load Data from SharedPreferences and Ensure It's Updated with Intent Values
	private void loadDataFromSharedPreferences() {
		SharedPreferences prefs = getSharedPreferences("DrKishanPrefs", MODE_PRIVATE);
		String savedJson = prefs.getString("savedJson", "{}"); // Default: empty JSON

		Log.d("ENimesh","Data from SharedPreferences" + savedJson);

		try {
			JSONObject jsonObject = new JSONObject(savedJson);
			if (!jsonObject.has(userName)) jsonObject.put(userName, new JSONObject());
			JSONObject usersObj = jsonObject.getJSONObject(userName);

			if (!usersObj.has(productName)) usersObj.put(productName, new JSONObject());
			JSONObject productObj = usersObj.getJSONObject(productName);

			if (!productObj.has(stage)) productObj.put(stage, new JSONObject());
			JSONObject stageObj = productObj.getJSONObject(stage);

			if (!stageObj.has(subStage)) stageObj.put(subStage, new JSONObject());
			storedData = stageObj.getJSONObject(subStage);

			// ✅ Update storedData with latest Intent values
			storedData.put("count", amount);
			storedData.put("countingValue", unit);
			storedData.put("date", mainDate);
			storedData.put("interval", interval);

			// ✅ Save updated JSON back to SharedPreferences
			prefs.edit().putString("savedJson", jsonObject.toString()).apply();

			Log.d("ENimesh", "Updated JSON Data: " + jsonObject.toString());

			// ✅ Ensure RecyclerView gets updated values
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
					newProductUnits.add(obj.getString("k"));
					newProductDates.add(sdf.format(calendar.getTime()));
					calendar.add(Calendar.DAY_OF_MONTH, interval);
				}

				// ✅ Update RecyclerView with updated data
				runOnUiThread(() -> {
					if (adapter != null) {
						adapter.updateList(newProductDates, newProductMessages, newProductQuantities, newProductUnits);
						adapter.notifyDataSetChanged();
					} else {
						adapter = new ProductDataAdapter(this, productDates, productMessages, productQuantities, productUnits,
						                                 amount, unit, userName, productName, stage, subStage);

						recyclerView.setAdapter(adapter);
					}
				});
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
		View view = LayoutInflater.from(this).inflate(R.layout.dialog_add_detail_messages, null);
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
			Double newQuantity;

			try {
				newQuantity = Double.parseDouble(etProductQuantity.getText().toString().trim());
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
		getSharedPreferences("DrKishanPrefs", MODE_PRIVATE).edit().putString(key, value).apply();
	}

	private String getSavedText(String key) {
		return getSharedPreferences("DrKishanPrefs", MODE_PRIVATE).getString(key, ""); // Default: empty string
	}

	private void uploadDataToFirebase() {
		DatabaseReference reference = FirebaseDatabase.getInstance().getReference();

		try {
			// ✅ Fetch stored JSON from SharedPreferences
			SharedPreferences prefs = getSharedPreferences("DrKishanPrefs", MODE_PRIVATE);
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
				} else {
					// ✅ If key does not exist, create it
					if (key.equals("count")) subStageObject.put(key, productMessages.size());
					else if (key.equals("countingValue")) subStageObject.put(key, unit);
					else if (key.equals("date")) subStageObject.put(key, mainDate);
					else if (key.equals("interval")) subStageObject.put(key, interval);
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
																			                      Toast.makeText(_6_ShowAllMessages.this, "Data Synced to Firebase!", Toast.LENGTH_SHORT).show()
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


	// ✅ Add Data Locally in the correct format
	private void addDataLocally(String newMessage, Double quantity, String unit) {
		try {
			SharedPreferences prefs = getSharedPreferences("DrKishanPrefs", MODE_PRIVATE);
			String savedJson = prefs.getString("savedJson", "{}"); // Default empty JSON

			Log.d("ENimesh","Add Data to SharedPreferences" + savedJson);

			JSONObject jsonObject = new JSONObject(savedJson);

			// ✅ Ensure correct structure exists in SharedPreferences
			if (!jsonObject.has(userName)) jsonObject.put(userName, new JSONObject());
			JSONObject usersObj = jsonObject.getJSONObject(userName);

			if (!usersObj.has(productName)) usersObj.put(productName, new JSONObject());
			JSONObject productObj = usersObj.getJSONObject(productName);

			if (!productObj.has(stage)) productObj.put(stage, new JSONObject());
			JSONObject stageObj = productObj.getJSONObject(stage);

			if (!stageObj.has(subStage)) stageObj.put(subStage, new JSONObject());
			JSONObject subStageObj = stageObj.getJSONObject(subStage);

			// ✅ Retrieve or create messages array
			JSONArray messagesArray;
			if (subStageObj.has("data")) {
				try {
					messagesArray = new JSONArray(subStageObj.getJSONObject("data").getString("value"));
				} catch (JSONException e) {
					messagesArray = new JSONArray();
				}
			} else {
				messagesArray = new JSONArray();
			}

			// ✅ Append new message
			JSONObject newMessageObj = new JSONObject();
			newMessageObj.put("m", newMessage);
			newMessageObj.put("q", quantity);
			newMessageObj.put("qt", unit);
			messagesArray.put(newMessageObj);

			// ✅ Store updated messages in correct format
			JSONObject formattedData = new JSONObject();
			formattedData.put("value", messagesArray.toString());
			subStageObj.put("data", formattedData);

			// ✅ Save updated JSON to SharedPreferences
			prefs.edit().putString("savedJson", jsonObject.toString()).apply();
			storedData = subStageObj; // ✅ Ensure `storedData` is updated

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
			productDates.add(sdf.format(calendar.getTime())); // ✅ Adds next date correctly

			double convertedQuantity = quantity * amount; // ✅ Multiply with amount
			productQuantities.add(convertedQuantity);
			productUnits.add(unit);

			// ✅ Notify Adapter
			runOnUiThread(() -> {
				if (adapter != null) {
					adapter.notifyItemInserted(productMessages.size() - 1);
				}
			});

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

			// ✅ Retrieve Message, Quantity, and Unit
			String message = productMessages.get(i);
			double quantity = productQuantities.get(i) * amount;
			String unit = productUnits.get(i);

			// ✅ Format Quantity with Full Unit Name & Proper Formatting
			String formattedQuantity = formatQuantityWithFullUnit(quantity, unit);

			// ✅ Append Formatted Message
			copiedText.append("- ").append(message).append(" -- ").append(formattedQuantity).append("\n\n");
		}

		// ✅ Add Footer if provided
		if (!footer.isEmpty()) {
			copiedText.append("\n").append(footer);
		}

		// ✅ Copy to Clipboard
		ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
		ClipData clip = ClipData.newPlainText("Product Data", copiedText.toString());
		clipboard.setPrimaryClip(clip);

		Toast.makeText(this, "Copied to Clipboard!", Toast.LENGTH_SHORT).show();
	}

	private String formatQuantityWithFullUnit(double quantity, String unit) {
		double convertedQuantity = quantity;
		String finalUnit = unit;

		switch (unit.toLowerCase()) {
			case "g":
			case "gram":
				if (quantity >= 1000) {
					convertedQuantity = quantity / 1000;
					finalUnit = "KG"; // Convert to KG
				} else {
					finalUnit = "grams"; // Keep grams
				}
				break;

			case "kg":
			case "kilogram":
				finalUnit = "KG"; // Keep KG
				break;

			case "ml":
			case "milliliter":
				if (quantity >= 1000) {
					convertedQuantity = quantity / 1000;
					finalUnit = "Letter"; // Convert ML to L (Letter)
				} else {
					finalUnit = "ML"; // Keep ML
				}
				break;

			case "l":
			case "litre":
			case "liter":
				finalUnit = "Letter"; // Convert to Letter
				break;

			default:
				finalUnit = unit; // Default: Keep original unit
		}

		return formatNumber(convertedQuantity) + " " + finalUnit;
	}

	// ✅ Formats numbers with commas, ensuring proper decimal places
	private String formatNumber(double value) {
		if (value % 1 == 0) {
			return String.format(Locale.getDefault(), "%,d", (long) value); // Whole number
		} else {
			return String.format(Locale.getDefault(), "%,.2f", value); // Two decimal places
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		loadDataFromSharedPreferences(); // ✅ Ensure RecyclerView is updated when reopening the page
	}

}
