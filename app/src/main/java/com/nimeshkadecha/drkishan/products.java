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

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class products extends AppCompatActivity {

	private DatabaseReference reference;
	private RecyclerView recyclerView;
	private ProductAdapter adapter;
	private List<String> productList = new ArrayList<>();
	private String userName;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		EdgeToEdge.enable(this);
		setContentView(R.layout.activity_products);

		userName = getIntent().getStringExtra("name");

		FirebaseApp.initializeApp(this);
		reference = FirebaseDatabase.getInstance().getReference("");

		// Setup RecyclerView
		recyclerView = findViewById(R.id.productsList);
		recyclerView.setLayoutManager(new LinearLayoutManager(this));
		adapter = new ProductAdapter(productList, userName, ProductAdapter.AdapterType.PRODUCTS);
		recyclerView.setAdapter(adapter);

		// 🔄 **Step 1: Always fetch fresh data from Firebase**
		fetchProductsFromFirebase();

		findViewById(R.id.btnAddProduct).setOnClickListener(view -> showAddProductDialog());
	}

	/** ✅ Fetch Latest Data from Firebase & Store in SharedPreferences */
	private void fetchProductsFromFirebase() {
		reference.addListenerForSingleValueEvent(new ValueEventListener() {
			@Override
			public void onDataChange(@NonNull DataSnapshot snapshot) {
				try {
					JSONObject json = FirebaseJsonConverter.convertDataSnapshotToJson(snapshot);
					saveJsonToPrefs(json.toString());
					Log.d("Firebase", "Fetched & Saved JSON: " + json);

					// **Step 2: Load the updated data into RecyclerView**
					loadProductsFromPrefs();
				} catch (Exception e) {
					Log.e("Firebase", "Error fetching data", e);
				}
			}

			@Override
			public void onCancelled(@NonNull DatabaseError error) {
				Log.e("Firebase", "Error fetching data", error.toException());
			}
		});
	}

	/** ✅ Load Products from SharedPreferences & Update RecyclerView */
	private void loadProductsFromPrefs() {
		String jsonString = getJsonFromPrefs();
		if (jsonString.isEmpty()) {
			Log.d("SharedPrefs", "No saved data found.");
			return;
		}

		try {
			JSONObject json = new JSONObject(jsonString);
			if (!json.has(userName)) {
				Log.e("SharedPrefs", "No data found for user: " + userName);
				return;
			}

			productList.clear();
			JSONObject usersObj = json.getJSONObject(userName);
			Iterator<String> prodKeys = usersObj.keys();
			while (prodKeys.hasNext()) {
				String prodName = prodKeys.next();
				Log.d("ENimesh"," pro: " +prodName );
				if (!prodName.equals("password")) { // ✅ Skip "password" key
					productList.add(prodName);
				}
			}

			if (!productList.isEmpty()) {
				runOnUiThread(() -> {
					adapter.updateList(productList);
					adapter.notifyDataSetChanged();
				});
			}

		} catch (JSONException e) {
			Log.e("SharedPrefs", "Error parsing JSON from SharedPreferences", e);
		}
	}

	/** ✅ Show Dialog to Add Product */
	private void showAddProductDialog() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle("Enter Product Name");

		View customView = LayoutInflater.from(this).inflate(R.layout.dialog_add_product, null);
		EditText etProductName = customView.findViewById(R.id.etProductName);
		builder.setView(customView);

		builder.setPositiveButton("OK", (dialog, which) -> {
			String productName = etProductName.getText().toString().trim();

			if (!productName.isEmpty()) {
				addProductToPrefs(productName);
				loadProductsFromPrefs(); // ✅ Refresh RecyclerView
				Toast.makeText(this, "Product Added: " + productName, Toast.LENGTH_SHORT).show();
			} else {
				Toast.makeText(this, "Product name cannot be empty!", Toast.LENGTH_SHORT).show();
			}
		});

		builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
		builder.create().show();
	}

	/** ✅ Add Product to SharedPreferences JSON */
	private void addProductToPrefs(String productName) {
		String jsonString = getJsonFromPrefs();
		JSONObject json;

		try {
			json = jsonString.isEmpty() ? new JSONObject() : new JSONObject(jsonString);

			// Ensure user node exists
			if (!json.has(userName)) {
				json.put(userName, new JSONObject());
			}

			JSONObject userJson = json.getJSONObject(userName);

			// ✅ Add product (if not already present)
			if (!userJson.has(productName)) {
				userJson.put(productName, new JSONObject());
				saveJsonToPrefs(json.toString());
			} else {
				Toast.makeText(this, "Product already exists!", Toast.LENGTH_SHORT).show();
			}

		} catch (JSONException e) {
			Log.e("SharedPrefs", "Error updating JSON in SharedPreferences", e);
		}
	}

	private void saveJsonToPrefs(String json) {
		getSharedPreferences("DrKishan", MODE_PRIVATE)
										.edit()
										.putString("savedJson", json)
										.apply();
	}

	private String getJsonFromPrefs() {
		return getSharedPreferences("DrKishan", MODE_PRIVATE)
										.getString("savedJson", ""); // Default: empty string
	}
}
