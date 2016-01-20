package com.miui.upnp;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;

public class MediaCover {

    private static final String TAG = MediaCover.class.getSimpleName();

    public static String getAlbumUri(String artist) throws IOException {
        String name = URLEncoder.encode(artist, "utf-8");
        String http = String.format("http://music.search.xiaomi.net/v61/getCovers?artist=%s", name);
        Log.d(TAG, "getAlbumUri: " + http);

        URL url = new URL(http);

        URLConnection connection = url.openConnection();
        connection.setRequestProperty("User-Agent", "MiLink");
        connection.setConnectTimeout(3 * 1000);
        connection.setReadTimeout(3 * 1000);
        connection.connect();

        return getString(connection.getInputStream());
    }

    private static String getString(InputStream stream) {
        BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
        StringBuilder builder = new StringBuilder();
        String line;

        try {
            while ((line = reader.readLine()) != null) {
                builder.append(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                reader.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return getCoverUrl(builder.toString());
    }

    private static String getCoverUrl(String json) {
        JSONObject info;

        try {
            info = new JSONObject(json);
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }

        int status = info.optInt("status", 0);
        if (status != 1) {
            Log.d(TAG, "status is: " + status);
            return null;
        }

        JSONArray list = info.optJSONArray("list");
        if (list == null) {
            Log.d(TAG, "list not found");
            return null;
        }

        JSONObject item;

        try {
            item = (JSONObject)list.get(0);
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }

        return item.optString("cover_url");
    }
}