package com.example.tvremote;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.database.Cursor;
import android.provider.OpenableColumns;
import android.os.Handler;
import android.os.Looper;
import android.content.Context;

final class CastBridge {
    private final Activity act;
    private final WebView web;
    private final TvBridge parent;
    private final Handler ui = new Handler(Looper.getMainLooper());
    private final MediaHttpServer server;
    private CastClient client;
    private String host;

    CastBridge(Activity a, WebView w, TvBridge p) {
        act=a; web=w; parent=p; server=new MediaHttpServer(a);
    }

    void setHost(String h) { host=h; }

    @JavascriptInterface
    public void pick(String kind) {
        if (host == null || host.length()==0) {
            toast("TV se connect nahi hai");
            return;
        }
        Intent i = new Intent(act, CastPickerActivity.class);
        i.putExtra("kind", kind == null ? "all" : kind);
        act.startActivity(i);
    }

    static void picked(final Uri uri, final String kind) {
        if (TvBridge.getInstance() != null) {
            TvBridge.getInstance().castPicked(uri, kind);
        }
    }

    void handlePicked(Uri uri, String kind) {
        if (uri == null) return;
        if (host == null || host.length()==0) { toast("TV se connect nahi hai"); return; }
        try {
            act.getContentResolver().takePersistableUriPermission(uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } catch (Exception ignored) {}

        String mimeType = act.getContentResolver().getType(uri);
        if (mimeType == null) mimeType = "application/octet-stream";
        final String mime = mimeType;
        final String title = displayName(uri);

        final String url;
        try {
            url = server.publish(uri, mime, title);
        } catch (Exception e) {
            toast("Media publish fail ho gaya: " + e.getMessage());
            return;
        }

        final boolean photo = mime.startsWith("image/");
        if (client != null) client.close();

        client = new CastClient(host, new CastClient.Listener() {
            public void onStatus(final String state, final String title) {
                js("onCastStatus(" + q(state) + "," + q(title) + ")");
            }
            public void onError(final String message) {
                js("onCastError(" + q(message) + ")");
            }
            public void onMediaStatus(final String state, final long pos, final long dur) {
                js("onCastMediaStatus(" + q(state) + "," + pos + "," + dur + ")");
            }
        });
        toast("TV se Cast connect ho raha hai...");
        client.start();
        // Give the Cast receiver a moment to establish; CastClient also handles the launch.
        new Thread(new Runnable() {
            public void run() {
                try { Thread.sleep(1200); } catch(Exception ignored) {}
                if (client != null) client.load(url,mime,title,photo);
            }
        }, "cast-load").start();
    }

    void play() { if(client!=null) client.play(); }
    void pause() { if(client!=null) client.pause(); }
    void stop() {
        if(client!=null) { client.stop(); client.close(); client=null; }
        server.stop();
        js("onCastStatus(\"stopped\",\"\")");
    }
    void seek(long ms) { if(client!=null) client.seek(ms); }

    void dispose() {
        if(client!=null) { client.close(); client=null; }
        server.stop();
    }

    private void js(final String s) {
        ui.post(new Runnable() {
            public void run() {
                try{ web.evaluateJavascript("(function(){var t=window.__tv;if(t){t."+s+";}})()",null); }catch(Exception ignored){}
            }
        });
    }
    private static String q(String s) {
        return org.json.JSONObject.quote(s==null?"":s);
    }
    private String displayName(Uri u) {
        Cursor c=null;
        try {
            c=act.getContentResolver().query(u,new String[]{OpenableColumns.DISPLAY_NAME},null,null,null);
            if(c!=null && c.moveToFirst()) return c.getString(0);
        } catch(Exception ignored){} finally{if(c!=null)c.close();}
        return "Cast media";
    }
    private void toast(final String m) {
        ui.post(new Runnable() {
            public void run() { android.widget.Toast.makeText(act,m,android.widget.Toast.LENGTH_SHORT).show(); }
        });
    }
}
