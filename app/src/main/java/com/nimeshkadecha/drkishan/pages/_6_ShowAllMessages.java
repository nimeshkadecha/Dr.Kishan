package com.nimeshkadecha.drkishan.pages;

import android.annotation.SuppressLint;
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
	private String productName, stage, subStage, mainDate, userName, unit,headerST,footer;
	private double amount;

	static boolean needToSave = false;

	private boolean isDrip = false;

	private double calculateAm;
	private int interval;

	private HashMap<Integer, ArrayList<String>> messagesMap;
	private JSONObject storedData;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		EdgeToEdge.enable(this);
		setContentView(R.layout.activity_6_show_all_messages);

		setNeedToSave(false);

		// Get Intent Data
		productName = getIntent().getStringExtra("productName");
		userName = getIntent().getStringExtra("userName");
		stage = getIntent().getStringExtra("stage");
		subStage = getIntent().getStringExtra("subStage");
		mainDate = getIntent().getStringExtra("date");
		amount = Double.parseDouble(Objects.requireNonNull(getIntent().getStringExtra("amount")));
		unit = getIntent().getStringExtra("unit");
		headerST = getIntent().getStringExtra("header");
		footer = getIntent().getStringExtra("footer");
		isDrip = getIntent().getBooleanExtra("isDrip", false);
		if(unit != null && unit.equalsIgnoreCase("Vigha")){
			calculateAm = amount;
		}else{
			calculateAm = amount * 2.5d;
		}
		interval = Integer.parseInt(Objects.requireNonNull(getIntent().getStringExtra("days")));

		// ✅ setting header
		TextView header = findViewById(R.id.textView_Header);

		header.setText(MessageFormat.format("FP > {0} > {1} > {2}", extractName(productName), extractName(stage), extractName(subStage)));

		header.setTextSize(20f);

		HorizontalScrollView scrollView = findViewById(R.id.horizontalScrollView);
		scrollView.post(() -> scrollView.smoothScrollTo(header.getWidth(), 0));

		recyclerView = findViewById(R.id.ProductListWithInfo);
		recyclerView.setLayoutManager(new LinearLayoutManager(this));

		messagesMap = new HashMap<>();

		adapter = new ProductDataAdapter(this, messagesMap,mainDate,interval,calculateAm,userName,productName,stage,subStage,isDrip);

		recyclerView.setAdapter(adapter); // ✅ Set adapter after loading data

		loadDataFromSharedPreferences(); // ✅ Load data first

		// Set Click Listener for Add Button
		Button btnAdd = findViewById(R.id.button);
		btnAdd.setOnClickListener(v -> showAddProductDialog());

		// Copy Button Functionality
		Button btnCopy = findViewById(R.id.button2_copy);
		btnCopy.setOnClickListener(v -> copyDataToClipboard(headerST,footer));

		// ✅ Save Button - Always Upload Data to Firebase
		Button btnSave = findViewById(R.id.button_Upload);
		btnSave.setOnClickListener(v -> uploadDataToFirebase()); // ✅ No check, always upload
	}

	// ✅ Load Data from SharedPreferences and Ensure It's Updated with Intent Values
	@SuppressLint("NotifyDataSetChanged")
	private void loadDataFromSharedPreferences() {
		SharedPreferences prefs = getSharedPreferences("DrKishanPrefs", MODE_PRIVATE);
		String savedJson = prefs.getString("savedJson", "{}"); // Default: empty JSON

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


			// ✅ Update storedData with latest Intent value
			storedData.put("count", amount);
			storedData.put("countingValue", unit);
			storedData.put("date", mainDate);
			storedData.put("interval", interval);
			storedData.put("isDrip", isDrip);
			storedData.put("header", headerST);
			storedData.put("footer", footer);

			// ✅ Save updated JSON back to SharedPreferences
			prefs.edit().putString("savedJson", jsonObject.toString()).apply();

			if (storedData.has("data")) {
				JSONObject dataObject = storedData.getJSONObject("data");
				JSONArray messagesArray = new JSONArray(dataObject.getString("value"));

				for (int i = 0; i < messagesArray.length(); i++) {
					JSONObject obj = messagesArray.getJSONObject(i);
					String formattedMessage;

					int k = obj.getInt("k");
					formattedMessage = obj.getString("m");
					if (!isDrip) {
						double q = obj.getDouble("q") * calculateAm;
						String qt = obj.getString("qt");
						String formattedQuantity = formatQuantity(q, qt);
						formattedMessage += " - " + formattedQuantity;
					}

					messagesMap.computeIfAbsent(k, key -> new ArrayList<>()).add(formattedMessage);
				}

				runOnUiThread(() -> {
					if (adapter != null) {
						adapter.updateList(messagesMap);
					} else {
						adapter = new ProductDataAdapter(this, messagesMap,mainDate,interval,calculateAm,userName,productName,stage,subStage,isDrip);
						recyclerView.setAdapter(adapter);
					}
						adapter.notifyDataSetChanged();
				});
			}

		} catch (JSONException e) {
			Log.e("SharedPreferences", "Error parsing JSON", e);
		}
	}

	@SuppressLint("NotifyDataSetChanged")
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

		// ✅ Hide quantity and unit if `isDrip` is true
		if (isDrip) {
			etProductQuantity.setVisibility(View.GONE);
			unitSpinner.setVisibility(View.GONE);
		} else {
			etProductQuantity.setVisibility(View.VISIBLE);
			unitSpinner.setVisibility(View.VISIBLE);

			// ✅ Populate Spinner if `isDrip` is false
			ArrayAdapter<CharSequence> arr_adapter = ArrayAdapter.createFromResource(this, R.array.unit_options, android.R.layout.simple_spinner_item);
			arr_adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
			unitSpinner.setAdapter(arr_adapter);
		}

		List<String> dialogList = new ArrayList<>();
		DialogMessageAdapter dialogAdapter = new DialogMessageAdapter(isDrip, dialogList, (message, quantity, unit) -> {
			etProductMessage.setText(message);
			if (!isDrip) {
				etProductQuantity.setText(quantity);

				// ✅ Set spinner selection dynamically
				for (int i = 0; i < unitSpinner.getAdapter().getCount(); i++) {
					if (unitSpinner.getAdapter().getItem(i).toString().equalsIgnoreCase(unit)) {
						unitSpinner.setSelection(i);
						break;
					}
				}
			}
		});

		recyclerViewMessages.setAdapter(dialogAdapter);
		recyclerViewMessages.setLayoutManager(new LinearLayoutManager(this));

		SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
		Calendar calendar = Calendar.getInstance();

		try {
			calendar.setTime(Objects.requireNonNull(sdf.parse(mainDate)));
			int numberOfKeys = messagesMap.size();
			int totalDaysToAdd = numberOfKeys * interval;
			calendar.add(Calendar.DAY_OF_MONTH, totalDaysToAdd);
		} catch (ParseException e) {
			e.printStackTrace();
		}

		etProductDate.setText(sdf.format(calendar.getTime()));

		addMsg.setOnClickListener(v -> {
			String newMessage = etProductMessage.getText().toString().trim();
			if (newMessage.isEmpty()) {
				Toast.makeText(this, "Message cannot be empty!", Toast.LENGTH_SHORT).show();
				return;
			}

			String entry;
				entry = newMessage;
			if (!isDrip) {
				// ✅ Include quantity and unit if `isDrip` is false
				String selectedUnit = unitSpinner.getSelectedItem().toString();
				String rawQuantity = etProductQuantity.getText().toString().trim();

				// ✅ Remove any characters that are not digits or '.'
				rawQuantity = rawQuantity.replaceAll("[^0-9.]", "");

				String formattedQuantity;
				try {
					double newQuantity = Double.parseDouble(rawQuantity) * calculateAm;
					formattedQuantity = formatQuantity(newQuantity, selectedUnit);
				} catch (NumberFormatException e) {
					Toast.makeText(this, "Enter a valid quantity", Toast.LENGTH_SHORT).show();
					return;
				}
				entry += " - " + formattedQuantity;
			}

			if (!dialogList.contains(entry)) {
				dialogList.add(entry);
				dialogAdapter.notifyDataSetChanged(); // Refresh RecyclerView
			} else {
				Log.d("ENimesh", "Duplicate skipped: " + entry);
			}

			// ✅ Clear input fields after adding
			etProductMessage.setText("");
			if (!isDrip) {
				etProductQuantity.setText("");
			}
		});

		builder.setView(view);
		builder.setPositiveButton("Save", (dialog, which) -> {
			if (dialogList.isEmpty()) {
				Toast.makeText(this, "No messages to save", Toast.LENGTH_SHORT).show();
				return;
			}

			int newKey = messagesMap.isEmpty() ? 1 : Collections.max(messagesMap.keySet()) + 1;
			messagesMap.put(newKey, new ArrayList<>(dialogList));
			adapter.addMessagesToMap(newKey, new ArrayList<>(dialogList));
			adapter.notifyDataSetChanged();
			addDataLocally(newKey);
		});

		builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());

		builder.create().show();
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
					// Split the message format "message - quantity unit"
					String[] parts = messageEntry.split(" - ");
					if (parts.length < 2 && !isDrip) {
						Log.e("ENimesh", "Unexpected message format: " + messageEntry);
						continue;
					}

					JSONObject messageObj = new JSONObject();
					messageObj.put("m", parts[0].trim()); // Store message
					messageObj.put("k", key); // Store key

					if (!isDrip) { // Only add quantity & unit if isDrip is false
						String qtyUnit = parts[1].trim();
						String quantityStr = qtyUnit.replaceAll("[^\\d.]", "").trim(); // Extract number
						String unitStr = qtyUnit.replaceAll("[\\d.]", "").trim(); // Extract unit

						double quantityValue = 0;
						try {
							quantityValue = Double.parseDouble(quantityStr);
						} catch (NumberFormatException e) {
							Log.e("ENimesh", "Failed to parse quantity: " + quantityStr, e);
						}

						double originalQuantity = quantityValue / amount; // Adjust based on `amount`
						messageObj.put("q", originalQuantity);
						messageObj.put("qt", unitStr);
					}

					messagesArray.put(messageObj);
				}
			}

			// ✅ Replace the data field with the new messages array (stored as JSON string)
			subStageObject.put("data", messagesArray.toString());

			// ✅ Update other fields: count, countingValue, date, interval, isDrip
			String[] keysToFix = {"count", "countingValue", "date", "interval", "isDrip","footer","header"};
			for (String key : keysToFix) {
				if (subStageObject.has(key)) {
					Object valueObj = subStageObject.get(key);
					if (valueObj instanceof JSONObject) {
						JSONObject valueJson = (JSONObject) valueObj;
						if (valueJson.has("value")) {
							subStageObject.put(key, valueJson.get("value"));
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
							subStageObject.put(key, isDrip ? "" : unit); // Empty if isDrip is true
							break;
						case "date":
							subStageObject.put(key, mainDate);
							break;
						case "interval":
							subStageObject.put(key, interval);
							break;
							case "header":
							subStageObject.put(key, "");
							break;
						case "footer":
							subStageObject.put(key, "");
						case "isDrip":
							subStageObject.put(key, isDrip); // Store actual value of isDrip
							break;
					}
				}
			}

			// ✅ Convert updated subStageObject to a Map for Firebase upload.
			Map<String, Object> firebaseData = jsonToMapWithoutValueWrapper(subStageObject);

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

			setNeedToSave(false);

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

	private void addDataLocally(int key) {
		try {
			SharedPreferences prefs = getSharedPreferences("DrKishanPrefs", MODE_PRIVATE);
			String savedJson = prefs.getString("savedJson", "{}"); // Default empty JSON

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
					String[] parts = message.split(" - ");
					messageObj.put("m", parts[0]); // Extract message
					if(!isDrip){
						messageObj.put("q", parts[1].replaceAll("[^\\d.]", ""));
						messageObj.put("qt", parts[1].replaceAll("[^a-zA-Z]", "")); // Extract unit only
					}
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

		// ✅ Format Header if provided
		if (!header.isEmpty()) {
			String[] headerLines = header.split("\\n");
			for (String line : headerLines) {
				copiedText.append("*").append(line).append("*\n");
			}
			copiedText.append("\n"); // Extra line break after header
		}

		// ✅ Check if interval is 0 (add all messages to mainDate)
		if (interval == 0) {
			StringBuilder finalCopiedString = new StringBuilder();

			// Append messages to mainDate directly
			for (ArrayList<String> messages : messagesMap.values()) {
				for (String message : messages) {
					finalCopiedString.append("- ").append(translateUnits(message)).append("\n");
				}
			}

			copiedText.append("*").append(mainDate).append("*\n");
			copiedText.append(finalCopiedString).append("\n");
		} else {
			// ✅ Iterate over each `k` in messagesMap (sorted automatically)
			int counter = 0;
			for (Integer k : messagesMap.keySet()) {
				String date = addDaysToDate(mainDate, counter * interval); // ✅ Generate date dynamically
				copiedText.append("*").append(date).append("*\n"); // ✅ Append Date
				counter++; // Increment for next interval

				// ✅ Retrieve messages for the current `k`
				ArrayList<String> messages = messagesMap.get(k);

				if (messages != null) {
					StringBuilder finalCopiedString = new StringBuilder();
					for (String message : messages) {
						finalCopiedString.append(translateUnits(message)).append("\n");
					}
					copiedText.append(finalCopiedString).append("\n"); // ✅ Append Message
				}
			}
		}

		// ✅ Format Footer if provided
		if (!footer.isEmpty()) {
			String[] footerLines = footer.split("\\n");
			for (String line : footerLines) {
				copiedText.append("*").append(line).append("*\n");
			}
		}

		// ✅ Copy to Clipboard
		ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
		ClipData clip = ClipData.newPlainText("Product Data", copiedText.toString());
		clipboard.setPrimaryClip(clip);

		Toast.makeText(this, "Copied to Clipboard!", Toast.LENGTH_SHORT).show();
	}

	// ✅ Function to translate units while keeping other text unchanged
	private String translateUnits(String message) {
		return message.replace("grams", "ગ્રામ")
										.replace("KG.", "કિલો")
										.replace("KG", "કિલો")
										.replace("Letter", "લિટર")
										.replace("liter", "લિટર")
										.replace("ML", "મિલી");
	}

	private String addDaysToDate(String startDate, int daysToAdd) {
		SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
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
		String finalUnit;

		switch (unit.toLowerCase()) {
			case "ml":
			case "milliliter":
				if (quantity >= 1000) {
					convertedQuantity = quantity / 1000;
					finalUnit = "liter"; // Convert ML to L
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
				finalUnit = "liter"; // Convert L to Letter
				break;

			default:
				finalUnit = unit; // Keep the original unit if unrecognized
		}

		return formatNumber(convertedQuantity) + finalUnit;
	}

	public static void setNeedToSave(boolean val){
		needToSave = val;
	}

	private String extractName(String item) {
		return item.contains("@") ? item.substring(item.indexOf("@") + 1) : item;
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