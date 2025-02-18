package com.nimeshkadecha.drkishan.pages;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.nimeshkadecha.drkishan.Helper.ProductAdapter;
import com.nimeshkadecha.drkishan.R;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class _4_ProductList_3_subStage extends AppCompatActivity {

	private String userName, productName, stage;
	private List<String> subStageList = new ArrayList<>();
	private RecyclerView recyclerView;
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

		// ✅ Setup RecyclerView
		recyclerView = findViewById(R.id.subStageList);
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
		builder.setTitle("Enter Sub-Stage Name");

		View customView = LayoutInflater.from(this).inflate(R.layout.dialog_add_to_list, null);
		EditText etSubStageName = customView.findViewById(R.id.etProductName);
		builder.setView(customView);

		builder.setPositiveButton("OK", (dialog, which) -> {
			String subStageName = etSubStageName.getText().toString().trim();

			if (!subStageName.isEmpty()) {
				addSubStageToPrefs(subStageName);
			} else {
				Toast.makeText(this, "Sub-stage name cannot be empty!", Toast.LENGTH_SHORT).show();
			}
		});

		builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());

		AlertDialog alertDialog = builder.create();
		alertDialog.show();
	}

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

			// ✅ Add the new sub-stage
			stageObj.put(subStageName, new JSONObject());
			productObj.put(stage, stageObj);
			userObj.put(productName, productObj);
			jsonObject.put(userName, userObj);

			prefs.edit().putString("savedJson", jsonObject.toString()).apply();

			subStageList.add(subStageName);
			adapter.notifyDataSetChanged();

			Toast.makeText(this, "Sub-stage Added!", Toast.LENGTH_SHORT).show();

		} catch (Exception e) {
			Log.e("ENimesh", "Error updating JSON", e);
			Toast.makeText(this, "Failed to add sub-stage!", Toast.LENGTH_SHORT).show();
		}
	}
}
