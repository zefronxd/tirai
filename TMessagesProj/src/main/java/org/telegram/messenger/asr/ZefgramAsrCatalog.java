/*
 * This file is part of zefgram, a fork of Telegram for Android.
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Copyright webarmour, 2026.
 */

package org.telegram.messenger.asr;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/** Release index published after all versioned assets are available. */
final class ZefgramAsrCatalog {
    // At most the current catalog and one installed snapshot per model are in use.
    private static final android.util.LruCache<String, ZefgramAsrModels.Model> models = new android.util.LruCache<>(24);

    static String fetch() throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(ZefgramAsrModels.MIRROR + "catalog.json").openConnection();
        try {
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);
            if (conn.getResponseCode() == 404) return null; // original v1 release
            try (InputStream in = conn.getInputStream(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                byte[] buf = new byte[4096];
                int n;
                while ((n = in.read(buf)) != -1) {
                    if (out.size() + n > 65536) throw new IOException("Oversized ASR catalog");
                    out.write(buf, 0, n);
                }
                JSONObject index = new JSONObject(out.toString("UTF-8"));
                for (ZefgramAsrModels.Model m : ZefgramAsrModels.all()) parse(m, index);
                return index.toString();
            }
        } finally {
            conn.disconnect();
        }
    }

    static ZefgramAsrModels.Model resolve(ZefgramAsrModels.Model model, String json) {
        if (json.isEmpty()) return model;
        String key = model.id + json;
        ZefgramAsrModels.Model cached = models.get(key);
        if (cached != null) return cached;
        try {
            ZefgramAsrModels.Model resolved = parse(model, new JSONObject(json));
            models.put(key, resolved);
            return resolved;
        } catch (Exception e) {
            return model;
        }
    }

    private static ZefgramAsrModels.Model parse(ZefgramAsrModels.Model m, JSONObject index) throws Exception {
        String tag = index.getString("tag");
        if (!tag.matches("asr-models-v[0-9]+")) throw new IOException("Invalid model release");
        JSONObject entries = index.getJSONObject("models").getJSONObject(m.id);
        ZefgramAsrModels.ModelFile[] files = new ZefgramAsrModels.ModelFile[m.files.length];
        for (int i = 0; i < files.length; i++) {
            String name = m.files[i].name;
            JSONObject entry = entries.getJSONObject(name);
            long size = entry.getLong("size");
            String hash = entry.getString("sha256");
            if (size <= 0 || size > Integer.MAX_VALUE || !hash.matches("[0-9a-f]{64}")) throw new IOException("Invalid model file");
            files[i] = new ZefgramAsrModels.ModelFile(name, size, hash,
                    "https://github.com/zefronxd/tirai/releases/download/" + tag + "/" + m.id + "--" + name);
        }
        return new ZefgramAsrModels.Model(m.id, m.lang, m.tier, m.engine, m.title, m.punctuation, m.license, files);
    }
}
