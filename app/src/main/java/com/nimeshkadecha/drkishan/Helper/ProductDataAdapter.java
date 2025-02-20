package com.nimeshkadecha.drkishan.Helper;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.nimeshkadecha.drkishan.R;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.MessageFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class ProductDataAdapter extends RecyclerView.Adapter<ProductDataAdapter.ProductViewHolder> {

	private Context context;
	private HashMap<Integer, ArrayList<String>> messageMap;
	private List<Integer> sortedKeys;

	private List<String> productMessages;
	private List<String> productDates;
	private List<Double> productQuantities;
	private List<String> productUnits;
//	private Context context;
	private double amount;
	private String amountUnit;
	private String sharedPrefsKey = "DrKishanPrefs"; // SharedPreferences Key

	private Date startDate;

	private int interval;
	private String userName, productName, stage, subStage;

	public ProductDataAdapter(Context context, HashMap<Integer, ArrayList<String>> messageMap, String startDateStr, int interval) {
		this.context = context;
		this.messageMap = messageMap;
		this.sortedKeys = new ArrayList<>(messageMap.keySet());
		Collections.sort(this.sortedKeys); // Sort keys in ascending order

		this.interval = interval;

		try {
			this.startDate = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).parse(startDateStr);
		} catch (ParseException e) {
			e.printStackTrace();
		}


//		Log.d("ENimesh","Date = " + startDateStr + " i =" + interval + " Size -" + sortedKeys.size() +  " mes - " + messageMap.toString() );
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
		holder.itemView.setOnClickListener(v -> showEditDialog(position));
	}

	public void updateList(HashMap<Integer, ArrayList<String>> newMessageMap) {
		this.sortedKeys = new ArrayList<>(newMessageMap.keySet());
		Collections.sort(this.sortedKeys);
		notifyDataSetChanged();
	}

	public void addMessagesToMap(int key, List<String> messages) {
		if (!messageMap.containsKey(key)) {
			messageMap.put(key, new ArrayList<>()); // Create key if not exists
		}

		// ✅ Prevent duplicates using HashSet
		List<String> existingMessages = messageMap.get(key);
		Set<String> uniqueMessages = new HashSet<>(existingMessages);
		uniqueMessages.addAll(messages); // Add only unique values

		// ✅ Convert back to List and update map
		messageMap.put(key, new ArrayList<>(uniqueMessages));

		// ✅ Refresh sorted keys
		sortedKeys = new ArrayList<>(messageMap.keySet());
		Collections.sort(sortedKeys);

		Log.d("ENimesh", "Updated messageMap: " + messageMap);

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

	// ✅ Open Dialog to Edit Message
	private void showEditDialog(int position) {
		if (!(context instanceof Activity) || ((Activity) context).isFinishing()) {
			Log.e("Dialog", "Activity is not valid or is finishing, cannot show dialog");
			return;
		}

		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		builder.setTitle("Edit Message");

		// Inflate custom layout
		View view = LayoutInflater.from(context).inflate(R.layout.dialog_add_detail_messages, null);
		EditText etProductDate = view.findViewById(R.id.etProductDate);
		EditText etMessage = view.findViewById(R.id.etProductMessage);
		EditText etQuantity = view.findViewById(R.id.etProductQuantity);
		Spinner unitSpinner = view.findViewById(R.id.unitSpinner);

		// ✅ Populate Spinner with units
		ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(context, R.array.unit_options, android.R.layout.simple_spinner_item);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		unitSpinner.setAdapter(adapter);

		// ✅ Pre-fill fields
		etProductDate.setText(productDates.get(position)); // ✅ Pre-fill date
		etMessage.setText(productMessages.get(position));

		// ✅ Restore original quantity (undo multiplication for editing)
		double originalQuantity = productQuantities.get(position) / amount;

		// ✅ Set formatted value in EditText (always 2 decimal places)
		etQuantity.setText(formatNumber(originalQuantity));

		// ✅ Set spinner to correct unit
		int unitIndex = adapter.getPosition(productUnits.get(position));
		if (unitIndex >= 0) {
			unitSpinner.setSelection(unitIndex);
		}

		builder.setView(view);

		builder.setPositiveButton("Save", (dialog, which) -> {
			String updatedDate = etProductDate.getText().toString().trim();
			String updatedMessage = etMessage.getText().toString().trim();
			String selectedUnit = unitSpinner.getSelectedItem().toString();

			// ✅ Get quantity from input or keep original
			double updatedQuantity;
			if (!etQuantity.getText().toString().trim().isEmpty()) {
				updatedQuantity = Double.parseDouble(etQuantity.getText().toString().trim());
			} else {
				updatedQuantity = originalQuantity;
			}

			// ✅ Apply multiplication again before saving
			double recalculatedQuantity = updatedQuantity * amount;

			if (!updatedMessage.isEmpty()) {
				updateMessageInStorage(position, updatedMessage, recalculatedQuantity, selectedUnit, updatedDate);
			} else {
				Toast.makeText(context, "Message cannot be empty!", Toast.LENGTH_SHORT).show();
			}
		});

		builder.setNegativeButton("Delete", (dialog, which) -> deleteMessageFromStorage(position));
		builder.setNeutralButton("Cancel", (dialog, which) -> dialog.dismiss());

		AlertDialog alertDialog = builder.create();
		if (context instanceof Activity && !((Activity) context).isFinishing()) {
			alertDialog.show();
		}
	}

	// ✅ Update Message in SharedPreferences
	private void updateMessageInStorage(int position, String updatedMessage, double updatedQuantity, String updatedUnit, String updatedDate) {
		try {
			SharedPreferences prefs = context.getSharedPreferences("DrKishanPrefs", Context.MODE_PRIVATE);
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
				JSONObject updatedObject = new JSONObject();
				updatedObject.put("m", updatedMessage);
				updatedObject.put("q", updatedQuantity / amount); // ✅ Store RAW value
				updatedObject.put("qt", updatedUnit);
				updatedObject.put("date", updatedDate);

				messagesArray.put(position, updatedObject);
			}

			JSONObject formattedData = new JSONObject();
			formattedData.put("value", messagesArray.toString());
			subStageObj.put("data", formattedData);

			prefs.edit().putString("savedJson", jsonObject.toString()).apply();

			productMessages.set(position, updatedMessage);
			productQuantities.set(position, updatedQuantity); // ✅ Save RAW value in list
			productUnits.set(position, updatedUnit);
			productDates.set(position, updatedDate);

			notifyDataSetChanged();
			Toast.makeText(context, "Message Updated!", Toast.LENGTH_SHORT).show();
		} catch (Exception e) {
			Log.e("Storage", "Error updating message", e);
		}
	}

	// ✅ Delete Message from SharedPreferences (No Changes)
	private void deleteMessageFromStorage(int position) {
		try {
			SharedPreferences prefs = context.getSharedPreferences("DrKishanPrefs", Context.MODE_PRIVATE);
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

			productMessages.remove(position);
			productQuantities.remove(position);
			productUnits.remove(position);
			productDates.remove(position);

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
		String date = formatter.format(calendar.getTime());

		return date;
	}

	public static class ProductViewHolder extends RecyclerView.ViewHolder {
		TextView txtDate, txtMessage;
		public ProductViewHolder(@NonNull View itemView) {
			super(itemView);
			txtDate = itemView.findViewById(R.id.txtProductName_d); // ✅ Check ID
			txtMessage = itemView.findViewById(R.id.txtProductDetails); // ✅ Check ID
//			txtQuantity = itemView.findViewById(R.id.quentity); // ✅ Check ID (should be txtQuantity)
		}
	}
}
