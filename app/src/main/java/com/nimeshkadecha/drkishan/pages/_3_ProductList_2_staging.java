package com.nimeshkadecha.drkishan.pages;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.nimeshkadecha.drkishan.Helper.ProductAdapter;
import com.nimeshkadecha.drkishan.R;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class _3_ProductList_2_staging extends AppCompatActivity {

	private ProductAdapter adapter;
	private final List<String> stageList = new ArrayList<>();
	private String productName, userName;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		EdgeToEdge.enable(this);
		setContentView(R.layout.activity_3_product_list_2_staging);

		productName = getIntent().getStringExtra("productName");
		userName = getIntent().getStringExtra("userName");

		// ✅ Setting header
		TextView header = findViewById(R.id.textView_Header);
		header.setText(MessageFormat.format("FP > {0}", productName));
		header.setTextSize(20f);
		HorizontalScrollView scrollView = findViewById(R.id.horizontalScrollView);
		scrollView.post(() -> scrollView.smoothScrollTo(header.getWidth(), 0));

		// Setup RecyclerView
		RecyclerView recyclerView = findViewById(R.id.stageList);
		recyclerView.setLayoutManager(new LinearLayoutManager(this));
		adapter = new ProductAdapter(this, stageList, userName, productName, ProductAdapter.AdapterType.STAGES);

		recyclerView.setAdapter(adapter);

		// ✅ Load Stages from SharedPreferences
		loadStagesFromPrefs();

		findViewById(R.id.btnStageProduct).setOnClickListener(view -> showAddStageDialog());
	}

	/** ✅ Load Stages from SharedPreferences */
	@SuppressLint("NotifyDataSetChanged")
	private void loadStagesFromPrefs() {
		String jsonString = getJsonFromPrefs();

		if (jsonString.isEmpty()) {
			Log.d("SharedPrefs", "No saved data found.");
			return;
		}

		try {
			JSONObject json = new JSONObject(jsonString);
			if (!json.has(userName) || !json.getJSONObject(userName).has(productName)) {
				Log.e("SharedPrefs", "No stages found for product: " + productName);
				return;
			}

			stageList.clear();
			JSONObject productObj = json.getJSONObject(userName).getJSONObject(productName);
			Iterator<String> stageKeys = productObj.keys();
			while (stageKeys.hasNext()) {
				stageList.add(stageKeys.next());
			}

			// ✅ Sort by extracted number
			Collections.sort(stageList, (a, b) -> {
				int numA = extractNumber(a);
				int numB = extractNumber(b);
				return Integer.compare(numA, numB);
			});

			if (!stageList.isEmpty()) {
				runOnUiThread(() -> {
					adapter.updateList(stageList);
					adapter.notifyDataSetChanged();
				});
			}

		} catch (JSONException e) {
			Log.e("SharedPrefs", "Error parsing JSON from SharedPreferences", e);
		}
	}

	/** ✅ Show Dialog to Add Stage */
	private void showAddStageDialog() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle("Enter stage Name");

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
				etName.setError("Stage name cannot be empty!");
				return;
			}

			if (subStageName.matches(".*[.#$\\[\\]].*")) {
				etName.setError("Name cannot contain '.', '#', '$', '[', or ']'");
				return;
			}

			// If validation passes, dismiss dialog and proceed
			addStageToPrefs(subStageName);
			alertDialog.dismiss();
		});
	}

	/** ✅ Add Stage to SharedPreferences */
	private void addStageToPrefs(String stageName) {
		String jsonString = getJsonFromPrefs();
		JSONObject json;

		try {
			json = jsonString.isEmpty() ? new JSONObject() : new JSONObject(jsonString);

			if (!json.has(userName)) {
				json.put(userName, new JSONObject());
			}
			JSONObject userJson = json.getJSONObject(userName);

			if (!userJson.has(productName)) {
				userJson.put(productName, new JSONObject());
			}
			JSONObject productJson = userJson.getJSONObject(productName);

			// ✅ Determine next number
			int nextNumber = getNextStageNumber();
			String formattedStageName = nextNumber + "@" + stageName;

			// ✅ Add stage
			if (!productJson.has(formattedStageName)) {
				productJson.put(formattedStageName, new JSONObject());
				saveJsonToPrefs(json.toString());
			} else {
				Toast.makeText(this, "Stage already exists!", Toast.LENGTH_SHORT).show();
			}

		} catch (JSONException e) {
			Log.e("SharedPrefs", "Error updating JSON in SharedPreferences", e);
		}
	}

	private int getNextStageNumber() {
		int maxNumber = 0;
		for (String stage : stageList) {
			maxNumber = Math.max(maxNumber, extractNumber(stage));
		}
		return maxNumber + 1;
	}

	private int extractNumber(String text) {
		try {
			return Integer.parseInt(text.split("@")[0]);
		} catch (Exception e) {
			return 0;
		}
	}

	private void saveJsonToPrefs(String json) {
		getSharedPreferences("DrKishanPrefs", MODE_PRIVATE).edit().putString("savedJson", json).apply();
	}

	private String getJsonFromPrefs() {
		return getSharedPreferences("DrKishanPrefs", MODE_PRIVATE).getString("savedJson", "");
	}
}
