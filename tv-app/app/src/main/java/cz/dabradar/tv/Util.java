package cz.dabradar.tv;

import android.content.Context;
import android.content.SharedPreferences;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Collections;
import java.util.Random;

final class Util {
    static final int PORT = 8765;

    private Util() {}

    static String code(Context c) {
        SharedPreferences p = c.getSharedPreferences("dabradar", Context.MODE_PRIVATE);
        String code = p.getString("code", null);
        if (code == null) {
            code = String.valueOf(100000 + new Random().nextInt(900000));
            p.edit().putString("code", code).apply();
        }
        return code;
    }

    static String localIp() {
        try {
            for (NetworkInterface ni : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                if (!ni.isUp() || ni.isLoopback()) continue;
                for (InetAddress a : Collections.list(ni.getInetAddresses())) {
                    if (a instanceof Inet4Address && a.isSiteLocalAddress()) {
                        return a.getHostAddress();
                    }
                }
            }
        } catch (Exception ignored) {}
        return "?";
    }
}
