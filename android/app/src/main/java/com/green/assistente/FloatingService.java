package com.green.assistente;

import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.IBinder;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;

public class FloatingService extends Service {
    private WindowManager manager;
    private View bubble;
    private View panel;
    private WindowManager.LayoutParams bubbleParams;
    private WindowManager.LayoutParams panelParams;

    @Override
    public void onCreate() {
        super.onCreate();
        manager = (WindowManager) getSystemService(WINDOW_SERVICE);
        showBubble();
    }

    private int overlayType() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                : WindowManager.LayoutParams.TYPE_PHONE;
    }

    private void showBubble() {
        if (bubble != null) return;

        TextView button = new TextView(this);
        button.setText("G");
        button.setTextColor(0xFFFFFFFF);
        button.setTextSize(22);
        button.setGravity(Gravity.CENTER);
        button.setBackgroundColor(0xFF168A50);
        bubble = button;

        bubbleParams = new WindowManager.LayoutParams(
                58, 58, overlayType(),
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);
        bubbleParams.gravity = Gravity.TOP | Gravity.START;
        bubbleParams.x = 24;
        bubbleParams.y = 220;

        button.setOnTouchListener(new View.OnTouchListener() {
            private int startX, startY;
            private float downX, downY;
            private boolean moved;

            @Override
            public boolean onTouch(View view, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        startX = bubbleParams.x;
                        startY = bubbleParams.y;
                        downX = event.getRawX();
                        downY = event.getRawY();
                        moved = false;
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        int dx = (int) (event.getRawX() - downX);
                        int dy = (int) (event.getRawY() - downY);
                        if (Math.abs(dx) > 6 || Math.abs(dy) > 6) moved = true;
                        bubbleParams.x = startX + dx;
                        bubbleParams.y = startY + dy;
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
            manager.removeView(panel);
            panel = null;
            return;
        }

        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(28, 24, 28, 24);
        box.setBackgroundColor(0xFF202A24);

        TextView heading = new TextView(this);
        heading.setText("Assistente Green");
        heading.setTextColor(0xFFFFFFFF);
        heading.setTextSize(20);
        box.addView(heading);

        TextView message = new TextView(this);
        message.setText("Olá! A bolha está funcionando. A conversa com IA será conectada em uma próxima etapa.");
        message.setTextColor(0xFFE7F5EA);
        message.setTextSize(15);
        message.setPadding(0, 18, 0, 18);
        box.addView(message);

        TextView close = new TextView(this);
        close.setText("Fechar painel");
        close.setTextColor(0xFF8BE0A8);
        close.setTextSize(16);
        box.addView(close);
        close.setOnClickListener(v -> togglePanel());

        panel = box;
        panelParams = new WindowManager.LayoutParams(
                280, WindowManager.LayoutParams.WRAP_CONTENT, overlayType(),
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);
        panelParams.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
        panelParams.y = 100;
        manager.addView(panel, panelParams);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        if (panel != null) manager.removeView(panel);
        if (bubble != null) manager.removeView(bubble);
        panel = null;
        bubble = null;
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
