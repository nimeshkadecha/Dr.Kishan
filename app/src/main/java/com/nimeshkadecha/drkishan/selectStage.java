package com.nimeshkadecha.drkishan;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class selectStage extends AppCompatActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		EdgeToEdge.enable(this);
		setContentView(R.layout.activity_select_stage);

		String productName = getIntent().getStringExtra("productName");
		String userName = getIntent().getStringExtra("userName");
//		String stageList = getIntent().getStringExtra("stageList");
//		String levelList = getIntent().getStringExtra("levelList");
//		String dateList = getIntent().getStringExtra("dateList");
//		String intervalList = getIntent().getStringExtra("intervalList");
//		String dataList = getIntent().getStringExtra("dataList");

		Log.d("ENimesh", "Received Data: " + productName);
//		Log.d("ENimesh", "Received Data: " + stageList);
//		Log.d("ENimesh", "Received Data: " + levelList);
//		Log.d("ENimesh", "Received Data: " + dateList);
//		Log.d("ENimesh", "Received Data: " + intervalList);
//		Log.d("ENimesh", "Received Data: " + dataList);


		Button s1 = findViewById(R.id.button_1);
		Button s2 = findViewById(R.id.button_2);
		Button s3 = findViewById(R.id.button_3);

		Button f1 = findViewById(R.id.button_1_f);
		Button f2 = findViewById(R.id.button_2_f);
		Button f3 = findViewById(R.id.button_3_f);

		Intent intent = new Intent(this, basicInfo.class);

		intent.putExtra("productName", productName);
		intent.putExtra("userName", userName);
//		intent.putExtra("stageList", stageList);
//		intent.putExtra("levelList", levelList);
//		intent.putExtra("dateList", dateList);
//		intent.putExtra("intervalList", intervalList);
//		intent.putExtra("dataList", dataList);

		s1.setOnClickListener(view -> {
			intent.putExtra("stage", "1");
			intent.putExtra("level" ,"start");
			startActivity(intent);
		});
		s2.setOnClickListener(view -> {
			intent.putExtra("stage", "2");
			intent.putExtra("level" ,"start");
			startActivity(intent);
			});
		s3.setOnClickListener(view -> {
			intent.putExtra("stage", "3");
			intent.putExtra("level" ,"start");
			startActivity(intent);
			});

		f1.setOnClickListener(view -> {
			intent.putExtra("stage", "1");
			intent.putExtra("level" ,"flowering");
			startActivity(intent);
			});
		f2.setOnClickListener(view -> {
			intent.putExtra("stage", "2");
			intent.putExtra("level" ,"flowering");
			startActivity(intent);
			});
		f3.setOnClickListener(view -> {
			intent.putExtra("stage", "3");
			intent.putExtra("level" ,"flowering");
			startActivity(intent);
			});
	}
}