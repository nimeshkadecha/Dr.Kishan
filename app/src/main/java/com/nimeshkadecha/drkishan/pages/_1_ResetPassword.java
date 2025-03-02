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
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.nimeshkadecha.drkishan.Helper.FirebaseJsonConverter;
import com.nimeshkadecha.drkishan.R;

import org.json.JSONObject;

public class _1_ResetPassword extends AppCompatActivity {

	EditText currentPassword,newPassword,userName;

	private SharedPreferences sharedPreferences;

	private DatabaseReference reference;

	Button confirm;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		EdgeToEdge.enable(this);
		setContentView(R.layout.activity_1_reset_password);

		sharedPreferences = getSharedPreferences("DrKishanPrefs", MODE_PRIVATE);

		reference = FirebaseDatabase.getInstance().getReference("");

		userName = findViewById(R.id.userName);
		currentPassword = findViewById(R.id.password);
		newPassword = findViewById(R.id.newPassword);

		confirm = findViewById(R.id.confirm);

		confirm.setOnClickListener(View->{
			String userNameStr = this.userName.getText().toString();
			String currentPasswordStr = currentPassword.getText().toString();
			String newPasswordStr = newPassword.getText().toString();

			if(userNameStr.isEmpty()){
				userName.setError("Please enter username");
				return;
			}

			if(currentPasswordStr.isEmpty()){
				currentPassword.setError("Please enter current password");
				return;
			}

			if(newPasswordStr.isEmpty()){
				newPassword.setError("Please enter new password");
				return;
			}

			if(currentPasswordStr.equals(newPasswordStr)){
				newPassword.setError("New password cannot be same as current password");
				return;
			}

			if(newPasswordStr.length()<8){
				newPassword.setError("Password must be at least 8 characters long");
				return;
			}

			ValidateLoginPassword(userNameStr,currentPasswordStr,newPasswordStr);



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


	private void ValidateLoginPassword(String name, String pass,String newPass) {

		if (!isInternetAvailable()) {
			Toast.makeText(this, "No Internet connect to internet first !", Toast.LENGTH_SHORT).show();
//			showNoInternetDialog();
			return ;
		}


		reference.addListenerForSingleValueEvent(new ValueEventListener() {
			@Override
			public void onDataChange(@NonNull DataSnapshot snapshot) {
				if (snapshot.hasChild(name)) {
					String storedPassword = snapshot.child(name).child("password").getValue(String.class);

					if (storedPassword != null && storedPassword.equals(pass)) {
						// ✅ Convert Firebase snapshot to JSON & save
						JSONObject json = FirebaseJsonConverter.convertDataSnapshotToJson(snapshot);

						resetPassword(newPass,json,name);

//						saveUserLogin(name, json.toString());
//						Log.d("ENimesh", "Firebase User data = " + json);
//						// ✅ Redirect to Products page
//						navigateToProducts(name);
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

	public void resetPassword(String newPass,JSONObject json,String name){
		reference.child(userName.getText().toString()).child("password").setValue(newPass);
		Toast.makeText(this, "Password reset successfully", Toast.LENGTH_SHORT).show();

		    saveUserLogin(name, json.toString());
						Log.d("ENimesh", "Firebase User data = " + json);
						// ✅ Redirect to Products page
						navigateToProducts(name);

	}

	private void saveUserLogin(String username, String jsonData) {
		SharedPreferences.Editor editor = sharedPreferences.edit();
		editor.putString("loggedInUser", username);
		editor.putString("savedJson", jsonData);  // ✅ Save Firebase data
		editor.apply();
	}

	private void navigateToProducts(String username) {
		Intent intent = new Intent(_1_ResetPassword.this, _2_ProductList_1_main.class);
		intent.putExtra("name", username);
		startActivity(intent);
		finish();
	}

}