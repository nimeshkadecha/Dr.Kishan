package com.nimeshkadecha.drkishan.pages;

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
import java.util.Iterator;
import java.util.List;

public class _3_ProductList_2_staging extends AppCompatActivity {

	private RecyclerView recyclerView;
	private ProductAdapter adapter;
	private List<String> stageList = new ArrayList<>();
	private String productName, userName;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		EdgeToEdge.enable(this);
		setContentView(R.layout.activity_3_product_list_2_staging);

		productName = getIntent().getStringExtra("productName");
		userName = getIntent().getStringExtra("userName");

		Log.d("ENimesh", "Received Product: " + productName);

		// ✅ setting header
		TextView header = findViewById(R.id.textView_Header);
		header.setText(MessageFormat.format("FP > {0}", productName));
		header.setTextSize(20f);
		HorizontalScrollView scrollView = findViewById(R.id.horizontalScrollView);
		scrollView.post(() -> scrollView.smoothScrollTo(header.getWidth(), 0));

		// Setup RecyclerView
		recyclerView = findViewById(R.id.stageList);
		recyclerView.setLayoutManager(new LinearLayoutManager(this));
		adapter = new ProductAdapter(_3_ProductList_2_staging.this, stageList, userName, productName, ProductAdapter.AdapterType.STAGES);

		recyclerView.setAdapter(adapter);

		// ✅ Load Stages from SharedPreferences
		loadStagesFromPrefs();

		findViewById(R.id.btnStageProduct).setOnClickListener(view -> showAddStageDialog());
	}

	/** ✅ Load Stages from SharedPreferences */
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
		builder.setTitle("Enter Stage Name");

		View customView = LayoutInflater.from(this).inflate(R.layout.dialog_add_to_list, null);
		EditText etStageName = customView.findViewById(R.id.etProductName);
		builder.setView(customView);

		builder.setPositiveButton("OK", (dialog, which) -> {
			String stageName = etStageName.getText().toString().trim();

			if (!stageName.isEmpty()) {
				addStageToPrefs(stageName);
				loadStagesFromPrefs(); // ✅ Refresh RecyclerView
				Toast.makeText(this, "Stage Added: " + stageName, Toast.LENGTH_SHORT).show();
			} else {
				Toast.makeText(this, "Stage name cannot be empty!", Toast.LENGTH_SHORT).show();
			}
		});

		builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
		builder.create().show();
	}

	/** ✅ Add Stage to SharedPreferences */
	private void addStageToPrefs(String stageName) {
		String jsonString = getJsonFromPrefs();
		JSONObject json;

		try {
			json = jsonString.isEmpty() ? new JSONObject() : new JSONObject(jsonString);

			// Ensure user & product nodes exist
			if (!json.has(userName)) {
				json.put(userName, new JSONObject());
			}
			JSONObject userJson = json.getJSONObject(userName);

			if (!userJson.has(productName)) {
				userJson.put(productName, new JSONObject());
			}
			JSONObject productJson = userJson.getJSONObject(productName);

			// ✅ Add stage (if not already present)
			if (!productJson.has(stageName)) {
				productJson.put(stageName, new JSONObject());
				saveJsonToPrefs(json.toString());
			} else {
				Toast.makeText(this, "Stage already exists!", Toast.LENGTH_SHORT).show();
			}

		} catch (JSONException e) {
			Log.e("SharedPrefs", "Error updating JSON in SharedPreferences", e);
		}
	}

	private void saveJsonToPrefs(String json) {
		getSharedPreferences("DrKishanPrefs", MODE_PRIVATE)
										.edit()
										.putString("savedJson", json)
										.apply();
	}

	private String getJsonFromPrefs() {
		return getSharedPreferences("DrKishanPrefs", MODE_PRIVATE)
										.getString("savedJson", ""); // Default: empty string
	}
}
