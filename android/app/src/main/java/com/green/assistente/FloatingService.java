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
import android.view.inputmethod.InputMethodManager;
import android.content.Context;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
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
    private TextView conversation;
    private EditText commandInput;
    private String lastMessage = "Oi! Eu sou o Green. Escreva um comando ou compartilhe uma mensagem comigo.";

    @Override public void onCreate() {
        super.onCreate();
        manager = (WindowManager) getSystemService(WINDOW_SERVICE);
        tts = new TextToSpeech(this, this);
        showBubble();
    }

    @Override public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            int language = tts.setLanguage(new Locale("pt", "BR"));
            ttsReady = language != TextToSpeech.LANG_MISSING_DATA
                    && language != TextToSpeech.LANG_NOT_SUPPORTED;
            speak("Oi! Eu sou o Green, seu robô assistente.");
        }
    }

    private void speak(String text) {
        lastMessage = text;
        if (conversation != null) conversation.setText(text);
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

    private TextView text(String value, int size) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextColor(0xFFFFFFFF);
        view.setTextSize(size);
        return view;
    }

    private void togglePanel() {
        if (panel != null) {
            manager.removeView(panel);
            panel = null;
            return;
        }

        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(22, 20, 22, 18);
        box.setBackgroundColor(0xFF17251D);

        TextView heading = text("🤖 Green", 22);
        box.addView(heading);
        conversation = text(lastMessage, 15);
        conversation.setPadding(0, 14, 0, 14);
        box.addView(conversation);

        commandInput = new EditText(this);
        commandInput.setSingleLine(false);
        commandInput.setMinLines(1);
        commandInput.setMaxLines(3);
        commandInput.setTextSize(16);
        commandInput.setHint("Digite o que quer que eu faça...");
        commandInput.setHintTextColor(0xFFB7C9BC);
        commandInput.setTextColor(0xFFFFFFFF);
        commandInput.setPadding(12, 8, 12, 8);
        commandInput.setBackgroundColor(0xFF304438);
        box.addView(commandInput);

        Button send = new Button(this);
        send.setText("Enviar comando");
        box.addView(send);
        send.setOnClickListener(v -> {
            String value = commandInput.getText().toString().trim();
            if (!value.isEmpty()) {
                ((InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE))
                        .hideSoftInputFromWindow(commandInput.getWindowToken(), 0);
                handleCommand(value);
                commandInput.setText("");
            }
        });

        Button repeat = new Button(this);
        repeat.setText("🔊 Repetir fala");
        box.addView(repeat);
        repeat.setOnClickListener(v -> speak(lastMessage));

        Button hide = new Button(this);
        hide.setText("Ocultar robô");
        box.addView(hide);
        hide.setOnClickListener(v -> {
            if (panel != null) { manager.removeView(panel); panel = null; }
            if (bubble != null) { manager.removeView(bubble); bubble = null; }
            stopSelf();
        });

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                310, WindowManager.LayoutParams.WRAP_CONTENT, overlayType(),
                WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE,
                PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
        params.y = 80;
        panel = box;
        manager.addView(panel, params);
        commandInput.requestFocus();
    }

    private void handleCommand(String original) {
        String command = original.trim().toLowerCase(new Locale("pt", "BR"));
        if (command.equals("abrir configurações") || command.equals("abre configurações")
                || command.equals("configurações") || command.equals("abrir ajustes")) {
            openSettings();
        } else if (command.contains("abrir navegador") || command.contains("abre navegador")
                || command.contains("abrir google") || command.contains("abre o google")) {
            openUrl("https://www.google.com", "Abrindo o Google.");
        } else if (command.startsWith("abrir site ")) {
            String address = original.substring("abrir site ".length()).trim();
            if (!address.startsWith("http://") && !address.startsWith("https://")) address = "https://" + address;
            openUrl(address, "Abrindo o site.");
        } else if (command.startsWith("fale ") || command.startsWith("fala ")) {
            speak(original.substring(original.indexOf(' ') + 1));
        } else if (command.contains("ocultar robô") || command.contains("esconder robô")) {
            speak("Vou me esconder. Até já.");
            if (panel != null) { manager.removeView(panel); panel = null; }
            if (bubble != null) { manager.removeView(bubble); bubble = null; }
            stopSelf();
        } else {
            speak("Recebi seu pedido: " + original + ". Ainda não tenho conexão com uma inteligência artificial para entender e executar qualquer tarefa. Posso abrir configurações, abrir o Google ou falar um texto.");
        }
    }

    private void openSettings() {
        try {
            Intent i = new Intent(Settings.ACTION_SETTINGS);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
            speak("Abrindo as configurações do celular.");
        } catch (Exception e) { speak("Não consegui abrir as configurações."); }
    }

    private void openUrl(String url, String response) {
        try {
            Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
            speak(response);
        } catch (Exception e) { speak("Não consegui abrir esse endereço."); }
    }

    private void handleSharedText(String text) {
        speak("Mensagem recebida: " + text);
        String c = text.toLowerCase(new Locale("pt", "BR"));
        if (c.contains("abrir configurações") || c.contains("abre configurações")) {
            openSettings();
        } else if (c.contains("abrir navegador") || c.contains("abre o google")) {
            openUrl("https://www.google.com", "Abrindo o Google.");
        }
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.hasExtra(EXTRA_SHARED_TEXT)) {
            String shared = intent.getStringExtra(EXTRA_SHARED_TEXT);
            if (shared != null && !shared.trim().isEmpty()) handleSharedText(shared);
        }
        return START_STICKY;
    }

    @Override public void onDestroy() {
        if (panel != null) manager.removeView(panel);
        if (bubble != null) manager.removeView(bubble);
        if (tts != null) { tts.stop(); tts.shutdown(); }
        panel = null; bubble = null; conversation = null; commandInput = null;
        super.onDestroy();
    }

    @Override public IBinder onBind(Intent intent) { return null; }
}
