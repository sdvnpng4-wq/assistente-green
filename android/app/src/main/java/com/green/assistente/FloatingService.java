package com.green.assistente;

import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.provider.Settings;
import android.speech.tts.TextToSpeech;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.Locale;

public class FloatingService extends Service implements TextToSpeech.OnInitListener {
    public static final String EXTRA_SHARED_TEXT = "shared_text";
    private WindowManager manager;
    private View bubble;
    private View panel;
    private WindowManager.LayoutParams bubbleParams;
    private TextToSpeech tts;
    private boolean ttsReady = false;
    private String lastMessage = "Oi! Eu sou o Green. Pode compartilhar uma mensagem comigo para eu ler em voz alta.";

    @Override
    public void onCreate() {
        super.onCreate();
        manager = (WindowManager) getSystemService(WINDOW_SERVICE);
        tts = new TextToSpeech(this, this);
        showBubble();
    }

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            tts.setLanguage(new Locale("pt", "BR"));
            ttsReady = true;
            speak("Oi! Eu sou o Green, seu robô assistente.");
        }
    }

    private void speak(String text) {
        lastMessage = text;
        if (ttsReady) tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "green_speech");
    }

    private int overlayType() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                : WindowManager.LayoutParams.TYPE_PHONE;
    }

    private void showBubble() {
        if (bubble != null) return;
        TextView avatar = new TextView(this);
        avatar.setText("🤖");
        avatar.setTextSize(28);
        avatar.setGravity(Gravity.CENTER);
        avatar.setBackgroundColor(0xFF168A50);
        bubble = avatar;

        bubbleParams = new WindowManager.LayoutParams(64, 64, overlayType(),
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, PixelFormat.TRANSLUCENT);
        bubbleParams.gravity = Gravity.TOP | Gravity.START;
        bubbleParams.x = 20;
        bubbleParams.y = 220;

        avatar.setOnTouchListener(new View.OnTouchListener() {
            private int startX, startY;
            private float downX, downY;
            private boolean moved;
            @Override public boolean onTouch(View view, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        startX = bubbleParams.x; startY = bubbleParams.y;
                        downX = event.getRawX(); downY = event.getRawY(); moved = false;
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        int dx = (int)(event.getRawX() - downX);
                        int dy = (int)(event.getRawY() - downY);
                        if (Math.abs(dx) > 6 || Math.abs(dy) > 6) moved = true;
                        bubbleParams.x = startX + dx; bubbleParams.y = startY + dy;
                        manager.updateViewLayout(bubble, bubbleParams);
                        return true;
                    case MotionEvent.ACTION_UP:
                        if (!moved) togglePanel();
                        return true;
                }
                return false;
            }
        });
        manager.addView(bubble, bubbleParams);
    }

    private void togglePanel() {
        if (panel != null) {
            manager.removeView(panel); panel = null; return;
        }
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(28, 24, 28, 24);
        box.setBackgroundColor(0xFF202A24);

        TextView heading = new TextView(this);
        heading.setText("🤖 Green — seu robô");
        heading.setTextColor(0xFFFFFFFF);
        heading.setTextSize(20);
        box.addView(heading);

        TextView info = new TextView(this);
        info.setText("Posso falar mensagens que você compartilhar comigo. Toque em um botão abaixo.");
        info.setTextColor(0xFFE7F5EA);
        info.setTextSize(14);
        info.setPadding(0, 14, 0, 18);
        box.addView(info);

        addAction(box, "🔊 Falar novamente", () -> speak(lastMessage));
        addAction(box, "⚙ Abrir configurações", () -> openSettings());
        addAction(box, "Ocultar robô", () -> {
            if (panel != null) { manager.removeView(panel); panel = null; }
            if (bubble != null) { manager.removeView(bubble); bubble = null; }
            stopSelf();
        });

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                300, WindowManager.LayoutParams.WRAP_CONTENT, overlayType(),
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
        params.y = 100;
        panel = box;
        manager.addView(panel, params);
    }

    private void addAction(LinearLayout box, String label, Runnable action) {
        TextView item = new TextView(this);
        item.setText(label);
        item.setTextColor(0xFF8BE0A8);
        item.setTextSize(16);
        item.setPadding(0, 12, 0, 12);
        box.addView(item);
        item.setOnClickListener(v -> action.run());
    }

    private void openSettings() {
        try {
            Intent i = new Intent(Settings.ACTION_SETTINGS);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
            speak("Abrindo as configurações.");
        } catch (Exception e) {
            speak("Não consegui abrir as configurações.");
        }
    }

    private void handleSharedText(String text) {
        String command = text.trim().toLowerCase(new Locale("pt", "BR"));
        if (command.contains("abrir configurações") || command.contains("abre configurações")
                || command.equals("configurações")) {
            openSettings();
        } else if (command.contains("abrir navegador") || command.contains("abre o navegador")) {
            try {
                Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com"));
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(i);
                speak("Abrindo o navegador.");
            } catch (Exception e) { speak("Não consegui abrir o navegador."); }
        } else {
            speak("Recebi sua mensagem. Vou ler para você: " + text);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.hasExtra(EXTRA_SHARED_TEXT)) {
            String text = intent.getStringExtra(EXTRA_SHARED_TEXT);
            if (text != null && !text.trim().isEmpty()) handleSharedText(text);
        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        if (panel != null) manager.removeView(panel);
        if (bubble != null) manager.removeView(bubble);
        if (tts != null) { tts.stop(); tts.shutdown(); }
        panel = null; bubble = null;
        super.onDestroy();
    }

    @Override public IBinder onBind(Intent intent) { return null; }
}
