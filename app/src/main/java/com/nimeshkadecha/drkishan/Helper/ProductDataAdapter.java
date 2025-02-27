package com.nimeshkadecha.drkishan.Helper;

import static android.content.Context.MODE_PRIVATE;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.nimeshkadecha.drkishan.R;
import com.nimeshkadecha.drkishan.pages._6_ShowAllMessages;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class ProductDataAdapter extends RecyclerView.Adapter<ProductDataAdapter.ProductViewHolder> {

	private final Context context;
	private final HashMap<Integer, ArrayList<String>> messageMap;
	private List<Integer> sortedKeys;

	private Date startDate;

	private final double calculateAm;
	private final int interval;
	private final String userName;
	private final String productName;
	private final String stage;
	private final String subStage;

	public ProductDataAdapter(Context context, HashMap<Integer, ArrayList<String>> messageMap, String startDateStr, int interval,double calculateAm,String userName,String productName,String stage,String subStage) {
		this.context = context;
		this.messageMap = messageMap;
		this.calculateAm = calculateAm;
		this.userName = userName;
		this.productName = productName;
		this.stage = stage;
		this.subStage = subStage;
		this.sortedKeys = new ArrayList<>(messageMap.keySet());
		Collections.sort(this.sortedKeys); // Sort keys in ascending order

		this.interval = interval;

		try {
			this.startDate = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).parse(startDateStr);
		} catch (ParseException e) {
			e.printStackTrace();
		}
	}

	@NonNull
	@Override
	public ProductViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
		View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_detail_message, parent, false);
		return new ProductViewHolder(view);
	}

	@Override
	public void onBindViewHolder(@NonNull ProductViewHolder holder, int position) {

		int key = sortedKeys.get(position);
		ArrayList<String> messages = messageMap.get(key);

		if (messages != null) {
			String combinedMessage = TextUtils.join("\n", messages);
			holder.txtMessage.setText(combinedMessage);
		}

		holder.txtDate.setText(getDate(position));

		// ✅ Set Click Listener for Editing
		holder.itemView.setOnClickListener(v -> showEditDialog(position, key));
	}

	@SuppressLint("NotifyDataSetChanged")
	public void updateList(HashMap<Integer, ArrayList<String>> newMessageMap) {
		// Create a new HashMap to store unique messages
		HashMap<Integer, ArrayList<String>> uniqueMessageMap = new HashMap<>();

		for (Map.Entry<Integer, ArrayList<String>> entry : newMessageMap.entrySet()) {
			HashSet<String> uniqueMessages = new HashSet<>(entry.getValue()); // Remove duplicates
			uniqueMessageMap.put(entry.getKey(), new ArrayList<>(uniqueMessages)); // Convert back to ArrayList
		}

		// Update sortedKeys with unique keys
		this.sortedKeys = new ArrayList<>(uniqueMessageMap.keySet());
		Collections.sort(this.sortedKeys);

		// Update the adapter’s dataset (if necessary)
		newMessageMap.clear();
		newMessageMap.putAll(uniqueMessageMap);

		notifyDataSetChanged();
	}

	@SuppressLint("NotifyDataSetChanged")
	public void addMessagesToMap(int key, List<String> messages) {
		if (!messageMap.containsKey(key)) {
			messageMap.put(key, new ArrayList<>()); // Create key if not exists
		}

		// ✅ Prevent duplicates using HashSet
		List<String> existingMessages = messageMap.get(key);
		Set<String> uniqueMessages = new HashSet<>(Objects.requireNonNull(existingMessages));
		uniqueMessages.addAll(messages); // Add only unique values

		// ✅ Convert back to List and update map
		messageMap.put(key, new ArrayList<>(uniqueMessages));

		// ✅ Refresh sorted keys
		sortedKeys = new ArrayList<>(messageMap.keySet());
		Collections.sort(sortedKeys);

		notifyDataSetChanged(); // Refresh RecyclerView
	}

	@Override
	public int getItemCount() {
		return sortedKeys.size();
	}

	// ✅ Formats numbers with commas
	private String formatNumber(double value) {
		double roundedValue = Math.round(value * 100.0) / 100.0; // ✅ Round to 2 decimal places
		return String.format(Locale.US, "%.2f", roundedValue); // ✅ Always 2 decimal places
	}
	@SuppressLint("NotifyDataSetChanged")
	private void showEditDialog(int position, int key) {

		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		builder.setTitle("Edit Messages");

		View view = LayoutInflater.from(context).inflate(R.layout.dialog_add_detail_messages, null);
		EditText etProductMessage = view.findViewById(R.id.etProductMessage);
		EditText etProductQuantity = view.findViewById(R.id.etProductQuantity);
		EditText etProductDate = view.findViewById(R.id.etProductDate);
		Spinner unitSpinner = view.findViewById(R.id.unitSpinner);
		Button addMsg = view.findViewById(R.id.addMsg);
		RecyclerView recyclerViewMessages = view.findViewById(R.id.recyclerViewMessages);
		recyclerViewMessages.setLayoutManager(new LinearLayoutManager(context));

		ArrayAdapter<CharSequence> arr_adapter = ArrayAdapter.createFromResource(context, R.array.unit_options, android.R.layout.simple_spinner_item);
		arr_adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		unitSpinner.setAdapter(arr_adapter);

		// Create a copy of the message list for this key
		List<String> dialogList = new ArrayList<>(Objects.requireNonNull(messageMap.getOrDefault(key, new ArrayList<>())));

		// Create adapter with a modified listener to remove the clicked item after setting fields.
		final DialogMessageAdapter[] adapterRef = new DialogMessageAdapter[1];
		adapterRef[0] = new DialogMessageAdapter(dialogList, (message, quantity, unit) -> {
			// Set the fields with the item's details
			etProductMessage.setText(message);
			etProductQuantity.setText(quantity);
			etProductDate.setText(getDate(position)); // assuming getDate(position) returns the proper date string

			// Set spinner selection dynamically
			for (int i = 0; i < unitSpinner.getAdapter().getCount(); i++) {
				if (unitSpinner.getAdapter().getItem(i).toString().equalsIgnoreCase(unit)) {
					unitSpinner.setSelection(i);
					break;
				}
			}

			// Remove the item from the list after populating the fields
			int index = dialogList.indexOf(message + " - " + quantity + " " + unit);
			if (index != -1) {
				dialogList.remove(index);
				adapterRef[0].notifyItemRemoved(index);
			}
		});

		recyclerViewMessages.setAdapter(adapterRef[0]);

		etProductDate.setText(getDate(position));

		addMsg.setOnClickListener(v -> {
			String newMessage = etProductMessage.getText().toString().trim();
			String selectedUnit = unitSpinner.getSelectedItem().toString();

			if (newMessage.isEmpty()) {
				Toast.makeText(context, "Message cannot be empty!", Toast.LENGTH_SHORT).show();
				return;
			}

			String formattedQuantity;
			try {
				double newQuantity = Double.parseDouble(etProductQuantity.getText().toString().trim())*calculateAm;
				formattedQuantity = formatQuantity(newQuantity,selectedUnit);
			} catch (NumberFormatException e) {
				return;
			}

			String entry = newMessage + " - " + formattedQuantity;

			// Check if entry already exists in dialogList
			int editIndex = dialogList.indexOf(entry);
			if (editIndex != -1) {
				// Replace the existing item
				dialogList.set(editIndex, entry);
			} else {
				dialogList.add(entry);
			}
			adapterRef[0].notifyDataSetChanged();

			// Clear fields after adding/updating
			etProductMessage.setText("");
			etProductQuantity.setText("");
		});

		builder.setView(view);
		builder.setPositiveButton("Save", (dialog, which) -> {
			if (dialogList.isEmpty()) {
				// Show a confirmation dialog if dialogList is empty
				new AlertDialog.Builder(context)
												.setTitle("Confirm Delete")
												.setMessage("The list is empty and the product will be deleted. Do you want to continue?")
												.setPositiveButton("Yes", (confirmDialog, confirmWhich) -> {

													// Remove the product
													messageMap.remove(key);
													sortedKeys.remove((Integer) key);
													editDataLocally(key);
													Toast.makeText(context, "Updated", Toast.LENGTH_SHORT).show();
													notifyDataSetChanged();
												})
												.setNegativeButton("No", (confirmDialog, confirmWhich) -> {
													// Just dismiss the confirmation dialog
													confirmDialog.dismiss();
												})
												.show();
			} else {
				// If the list is not empty, proceed normally
				messageMap.put(key, new ArrayList<>(dialogList));
				editDataLocally(key);
				Toast.makeText(context, "Updated", Toast.LENGTH_SHORT).show();
				notifyDataSetChanged();
			}
		});


		builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());

		builder.setNeutralButton("Delete",(dialog, which) -> {
			deleteMessageFromStorage(position);
			messageMap.remove(key);
			sortedKeys.remove((Integer) key);
			notifyDataSetChanged();
		});
		builder.create().show();
	}
	private void editDataLocally(int key) {
		try {
			SharedPreferences prefs = context.getSharedPreferences("DrKishanPrefs", MODE_PRIVATE);
			String savedJson = prefs.getString("savedJson", "{}"); // Default empty JSON

			JSONObject jsonObject = new JSONObject(savedJson);

			// Ensure correct structure exists
			if (!jsonObject.has(userName)) return;
			JSONObject usersObj = jsonObject.getJSONObject(userName);

			if (!usersObj.has(productName)) return;
			JSONObject productObj = usersObj.getJSONObject(productName);

			if (!productObj.has(stage)) return;
			JSONObject stageObj = productObj.getJSONObject(stage);

			if (!stageObj.has(subStage)) return;
			JSONObject subStageObj = stageObj.getJSONObject(subStage);

			if (!subStageObj.has("data")) return;

			// Retrieve existing data array
			JSONArray existingDataArray = new JSONArray(subStageObj.getJSONObject("data").getString("value"));

			// Remove old messages for the given key
			JSONArray updatedDataArray = new JSONArray();
			for (int i = 0; i < existingDataArray.length(); i++) {
				JSONObject messageObj = existingDataArray.getJSONObject(i);
				if (messageObj.getInt("k") != key) {
					updatedDataArray.put(messageObj); // Keep messages from other keys
				}
			}

			// Add updated messages from messageMap
			if (messageMap.containsKey(key)) {
				for (String message : Objects.requireNonNull(messageMap.get(key))) {
					JSONObject newMessageObj = new JSONObject();
					String[] parts = message.split(" - ");
					newMessageObj.put("m", parts[0]); // Extract message text
					newMessageObj.put("q", parts[1].replaceAll("[^\\d.]", ""));
					newMessageObj.put("qt", parts[1].replaceAll("[^a-zA-Z]", "")); // Extract unit
					newMessageObj.put("k", key); // Store key
					updatedDataArray.put(newMessageObj);
				}
			}else{
				Log.d("ENimesh","Key Not found");
			}

			// Store updated messages in correct format
			JSONObject formattedData = new JSONObject();
			formattedData.put("value", updatedDataArray.toString());
			subStageObj.put("data", formattedData);

			// Save updated JSON to SharedPreferences
			prefs.edit().putString("savedJson", jsonObject.toString()).apply();

			Toast.makeText(context, "Data Edited Successfully!", Toast.LENGTH_SHORT).show();

			_6_ShowAllMessages.setNeedToSave(true);
		} catch (JSONException e) {
			Log.e("SharedPreferences", "Error updating JSON", e);
			Toast.makeText(context, "Failed to edit data!", Toast.LENGTH_SHORT).show();
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

		return formatNumber(convertedQuantity) + " " + finalUnit;
	}

	// ✅ Delete Message from SharedPreferences (No Changes)
	@SuppressLint("NotifyDataSetChanged")
	private void deleteMessageFromStorage(int position) {
		try {
			SharedPreferences prefs = context.getSharedPreferences("DrKishanPrefs", MODE_PRIVATE);
			String savedJson = prefs.getString("savedJson", "{}");
			JSONObject jsonObject = new JSONObject(savedJson);

			if (!jsonObject.has(userName)) return;
			JSONObject usersObj = jsonObject.getJSONObject(userName);

			if (!usersObj.has(productName)) return;
			JSONObject productObj = usersObj.getJSONObject(productName);

			if (!productObj.has(stage)) return;
			JSONObject stageObj = productObj.getJSONObject(stage);

			if (!stageObj.has(subStage)) return;
			JSONObject subStageObj = stageObj.getJSONObject(subStage);

			JSONArray messagesArray = new JSONArray();
			if (subStageObj.has("data")) {
				messagesArray = new JSONArray(subStageObj.getJSONObject("data").getString("value"));
			}

			if (position >= 0 && position < messagesArray.length()) {
				messagesArray.remove(position);
			}

			JSONObject formattedData = new JSONObject();
			formattedData.put("value", messagesArray.toString());
			subStageObj.put("data", formattedData);

			prefs.edit().putString("savedJson", jsonObject.toString()).apply();

			notifyDataSetChanged();
			Toast.makeText(context, "Message Deleted Successfully!", Toast.LENGTH_SHORT).show();
		} catch (Exception e) {
			Log.e("Storage", "Error deleting message", e);
		}
	}

	private String getDate(int position){

		// Calculate date dynamically instead of using a separate list
		SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(startDate);
		calendar.add(Calendar.DAY_OF_MONTH, position * interval);

		return formatter.format(calendar.getTime());
	}

	public static class ProductViewHolder extends RecyclerView.ViewHolder {
		TextView txtDate, txtMessage;
		public ProductViewHolder(@NonNull View itemView) {
			super(itemView);
			txtDate = itemView.findViewById(R.id.txtProductName_d);
			txtMessage = itemView.findViewById(R.id.txtProductDetails);
		}
	}
}
