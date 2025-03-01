package com.nimeshkadecha.drkishan.pages;

import android.annotation.SuppressLint;
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
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.nimeshkadecha.drkishan.Helper.ProductAdapter;
import com.nimeshkadecha.drkishan.R;

import org.json.JSONObject;

import java.text.MessageFormat;
import java.util.ArrayList;
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
		header.setText(MessageFormat.format("FP > {0} > {1}", productName, stage));
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

	@SuppressLint("NotifyDataSetChanged")
	private void addSubStageToPrefs(String subStageName) {
		try {
			SharedPreferences prefs = getSharedPreferences("DrKishanPrefs", MODE_PRIVATE);
			String savedJson = prefs.getString("savedJson", "");

			JSONObject jsonObject = new JSONObject(savedJson);
			JSONObject userObj = jsonObject.optJSONObject(userName);
			if (userObj == null) userObj = new JSONObject();

			JSONObject productObj = userObj.optJSONObject(productName);
			if (productObj == null) productObj = new JSONObject();

			JSONObject stageObj = productObj.optJSONObject(stage);
			if (stageObj == null) stageObj = new JSONObject();

			// ✅ Generate new sub-stage number
			int maxNumber = 0;
			Iterator<String> keys = stageObj.keys();
			while (keys.hasNext()) {
				String key = keys.next();
				if (key.contains("@")) {
					try {
						int num = Integer.parseInt(key.split("@")[0]);
						if (num > maxNumber) maxNumber = num;
					} catch (NumberFormatException ignored) {}
				}
			}
			String numberedSubStage = (maxNumber + 1) + "@" + subStageName;

			// ✅ Add the new sub-stage
			stageObj.put(numberedSubStage, new JSONObject());
			productObj.put(stage, stageObj);
			userObj.put(productName, productObj);
			jsonObject.put(userName, userObj);

			prefs.edit().putString("savedJson", jsonObject.toString()).apply();

			subStageList.add(numberedSubStage);
			adapter.notifyDataSetChanged();

			Toast.makeText(this, "Sub-stage Added!", Toast.LENGTH_SHORT).show();

		} catch (Exception e) {
			Log.e("ENimesh", "Error updating JSON", e);
			Toast.makeText(this, "Failed to add sub-stage!", Toast.LENGTH_SHORT).show();
		}
	}
}