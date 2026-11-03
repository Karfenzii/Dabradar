package cz.dabradar.tv;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MainActivity extends Activity {
    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        if (Build.VERSION.SDK_INT >= 33) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1);
        }
        try { startForegroundService(new Intent(this, ReceiverService.class)); } catch (Exception ignored) {}

        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        l.setGravity(Gravity.CENTER_VERTICAL);
        l.setPadding(80, 40, 80, 40);

        String addr = Util.localIp() + ":" + Util.PORT + "/" + Util.code(this);
        TextView t = new TextView(this);
        t.setTextSize(24);
        t.setText("DABRADAR TV je spuštěné.\n\nTento řádek vlož na webu DABRADAR do Nastavení, Televize:\n\n" + addr
                + "\n\nPo vypnutí a zapnutí TV se aplikace spustí sama. Pokud se Stremio po odeslání neotevře, klepni níže na povolení zobrazení přes jiné aplikace.");
        l.addView(t);

        Button o = new Button(this);
        o.setText("Povolit zobrazení přes jiné aplikace");
        o.setOnClickListener(v -> {
            try {
                startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName())));
            } catch (Exception e) {
                ((TextView) t).append("\n\nTuhle obrazovku televize nemá. Povolení nastav příkazem v Termuxu:\nadb shell appops set cz.dabradar.tv SYSTEM_ALERT_WINDOW allow");
            }
        });
        l.addView(o);
        setContentView(l);
        o.requestFocus();
    }
}
