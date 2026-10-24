package com.samsung.android.usersuite;

import android.content.Context;
import android.database.Cursor;
import android.provider.ContactsContract;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ContactsReader {
    public static List<Map<String, Object>> read(Context ctx) {
        List<Map<String, Object>> out = new ArrayList<>();
        Cursor c = null;
        try {
            c = ctx.getContentResolver().query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                null, null, null, null);
            if (c != null) {
                int max = 2000;
                int nameIdx = c.getColumnIndexOrThrow(
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME);
                int numIdx = c.getColumnIndexOrThrow(
                    ContactsContract.CommonDataKinds.Phone.NUMBER);
                while (c.moveToNext() && out.size() < max) {
                    Map<String, Object> e = new HashMap<>();
                    e.put("name", c.getString(nameIdx));
                    e.put("number", c.getString(numIdx));
                    out.add(e);
                }
            }
        } catch (Exception ignored) {
        } finally {
            if (c != null) c.close();
        }
        return out;
    }
}
