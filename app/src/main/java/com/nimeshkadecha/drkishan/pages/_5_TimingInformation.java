package com.nimeshkadecha.drkishan.pages;

import android.app.DatePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.FirebaseApp;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.nimeshkadecha.drkishan.R;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class _5_TimingInformation extends AppCompatActivity {

	private DatabaseReference reference;
	private EditText date, days, amount;
	private Spinner spinner;

	private static boolean isDrip = false;
	private Switch drip;

	private String o_days,o_amo;
	private int o_position;

	private String userName, productName, stage, subStage;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		EdgeToEdge.enable(this);
		setContentView(R.layout.activity_5_timing_info);

		Log.d("ENimesh","_5_TimingInformation");

		// Get values from Intent
		userName = getIntent().getStringExtra("userName");
		productName = getIntent().getStringExtra("productName");
		stage = getIntent().getStringExtra("stage");
		subStage = getIntent().getStringExtra("subStage");

		// ✅ setting header
		TextView header = findViewById(R.id.textView_Header);
		header.setText(MessageFormat.format("FP > {0} > {1} > {2}",productName, stage, subStage));
		header.setTextSize(20f);
		HorizontalScrollView scrollView = findViewById(R.id.horizontalScrollView);
		scrollView.post(() -> scrollView.smoothScrollTo(header.getWidth(), 0));

		// Initialize UI elements
		days = findViewById(R.id.days);
		date = findViewById(R.id.date);
		amount = findViewById(R.id.editTextText);
		spinner = findViewById(R.id.spinner);
		drip = findViewById(R.id.switch1Drip);

//		o_days = "0";
//		o_position = 0;
//		o_amo = "0";


		SharedPreferences prefs = getSharedPreferences("DrKishanPrefs", MODE_PRIVATE);
		String savedJson = prefs.getString("savedJson", "{}"); // Default: empty JSON
		try {
			JSONObject jsonObject = new JSONObject(savedJson);
			if (jsonObject.has(userName)) {
				JSONObject usersObj = jsonObject.getJSONObject(userName);
				if (usersObj.has(productName) && usersObj.getJSONObject(productName).has(stage)) {
					JSONObject subStageObj = usersObj.getJSONObject(productName).getJSONObject(stage).getJSONObject(subStage);
					Object dripObj = subStageObj.get("isDrip");
					String dripVal = (dripObj instanceof JSONObject) ? ((JSONObject) dripObj).optString("value", "") : dripObj.toString();
					isDrip = dripVal.equals("true");
					drip.setChecked(isDrip);
				}
			}

		}
		catch (JSONException e) {
			e.printStackTrace();
		}

		// Populate Spinner with "Acr" and "Vigha"
		ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, new String[]{"Vigha", "Acr"});
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinner.setAdapter(adapter);

		drip.setOnCheckedChangeListener((compoundButton, b) -> {
			isDrip = b;

			// Get your SharedPreferences instance
			SharedPreferences prefs1 = getSharedPreferences("DrKishanPrefs", MODE_PRIVATE);
			String savedJson1 = prefs1.getString("savedJson", "{}");

			try {
				// Parse the JSON string
				JSONObject jsonObject = new JSONObject(savedJson1);

				// Navigate to the correct subStage location.
				// Make sure to check for existence of keys to avoid exceptions.
				if (jsonObject.has(userName)) {
					JSONObject userObject = jsonObject.getJSONObject(userName);
					if (userObject.has(productName)) {
						JSONObject productObject = userObject.getJSONObject(productName);
						if (productObject.has(stage)) {
							JSONObject stageObject = productObject.getJSONObject(stage);
							if (stageObject.has(subStage)) {
								JSONObject subStageObject = stageObject.getJSONObject(subStage);
								// Update the isDrip value
								subStageObject.put("isDrip", isDrip);
							}
						}
					}
				}

				// Save the updated JSON back to SharedPreferences
				SharedPreferences.Editor editor = prefs1.edit();
				editor.putString("savedJson", jsonObject.toString());
				editor.apply();
			} catch (JSONException e) {
				e.printStackTrace();
			}

			// Now update your UI based on the new state
			if (b) {
				amount.setText("1");
				spinner.setSelection(0);
				days.setText("0");
				findViewById(R.id.textInputLayoutName).setVisibility(View.INVISIBLE);
				findViewById(R.id.textInputLayoutAMo).setVisibility(View.INVISIBLE);
				spinner.setVisibility(View.INVISIBLE);
			} else {
				Log.d("ST", "om = " + o_amo + " days - " + o_days);
				amount.setText(o_amo);
				spinner.setSelection(o_position);
				days.setText(o_days);
				findViewById(R.id.textInputLayoutName).setVisibility(View.VISIBLE);
				findViewById(R.id.textInputLayoutAMo).setVisibility(View.VISIBLE);
				spinner.setVisibility(View.VISIBLE);
			}
		});


		if(isDrip){
			amount.setText("1");
			spinner.setSelection(0);
			days.setText("0");
			findViewById(R.id.textInputLayoutName).setVisibility(View.INVISIBLE);
			findViewById(R.id.textInputLayoutAMo).setVisibility(View.INVISIBLE);
			spinner.setVisibility(View.INVISIBLE);
		}

		// Initialize Firebase
		FirebaseApp.initializeApp(this);
		if (FirebaseApp.getApps(this).isEmpty()) {
			return;
		}

		// Set Firebase reference path
		reference = FirebaseDatabase.getInstance().getReference(userName + "/" + productName + "/" + stage + "/" + subStage);
		Log.d("ENimesh", "Firebase Reference: " + reference.toString());

		// Fetch data from SharedPreferences (Local Storage)
		loadDataFromSharedPreferences();

		// Fetch data from Firebase (if needed)
