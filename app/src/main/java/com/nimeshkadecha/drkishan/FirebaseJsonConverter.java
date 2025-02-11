package com.nimeshkadecha.drkishan;

import com.google.firebase.database.DataSnapshot;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;
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
}
