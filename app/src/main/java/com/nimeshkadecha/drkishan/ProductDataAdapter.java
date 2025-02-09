package com.nimeshkadecha.drkishan;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;

import org.json.JSONArray;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class ProductDataAdapter extends RecyclerView.Adapter<ProductDataAdapter.ProductViewHolder> {
	private List<String> productMessages;
	private List<String> productDates;
	private DatabaseReference reference;
	private Context context;

	private int interval;

	public ProductDataAdapter(Context context, List<String> productDates, List<String> productMessages, DatabaseReference reference,int interval) {
		this.context = context;
		this.interval = interval;
		this.reference = reference;
		this.productDates = (productDates != null) ? productDates : new ArrayList<>();
		this.productMessages = (productMessages != null) ? productMessages : new ArrayList<>();
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

		Log.d("Adapter", "Binding Date: " + date);
		Log.d("Adapter", "Binding Message: " + message);

		holder.txtProduct.setText("Date: " + date);
		holder.txtDetails.setText("Message: " + message);

		// ✅ Set Click Listener for Editing
		holder.itemView.setOnClickListener(v -> showEditDialog(position));
	}

	@Override
	public int getItemCount() {
		return productDates.size();
	}

	public void updateList(List<String> newProductDates, List<String> newProductMessages) {
		if (newProductDates == null || newProductMessages == null) {
			Log.e("Adapter", "updateList() received null lists!");
			return;
		}

		Log.d("Adapter", "Received Dates: " + newProductDates.toString());
		Log.d("Adapter", "Received Messages: " + newProductMessages.toString());

		if (productDates == null) productDates = new ArrayList<>();
		if (productMessages == null) productMessages = new ArrayList<>();

		// Ensure we are only adding NEW data
		for (int i = 0; i < newProductDates.size(); i++) {
			if (!productDates.contains(newProductDates.get(i))) { // Prevent duplicate dates
				productDates.add(newProductDates.get(i));
				productMessages.add(newProductMessages.get(i));
			}
		}

		notifyDataSetChanged();
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
		EditText etMessage = view.findViewById(R.id.etProductMessage);
		EditText etDate = view.findViewById(R.id.etProductDate); // Get Date field

		etMessage.setText(productMessages.get(position)); // Pre-fill current message

		// ✅ Hide the Date Field (Set it as GONE)
		if (etDate != null) {
			etDate.setVisibility(View.GONE);
		}

		builder.setView(view);

		// ✅ Save Button Click (Update Message)
		builder.setPositiveButton("Save", (dialog, which) -> {
			String updatedMessage = etMessage.getText().toString().trim();
			if (!updatedMessage.isEmpty()) {
				updateMessageInFirebase(position, updatedMessage);
			} else {
				Toast.makeText(context, "Message cannot be empty!", Toast.LENGTH_SHORT).show();
			}
		});

		// ✅ Delete Button Click (Remove Message)
		builder.setNegativeButton("Delete", (dialog, which) -> {
			deleteMessageFromFirebase(position);
		});

		// Cancel Button
		builder.setNeutralButton("Cancel", (dialog, which) -> dialog.dismiss());

		AlertDialog alertDialog = builder.create();

		// ✅ Prevent crash if activity is destroyed
		if (context instanceof Activity && !((Activity) context).isFinishing()) {
			alertDialog.show();
		}
	}
	private void deleteMessageFromFirebase(int position) {
		reference.addListenerForSingleValueEvent(new ValueEventListener() {
			@Override
			public void onDataChange(@NonNull DataSnapshot snapshot) {
				List<String> messages = new ArrayList<>();

				// ✅ Extract existing data from Firebase
				if (snapshot.child("data").exists()) {
					try {
						String rawData = snapshot.child("data").getValue(String.class);
						JSONArray dataArray = new JSONArray(rawData);

						for (int i = 0; i < dataArray.length(); i++) {
							messages.add(dataArray.getString(i));
						}
					} catch (Exception e) {
						Log.e("Firebase", "Error parsing existing data", e);
						return;
					}
				}

				// ✅ Remove the selected message
				if (position >= 0 && position < messages.size()) {
					messages.remove(position);
				}

				// ✅ Convert the updated list back to a proper JSON string
				String updatedDataString = new JSONArray(messages).toString();

				// ✅ Update Firebase with the corrected format
				reference.child("data").setValue(updatedDataString)
												.addOnSuccessListener(aVoid -> {
													productMessages.remove(position);
													productDates.remove(position);

													// ✅ Recalculate dates after deletion
													recalculateDates();

													notifyDataSetChanged();
													Toast.makeText(context, "Message Deleted Successfully!", Toast.LENGTH_SHORT).show();
												})
												.addOnFailureListener(e -> {
													Toast.makeText(context, "Failed to delete message!", Toast.LENGTH_SHORT).show();
													Log.e("Firebase", "Error deleting message", e);
												});
			}

			@Override
			public void onCancelled(@NonNull DatabaseError error) {
				Log.e("Firebase", "Error reading database", error.toException());
			}
		});
	}

	private void recalculateDates() {
		if (productDates.isEmpty()) return;

		List<String> newDates = new ArrayList<>();
		try {
			SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
			Calendar calendar = Calendar.getInstance();
			calendar.setTime(sdf.parse(productDates.get(0)));

			for (int i = 0; i < productMessages.size(); i++) {
				newDates.add(sdf.format(calendar.getTime()));
				calendar.add(Calendar.DAY_OF_MONTH, interval);
			}

			productDates.clear();
			productDates.addAll(newDates);
			notifyDataSetChanged();
		} catch (ParseException e) {
			Log.e("Date", "Error parsing date", e);
		}
	}


	// ✅ Update Message in Firebase at the same position
	private void updateMessageInFirebase(int position, String updatedMessage) {
		productMessages.set(position, updatedMessage); // Update locally
		notifyDataSetChanged(); // Refresh UI

		reference.child("data").get().addOnSuccessListener(snapshot -> {
			if (snapshot.exists()) {
				List<String> messages = new ArrayList<>();

				try {
					String dataString = snapshot.getValue(String.class);
					org.json.JSONArray dataArray = new org.json.JSONArray(dataString);

					for (int i = 0; i < dataArray.length(); i++) {
						messages.add(dataArray.getString(i));
					}

					// ✅ Update only the edited message
					messages.set(position, updatedMessage);
					String updatedDataString = new org.json.JSONArray(messages).toString();

					// ✅ Save back to Firebase
					reference.child("data").setValue(updatedDataString)
													.addOnSuccessListener(aVoid -> Log.d("Firebase", "Message updated successfully!"))
													.addOnFailureListener(e -> Log.e("Firebase", "Failed to update message", e));
				} catch (Exception e) {
					Log.e("Firebase", "Error updating message", e);
				}
			}
		}).addOnFailureListener(e -> Log.e("Firebase", "Failed to read database", e));
	}

	public static class ProductViewHolder extends RecyclerView.ViewHolder {
		TextView txtProduct, txtDetails;

		public ProductViewHolder(@NonNull View itemView) {
			super(itemView);
			txtProduct = itemView.findViewById(R.id.txtProductName_d);
			txtDetails = itemView.findViewById(R.id.txtProductDetails);
		}
	}
}
