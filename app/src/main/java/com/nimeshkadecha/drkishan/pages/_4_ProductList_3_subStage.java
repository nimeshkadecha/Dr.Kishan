package com.nimeshkadecha.drkishan.pages;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.nimeshkadecha.drkishan.Helper.ProductAdapter;
import com.nimeshkadecha.drkishan.R;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

public class _4_ProductList_3_subStage extends AppCompatActivity {

	private String userName, productName, stage;
	private final List<String> subStageList = new ArrayList<>();
	private ProductAdapter adapter;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		EdgeToEdge.enable(this);
		setContentView(R.layout.activity_4_product_list_3_sub_stage);

		// ✅ Get Data from Intent
		userName = getIntent().getStringExtra("userName");
		productName = getIntent().getStringExtra("productName");
		stage = getIntent().getStringExtra("stage");

		// ✅ setting header
		TextView header = findViewById(R.id.textView_Header);
		header.setText(MessageFormat.format("FP > {0} > {1}", extractName(productName), extractName(stage)));
		header.setTextSize(20f);

		HorizontalScrollView scrollView = findViewById(R.id.horizontalScrollView);
		scrollView.post(() -> scrollView.smoothScrollTo(header.getWidth(), 0));

		// ✅ Setup RecyclerView
		RecyclerView recyclerView = findViewById(R.id.subStageList);
		recyclerView.setLayoutManager(new LinearLayoutManager(this));
		adapter = new ProductAdapter(_4_ProductList_3_subStage.this, subStageList, userName, productName, stage, ProductAdapter.AdapterType.SUB_STAGES);
		recyclerView.setAdapter(adapter);

		// ✅ Load Sub-Stages from SharedPreferences
		loadSubStagesFromPrefs();

		// ✅ Button to Add New Sub-Stage
		Button btnAddSubStage = findViewById(R.id.btnSubStageProduct);
		btnAddSubStage.setOnClickListener(v -> showAddSubStageDialog());

		ItemTouchHelper itemTouchHelper = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(
										ItemTouchHelper.UP | ItemTouchHelper.DOWN | ItemTouchHelper.START | ItemTouchHelper.END,
										ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT // ✅ Enable swipe
		) {
			@Override
			public boolean onMove(@NonNull RecyclerView recyclerView,
			                      @NonNull RecyclerView.ViewHolder viewHolder,
			                      @NonNull RecyclerView.ViewHolder target) {
				int fromPosition = viewHolder.getAdapterPosition();
				int toPosition = target.getAdapterPosition();

				// ✅ Call adapter method to swap items
				adapter.onItemMove(fromPosition, toPosition);
				return true;
			}

			@Override
			public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
				int position = viewHolder.getAdapterPosition();
//				String stageName = subStageList.get(position); // Get the stage name
//				Context context = viewHolder.itemView.getContext();

				// ✅ Call edit/delete dialog
				adapter.onItemSwipe(position);

				// Prevent automatic deletion by refreshing item
				adapter.notifyItemChanged(position);
			}

			@Override
			public void clearView(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
				super.clearView(recyclerView, viewHolder);

				// ✅ Update order in SharedPreferences after drag-and-drop
				updateSubStageOrder(new ArrayList<>(subStageList));
			}
		});

