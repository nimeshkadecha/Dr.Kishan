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
import com.nimeshkadecha.drkishan.Helper.DialogMessageAdapter;
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
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public class _6_ShowAllMessages extends AppCompatActivity {


	private RecyclerView recyclerView;
	private ProductDataAdapter adapter;
	private String productName, stage, subStage, mainDate, userName, unit;
	private double amount;

	static boolean needToSave = false;

	private double calculateAm;
	private int interval;

	private HashMap<Integer, ArrayList<String>> messagesMap;
	private JSONObject storedData;
	private List<String> productDates, productMessages;

	List<Double> productQuantities;
	List<String> productUnits;
//	private JSONObject storedData; // Holds data from SharedPreferences

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		EdgeToEdge.enable(this);
		setContentView(R.layout.activity_6_show_all_messages);

		Log.d("ENimesh","_6_ShowAllMessages");

		setNeedToSave(false);

		// Get Intent Data
		productName = getIntent().getStringExtra("productName");
		userName = getIntent().getStringExtra("userName");
		stage = getIntent().getStringExtra("stage");
		subStage = getIntent().getStringExtra("subStage");
		mainDate = getIntent().getStringExtra("date");
		amount = Double.parseDouble(Objects.requireNonNull(getIntent().getStringExtra("amount")));
		unit = getIntent().getStringExtra("unit");
		if(unit != null && unit.equalsIgnoreCase("Vigha")){
			calculateAm = amount;
		}else{
			calculateAm = amount * 2.5d;
		}
		interval = Integer.parseInt(Objects.requireNonNull(getIntent().getStringExtra("days")));

		// ✅ setting header
		TextView header = findViewById(R.id.textView_Header);
		header.setText(MessageFormat.format("FP > {0} > {1} > {2}", productName, stage, subStage));
		header.setTextSize(20f);
		HorizontalScrollView scrollView = findViewById(R.id.horizontalScrollView);
		scrollView.post(() -> scrollView.smoothScrollTo(header.getWidth(), 0));

		recyclerView = findViewById(R.id.ProductListWithInfo);
		recyclerView.setLayoutManager(new LinearLayoutManager(this));

		messagesMap = new HashMap<>();

		adapter = new ProductDataAdapter(this, messagesMap,mainDate,interval,calculateAm,userName,productName,stage,subStage);

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
//			NOTE: I guess i should remove it
			storedData.put("count", amount);
			storedData.put("countingValue", unit);
			storedData.put("date", mainDate);
			storedData.put("interval", interval);

			// ✅ Save updated JSON back to SharedPreferences
			prefs.edit().putString("savedJson", jsonObject.toString()).apply();

			Log.d("ENimesh", "Updated JSON Data: " + jsonObject.toString());

			if (storedData.has("data")) {
				JSONObject dataObject = storedData.getJSONObject("data");
				JSONArray messagesArray = new JSONArray(dataObject.getString("value"));

				for (int i = 0; i < messagesArray.length(); i++) {
					JSONObject obj = messagesArray.getJSONObject(i);
					int k = obj.getInt("k");
					String message = obj.getString("m");
					double q = obj.getDouble("q") * calculateAm;
					String qt = obj.getString("qt");

					String formattedQuantity = formatQuantity(q, qt);
					String formattedMessage = message + " -- " + formattedQuantity;


					messagesMap.computeIfAbsent(k, key -> new ArrayList<>()).add(formattedMessage);
				}

					Log.d("ENimesh","formated s " + messagesMap);

				runOnUiThread(() -> {
					if (adapter != null) {
						adapter.updateList(messagesMap);
					} else {
						adapter = new ProductDataAdapter(this, messagesMap,mainDate,interval,calculateAm,userName,productName,stage,subStage);
						recyclerView.setAdapter(adapter);
					}
						adapter.notifyDataSetChanged();
				});
			}

		} catch (JSONException e) {
			Log.e("SharedPreferences", "Error parsing JSON", e);
		}
	}

	private void showAddProductDialog() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle("Add Message");

		View view = LayoutInflater.from(this).inflate(R.layout.dialog_add_detail_messages, null);
		EditText etProductMessage = view.findViewById(R.id.etProductMessage);
		EditText etProductQuantity = view.findViewById(R.id.etProductQuantity);
		EditText etProductDate = view.findViewById(R.id.etProductDate);
		Spinner unitSpinner = view.findViewById(R.id.unitSpinner);
		Button addMsg = view.findViewById(R.id.addMsg);
		RecyclerView recyclerViewMessages = view.findViewById(R.id.recyclerViewMessages);

		ArrayAdapter<CharSequence> arr_adapter = ArrayAdapter.createFromResource(this, R.array.unit_options, android.R.layout.simple_spinner_item);
		arr_adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		unitSpinner.setAdapter(arr_adapter);

		List<String> dialogList = new ArrayList<>();
		DialogMessageAdapter dialogAdapter = new DialogMessageAdapter(dialogList, (message, quantity, unit) -> {
			etProductMessage.setText(message);
			etProductQuantity.setText(quantity);

			// ✅ Set spinner selection dynamically
			for (int i = 0; i < unitSpinner.getAdapter().getCount(); i++) {
				if (unitSpinner.getAdapter().getItem(i).toString().equalsIgnoreCase(unit)) {
					unitSpinner.setSelection(i);
					break;
				}
			}
		});
		recyclerViewMessages.setAdapter(dialogAdapter);
		recyclerViewMessages.setLayoutManager(new LinearLayoutManager(this));

		SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
		Calendar calendar = Calendar.getInstance();

		try {
			// Use the starting date from intent
			calendar.setTime(Objects.requireNonNull(sdf.parse(mainDate)));

			// Calculate the next date based on the number of distinct keys in messageMap
			int numberOfKeys = messagesMap.size(); // Number of distinct 'k'
			int totalDaysToAdd = numberOfKeys * interval;

			calendar.add(Calendar.DAY_OF_MONTH, totalDaysToAdd);
		} catch (ParseException e) {
			e.printStackTrace();
		}

		etProductDate.setText(sdf.format(calendar.getTime()));

		addMsg.setOnClickListener(v -> {
			String newMessage = etProductMessage.getText().toString().trim();
			String selectedUnit = unitSpinner.getSelectedItem().toString();

			if (newMessage.isEmpty()) {
				Toast.makeText(this, "Message cannot be empty!", Toast.LENGTH_SHORT).show();
				return;
			}

			String formattedQuantity;
			try {
				double newQuantity = Double.parseDouble(etProductQuantity.getText().toString().trim()) * calculateAm;
				Log.d("ENimesh","data = " + calculateAm + " " + newQuantity);
				formattedQuantity = formatQuantity(newQuantity,selectedUnit);
			} catch (NumberFormatException e) {
				Toast.makeText(this, "Enter a valid quantity", Toast.LENGTH_SHORT).show();
				return;
			}

			// ✅ Correctly add message to `dialogList`
			String entry = newMessage + " -- " + formattedQuantity;

			if (!dialogList.contains(entry)) {
				dialogList.add(entry);
				dialogAdapter.notifyDataSetChanged(); // Refresh RecyclerView
				Log.d("ENimesh", "Added entry: " + entry);
			} else {
				Log.d("ENimesh", "Duplicate skipped: " + entry);
			}

			// ✅ Clear input fields after adding
			etProductMessage.setText("");
			etProductQuantity.setText("");
		});

		builder.setView(view);
		builder.setPositiveButton("Save", (dialog, which) -> {
			if (dialogList.isEmpty()) {
				Toast.makeText(this, "No messages to save", Toast.LENGTH_SHORT).show();
				return;
			}

			// ✅ Auto-increment "k" correctly
			int newKey = messagesMap.isEmpty() ? 1 : Collections.max(messagesMap.keySet()) + 1;

			// ✅ Add new messages directly to `messagesMap`
			Log.d("ENimesh","List = " + dialogList);
			messagesMap.put(newKey, new ArrayList<>(dialogList));

			// ✅ Update RecyclerView through adapter
			adapter.addMessagesToMap(newKey, new ArrayList<>(dialogList));

			// ✅ Ensure RecyclerView refreshes
			adapter.notifyDataSetChanged();

			// ✅ Pass `newKey` to `addDataLocally()`
			addDataLocally(newKey);


		});

		builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());

		builder.create().show();
	}

	// ✅ Copy Functionality
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

			// ✅ Build a new JSONArray from the messageMap
			JSONArray messagesArray = new JSONArray();

			// Iterate over all keys in your HashMap (assumed type: HashMap<Integer, ArrayList<String>>)
			for (Map.Entry<Integer, ArrayList<String>> entry : messagesMap.entrySet()) {
				int key = entry.getKey();
				ArrayList<String> messages = entry.getValue();
				for (String messageEntry : messages) {
					// We expect the editing format "message -- quantity unit"
					String[] parts = messageEntry.split(" -- ");
					if (parts.length < 2) {
						Log.e("ENimesh", "Unexpected message format: " + messageEntry);
						continue;
					}
					String messageText = parts[0].trim();
					String qtyUnit = parts[1].trim();
					// Extract quantity and unit using regex (adjust if necessary)
					String quantityStr = qtyUnit.replaceAll("[^\\d.]", "").trim();
					String unitStr = qtyUnit.replaceAll("[\\d.]", "").trim();

					JSONObject messageObj = new JSONObject();
					messageObj.put("m", messageText);

					// Assuming you want to store the original quantity,
					// if you have a conversion factor (amount), adjust as needed.
					double quantityValue = 0;
					try {
						quantityValue = Double.parseDouble(quantityStr);
					} catch (NumberFormatException e) {
						Log.e("ENimesh", "Failed to parse quantity: " + quantityStr, e);
					}
					double originalQuantity = quantityValue / amount; // adjust if necessary
					messageObj.put("q", originalQuantity);
					messageObj.put("qt", unitStr);
					messageObj.put("k", key); // store the key if needed

					messagesArray.put(messageObj);
				}
			}

			// ✅ Replace the data field with the new messages array (stored as JSON string)
			subStageObject.put("data", messagesArray.toString());

			// ✅ Update other fields: count, countingValue, date, interval
			String[] keysToFix = {"count", "countingValue", "date", "interval","isDrip"};
			for (String key : keysToFix) {
				if (subStageObject.has(key)) {
					Object valueObj = subStageObject.get(key);
					if (valueObj instanceof JSONObject) {
						JSONObject valueJson = (JSONObject) valueObj;
						if (valueJson.has("value")) {
							subStageObject.put(key, valueJson.get("value"));
							Log.d("ENimesh","valll " +valueJson.get("value"));
						}
					}
				} else {
					// Create the key if it doesn't exist
					switch (key) {
						case "count":
							// Total count across all keys in messageMap
							int totalMessages = 0;
							for (ArrayList<String> list : messagesMap.values()) {
								totalMessages += list.size();
							}
							subStageObject.put(key, totalMessages);
							break;
						case "countingValue":
							subStageObject.put(key, unit);
							break;
						case "date":
							subStageObject.put(key, mainDate);
							break;
						case "interval":
							subStageObject.put(key, interval);
							break;
						case "isDrip":
							subStageObject.put("isDrip", false);
							break;
					}
				}
			}
			// ✅ Convert updated subStageObject to a Map for Firebase upload.
			Map<String, Object> firebaseData = jsonToMapWithoutValueWrapper(subStageObject);
			Log.d("ENimesh", "FD: " + userName + " /" + productName + "/ " + stage + " /" + subStage + " /" + firebaseData);

			// ✅ Upload only the relevant subStage as a proper JSON object
			reference.child(userName)
											.child(productName)
											.child(stage)
											.child(subStage)
											.setValue(firebaseData)
											.addOnSuccessListener(aVoid ->
																			                      Toast.makeText(this, "Data Synced to Firebase!", Toast.LENGTH_SHORT).show()
											                     )
											.addOnFailureListener(e -> Log.e("Firebase", "Error uploading data", e));

		} catch (JSONException e) {
			Log.e("Firebase", "JSON Processing Error", e);
		}
	}

