package com.nimeshkadecha.drkishan;

import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class ProductAdapter extends RecyclerView.Adapter<ProductAdapter.ProductViewHolder> {

	private List<String> itemList;
	private String userName, productName, stage;
	private AdapterType adapterType;

	public enum AdapterType {
		PRODUCTS, STAGES, SUB_STAGES
	}

	// ✅ Constructor for Products
	public ProductAdapter(List<String> itemList, String userName, AdapterType adapterType) {
		this.itemList = itemList;
		this.userName = userName;
		this.adapterType = adapterType;
	}

	// ✅ Constructor for Stages
	public ProductAdapter(List<String> itemList, String userName, String productName, AdapterType adapterType) {
		this.itemList = itemList;
		this.userName = userName;
		this.productName = productName;
		this.adapterType = adapterType;
	}

	// ✅ Constructor for Sub-Stages
	public ProductAdapter(List<String> itemList, String userName, String productName, String stage, AdapterType adapterType) {
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
				// ✅ If displaying products, go to stage selection
				intent = new Intent(v.getContext(), selectStage.class);
				intent.putExtra("productName", itemList.get(position));
				intent.putExtra("userName", userName);
			} else if (adapterType == AdapterType.STAGES) {
				// ✅ If displaying stages, go to subStage selection
				intent = new Intent(v.getContext(), selectSubStage.class);
				intent.putExtra("productName", productName);
				intent.putExtra("stage", itemList.get(position));
				intent.putExtra("userName", userName);
			} else {
				// ✅ If displaying sub-stages, go to Basic Info
				intent = new Intent(v.getContext(), basicInfo.class);
				intent.putExtra("productName", productName);
				intent.putExtra("stage", stage);
				intent.putExtra("subStage", itemList.get(position));
				intent.putExtra("userName", userName);
			}
			v.getContext().startActivity(intent);
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

	public static class ProductViewHolder extends RecyclerView.ViewHolder {
		TextView txtItem;

		public ProductViewHolder(@NonNull View itemView) {
			super(itemView);
			txtItem = itemView.findViewById(R.id.txtProductName);
		}
	}
}
