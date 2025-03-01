package com.nimeshkadecha.drkishan.pages;

import android.annotation.SuppressLint;
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

import com.nimeshkadecha.drkishan.Helper.ProductAdapter;
import com.nimeshkadecha.drkishan.R;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class _2_ProductList_1_main extends AppCompatActivity {

	android.app.AlertDialog.Builder alert;

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

	private ProductAdapter adapter;
	private final List<String> productList = new ArrayList<>();
	private String userName;
	private SharedPreferences sharedPreferences;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		EdgeToEdge.enable(this);
		setContentView(R.layout.activity_2_product_list_1_main);

		userName = getIntent().getStringExtra("name");
		sharedPreferences = getSharedPreferences("DrKishanPrefs", MODE_PRIVATE);

		RecyclerView recyclerView = findViewById(R.id.productsList);
		recyclerView.setLayoutManager(new LinearLayoutManager(this));
		adapter = new ProductAdapter(this, productList, userName, ProductAdapter.AdapterType.PRODUCTS);
		recyclerView.setAdapter(adapter);

		loadProductsFromPrefs();

		findViewById(R.id.btnAddProduct).setOnClickListener(view -> showAddProductDialog());
		findViewById(R.id.button2_logOut).setOnClickListener(view -> logoutUser());
	}

	/** ✅ Load Products from SharedPreferences */
	@SuppressLint("NotifyDataSetChanged")
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

			Collections.sort(productList, (a, b) -> extractNumber(a) - extractNumber(b));
			adapter.updateList(productList);
			adapter.notifyDataSetChanged();
		} catch (JSONException e) {
			Log.e("SharedPrefs", "Error parsing JSON from SharedPreferences", e);
		}
	}

	/** ✅ Show Dialog to Add Product */

	private void showAddProductDialog() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle("Enter product Name");

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
				etName.setError("Product name cannot be empty!");
				return;
			}

			if (subStageName.matches(".*[.#$\\[\\]].*")) {
				etName.setError("Name cannot contain '.', '#', '$', '[', or ']'");
				return;
			}

			// If validation passes, dismiss dialog and proceed
			addProductToPrefs(subStageName);
			alertDialog.dismiss();
		});
	}


	/** ✅ Add Product to SharedPreferences */

	private void addProductToPrefs(String productName) {
		String jsonString = sharedPreferences.getString("savedJson", "{}");

		try {
			JSONObject json = new JSONObject(jsonString);
			if (!json.has(userName)) {
				json.put(userName, new JSONObject());
			}

			JSONObject userJson = json.getJSONObject(userName);
			int nextNumber = findNextAvailableNumber(userJson);
			String formattedName = nextNumber + "@" + productName;

			if (!userJson.has(formattedName)) {
				userJson.put(formattedName, new JSONObject());
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
		startActivity(new Intent(this, _1_LoginPage.class));
		finish();
	}

	private int findNextAvailableNumber(JSONObject userJson) {
		int maxNum = 0;
		Iterator<String> keys = userJson.keys();
		while (keys.hasNext()) {
			String key = keys.next();
			maxNum = Math.max(maxNum, extractNumber(key));
		}
		return maxNum + 1;
	}

	private int extractNumber(String str) {
		Pattern pattern = Pattern.compile("^(\\d+)@");
		Matcher matcher = pattern.matcher(str);
		if (matcher.find()) {
			return Integer.parseInt(matcher.group(1));
		}
		return 0;
	}

	private void showNoInternetDialog() {
		new AlertDialog.Builder(this)
										.setTitle("No Internet Connection")
										.setMessage("Please check your internet and try again.")
										.setPositiveButton("Retry", (dialog, which) -> loadProductsFromPrefs())
										.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
										.show();
	}

	//  Alert dialog box for Exiting Application ======================================================
	@SuppressLint("MissingSuperCall")
	@Override
	public void onBackPressed() {

			alert = new android.app.AlertDialog.Builder(_2_ProductList_1_main.this);
			alert.setTitle("Confirmation");
			alert.setMessage("Are you sure you want to exit?");
			alert.setPositiveButton("Exit", (dialogInterface, i) -> finishAffinity());
			alert.setNegativeButton("No", (dialogInterface, i) -> dialogInterface.dismiss());

			alert.show();

	}


}