//		reference.addValueEventListener(new ValueEventListener() {
//			@Override
//			public void onDataChange(@NonNull DataSnapshot snapshot) {
//				if (snapshot.exists()) {
//					// Fetch interval (days)
//					if (snapshot.child("interval").exists()) {
//						String intervalValue = snapshot.child("interval").getValue().toString();
//						days.setText(intervalValue != null ? intervalValue : "");
//					}
//
//					// Fetch date
//					if (snapshot.child("date").exists()) {
//						String dateValue = snapshot.child("date").getValue().toString();
//						date.setText(dateValue != null ? dateValue : getCurrentDate());
//					}
//
//					// Fetch amount
//					if (snapshot.child("amount").exists()) {
//						String amountValue = snapshot.child("amount").getValue().toString();
//						amount.setText(amountValue != null ? amountValue : "");
//					}
//
//					// Fetch Counting Unit (Spinner)
//					if (snapshot.child("countingValue").exists()) {
//						String unit = snapshot.child("countingValue").getValue().toString();
//						int spinnerPosition = adapter.getPosition(unit);
//						spinner.setSelection(spinnerPosition);
//					}
//				}
//			}
//
//			@Override
//			public void onCancelled(@NonNull DatabaseError error) {
//				Log.e("Firebase", "Error fetching data", error.toException());
//			}
//		});

		// Date Picker

		date.setOnClickListener(v -> show_date_time_picker());
		findViewById(R.id.textInputLayoutName2).setOnClickListener(view -> show_date_time_picker());

		// Continue button click listener
		findViewById(R.id.continueToNext).setOnClickListener(view -> validateAndContinue());


	}

	// ✅ Load data from SharedPreferences
	private void loadDataFromSharedPreferences() {
		SharedPreferences prefs = getSharedPreferences("DrKishanPrefs", MODE_PRIVATE);
		String savedJson = prefs.getString("savedJson", "{}"); // Default: empty JSON

		try {
			JSONObject jsonObject = new JSONObject(savedJson);
			if (jsonObject.has(userName)) {
				JSONObject usersObj = jsonObject.getJSONObject(userName);
				if (usersObj.has(productName) && usersObj.getJSONObject(productName).has(stage)) {
					JSONObject subStageObj = usersObj.getJSONObject(productName).getJSONObject(stage).getJSONObject(subStage);

					// ✅ Prefill Days (interval)
					if(subStageObj.has("interval")){
						Object intervalObj = subStageObj.get("interval");
						String intervalValue = (intervalObj instanceof JSONObject) ? ((JSONObject) intervalObj).optString("value", "") : intervalObj.toString();
						Log.d("ST","Days - " + o_days);
						if(!isDrip) days.setText(intervalValue);
						o_days = intervalValue;
					}

					if (subStageObj.has("date")){
						Object dateObj = subStageObj.get("date");
						String dateValue = (dateObj instanceof JSONObject) ? ((JSONObject) dateObj).optString("value", "") : dateObj.toString();
						date.setText(dateValue);
					}
					// ✅ Prefill Date (Use current date if not found)

					// ✅ Extract "count" correctly from JSON and set in Amount field
					if (subStageObj.has("count")) {
						Object countObj = subStageObj.get("count");
						String countValue = (countObj instanceof JSONObject) ? ((JSONObject) countObj).optString("value", "0") : countObj.toString();
						Log.d("ST" , " amo " + countValue);
						if(!isDrip) amount.setText(countValue); // ✅ Set count as amount
						o_amo = countValue;
					}

					// ✅ Extract "countValue" correctly from SharedPreferences and preselect in Spinner
					int countValue = 0;
					if (prefs.contains("count")) {
						Object countPref = prefs.getAll().get("count");
						countValue = (countPref instanceof JSONObject) ? ((JSONObject) countPref).optInt("value", 0) : Integer.parseInt(countPref.toString());
					}

					ArrayAdapter<String> adapter = (ArrayAdapter<String>) spinner.getAdapter();
					if (adapter != null) {
						int spinnerPosition = adapter.getPosition(String.valueOf(countValue)); // Convert int to String
						if (spinnerPosition >= 0) { // Ensure the position exists in the adapter
							if(!isDrip) spinner.setSelection(spinnerPosition); // ✅ Preselect unit
							o_position = spinnerPosition;
						}
					}
				}
			}
		} catch (JSONException e) {
			Log.e("SharedPreferences", "Error parsing JSON", e);
		}
	}

	// ✅ Validate Fields & Continue

	private void validateAndContinue() {
		if (days.getText().toString().isEmpty()) {
			days.setError("Enter interval da ys");
			return;
		}
		if (date.getText().toString().isEmpty()) {
			date.setError("Select date");
			return;
		}
		if (amount.getText().toString().isEmpty()) {
			amount.setError("Enter amount");
			return;
		}

		SharedPreferences prefs = getSharedPreferences("DrKishanPrefs", MODE_PRIVATE);
		SharedPreferences.Editor editor = prefs.edit();

		int countValue = 0; // Default count
		if (!prefs.getString("savedJson", "{}").equals("{}")) {
			try {
				JSONObject jsonObject = new JSONObject(prefs.getString("savedJson", "{}"));
				if (jsonObject.has(userName)) {
					JSONObject usersObj = jsonObject.getJSONObject(userName);
					if (usersObj.has(productName) && usersObj.getJSONObject(productName).has(stage)) {
						JSONObject subStageObj = usersObj.getJSONObject(productName).getJSONObject(stage).getJSONObject(subStage);
						countValue = subStageObj.optInt("count", 0);
					}
				}
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}

		editor.putInt("count", countValue); // ✅ Store count

		editor.apply();

		Intent gotoFinalStep = new Intent(this, _6_ShowAllMessages.class);
		gotoFinalStep.putExtra("productName", productName);
		gotoFinalStep.putExtra("userName", userName);
		gotoFinalStep.putExtra("stage", stage);
		gotoFinalStep.putExtra("subStage", subStage);
		gotoFinalStep.putExtra("date", date.getText().toString());
		gotoFinalStep.putExtra("days", days.getText().toString());
		gotoFinalStep.putExtra("amount", amount.getText().toString());
		gotoFinalStep.putExtra("unit", spinner.getSelectedItem().toString());

		startActivity(gotoFinalStep);
	}

	// ✅ Date Picker Dialog
	private void show_date_time_picker() {
		InputMethodManager inm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
		if (getCurrentFocus() != null) {
			inm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
		}

		final Calendar calendar = Calendar.getInstance();
		int year = calendar.get(Calendar.YEAR);
		int month = calendar.get(Calendar.MONTH);
		int day = calendar.get(Calendar.DAY_OF_MONTH);

		DatePickerDialog datePickerDialog = new DatePickerDialog(_5_TimingInformation.this, (view, year1, month1, dayOfMonth) -> {
			date.setText(dayOfMonth + "/" + (month1 + 1) + "/" + year1);
		}, year, month, day);
		datePickerDialog.show();
	}

	// ✅ Get Current Date
	private String getCurrentDate() {
		SimpleDateFormat df = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
		return df.format(new Date());
	}

	public static boolean getDripState(){
		return isDrip;
	}


	@Override
	protected void onResume() {
		super.onResume();
		loadDataFromSharedPreferences(); // ✅ Refresh RecyclerView every time the page is reopened
	}


}
