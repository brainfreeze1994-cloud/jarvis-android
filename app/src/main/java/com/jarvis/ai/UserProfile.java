package com.jarvis.ai;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONObject;

/**
 * Stores the user's personal profile so H.E.N.R.Y can personalise every response.
 * Saved locally, sent to backend on every API call.
 */
public class UserProfile {

    private static final String PREFS = "henry_profile";

    public String name;
    public String city;
    public String job;
    public String interests;
    public String language;   // preferred response language hint
    public String nickname;   // what HENRY calls them

    public static UserProfile load(Context ctx) {
        SharedPreferences p = prefs(ctx);
        UserProfile u = new UserProfile();
        u.name      = p.getString("name",      "");
        u.city      = p.getString("city",      "Dubai");
        u.job       = p.getString("job",       "");
        u.interests = p.getString("interests", "");
        u.language  = p.getString("language",  "");
        u.nickname  = p.getString("nickname",  "sir");
        return u;
    }

    public void save(Context ctx) {
        prefs(ctx).edit()
            .putString("name",      name      != null ? name      : "")
            .putString("city",      city      != null ? city      : "Dubai")
            .putString("job",       job       != null ? job       : "")
            .putString("interests", interests != null ? interests : "")
            .putString("language",  language  != null ? language  : "")
            .putString("nickname",  nickname  != null ? nickname  : "sir")
            .apply();
    }

    public boolean isEmpty() {
        return (name == null || name.trim().isEmpty())
            && (job == null || job.trim().isEmpty())
            && (interests == null || interests.trim().isEmpty());
    }

    /** Convert to JSON for API call. */
    public JSONObject toJson() {
        try {
            JSONObject j = new JSONObject();
            if (name      != null && !name.trim().isEmpty())      j.put("name",      name.trim());
            if (city      != null && !city.trim().isEmpty())      j.put("city",      city.trim());
            if (job       != null && !job.trim().isEmpty())       j.put("job",       job.trim());
            if (interests != null && !interests.trim().isEmpty()) j.put("interests", interests.trim());
            if (language  != null && !language.trim().isEmpty())  j.put("language",  language.trim());
            if (nickname  != null && !nickname.trim().isEmpty())  j.put("nickname",  nickname.trim());
            return j;
        } catch (Exception e) { return new JSONObject(); }
    }

    private static SharedPreferences prefs(Context ctx) {
        return ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
}
