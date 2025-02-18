package com.nimeshkadecha.drkishan.pages;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.nimeshkadecha.drkishan.Helper.FirebaseJsonConverter;
import com.nimeshkadecha.drkishan.R;

import org.json.JSONObject;

public class _1_LoginPage extends AppCompatActivity {

	private DatabaseReference reference;
	private SharedPreferences sharedPreferences;
	private static final String PREFS_NAME = "DrKishanPrefs";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		EdgeToEdge.enable(this);
		setContentView(R.layout.activity_1_login);

		Button login = findViewById(R.id.loginBtn);
		reference = FirebaseDatabase.getInstance().getReference("");
		sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

		// ✅ If user is logged in, use SharedPreferences & skip Firebase
		if (isUserLoggedIn()) {
			String loggedInUser = sharedPreferences.getString("loggedInUser", "");
			navigateToProducts(loggedInUser);
			return;
		}

		login.setOnClickListener(v -> {
			String name = ((EditText) findViewById(R.id.rName)).getText().toString().trim();
			String pass = ((EditText) findViewById(R.id.password)).getText().toString().trim();

			if (name.isEmpty()) {
				((EditText) findViewById(R.id.rName)).setError("Enter Name Here");
				return;
			}

			if (pass.isEmpty()) {
				((EditText) findViewById(R.id.password)).setError("Enter Password Here");
				return;
			}

			ValidateLoginPassword(name, pass);
		});
	}

	private boolean isUserLoggedIn() {
		return sharedPreferences.contains("loggedInUser") && sharedPreferences.contains("savedJson");
	}

	private void saveUserLogin(String username, String jsonData) {
		SharedPreferences.Editor editor = sharedPreferences.edit();
		editor.putString("loggedInUser", username);
		editor.putString("savedJson", jsonData);  // ✅ Save Firebase data
		editor.apply();
	}

	private void navigateToProducts(String username) {
		Intent intent = new Intent(_1_LoginPage.this, _2_ProductList_1_main.class);
		intent.putExtra("name", username);
		startActivity(intent);
		finish();
	}

	private void ValidateLoginPassword(String name, String pass) {

		if (!isInternetAvailable()) {
			showNoInternetDialog();
			return;
		}


		reference.addListenerForSingleValueEvent(new ValueEventListener() {
			@Override
			public void onDataChange(@NonNull DataSnapshot snapshot) {
				if (snapshot.hasChild(name)) {
					String storedPassword = snapshot.child(name).child("password").getValue(String.class);

					if (storedPassword != null && storedPassword.equals(pass)) {
						// ✅ Convert Firebase snapshot to JSON & save
						JSONObject json = FirebaseJsonConverter.convertDataSnapshotToJson(snapshot);
						saveUserLogin(name, json.toString());

						// ✅ Redirect to Products page
						navigateToProducts(name);
					} else {
						((EditText) findViewById(R.id.password)).setError("Incorrect password");
					}
				} else {
					((EditText) findViewById(R.id.rName)).setError("User does not exist");
				}
			}

			@Override
			public void onCancelled(@NonNull DatabaseError error) {
				Log.e("Firebase", "Error fetching user data: " + error.getMessage());
			}
		});
	}

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

	private void showNoInternetDialog() {
		new AlertDialog.Builder(this)
										.setTitle("No Internet Connection")
										.setMessage("Please check your internet and try again.")
										.setPositiveButton("Retry", (dialog, which) -> ValidateLoginPassword(
																		((EditText) findViewById(R.id.rName)).getText().toString().trim(),
																		((EditText) findViewById(R.id.password)).getText().toString().trim()))
										.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
										.show();
	}


}
