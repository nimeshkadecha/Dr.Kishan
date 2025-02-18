package com.nimeshkadecha.drkishan.Helper;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ProductDataAdapter extends RecyclerView.Adapter<ProductDataAdapter.ProductViewHolder> {
	private List<String> productMessages;
	private List<String> productDates;
	private List<Double> productQuantities;
	private List<String> productUnits;
	private Context context;
	private double amount;
	private String amountUnit;
	private String sharedPrefsKey = "DrKishanData"; // SharedPreferences Key

	private String userName, productName, stage, subStage;

	public ProductDataAdapter(Context context, List<String> productDates, List<String> productMessages,
	                          List<Double> productQuantities, List<String> productUnits,
	                          double amount, String amountUnit,
	                          String userName, String productName, String stage, String subStage) {
		this.context = context;
		this.amount = amount;
		this.amountUnit = amountUnit;
		this.userName = userName;
		this.productName = productName;
		this.stage = stage;
		this.subStage = subStage;

		this.productDates = (productDates != null) ? productDates : new ArrayList<>();
		this.productMessages = (productMessages != null) ? productMessages : new ArrayList<>();
		this.productQuantities = (productQuantities != null) ? productQuantities : new ArrayList<>();
		this.productUnits = (productUnits != null) ? productUnits : new ArrayList<>();
	}


	@NonNull
	@Override
	public ProductViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
		View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.detail_message_list, parent, false);
		return new ProductViewHolder(view);
	}

	@Override
	public void onBindViewHolder(@NonNull ProductViewHolder holder, int position) {
		String date = productDates.get(position);
		String message = productMessages.get(position);
		double quantity = (position < productQuantities.size()) ? productQuantities.get(position) : 0;
		String unit = (position < productUnits.size()) ? productUnits.get(position) : "";


		// ✅ Convert Quantity if necessary
		String formattedQuantity = formatQuantity(quantity, unit);

		holder.txtProduct.setText("Date: " + date);
		holder.txtDetails.setText("Message: " + message);
		holder.txtQuantity.setText(formattedQuantity); // ✅ Display correct quantity

		// ✅ Set Click Listener for Editing
		holder.itemView.setOnClickListener(v -> showEditDialog(position));
	}

	public void updateList(List<String> newProductDates, List<String> newProductMessages,
	                       List<Double> newProductQuantities, List<String> newProductUnits) {
		if (newProductDates == null || newProductMessages == null || newProductQuantities == null || newProductUnits == null) {
			Log.e("AdapterUpdate", "Received null values");
			return;
		}

		Log.d("AdapterUpdate", "Updating adapter with: " + newProductDates.size() + " items");

		productDates.clear();
		productMessages.clear();
		productQuantities.clear();
		productUnits.clear();

		productDates.addAll(newProductDates);
		productMessages.addAll(newProductMessages);

		// ✅ Convert and store adjusted quantities
		List<Double> adjustedQuantities = new ArrayList<>();
		for (Double q : newProductQuantities) {
			adjustedQuantities.add(q * amount); // ✅ Correct multiplication
		}
		productQuantities.addAll(adjustedQuantities);
		productUnits.addAll(newProductUnits);

		notifyDataSetChanged();
	}

	@Override
	public int getItemCount() {
		return productDates.size();
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



	public static class ProductViewHolder extends RecyclerView.ViewHolder {
		TextView txtProduct, txtDetails, txtQuantity;


		public ProductViewHolder(@NonNull View itemView) {
			super(itemView);
			txtProduct = itemView.findViewById(R.id.txtProductName_d); // ✅ Check ID
			txtDetails = itemView.findViewById(R.id.txtProductDetails); // ✅ Check ID
			txtQuantity = itemView.findViewById(R.id.quentity); // ✅ Check ID (should be txtQuantity)
		}
	}
}
