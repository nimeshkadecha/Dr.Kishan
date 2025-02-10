package com.nimeshkadecha.drkishan;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

public class ProductAdapter extends RecyclerView.Adapter<ProductAdapter.ProductViewHolder> {

	private List<String> itemList;
	private String userName, productName, stage;
	private AdapterType adapterType;
	private Context context;

	private static final String PREFS_NAME = "DrKishan"; // 🔹 SharedPreferences Key

	public enum AdapterType {
		PRODUCTS, STAGES, SUB_STAGES
	}

	// ✅ Constructor for Products
	public ProductAdapter(Context context, List<String> itemList, String userName, AdapterType adapterType) {
		this.context = context;
		this.itemList = itemList;
		this.userName = userName;
		this.adapterType = adapterType;
	}

	// ✅ Constructor for Stages
	public ProductAdapter(Context context, List<String> itemList, String userName, String productName, AdapterType adapterType) {
		this.context = context;
		this.itemList = itemList;
		this.userName = userName;
		this.productName = productName;
		this.adapterType = adapterType;
	}

	// ✅ Constructor for Sub-Stages
	public ProductAdapter(Context context, List<String> itemList, String userName, String productName, String stage, AdapterType adapterType) {
		this.context = context;
		this.itemList = itemList;
		this.userName = userName;
		this.productName = productName;
		this.stage = stage;
		this.adapterType = adapterType;
	}

	@NonNull
	@Override
	public ProductViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
		View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.product_item, parent, false);
		return new ProductViewHolder(view);
	}

	@Override
	public void onBindViewHolder(@NonNull ProductViewHolder holder, int position) {
		holder.txtItem.setText(itemList.get(position));

		holder.txtItem.setOnClickListener(v -> {
			Intent intent;
			if (adapterType == AdapterType.PRODUCTS) {
				intent = new Intent(v.getContext(), selectStage.class);
				intent.putExtra("productName", itemList.get(position));
				intent.putExtra("userName", userName);
			} else if (adapterType == AdapterType.STAGES) {
				intent = new Intent(v.getContext(), selectSubStage.class);
				intent.putExtra("productName", productName);
				intent.putExtra("stage", itemList.get(position));
				intent.putExtra("userName", userName);
			} else {
				intent = new Intent(v.getContext(), basicInfo.class);
				intent.putExtra("productName", productName);
				intent.putExtra("stage", stage);
				intent.putExtra("subStage", itemList.get(position));
				intent.putExtra("userName", userName);
			}
			v.getContext().startActivity(intent);
		});

		// ✅ Long-click for deletion
		holder.txtItem.setOnLongClickListener(v -> {
			showDeleteConfirmation(v.getContext(), position);
			return true; // ✅ Return true to consume the long-click event
		});
	}


	@Override
	public int getItemCount() {
		return itemList.size();
	}

	public void updateList(List<String> newItemList) {
		if (this.itemList.equals(newItemList)) {
			Log.d("ENimesh", "No changes detected, skipping update.");
			return;
		}
		this.itemList.clear();
		this.itemList.addAll(newItemList);
		notifyDataSetChanged();
	}

	// ✅ Show Confirmation Dialog Before Deleting
	private void showDeleteConfirmation(Context context, int position) {
		new AlertDialog.Builder(context)
										.setTitle("Delete Confirmation")
										.setMessage("Are you sure you want to delete this item? This action cannot be undone.")
										.setPositiveButton("Delete", (dialog, which) -> {
											String itemToDelete = itemList.get(position);
											deleteItemFromStorage(context, position, itemToDelete);
										})
										.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
										.show();
	}


	private void deleteItemFromStorage(Context context, int position, String itemToDelete) {
		try {
			// ✅ Fetch SharedPreferences
			SharedPreferences prefs = context.getSharedPreferences("DrKishan", Context.MODE_PRIVATE);
			String savedJson = prefs.getString("savedJson", "{}"); // Default empty JSON
			JSONObject jsonObject = new JSONObject(savedJson);

			if (adapterType == AdapterType.PRODUCTS) {
				if (jsonObject.has(userName)) {
					jsonObject.getJSONObject(userName).remove(itemToDelete);
				}
			} else if (adapterType == AdapterType.STAGES) {
				if (jsonObject.has(userName) && jsonObject.getJSONObject(userName).has(productName)) {
					jsonObject.getJSONObject(userName).getJSONObject(productName).remove(itemToDelete);
				}
			} else if (adapterType == AdapterType.SUB_STAGES) {
				if (jsonObject.has(userName) && jsonObject.getJSONObject(userName).has(productName) &&
												jsonObject.getJSONObject(userName).getJSONObject(productName).has(stage)) {
					jsonObject.getJSONObject(userName).getJSONObject(productName).getJSONObject(stage).remove(itemToDelete);
				}
			}

			// ✅ Save Updated JSON to SharedPreferences
			prefs.edit().putString("savedJson", jsonObject.toString()).apply();

			// ✅ Delete from Firebase Immediately
			deleteFromFirebase(itemToDelete);

			// ✅ Update UI (RecyclerView)
			itemList.remove(position);
			notifyItemRemoved(position);
			notifyItemRangeChanged(position, itemList.size());

			Toast.makeText(context, "Item Deleted Successfully!", Toast.LENGTH_SHORT).show();

		} catch (Exception e) {
			Log.e("Delete", "Error deleting item", e);
		}
	}

	private void deleteFromFirebase(String itemToDelete) {
		DatabaseReference reference = FirebaseDatabase.getInstance().getReference(userName);

		if (adapterType == AdapterType.PRODUCTS) {
			reference.child(itemToDelete).removeValue();
		} else if (adapterType == AdapterType.STAGES) {
			reference.child(productName).child(itemToDelete).removeValue();
		} else if (adapterType == AdapterType.SUB_STAGES) {
			reference.child(productName).child(stage).child(itemToDelete).removeValue();
		}
	}

	public static class ProductViewHolder extends RecyclerView.ViewHolder {
		TextView txtItem;

		public ProductViewHolder(@NonNull View itemView) {
			super(itemView);
			txtItem = itemView.findViewById(R.id.txtProductName);
		}
	}
}
