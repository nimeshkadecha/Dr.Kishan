package com.nimeshkadecha.drkishan.Helper;

import android.annotation.SuppressLint;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.nimeshkadecha.drkishan.R;

import java.util.List;

public class DialogMessageAdapter extends RecyclerView.Adapter<DialogMessageAdapter.ViewHolder> {
	private final List<String> dialogList;
	private final OnItemClickListener listener;

	private boolean isDrip = false;

	// Interface for item click listener
	public interface OnItemClickListener {
		void onItemClick(String message, String quantity, String unit);
	}

	public DialogMessageAdapter(boolean isDrip, List<String> dialogList, OnItemClickListener listener) {
		this.dialogList = dialogList;
		this.listener = listener;
		this.isDrip = isDrip;
	}

	@NonNull
	@Override
	public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
		View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item, parent, false);
		return new ViewHolder(view);
	}

	@SuppressLint("NotifyDataSetChanged")
	@Override
	public void onBindViewHolder(@NonNull ViewHolder holder, @SuppressLint("RecyclerView") int position) {
		if (position >= dialogList.size()) {
			return; // Prevent accessing an invalid index
		}

		String item = dialogList.get(position);
		holder.tvMessage.setText(item);

		holder.itemView.setOnClickListener(v -> {
			if (listener != null) {
				if (isDrip) {
					listener.onItemClick(item, "", ""); // No quantity or unit when isDrip is true
				} else {
					String[] parts = item.split(" - ");
					if (parts.length == 2) {
						String message = parts[0];
						String quantity = parts[1].replaceAll("[^\\d.]", ""); // Extract numeric part
						String unit = parts[1].replace(quantity, "").trim(); // Extract unit

						listener.onItemClick(message, quantity, unit);
					}
				}

				if (position < dialogList.size()) {
					dialogList.remove(position);
					notifyDataSetChanged();
				}
			}
		});


		holder.itemView.setOnLongClickListener(view -> {
			if (position < dialogList.size()) { // Prevent invalid index
				dialogList.remove(position);
				notifyDataSetChanged();
				Toast.makeText(view.getContext(), "Removed", Toast.LENGTH_SHORT).show();
			}
			return true; // Ensures long press action is handled properly
		});
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
}
