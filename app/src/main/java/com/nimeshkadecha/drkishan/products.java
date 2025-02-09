package com.nimeshkadecha.drkishan;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.FirebaseApp;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class products extends AppCompatActivity {

	private DatabaseReference reference;

	private AlertDialog alertDialog;
	private RecyclerView recyclerView;
	private ProductAdapter adapter;

	// Lists to store data
	private List<String> productList = new ArrayList<>();
	private List<String> stageList = new ArrayList<>();
	private List<String> levelList = new ArrayList<>();
	private List<String> dateList = new ArrayList<>();
	private List<Integer> intervalList = new ArrayList<>();
	private List<String> dataList = new ArrayList<>();

	String userName;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		EdgeToEdge.enable(this);
		setContentView(R.layout.activity_products);


		userName = getIntent().getStringExtra("name");

		// Explicitly Initialize Firebase
		FirebaseApp.initializeApp(this);

		// Ensure FirebaseApp is initialized before accessing the database
		if (FirebaseApp.getApps(this).isEmpty()) {
			return;
		}

		reference = FirebaseDatabase.getInstance().getReference("");

		// Setup RecyclerView
		recyclerView = findViewById(R.id.productsList);
		recyclerView.setLayoutManager(new LinearLayoutManager(this));
		adapter = new ProductAdapter(productList, stageList, levelList, dateList, intervalList, dataList,userName);
		recyclerView.setAdapter(adapter);

		fetchProducts();

		findViewById(R.id.btnAddProduct).setOnClickListener(view -> showAddProductDialog());
	}
	private void fetchProducts() {
		reference.addValueEventListener(new ValueEventListener() {
			@Override
			public void onDataChange(@NonNull DataSnapshot snapshot) {
				// Ensure our global lists are initialized and then clear them.
				if (productList == null) productList = new ArrayList<>();
				if (stageList == null) stageList = new ArrayList<>();
				if (levelList == null) levelList = new ArrayList<>();
				if (dateList == null) dateList = new ArrayList<>();
				if (intervalList == null) intervalList = new ArrayList<>();
				if (dataList == null) dataList = new ArrayList<>();

				productList.clear();
				stageList.clear();
				levelList.clear();
				dateList.clear();
				intervalList.clear();
				dataList.clear();

				// Convert the Firebase snapshot to a JSON object.
				JSONObject json = FirebaseJsonConverter.convertDataSnapshotToJson(snapshot);
				Log.d("ENimesh", "Fetched JSON: " + json);

				// We expect our data under the key stored in userName (e.g. "users1")
				if (!json.has(userName)) {
					Log.e("Firebase", "No '" + userName + "' node found in Firebase!");
					return;
				}

				try {
					JSONObject usersObj = json.getJSONObject(userName);

					// We will build aggregated lists—one aggregated entry per product.
					List<String> aggProductList = new ArrayList<>();
					List<String> aggStageList = new ArrayList<>();
					List<String> aggLevelList = new ArrayList<>();
					List<String> aggDateList = new ArrayList<>();
					List<String> aggIntervalList = new ArrayList<>();
					List<String> aggDataList = new ArrayList<>();

					// Loop through all products (e.g. "watermellon", "tamoto")
					Iterator<String> prodKeys = usersObj.keys();
					while (prodKeys.hasNext()) {
						String prodName = prodKeys.next();
						if (prodName.equals("password")) continue; // ✅ Skip "password" key
						aggProductList.add(prodName);

						JSONObject productObj = usersObj.optJSONObject(prodName);
						// Create StringBuilders for aggregation.
						StringBuilder sbStages = new StringBuilder();
						StringBuilder sbLevels = new StringBuilder();
						StringBuilder sbDates = new StringBuilder();
						StringBuilder sbIntervals = new StringBuilder();
						StringBuilder sbMessages = new StringBuilder();

						boolean hasEntry = false; // flag if we found any stage/level entry

						// Loop through stages in the product (e.g. "start", "flowring")
						Iterator<String> stageKeys = productObj.keys();
						while (stageKeys.hasNext()) {
							String stageName = stageKeys.next();
							// The value for a stage is stored as an array.
							JSONArray stageArray = productObj.optJSONArray(stageName);
							if (stageArray == null) continue;

							// Loop through the array starting at index 1 (skip null entry)
							for (int i = 1; i < stageArray.length(); i++) {
								JSONObject levelObj = stageArray.optJSONObject(i);
								if (levelObj == null) continue;
								hasEntry = true;

								// Append the stage name.
								if (sbStages.length() > 0) sbStages.append("; ");
								sbStages.append(stageName);

								// Append the level (use the index)
								if (sbLevels.length() > 0) sbLevels.append("; ");
								sbLevels.append(i);

								// Get the date from levelObj: the object under key "date" has a "value"
								String lvlDate = levelObj.optJSONObject("date").optString("value", "Unknown");
								if (sbDates.length() > 0) sbDates.append("; ");
								sbDates.append(lvlDate);

								// Get the interval (we’ll aggregate all but later we pick the first one)
								int lvlInterval = levelObj.optJSONObject("interval").optInt("value", 0);
								if (sbIntervals.length() > 0) sbIntervals.append("; ");
								sbIntervals.append(lvlInterval);

								// Get the messages: now data is a JSON string (an array of strings)
								String rawData = levelObj.optJSONObject("data").optString("value", "[]");
								rawData = sanitizeJsonString(rawData);
								try {
									JSONArray messagesArray = new JSONArray(rawData);
									// Join the messages from this level with commas.
									StringBuilder levelMessages = new StringBuilder();
									for (int j = 0; j < messagesArray.length(); j++) {
										if (j > 0) levelMessages.append(", ");
										levelMessages.append(messagesArray.optString(j, "No Message"));
									}
									if (sbMessages.length() > 0) sbMessages.append("; ");
									sbMessages.append(levelMessages.toString());
								} catch (JSONException e) {
									Log.e("Firebase", "Error parsing data as JSONArray", e);
								}
							}
						}
						// If we found no stage data for this product, add placeholders.
						if (!hasEntry) {
							sbStages.append("N/A");
							sbLevels.append("N/A");
							sbDates.append("N/A");
							sbIntervals.append("0");
							sbMessages.append("No data available");
						}

						// Add the aggregated strings to the aggregated lists.
						aggStageList.add(sbStages.toString());
						aggLevelList.add(sbLevels.toString());
						aggDateList.add(sbDates.toString());
						aggIntervalList.add(sbIntervals.toString());
						aggDataList.add(sbMessages.toString());
					}

					// Now update the global lists with the aggregated ones.
					productList.clear();
					productList.addAll(aggProductList);
					stageList.clear();
					stageList.addAll(aggStageList);
					levelList.clear();
					levelList.addAll(aggLevelList);
					dateList.clear();
					dateList.addAll(aggDateList);
					// For intervals, the adapter originally expected List<Integer>.
					// Here, we’ll convert the aggregated interval string into an integer.
					// For simplicity, we’ll take the first number from the aggregated string.
					intervalList.clear();
					for (String intervalStr : aggIntervalList) {
						try {
							// Split by semicolon and parse the first interval.
							String[] parts = intervalStr.split(";");
							intervalList.add(Integer.parseInt(parts[0].trim()));
						} catch (Exception ex) {
							intervalList.add(0);
						}
					}
					dataList.clear();
					dataList.addAll(aggDataList);

				} catch (JSONException e) {
					Log.e("Firebase", "Error parsing JSON", e);
				}

				// Debugging logs
				Log.d("Firebase", "PL: " + productList);
				Log.d("Firebase", "Stages: " + stageList);
				Log.d("Firebase", "Levels: " + levelList);
				Log.d("Firebase", "Dates: " + dateList);
				Log.d("Firebase", "Intervals: " + intervalList);
				Log.d("Firebase", "Data: " + dataList);

				// Update the RecyclerView if there is any product.
				if (!productList.isEmpty()) {
					runOnUiThread(() -> {
						adapter.updateList(productList, stageList, levelList, dateList, intervalList, dataList);
						adapter.notifyDataSetChanged();
					});
				} else {
					Log.d("ENimesh", "Updated list is empty, RecyclerView will not update.");
				}
			}

			@Override
			public void onCancelled(@NonNull DatabaseError error) {
				Log.e("Firebase", "Error fetching data", error.toException());
			}
		});
	}

	// **Function to fix JSON issues**
	private static String sanitizeJsonString(String json) {
		return json.replace("\\\"", "\"")
										.replace("\"[", "[")
										.replace("]\"", "]")
										.trim();
	}


	private void showAddProductDialog() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle("Enter Product Name");

		View customView = LayoutInflater.from(this).inflate(R.layout.dialog_add_product, null);
		EditText etProductName = customView.findViewById(R.id.etProductName);
		builder.setView(customView);

		builder.setPositiveButton("OK", (dialog, which) -> {
			String productName = etProductName.getText().toString().trim();

			if (!productName.isEmpty()) {
				reference.child(userName).child(productName).setValue("")
												.addOnSuccessListener(aVoid -> {
													fetchProducts();
													Toast.makeText(this, "Product Added: " + productName, Toast.LENGTH_SHORT).show();
												})
												.addOnFailureListener(e -> {
													Toast.makeText(this, "Failed to add product!", Toast.LENGTH_SHORT).show();
												});
			} else {
				Toast.makeText(this, "Product name cannot be empty!", Toast.LENGTH_SHORT).show();
			}
		});

		builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());

		alertDialog = builder.create();
		alertDialog.show();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (alertDialog != null && alertDialog.isShowing()) {
			alertDialog.dismiss();
		}
	}
}
