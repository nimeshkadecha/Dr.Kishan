package com.nimeshkadecha.drkishan.Helper;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.nimeshkadecha.drkishan.pages._3_ProductList_2_staging;
import com.nimeshkadecha.drkishan.pages._4_ProductList_3_subStage;
import com.nimeshkadecha.drkishan.R;
import com.nimeshkadecha.drkishan.pages._5_TimingInformation;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class ProductAdapter extends RecyclerView.Adapter<ProductAdapter.ProductViewHolder> {

	private final List<String> itemList;
	private final String userName;
	private String productName;
	private String stage;
	private final AdapterType adapterType;
	private final Context context;

	private static final String PREFS_NAME = "DrKishanPrefs"; // 🔹 SharedPreferences Key

	public Context getContext() {
		return context;
	}

	public enum AdapterType {
		PRODUCTS, STAGES, SUB_STAGES
	}

	// ✅ Constructor for Products
	public ProductAdapter(Context context, List<String> itemList, String userName, AdapterType adapterType) {
		this.context = context;
		this.itemList = itemList;
		this.userName = userName;
		this.adapterType = adapterType;
		sortItems();
	}

	// ✅ Constructor for Stages
	public ProductAdapter(Context context, List<String> itemList, String userName, String productName, AdapterType adapterType) {
		this.context = context;
		this.itemList = itemList;
		this.userName = userName;
		this.productName = productName;
		this.adapterType = adapterType;
		sortItems();
	}

	// ✅ Constructor for Sub-Stages
	public ProductAdapter(Context context, List<String> itemList, String userName, String productName, String stage, AdapterType adapterType) {
		this.context = context;
		this.itemList = itemList;
		this.userName = userName;
		this.productName = productName;
		this.stage = stage;
		this.adapterType = adapterType;
		sortItems();
	}

	@NonNull
	@Override
	public ProductViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
		View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item, parent, false);
		return new ProductViewHolder(view);
	}

	@Override
	public void onBindViewHolder(@NonNull ProductViewHolder holder, int position) {
		String rawItem = itemList.get(position);
		String formattedItem = extractItemName(rawItem);
		holder.txtItem.setText(formattedItem);

		holder.txtItem.setOnClickListener(v -> {
			Intent intent;
			if (adapterType == AdapterType.PRODUCTS) {
				intent = new Intent(v.getContext(), _3_ProductList_2_staging.class);
				intent.putExtra("productName", itemList.get(position));
				intent.putExtra("userName", userName);
			} else if (adapterType == AdapterType.STAGES) {
				intent = new Intent(v.getContext(), _4_ProductList_3_subStage.class);
				intent.putExtra("productName", productName);
				intent.putExtra("stage", itemList.get(position));
				intent.putExtra("userName", userName);
			} else {
				intent = new Intent(v.getContext(), _5_TimingInformation.class);
				intent.putExtra("productName", productName);
				intent.putExtra("stage", stage);
				intent.putExtra("subStage", itemList.get(position));
				intent.putExtra("userName", userName);
			}
			v.getContext().startActivity(intent);
		});

		// ✅ Long-click for deletion
		holder.txtItem.setOnLongClickListener(v -> {
			showEditDeleteDialog(v.getContext(), position, rawItem);
			return true;
		});

	}

	private int getNextNumber() {
		int maxNumber = 0;
		for (String item : itemList) {
			int number = extractNumber(item);
			if (number > maxNumber) {
				maxNumber = number;
			}
		}
		return maxNumber + 1;
	}

	private int extractNumber(String item) {
		try {
			return Integer.parseInt(item.split("@")[0]);
		} catch (Exception e) {
			return 0;
		}
	}

	private String extractItemName(String item) {
		return item.substring(item.indexOf("@") + 1);
	}

	@Override
	public int getItemCount() {
		return itemList.size();
	}

	@SuppressLint("NotifyDataSetChanged")
	public void updateList(List<String> newItemList) {
		if (this.itemList.equals(newItemList)) {
			Log.d("ENimesh", "No changes detected, skipping update.");
			return;
		}
		this.itemList.clear();
		this.itemList.addAll(newItemList);
		notifyDataSetChanged();
	}

	private void deleteItemFromStorage(Context context, int position, String itemToDelete) {
		try {
			// ✅ Fetch SharedPreferences
			SharedPreferences prefs = context.getSharedPreferences("DrKishanPrefs", Context.MODE_PRIVATE);
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
	private void showEditDeleteDialog(Context context, int position, String originalItem) {
		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		builder.setTitle("Edit/Delete Item");

		// ✅ Inflate existing dialog layout
		View view = LayoutInflater.from(context).inflate(R.layout.dialog_add_to_list, null);
		EditText etProductName = view.findViewById(R.id.etProductName);
		etProductName.setText(extractItemName(originalItem)); // ✅ Pre-fill existing name

		builder.setView(view);

		builder.setPositiveButton("Save", null); // Set initially to null, we will override later
		builder.setNegativeButton("Delete", (dialog, which) -> deleteItemFromStorage(context, position, originalItem));
		builder.setNeutralButton("Cancel", (dialog, which) -> dialog.dismiss());

		AlertDialog alertDialog = builder.create();
		alertDialog.show();

		// ✅ Override "Save" button click
		alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
			String editedName = etProductName.getText().toString().trim();

			if (editedName.isEmpty()) {
				etProductName.setError("Product name cannot be empty!");
				return;
			}

			if (editedName.matches(".*[.#$\\[\\]].*")) {
				etProductName.setError("Name cannot contain '.', '#', '$', '[', or ']'");
				return;
			}

			// ✅ If validation passes, update storage and dismiss the dialog
			String updatedItem = extractNumber(originalItem) + "@" + editedName;
			itemList.set(position, updatedItem);
			updateItemInStorage(context, position, originalItem, updatedItem);
			notifyDataSetChanged();
			alertDialog.dismiss();
		});
	}

	private void updateItemInStorage(Context context, int position, String oldName, String newName) {
		try {
			// ✅ Fetch SharedPreferences
			SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
			String savedJson = prefs.getString("savedJson", "{}");
			JSONObject jsonObject = new JSONObject(savedJson);

			if (adapterType == AdapterType.PRODUCTS) {
				if (jsonObject.has(userName) && jsonObject.getJSONObject(userName).has(oldName)) {
					JSONObject productData = jsonObject.getJSONObject(userName).getJSONObject(oldName);
					jsonObject.getJSONObject(userName).remove(oldName);
					jsonObject.getJSONObject(userName).put(newName, productData);
				}
			} else if (adapterType == AdapterType.STAGES) {
				if (jsonObject.has(userName) && jsonObject.getJSONObject(userName).has(productName)) {
					JSONObject stageData = jsonObject.getJSONObject(userName).getJSONObject(productName).getJSONObject(oldName);
					jsonObject.getJSONObject(userName).getJSONObject(productName).remove(oldName);
					jsonObject.getJSONObject(userName).getJSONObject(productName).put(newName, stageData);
				}
			} else if (adapterType == AdapterType.SUB_STAGES) {
				if (jsonObject.has(userName) && jsonObject.getJSONObject(userName).has(productName) &&
												jsonObject.getJSONObject(userName).getJSONObject(productName).has(stage)) {
					JSONObject subStageData = jsonObject.getJSONObject(userName)
													.getJSONObject(productName)
													.getJSONObject(stage)
													.getJSONObject(oldName);
					jsonObject.getJSONObject(userName).getJSONObject(productName).getJSONObject(stage).remove(oldName);
					jsonObject.getJSONObject(userName).getJSONObject(productName).getJSONObject(stage).put(newName, subStageData);
				}
			}

			// ✅ Save Updated JSON to SharedPreferences
			prefs.edit().putString("savedJson", jsonObject.toString()).apply();

			// ✅ Update Firebase
			updateFirebase(oldName, newName);

			// ✅ Update UI
			itemList.set(position, newName);
			notifyItemChanged(position);

			Toast.makeText(context, "Item Updated Successfully!", Toast.LENGTH_SHORT).show();

		} catch (JSONException e) {
			Log.e("Update", "Error updating item", e);
		}
	}

	private void sortItems() {
		Collections.sort(itemList, Comparator.comparingInt(this::extractNumber));
	}

	private void updateFirebase(String oldName, String newName) {
		DatabaseReference reference = FirebaseDatabase.getInstance().getReference(userName);

		if (adapterType == AdapterType.PRODUCTS) {
			reference.child(oldName).get().addOnSuccessListener(dataSnapshot -> {
				reference.child(newName).setValue(dataSnapshot.getValue()); // ✅ Copy old data
				reference.child(oldName).removeValue(); // ✅ Remove old entry
			});
		} else if (adapterType == AdapterType.STAGES) {
			reference.child(productName).child(oldName).get().addOnSuccessListener(dataSnapshot -> {
				reference.child(productName).child(newName).setValue(dataSnapshot.getValue());
				reference.child(productName).child(oldName).removeValue();
			});
		} else if (adapterType == AdapterType.SUB_STAGES) {
			reference.child(productName).child(stage).child(oldName).get().addOnSuccessListener(dataSnapshot -> {
				reference.child(productName).child(stage).child(newName).setValue(dataSnapshot.getValue());
				reference.child(productName).child(stage).child(oldName).removeValue();
			});
		}
	}


}
