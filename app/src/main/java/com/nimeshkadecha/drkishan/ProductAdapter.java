package com.nimeshkadecha.drkishan;

import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.nio.Buffer;
import java.util.List;

public class ProductAdapter extends RecyclerView.Adapter<ProductAdapter.ProductViewHolder> {

	private List<String> productNames;
	private List<String> stageList;
	private List<String> levelList;
	private List<String> dateList;
	private List<Integer> intervalList;
	private List<String> dataList;

	private  String userName;

	public ProductAdapter(List<String> productNames, List<String> stageList, List<String> levelList, List<String> dateList, List<Integer> intervalList, List<String> dataList,String UserName) {
		this.productNames = productNames;
		this.stageList = stageList;
		this.levelList = levelList;
		this.dateList = dateList;
		this.intervalList = intervalList;
		this.dataList = dataList;
		this.userName = UserName;
	}

	@NonNull
	@Override
	public ProductViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
		View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.product_item, parent, false);
		return new ProductViewHolder(view);
	}

	@Override
	public void onBindViewHolder(@NonNull ProductViewHolder holder, int position) {
		holder.txtProduct.setText(productNames.get(position));
		holder.txtProduct.setOnClickListener(v -> {
			Intent gotoStages = new Intent(v.getContext(), selectStage.class);

			// ✅ Check if each list has data before adding to Intent
			gotoStages.putExtra("productName", productNames.get(position));
			gotoStages.putExtra("userName",userName);
//
//			if (stageList != null && position < stageList.size()) {
//				gotoStages.putExtra("stageList", stageList.get(position));
//			} else {
//				gotoStages.putExtra("stageList", "");
//			}
//
//			if (levelList != null && position < levelList.size()) {
//				gotoStages.putExtra("levelList", levelList.get(position));
//			} else {
//				gotoStages.putExtra("levelList","");
//			}
//
//			if (dateList != null && position < dateList.size()) {
//				gotoStages.putExtra("dateList", dateList.get(position));
//			} else {
//				gotoStages.putExtra("dateList","" );
//			}
//
//			if (intervalList != null && position < intervalList.size()) {
//				gotoStages.putExtra("intervalList", intervalList.get(position));
//			} else {
//				gotoStages.putExtra("intervalList","" );
//			}
//
//			if (dataList != null && position < dataList.size()) {
//				gotoStages.putExtra("dataList", dataList.get(position));
//			} else {
//				gotoStages.putExtra("dataList","" );
//			}

			v.getContext().startActivity(gotoStages);
		});

	}

	@Override
	public int getItemCount() {
		return productNames.size();
	}

	public void updateList(List<String> productNames, List<String> stageList, List<String> levelList, List<String> dateList, List<Integer> intervalList, List<String> dataList) {

		// Avoid resetting lists if new data is exactly the same
		if (this.productNames.equals(productNames) &&
										this.stageList.equals(stageList) &&
										this.levelList.equals(levelList) &&
										this.dateList.equals(dateList) &&
										this.intervalList.equals(intervalList) &&
										this.dataList.equals(dataList)) {
			Log.d("ENimesh", "No changes detected, skipping update.");
			return;
		}

		this.productNames.clear();
		this.stageList.clear();
		this.levelList.clear();
		this.dateList.clear();
		this.intervalList.clear();
		this.dataList.clear();

		this.productNames.addAll(productNames);
		this.stageList.addAll(stageList);
		this.levelList.addAll(levelList);
		this.dateList.addAll(dateList);
		this.intervalList.addAll(intervalList);
		this.dataList.addAll(dataList);

//		Log.d("ENimesh", "Updated List:" + productNames);
//		Log.d("ENimesh", "Updated List:" + stageList);
//		Log.d("ENimesh", "Updated List:" + levelList);
//		Log.d("ENimesh", "Updated List:" + dateList);
//		Log.d("ENimesh", "Updated List:" + intervalList);
//		Log.d("ENimesh", "Updated List:" + dataList);


		notifyDataSetChanged();
	}


	public static class ProductViewHolder extends RecyclerView.ViewHolder {
		TextView txtProduct;

		public ProductViewHolder(@NonNull View itemView) {
			super(itemView);
			txtProduct = itemView.findViewById(R.id.txtProductName);
//			txtProductDescription = itemView.findViewById(R.id.txtProductDescription);
		}
	}
}
