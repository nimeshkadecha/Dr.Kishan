package com.nimeshkadecha.drkishan;

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

	public ProductDataAdapter(Context context, List<String> productDates, List<String> productMessages,
	                          List<Double> productQuantities, List<String> productUnits, double amount, String amountUnit) {
		this.context = context;
		this.amount = amount;
		this.productDates = (productDates != null) ? productDates : new ArrayList<>();
		this.productMessages = (productMessages != null) ? productMessages : new ArrayList<>();
		this.productQuantities = (productQuantities != null) ? productQuantities : new ArrayList<>();
		this.productUnits = (productUnits != null) ? productUnits : new ArrayList<>();
	}

	@NonNull
	@Override
	public ProductViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
		View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.product_details, parent, false);
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
		if (value % 1 == 0) {
			return String.format(Locale.US, "%,d", (long) value); // Whole number
		} else {
			return String.format(Locale.US, "%,.2f", value); // Two decimal places
		}
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
		View view = LayoutInflater.from(context).inflate(R.layout.dialog_add_product_data, null);
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

		// ✅ Restore original quantity (undo multiplication)
		double originalQuantity = productQuantities.get(position) / amount;
		etQuantity.setText(String.valueOf((int) originalQuantity));

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
			int updatedQuantity;
			if (!etQuantity.getText().toString().trim().isEmpty()) {
				updatedQuantity = Integer.parseInt(etQuantity.getText().toString().trim());
			} else {
				updatedQuantity = (int) originalQuantity;
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
			SharedPreferences prefs = context.getSharedPreferences(sharedPrefsKey, Context.MODE_PRIVATE);
			String jsonData = prefs.getString("savedMessages", "[]");

			JSONArray dataArray = new JSONArray(jsonData);
			JSONObject updatedObject = new JSONObject();
			updatedObject.put("m", updatedMessage);
			updatedObject.put("q", updatedQuantity);
			updatedObject.put("qt", updatedUnit);
			updatedObject.put("date", updatedDate); // ✅ Save updated date

			dataArray.put(position, updatedObject);

			// ✅ Save updated JSON
			prefs.edit().putString("savedMessages", dataArray.toString()).apply();

			// ✅ Update UI lists
			productMessages.set(position, updatedMessage);
			productQuantities.set(position, updatedQuantity);
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
			SharedPreferences prefs = context.getSharedPreferences(sharedPrefsKey, Context.MODE_PRIVATE);
			String jsonData = prefs.getString("savedMessages", "[]");

			JSONArray dataArray = new JSONArray(jsonData);

			// ✅ Ensure position is valid before deleting
			if (position >= 0 && position < dataArray.length()) {
				dataArray.remove(position);
			}

			// ✅ Save updated JSON back to SharedPreferences
			prefs.edit().putString("savedMessages", dataArray.toString()).apply();

			// ✅ Remove from local lists
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