//
//	private void uploadDataToFirebase() {
//		DatabaseReference reference = FirebaseDatabase.getInstance().getReference();
//
//		try {
//			// ✅ Fetch stored JSON from SharedPreferences
//			SharedPreferences prefs = getSharedPreferences("DrKishanPrefs", MODE_PRIVATE);
//			String savedJson = prefs.getString("savedJson", "{}"); // Default: empty JSON
//			JSONObject jsonObject = new JSONObject(savedJson);
//
//			// ✅ Navigate to the correct path in SharedPreferences data
//			if (!jsonObject.has(userName)) {
//				Toast.makeText(this, "No data found for user!", Toast.LENGTH_SHORT).show();
//				return;
//			}
//
//			JSONObject userObject = jsonObject.getJSONObject(userName);
//			if (!userObject.has(productName)) return;
//			JSONObject productObject = userObject.getJSONObject(productName);
//			if (!productObject.has(stage)) return;
//			JSONObject stageObject = productObject.getJSONObject(stage);
//			if (!stageObject.has(subStage)) return;
//			JSONObject subStageObject = stageObject.getJSONObject(subStage);
//
//			// ✅ Fix `data` field: Convert existing messages to a valid JSON array
//			JSONArray messagesArray = new JSONArray();
//			if (subStageObject.has("data")) {
//				Object dataObject = subStageObject.get("data");
//
//				if (dataObject instanceof JSONObject) { // If `data` is wrapped in a `value` key
//					JSONObject dataJsonObject = (JSONObject) dataObject;
//					if (dataJsonObject.has("value")) {
//						messagesArray = new JSONArray(dataJsonObject.getString("value"));
//					}
//				} else if (dataObject instanceof JSONArray) {
//					messagesArray = (JSONArray) dataObject; // ✅ Already a JSONArray
//				}
//			}
//
//			// ✅ Append all current RecyclerView messages with ORIGINAL QUANTITY
//			messagesArray = new JSONArray();
//			for (int i = 0; i < productMessages.size(); i++) {
//				JSONObject messageObj = new JSONObject();
//				messageObj.put("m", productMessages.get(i));
//
//				// ✅ Store original quantity
//				double originalQuantity = productQuantities.get(i) / amount;
//				messageObj.put("q", originalQuantity);
//
//				messageObj.put("qt", productUnits.get(i));
//				messagesArray.put(messageObj);
//			}
//			subStageObject.put("data", messagesArray.toString()); // ✅ Store as JSON string
//
//			// ✅ Ensure `count`, `countingValue`, `date`, and `interval` are updated correctly
//			String[] keysToFix = {"count", "countingValue", "date", "interval"};
//			for (String key : keysToFix) {
//				if (subStageObject.has(key)) {
//					Object valueObj = subStageObject.get(key);
//					if (valueObj instanceof JSONObject) {
//						JSONObject valueJson = (JSONObject) valueObj;
//						if (valueJson.has("value")) {
//							subStageObject.put(key, valueJson.get("value")); // ✅ Directly replace in `subStageObject`
//						}
//					}
//				} else {
//					// ✅ If key does not exist, create it
//					switch (key) {
//						case "count":
//							subStageObject.put(key, productMessages.size());
//							break;
//						case "countingValue":
//							subStageObject.put(key, unit);
//							break;
//						case "date":
//							subStageObject.put(key, mainDate);
//							break;
//						case "interval":
//							subStageObject.put(key, interval);
//							break;
//					}
//				}
//			}
//
//			// ✅ Convert updated `subStageObject` to a Map for Firebase
//			Map<String, Object> firebaseData = jsonToMapWithoutValueWrapper(subStageObject);
//
//			Log.d("ENimesh", "FD: " + userName + " /" + productName + "/ " + stage + " /" + subStage + " /" + firebaseData);
//
//			// ✅ Upload only the relevant `subStage` as a proper JSON object
//			reference.child(userName)
//											.child(productName)
//											.child(stage)
//											.child(subStage)
//											.setValue(firebaseData)
//											.addOnSuccessListener(aVoid ->
//																			                      Toast.makeText(_6_ShowAllMessages.this, "Data Synced to Firebase!", Toast.LENGTH_SHORT).show()
//											                     )
//											.addOnFailureListener(e -> Log.e("Firebase", "Error uploading data", e));
//
//		} catch (JSONException e) {
//			Log.e("Firebase", "JSON Processing Error", e);
//		}
//	}

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

	private void addDataLocally(int key) {
		try {
			SharedPreferences prefs = getSharedPreferences("DrKishanPrefs", MODE_PRIVATE);
			String savedJson = prefs.getString("savedJson", "{}"); // Default empty JSON

			Log.d("ENimesh", "Before adding Data to SharedPreferences: " + savedJson);
			JSONObject jsonObject = new JSONObject(savedJson);

			// Ensure correct structure exists in SharedPreferences
			if (!jsonObject.has(userName)) jsonObject.put(userName, new JSONObject());
			JSONObject usersObj = jsonObject.getJSONObject(userName);

			if (!usersObj.has(productName)) usersObj.put(productName, new JSONObject());
			JSONObject productObj = usersObj.getJSONObject(productName);

			if (!productObj.has(stage)) productObj.put(stage, new JSONObject());
			JSONObject stageObj = productObj.getJSONObject(stage);

			if (!stageObj.has(subStage)) stageObj.put(subStage, new JSONObject());
			JSONObject subStageObj = stageObj.getJSONObject(subStage);

			// Retrieve existing data array if present
			JSONArray existingDataArray = new JSONArray();
			if (subStageObj.has("data")) {
				existingDataArray = new JSONArray(subStageObj.getJSONObject("data").getString("value"));
			}

			// Create new messages array from `messagesMap`
			JSONArray newMessageArray = new JSONArray();
			if (messagesMap.containsKey(key)) {
				for (String message : Objects.requireNonNull(messagesMap.get(key))) {
					JSONObject messageObj = new JSONObject(); // Create JSON object
					String[] parts = message.split(" -- ");
					messageObj.put("m", parts[0]); // Extract message
					messageObj.put("q", parts[1].replaceAll("[^\\d]", "")); // Extract quantity
					messageObj.put("qt", parts[1].replaceAll("[^a-zA-Z]", "")); // Extract unit only
					messageObj.put("k", key); // Store 'k'
					newMessageArray.put(messageObj);
				}
			}

			// Append new messages to existing ones
			for (int i = 0; i < newMessageArray.length(); i++) {
				existingDataArray.put(newMessageArray.getJSONObject(i));
			}

			// Store updated messages in correct format
			JSONObject formattedData = new JSONObject();
			formattedData.put("value", existingDataArray.toString());
			subStageObj.put("data", formattedData);

			// Save updated JSON to SharedPreferences
			prefs.edit().putString("savedJson", jsonObject.toString()).apply();
			storedData = subStageObj; // Ensure `storedData` is updated

			Toast.makeText(this, "Data Added Locally!", Toast.LENGTH_SHORT).show();

			setNeedToSave(true);

			savedJson = prefs.getString("savedJson", "{}"); // Retrieve updated JSON
			Log.d("ENimesh", "After adding Data to SharedPreferences: " + savedJson);
		} catch (JSONException e) {
			Log.e("SharedPreferences", "Error updating JSON", e);
			Toast.makeText(this, "Failed to add data!", Toast.LENGTH_SHORT).show();
		}
	}
	// ✅ Copy Data to Clipboard
	private void copyDataToClipboard(String header, String footer) {
		if (messagesMap.isEmpty()) {
			Toast.makeText(this, "No data to copy!", Toast.LENGTH_SHORT).show();
			return;
		}

		StringBuilder copiedText = new StringBuilder();

		// ✅ Add Header if provided
		if (!header.isEmpty()) {
			copiedText.append(header).append("\n\n");
		}

		// ✅ Check if interval is 0 (add all messages to mainDate)
		if (interval == 0) {
			StringBuilder finalCopiedString = new StringBuilder();

			// Append messages to mainDate directly
			for (ArrayList<String> messages : messagesMap.values()) {
				for (String message : messages) {
					finalCopiedString.append("- ").append(message).append("\n");
				}
			}

			copiedText.append("- *").append(mainDate).append("*\n");
			copiedText.append(finalCopiedString).append("\n\n");
		} else {
			// ✅ Iterate over each `k` in messagesMap (sorted automatically)
			Log.d("ENimesh", "keyset" + messagesMap.toString() + " " + messagesMap.keySet() + " " + messagesMap.values());
			int counter = 0;
			for (Integer k : messagesMap.keySet()) {
				String date = addDaysToDate(mainDate, counter * interval); // ✅ Generate date dynamically
				copiedText.append("- *").append(date).append("*\n"); // ✅ Append Date
				counter++; // Increment for next interval

				// ✅ Retrieve messages for the current `k`
				ArrayList<String> messages = messagesMap.get(k);

				if (messages != null) {
					StringBuilder finalCopiedString = new StringBuilder();
					for (String message : messages) {
						finalCopiedString.append("- ").append(message).append("\n");
					}
					Log.d("ENimesh", "fs = " + finalCopiedString);
					copiedText.append(finalCopiedString).append("\n\n"); // ✅ Append Message
				}
			}
		}

		// ✅ Add Footer if provided
		if (!footer.isEmpty()) {
			copiedText.append(footer);
		}

		// ✅ Copy to Clipboard
		ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
		ClipData clip = ClipData.newPlainText("Product Data", copiedText.toString());
		clipboard.setPrimaryClip(clip);

		Toast.makeText(this, "Copied to Clipboard!", Toast.LENGTH_SHORT).show();
	}

	private String addDaysToDate(String startDate, int daysToAdd) {
		SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
		Log.d("ENimesh","d = " + startDate);
		try {
			Date date = dateFormat.parse(startDate);
			if (date != null) {
				Calendar calendar = Calendar.getInstance();
				calendar.setTime(date);
				calendar.add(Calendar.DAY_OF_MONTH, daysToAdd); // ✅ Add the interval days
				return dateFormat.format(calendar.getTime()); // ✅ Return formatted new date
			}
		} catch (ParseException e) {
			Log.e("DateError", "Error parsing start date", e);
		}
		return startDate; // ✅ Fallback to original date if parsing fails
	}

	// ✅ Formats numbers with commas, ensuring proper decimal places
	private String formatNumber(double value) {
		if (value % 1 == 0) {
			return String.format(Locale.getDefault(), "%,d", (long) value); // Whole number
		} else {
			return String.format(Locale.getDefault(), "%,.2f", value); // Two decimal places
		}
	}

	// ✅ Corrected unit conversion & number formatting
	private String formatQuantity(double quantity, String unit) {
		double convertedQuantity = quantity;
		String finalUnit = unit;

		switch (unit.toLowerCase()) {
			case "ml":
			case "milliliter":
				if (quantity >= 1000) {
					convertedQuantity = quantity / 1000;
					finalUnit = "Letter"; // Convert ML to L
				} else {
					finalUnit = "ML"; // Keep ML
				}
				break;

			case "g":
			case "gram":
				if (quantity >= 1000) {
					convertedQuantity = quantity / 1000;
					finalUnit = "KG"; // Convert G to KG
				} else {
					finalUnit = "grams"; // Keep grams
				}
				break;

			case "kg":
			case "kilogram":
				finalUnit = "KG"; // Keep KG
				break;

			case "l":
			case "litre":
			case "liter":
				finalUnit = "Letter"; // Convert L to Letter
				break;

			default:
				finalUnit = unit; // Keep the original unit if unrecognized
		}

		return formatNumber(convertedQuantity) + " " + finalUnit;
	}

	public static void setNeedToSave(boolean val){
		needToSave = val;
	}

	@Override
	protected void onResume() {
		super.onResume();
		loadDataFromSharedPreferences(); // ✅ Ensure RecyclerView is updated when reopening the page
	}

	@Override
	public void onBackPressed() {
		if(needToSave) uploadDataToFirebase();
		super.onBackPressed();
	}
}