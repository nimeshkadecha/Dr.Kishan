package com.nimeshkadecha.drkishan;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class products extends AppCompatActivity {

	private boolean isInternetAvailable() {
		ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		if (cm != null) {
			NetworkCapabilities capabilities = cm.getNetworkCapabilities(cm.getActiveNetwork());
			return capabilities != null &&
											(capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
																			capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR));
		}
		return false;
	}


	private RecyclerView recyclerView;
	private ProductAdapter adapter;
	private List<String> productList = new ArrayList<>();
	private String userName;
	private SharedPreferences sharedPreferences;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		EdgeToEdge.enable(this);
		setContentView(R.layout.activity_products);

		userName = getIntent().getStringExtra("name");
		sharedPreferences = getSharedPreferences("DrKishanPrefs", MODE_PRIVATE);

		// ✅ Setup RecyclerView
		recyclerView = findViewById(R.id.productsList);
		recyclerView.setLayoutManager(new LinearLayoutManager(this));
		adapter = new ProductAdapter(products.this, productList, userName, ProductAdapter.AdapterType.PRODUCTS);
		recyclerView.setAdapter(adapter);

		// ✅ Load data from SharedPreferences
		loadProductsFromPrefs();

		findViewById(R.id.btnAddProduct).setOnClickListener(view -> showAddProductDialog());
		findViewById(R.id.button2_logOut).setOnClickListener(view -> logoutUser());
	}

	/** ✅ Load Products from SharedPreferences */
	private void loadProductsFromPrefs() {
		String jsonString = sharedPreferences.getString("savedJson", "{}");

		try {
			JSONObject json = new JSONObject(jsonString);
			if (!json.has(userName)) return;

			productList.clear();
			JSONObject usersObj = json.getJSONObject(userName);
			Iterator<String> prodKeys = usersObj.keys();
			while (prodKeys.hasNext()) {
				String prodName = prodKeys.next();
				if (!prodName.equals("password")) {
					productList.add(prodName);
				}
			}

			if (!productList.isEmpty()) {
				adapter.updateList(productList);
				adapter.notifyDataSetChanged();
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
				loadProductsFromPrefs();
				Toast.makeText(this, "Product Added: " + productName, Toast.LENGTH_SHORT).show();
			} else {
				Toast.makeText(this, "Product name cannot be empty!", Toast.LENGTH_SHORT).show();
			}
		});

		builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
		builder.create().show();
	}

	/** ✅ Add Product to SharedPreferences */
	private void addProductToPrefs(String productName) {

		if (!isInternetAvailable()) {
			showNoInternetDialog();
			return;
		}

		String jsonString = sharedPreferences.getString("savedJson", "{}");

		try {
			JSONObject json = new JSONObject(jsonString);
			if (!json.has(userName)) {
				json.put(userName, new JSONObject());
			}

			JSONObject userJson = json.getJSONObject(userName);

			// ✅ Add product (if not already present)
			if (!userJson.has(productName)) {
				userJson.put(productName, new JSONObject());
				sharedPreferences.edit().putString("savedJson", json.toString()).apply();
			} else {
				Toast.makeText(this, "Product already exists!", Toast.LENGTH_SHORT).show();
			}

		} catch (JSONException e) {
			Log.e("SharedPrefs", "Error updating JSON in SharedPreferences", e);
		}
	}

	/** ✅ Logout User */
	private void logoutUser() {
		sharedPreferences.edit().clear().apply();
		startActivity(new Intent(this, MainActivity.class));
		finish();
	}


	private void showNoInternetDialog() {
		new AlertDialog.Builder(this)
										.setTitle("No Internet Connection")
										.setMessage("Please check your internet and try again.")
										.setPositiveButton("Retry", (dialog, which) -> loadProductsFromPrefs())
										.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
										.show();
	}

}