// ✅ Attach to RecyclerView
		itemTouchHelper.attachToRecyclerView(recyclerView);

	}
	private void updateSubStageOrder(List<String> newOrder) {
		String jsonString = getJsonFromPrefs();

		try {
			JSONObject json = new JSONObject(jsonString);
			if (!json.has(userName) || !json.getJSONObject(userName).has(productName) ||
											!json.getJSONObject(userName).getJSONObject(productName).has(stage)) return;

			JSONObject stageJson = json.getJSONObject(userName).getJSONObject(productName).getJSONObject(stage);
			JSONObject newStageJson = new JSONObject();

			// ✅ Rename keys based on new order
			for (int i = 0; i < newOrder.size(); i++) {
				String oldKey = newOrder.get(i); // Example: "3@SubStageName"
				int newNumber = i + 1; // New position (1-based index)
				String newKey = newNumber + "@" + oldKey.split("@", 2)[1]; // Preserve sub-stage name

				if (stageJson.has(oldKey)) {
					newStageJson.put(newKey, stageJson.getJSONObject(oldKey));
				}
			}

			// ✅ Replace old data with new sorted data
			json.getJSONObject(userName).getJSONObject(productName).put(stage, newStageJson);

			// ✅ Save back to SharedPreferences
			saveJsonToPrefs(json.toString());
		} catch (JSONException e) {
			Log.e("SharedPrefs", "Error updating sub-stage order", e);
		}
	}

	private void loadSubStagesFromPrefs() {
		subStageList.clear();

		String savedJson = getSharedPreferences("DrKishanPrefs", MODE_PRIVATE)
										.getString("savedJson", "");

		try {
			JSONObject jsonObject = new JSONObject(savedJson);
			JSONObject userObj = jsonObject.optJSONObject(userName);
			if (userObj == null) return;

			JSONObject productObj = userObj.optJSONObject(productName);
			if (productObj == null) return;

			JSONObject stageObj = productObj.optJSONObject(stage);
			if (stageObj == null) return;

			Iterator<String> subStageKeys = stageObj.keys();
			while (subStageKeys.hasNext()) {
				subStageList.add(subStageKeys.next());
			}

		} catch (Exception e) {
			Log.e("ENimesh", "Error parsing JSON", e);
		}

		adapter.updateList(subStageList);
	}

	private void showAddSubStageDialog() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle("Enter sub-stage Name");

		View customView = LayoutInflater.from(this).inflate(R.layout.dialog_add_to_list, null);
		EditText etName = customView.findViewById(R.id.etProductName);
		builder.setView(customView);

		builder.setPositiveButton("OK", null); // Set initially to null, we will override later
		builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());

		AlertDialog alertDialog = builder.create();
		alertDialog.show();

		// Override OK button click to prevent automatic dialog dismissal
		alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
			String subStageName = etName.getText().toString().trim();

			if (subStageName.isEmpty()) {
				etName.setError("Sub-stage name cannot be empty!");
				return;
			}

			if (subStageName.matches(".*[.#$\\[\\]].*")) {
				etName.setError("Name cannot contain '.', '#', '$', '[', or ']'");
				return;
			}

			// If validation passes, dismiss dialog and proceed
			addSubStageToPrefs(subStageName);
			alertDialog.dismiss();
		});
	}

	private void saveJsonToPrefs(String json) {
		getSharedPreferences("DrKishanPrefs", MODE_PRIVATE).edit().putString("savedJson", json).apply();
	}

	private String getJsonFromPrefs() {
		return getSharedPreferences("DrKishanPrefs", MODE_PRIVATE).getString("savedJson", "");
	}
	private void addSubStageToPrefs(String subStageName) {
		try {
			SharedPreferences prefs = getSharedPreferences("DrKishanPrefs", MODE_PRIVATE);
			String savedJson = prefs.getString("savedJson", "{}");

			JSONObject jsonObject = new JSONObject(savedJson);
			JSONObject userObj = jsonObject.optJSONObject(userName);
			if (userObj == null) userObj = new JSONObject();

			JSONObject productObj = userObj.optJSONObject(productName);
			if (productObj == null) productObj = new JSONObject();

			JSONObject stageObj = productObj.optJSONObject(stage);
			if (stageObj == null) stageObj = new JSONObject();

			// ✅ Determine next available number
			int nextNumber = 1;
			Iterator<String> keys = stageObj.keys();
			while (keys.hasNext()) {
				String key = keys.next();
				if (key.contains("@")) {
					try {
						int num = Integer.parseInt(key.split("@")[0]);
						nextNumber = Math.max(nextNumber, num + 1);
					} catch (NumberFormatException ignored) {}
				}
			}

			String formattedSubStageName = nextNumber + "@" + subStageName;

			// ✅ Add the new sub-stage
			if (!stageObj.has(formattedSubStageName)) {
				stageObj.put(formattedSubStageName, new JSONObject());
				productObj.put(stage, stageObj);
				userObj.put(productName, productObj);
				jsonObject.put(userName, userObj);

				prefs.edit().putString("savedJson", jsonObject.toString()).apply();

				// ✅ Add to subStageList immediately and update RecyclerView
				subStageList.add(formattedSubStageName);
				Collections.sort(subStageList, Comparator.comparingInt(this::extractNumber));
				adapter.notifyDataSetChanged(); // Refresh RecyclerView instantly
			} else {
				Toast.makeText(this, "Sub-stage already exists!", Toast.LENGTH_SHORT).show();
			}
		} catch (Exception e) {
			Log.e("SharedPrefs", "Error updating JSON in SharedPreferences", e);
		}
	}

	private int extractNumber(String item) {
		try {
			return Integer.parseInt(item.split("@")[0]);
		} catch (Exception e) {
			return 0;
		}
	}

	private String extractName(String item) {
		return item.contains("@") ? item.substring(item.indexOf("@") + 1) : item;
	}

}