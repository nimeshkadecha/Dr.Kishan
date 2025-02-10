package com.nimeshkadecha.drkishan;

import com.google.firebase.database.DataSnapshot;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class FirebaseJsonConverter {

	public static JSONObject convertDataSnapshotToJson(DataSnapshot snapshot) {
		return convertObjectToJson(snapshot.getValue());
	}

	private static JSONObject convertObjectToJson(Object data) {
		try {
			if (data instanceof Map) {
				JSONObject jsonObject = new JSONObject();
				Map<?, ?> map = (Map<?, ?>) data;
				for (Map.Entry<?, ?> entry : map.entrySet()) {
					jsonObject.put(String.valueOf(entry.getKey()), convertObjectToJson(entry.getValue()));
				}
				return jsonObject;
			} else if (data instanceof List) {
				JSONArray jsonArray = new JSONArray();
				List<?> list = (List<?>) data;
				for (Object item : list) {
					jsonArray.put(convertObjectToJson(item));
				}
				return new JSONObject().put("array", jsonArray);
			} else {
				return new JSONObject().put("value", data);
			}
		} catch (JSONException e) {
			e.printStackTrace();
			return null;
		}
	}

//	public static List<String> getAllProductNames(JSONObject json) {
//		List<String> productNames = new ArrayList<>();
//
//		try {
//			// Navigate to userName object
//			JSONObject users1 = json.getJSONObject(userName);
//
//			// Iterate through all keys under userName (product names)
//			Iterator<String> keys = users1.keys();
//			while (keys.hasNext()) {
//				String productName = keys.next();
//				productNames.add(productName);  // Add to the list
//			}
//
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//
//		return productNames;
//	}

//	public static Map<String, List<String>> extractDatesAndMessages(JSONObject json) {
//		List<String> dateList = new ArrayList<>();
//		List<String> messageList = new ArrayList<>();
//		SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
//
//		try {
//			if (json.has("array")) {
//				JSONArray array = json.getJSONArray("array");
//				List<ProductEntry> productEntries = new ArrayList<>();
//
//				for (int i = 1; i < array.length(); i++) { // Skip null entry
//					JSONObject product = array.getJSONObject(i);
//
//					// Extract raw data safely
//					String rawData = product.getJSONObject("data").getString("value");
//
//					// **Fix JSON format before parsing**
//					rawData = sanitizeJsonString(rawData);
//
//					// Parse corrected JSON string
//					JSONArray dataArray = new JSONArray(rawData);
//
//					for (int j = 0; j < dataArray.length(); j++) {
//						JSONObject event = dataArray.getJSONObject(j);
//						String date = event.optString("d", "Unknown Date");
//						String message = event.optString("t", "No Message");
//
//						// Add to temporary list
//						productEntries.add(new ProductEntry(date, message));
//					}
//				}
//
//				// Sort product entries by date
//				Collections.sort(productEntries, (e1, e2) -> {
//					try {
//						return dateFormat.parse(e1.date).compareTo(dateFormat.parse(e2.date));
//					} catch (ParseException e) {
//						e.printStackTrace();
//						return 0;
//					}
//				});
//
//				// Populate lists with sorted values
//				for (ProductEntry entry : productEntries) {
//					dateList.add(entry.date);
//					messageList.add(entry.message);
//				}
//			}
//		} catch (JSONException e) {
//			e.printStackTrace();
//		}
//
//		// Store both lists in a Map and return
//		Map<String, List<String>> resultMap = new HashMap<>();
//		resultMap.put("dates", dateList);
//		resultMap.put("messages", messageList);
//		return resultMap;
//	}

	// **Function to fix JSON issues**
//	private static String sanitizeJsonString(String json) {
//		return json.replace("'", "\"")   // Convert single quotes to double quotes
//										.replace("\"\"", "\"") // Remove unnecessary double quotes
//										.trim(); // Remove leading & trailing spaces
//	}

	// Helper class to store product data
//	static class ProductEntry {
//		String date;
//		String message;
//
//		public ProductEntry(String date, String message) {
//			this.date = date;
//			this.message = message;
//		}
//	}
}
