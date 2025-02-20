package com.nimeshkadecha.drkishan.Helper;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.nimeshkadecha.drkishan.R;

import java.util.List;
public class DialogMessageAdapter extends RecyclerView.Adapter<DialogMessageAdapter.ViewHolder> {
	private List<String> dialogList;

	public DialogMessageAdapter(List<String> dialogList) {
		this.dialogList = dialogList;
	}

	@NonNull
	@Override
	public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
		View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item, parent, false);
		return new ViewHolder(view);
	}

	@Override
	public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
		holder.tvMessage.setText(dialogList.get(position));
	}

	@Override
	public int getItemCount() {
		return dialogList != null ? dialogList.size() : 0;
	}

	public static class ViewHolder extends RecyclerView.ViewHolder {
		TextView tvMessage;

		public ViewHolder(View itemView) {
			super(itemView);
			tvMessage = itemView.findViewById(R.id.txtProductName);
		}
	}


	public void updateList(List<String> newList) {
		if (newList == null || newList.isEmpty()) return; // Prevent empty updates

		for (String item : newList) {
			if (!dialogList.contains(item)) { // Prevent duplicates
				dialogList.add(item);
			}
		}

		Log.d("ENimesh", "Updated dialogList: " + dialogList);
		notifyDataSetChanged(); // Ensure RecyclerView refreshes
	}



}
