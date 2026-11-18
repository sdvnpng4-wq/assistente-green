package com.green.assistente;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MainActivity extends Activity {
    private static final int OVERLAY_REQUEST = 1001;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(32, 48, 32, 32);

        TextView title = new TextView(this);
        title.setText("Assistente Green");
        title.setTextSize(28);
        root.addView(title);

        TextView info = new TextView(this);
        info.setText("Ative a permissão de sobreposição para usar o botão flutuante. Depois, você poderá abrir ou ocultar o assistente na tela.");
        info.setTextSize(16);
        info.setPadding(0, 24, 0, 24);
        root.addView(info);

        Button start = new Button(this);
        start.setText("Ativar assistente flutuante");
        root.addView(start);
        start.setOnClickListener(v -> enableAssistant());

        setContentView(root);
    }

    private void enableAssistant() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            Intent permission = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            startActivityForResult(permission, OVERLAY_REQUEST);
            return;
        }
        startFloatingService();
    }

    private void startFloatingService() {
        Intent service = new Intent(this, FloatingService.class);
        startService(service);
        finish();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == OVERLAY_REQUEST && (Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(this))) {
            startFloatingService();
        }
    }
}
