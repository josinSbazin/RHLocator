package ru.com.rh.rhlocator.location_utils;


import android.util.Log;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;

class GeocoderByHttpAndJsonForGenymotion {

    private static JSONObject getLocationInfo(double lat, double lng) {

        HttpGet httpGet = new HttpGet("http://maps.googleapis.com/maps/api/geocode/json?latlng="+ lat + "," + lng +"&&language=ru&sensor=true");
        HttpClient client = new DefaultHttpClient();
        HttpResponse response;
        StringBuilder stringBuilder = new StringBuilder();

        try {
            response = client.execute(httpGet);
            HttpEntity entity = response.getEntity();
            InputStream stream = entity.getContent();
            int b;
            while ((b = stream.read()) != -1) {
                stringBuilder.append((char) b);
            }
        } catch (IOException e) { e.printStackTrace();
        }

        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject = new JSONObject(stringBuilder.toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return jsonObject;
    }

    static String getCurrentLocationViaJSON(double lat, double lng) {

        JSONObject jsonObj = getLocationInfo(lat, lng);

        String currentLocation = null;

        try {
            String status = jsonObj.getString("status");

            if(status.equalsIgnoreCase("OK")){
                JSONArray results = jsonObj.getJSONArray("results");
                int i = 0;
                do{
                    JSONObject r = results.getJSONObject(i);
                    JSONArray typesArray = r.getJSONArray("address_components");
                    for (int j = 0; j < typesArray.length(); j++) {
                        JSONObject m = typesArray.getJSONObject(j);
                        String types = m.toString();

                        if (types.matches(".*(locality).*(political).*")) {
                            currentLocation = new String(m.getString("long_name").getBytes("ISO-8859-1"), "UTF-8");
                            if (!currentLocation.equals("")) break;
                        }
                    }
                    i++;
                } while(i<results.length());

                return currentLocation == null ? "Can't resolve location name, sry" : currentLocation;
            }

        } catch (JSONException e) {
            Log.e("testing","Failed to load JSON");
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return null;
    }
}
