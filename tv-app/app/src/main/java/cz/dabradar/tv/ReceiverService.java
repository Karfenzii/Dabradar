package cz.dabradar.tv;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

public class ReceiverService extends Service {
    private volatile boolean running = false;
    private ServerSocket server;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        NotificationManager nm = getSystemService(NotificationManager.class);
        nm.createNotificationChannel(new NotificationChannel("rcv", "Příjem z DABRADARU", NotificationManager.IMPORTANCE_LOW));
        Notification n = new Notification.Builder(this, "rcv")
                .setContentTitle("DABRADAR TV")
                .setContentText("Čekám na odkazy z webu")
                .setSmallIcon(android.R.drawable.ic_media_play)
                .build();
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(1, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
        } else {
            startForeground(1, n);
        }
        if (!running) {
            running = true;
            new Thread(this::loop, "dabradar-http").start();
        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        running = false;
        try { if (server != null) server.close(); } catch (Exception ignored) {}
        super.onDestroy();
    }

    private void loop() {
        try {
            server = new ServerSocket(Util.PORT);
            while (running) {
                Socket s = server.accept();
                try { handle(s); } catch (Exception ignored) {}
                try { s.close(); } catch (Exception ignored) {}
            }
        } catch (Exception e) {
            running = false;
        }
    }

    private void handle(Socket s) throws Exception {
        s.setSoTimeout(4000);
        BufferedReader r = new BufferedReader(new InputStreamReader(s.getInputStream(), StandardCharsets.UTF_8));
        String line = r.readLine();
        if (line == null) return;
        String h;
        while ((h = r.readLine()) != null && !h.isEmpty()) { /* přečíst hlavičky */ }
        String[] parts = line.split(" ");
        String status = "200 OK";
        String msg;
        String target = parts.length > 1 ? parts[1] : "";
        int q = target.indexOf('?');
        String path = q >= 0 ? target.substring(0, q) : target;
        String query = q >= 0 ? target.substring(q + 1) : "";
        if (!path.equals("/" + Util.code(this))) {
            status = "403 Forbidden";
            msg = "Špatný kód.";
        } else {
            String link = null;
            String video = null;
            for (String kv : query.split("&")) {
                if (kv.startsWith("u=")) link = URLDecoder.decode(kv.substring(2), "UTF-8");
                if (kv.startsWith("v=")) video = URLDecoder.decode(kv.substring(2), "UTF-8");
            }
            if (video != null) {
                if (!(video.startsWith("http://") || video.startsWith("https://"))) {
                    status = "400 Bad Request";
                    msg = "Neplatný odkaz.";
                } else if (openVideo(video)) {
                    msg = "Odesláno na TV.";
                } else {
                    status = "500 Error";
                    msg = "Na TV není přehrávač, který by video otevřel.";
                }
            } else if (link == null || !link.startsWith("stremio:///")) {
                msg = "Připojeno. Čekám na odkaz.";
            } else if (open(link)) {
                msg = "Odesláno na TV.";
            } else {
                status = "500 Error";
                msg = "Stremio se nepodařilo otevřít.";
            }
        }
        String html = "<!doctype html><meta charset=utf-8><meta name=viewport content='width=device-width'><body style='font:20px sans-serif;padding:24px'>"
                + msg + "<script>setTimeout(function(){window.close()},700)</script>";
        byte[] body = html.getBytes(StandardCharsets.UTF_8);
        OutputStream o = s.getOutputStream();
        o.write(("HTTP/1.1 " + status + "\r\nContent-Type: text/html; charset=utf-8\r\nContent-Length: " + body.length + "\r\nConnection: close\r\n\r\n").getBytes(StandardCharsets.UTF_8));
        o.write(body);
        o.flush();
        s.shutdownOutput();
    }

    private boolean openVideo(String url) {
        try {
            Intent i = new Intent(Intent.ACTION_VIEW);
            i.setDataAndType(Uri.parse(url), "video/*");
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean open(String link) {
        try {
            Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(link));
            i.setPackage("com.stremio.one");
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
            return true;
        } catch (Exception e1) {
            try {
                Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(link));
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(i);
                return true;
            } catch (Exception e2) {
                return false;
            }
        }
    }
}
