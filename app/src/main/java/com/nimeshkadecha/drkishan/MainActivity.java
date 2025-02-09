package com.nimeshkadecha.drkishan;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class MainActivity extends AppCompatActivity {

	private DatabaseReference reference;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		EdgeToEdge.enable(this);
		setContentView(R.layout.activity_main);
		Button login = findViewById(R.id.loginBtn);

		reference = FirebaseDatabase.getInstance().getReference("");

		reference.addValueEventListener(new ValueEventListener() {
			@Override
			public void onDataChange(@NonNull DataSnapshot snapshot) {
				for (DataSnapshot ds : snapshot.getChildren()) {
					Log.d("FNimesh", " "+ds.getKey());
				}
			}

			@Override
			public void onCancelled(@NonNull DatabaseError error) {

			}
		});

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

			ValidateLoginPassword(name, pass); // 🔥 Call function directly
		});


	}

	private void ValidateLoginPassword(String name, String pass) {
		reference.addListenerForSingleValueEvent(new ValueEventListener() {
			@Override
			public void onDataChange(@NonNull DataSnapshot snapshot) {
				if (snapshot.hasChild(name)) { // ✅ Check if user exists
					String storedPassword = snapshot.child(name).child("password").getValue(String.class);

					if (storedPassword != null && storedPassword.equals(pass)) {
						// ✅ Password matches → Proceed to next activity
						Intent intent = new Intent(MainActivity.this, products.class);
						intent.putExtra("name", name);
						startActivity(intent);
					} else {
						// ❌ Incorrect password
						((EditText) findViewById(R.id.password)).setError("Incorrect password");
					}
				} else {
					// ❌ User does not exist
					((EditText) findViewById(R.id.rName)).setError("User does not exist");
				}
			}

			@Override
			public void onCancelled(@NonNull DatabaseError error) {
				Log.e("Firebase", "Error fetching user data: " + error.getMessage());
			}
		});
	}



}