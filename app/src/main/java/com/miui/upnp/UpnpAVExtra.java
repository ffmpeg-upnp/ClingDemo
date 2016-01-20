package com.miui.upnp;

import org.json.JSONException;
import org.json.JSONObject;

public class UpnpAVExtra {

    private String songId;
    private String songName;
    private String album;
    private String artist;

    public boolean parse(String extra) {
        boolean parsed = false;

        do {
            if (extra == null) {
                break;
            }

            JSONObject info;

            try {
                info = new JSONObject(extra);
            } catch (JSONException e) {
                e.printStackTrace();
                break;
            }

            songId = info.optString("sid");
            songName = info.optString("song");
            album = info.optString("album");
            artist = info.optString("artist");
            parsed = true;
        } while (false);

        return parsed;
    }

    public String getSongId() {
        return songId;
    }

    public String getName() {
        return songName;
    }

    public String getAlbum() {
        return album;
    }

    public String getArtist() {
        return artist;
    }
}
