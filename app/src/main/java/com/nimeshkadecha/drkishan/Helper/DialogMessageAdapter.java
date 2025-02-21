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
	private List<String> dialogList;
	private OnItemClickListener listener;

	// Interface for item click listener
	public interface OnItemClickListener {
		void onItemClick(String message, String quantity, String unit);
	}

	public DialogMessageAdapter(List<String> dialogList, OnItemClickListener listener) {
		this.dialogList = dialogList;
		this.listener = listener;
	}

	@NonNull
	@Override
	public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
		View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item, parent, false);
		return new ViewHolder(view);
	}

	@Override
	public void onBindViewHolder(@NonNull ViewHolder holder, @SuppressLint("RecyclerView") int position) {
		String item = dialogList.get(position);
		holder.tvMessage.setText(item);

		holder.itemView.setOnClickListener(v -> {
			Log.d("ENimesh", "Clicked on item: " + item);

			if (listener != null) {
				String[] parts = item.split(" -- ");
				Log.d("ENimesh" , "Len = " + parts.length);
				if (parts.length == 2) {
					String message = parts[0];
					String quantity = parts[1].replaceAll("[^\\d.]", ""); // Extract numeric part including decimal
					String unit = parts[1].replace(quantity, "").trim(); // Extract unit by removing quantity

					Log.d("ENimesh", "Extracted - Message: " + message + ", Quantity: " + quantity + ", Unit: " + unit);

					listener.onItemClick(message, quantity, unit);

					dialogList.remove(position); // Remove item from the list
					notifyDataSetChanged(); // Refresh RecyclerView
				}
			}
		});

		holder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
			@Override
			public boolean onLongClick(View view) {

				dialogList.remove(position);
				notifyDataSetChanged();
				Toast.makeText(view.getContext(), "Removed", Toast.LENGTH_SHORT).show();
				return false;

			}
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

	// ✅ Update list method (prevents duplicates)
	public void updateList(List<String> newList) {
		if (newList == null || newList.isEmpty()) return;

		for (String item : newList) {
			if (!dialogList.contains(item)) { // Prevent duplicate entries
				dialogList.add(item);
			}
		}
		notifyDataSetChanged();
	}
}
