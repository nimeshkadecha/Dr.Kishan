package com.nimeshkadecha.drkishan;

import android.annotation.SuppressLint;
import android.app.DatePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.inputmethod.InputMethodManager;
import android.widget.DatePicker;
import android.widget.EditText;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.FirebaseApp;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;

public class basicInfo extends AppCompatActivity {

	private DatabaseReference reference;
	private EditText date, days;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		EdgeToEdge.enable(this);
		setContentView(R.layout.activity_basic_info);

		// Get values from Intent
		String productName = getIntent().getStringExtra("productName");
		String stage = getIntent().getStringExtra("stage");
		String level = getIntent().getStringExtra("level");
		String userName = getIntent().getStringExtra("userName");

		// Initialize UI elements
		days = findViewById(R.id.days);
		date = findViewById(R.id.date);

		// Initialize Firebase
		FirebaseApp.initializeApp(this);
		if (FirebaseApp.getApps(this).isEmpty()) {
			return;
		}

		// Set Firebase reference path
		reference = FirebaseDatabase.getInstance().getReference(userName + "/" + productName + "/" + level  + "/" + stage);
		Log.d("ENimesh", "Firebase Reference: " + reference.toString());

		// Fetch data from Firebase
		reference.addValueEventListener(new ValueEventListener() {
			@Override
			public void onDataChange(@NonNull DataSnapshot snapshot) {
				// Fetch days (interval value)
				if (snapshot.child("interval").exists()) {
					String intervalValue = snapshot.child("interval").getValue().toString();
					Log.d("FNimesh","snap daate" + intervalValue);
					days.setText(intervalValue != null ? intervalValue : "");
				} else {
					days.setText("");
				}

				// Fetch date
				if (snapshot.child("date").child("value").exists()) {
					String dateValue = snapshot.child("date").getValue().toString();
					date.setText(dateValue != null ? dateValue : "");
				} else {
					date.setText(getCurrentDate()); // Default to today's date
				}
			}

			@Override
			public void onCancelled(@NonNull DatabaseError error) {
				Log.e("Firebase", "Error fetching data", error.toException());
			}
		});

		// Date Picker
		date.setOnClickListener(v -> show_date_time_picker());
		findViewById(R.id.textInputLayoutName2).setOnClickListener(view -> show_date_time_picker());

		// Continue button click listener
		findViewById(R.id.continueToNext).setOnClickListener(view -> {
			if (days.getText().toString().isEmpty()) {
				days.setError("Enter days of increment");
				return;
			}
			if (date.getText().toString().isEmpty()) {
				date.setError("Select date");
				return;
			}

			Intent gotoFinalStep = new Intent(this, allinfo.class);
			gotoFinalStep.putExtra("productName", productName);
			gotoFinalStep.putExtra("userName", userName);
			gotoFinalStep.putExtra("stage", stage);
			gotoFinalStep.putExtra("level", level);
			gotoFinalStep.putExtra("date", date.getText().toString());
			gotoFinalStep.putExtra("days", days.getText().toString());
			startActivity(gotoFinalStep);
		});
	}

	private void show_date_time_picker() {
		InputMethodManager inm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
		if (getCurrentFocus() != null) {
			inm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
		}

		final Calendar calendar = Calendar.getInstance();
		int year = calendar.get(Calendar.YEAR);
		int month = calendar.get(Calendar.MONTH);
		int day = calendar.get(Calendar.DAY_OF_MONTH);

		DatePickerDialog datePickerDialog = new DatePickerDialog(basicInfo.this, (view, year1, month1, dayOfMonth) -> {
			date.setText(dayOfMonth + "/" + (month1 + 1) + "/" + year1);
		}, year, month, day);
		datePickerDialog.show();
	}

	private String getCurrentDate() {
		SimpleDateFormat df = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
		return df.format(new Date());
	}
}
