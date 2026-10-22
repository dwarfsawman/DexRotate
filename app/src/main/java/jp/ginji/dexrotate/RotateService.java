package jp.ginji.dexrotate;

import android.app.Service;
import android.content.Intent;
import android.hardware.display.DisplayManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.view.Display;
import android.widget.Toast;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import rikka.shizuku.Shizuku;

/**
 * 矢印アイコンから起動され、画面を出さずに回転を行う。
 * Activity はすぐ finish() するので、処理中もプロセスが生き残るように Service で実行する。
 */
public class RotateService extends Service {

    public static final String EXTRA_ROTATION = "rotation";
    public static final String EXTRA_DISPLAY_ID = "display_id";

    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) {
            stopSelf(startId);
            return START_NOT_STICKY;
        }
        final int rotation = intent.getIntExtra(EXTRA_ROTATION, -1);
        final int displayId = intent.getIntExtra(EXTRA_DISPLAY_ID, Display.DEFAULT_DISPLAY);
        new Thread(() -> {
            try {
                run(rotation, displayId);
            } finally {
                stopSelf(startId);
            }
        }).start();
        return START_NOT_STICKY;
    }

    private void run(int rotation, int displayId) {
        if (rotation < 0 || rotation > 3) {
            return;
        }
        // 現在と同じ向きなら何もしない
        DisplayManager dm = getSystemService(DisplayManager.class);
        Display d = dm == null ? null : dm.getDisplay(displayId);
        if (d != null && d.getRotation() == rotation) {
            return;
        }
        if (!waitForShizuku(2000)) {
            toast(getString(Shizuku.pingBinder()
                    ? R.string.status_no_permission : R.string.status_not_running));
            Intent main = new Intent(this, MainActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(main);
            return;
        }
        try {
            Rotator.setRotation(displayId, rotation);
        } catch (Exception e) {
            toast(getString(R.string.error_rotate, e.getMessage()));
        }
    }

    /** プロセス起動直後は Shizuku のバインダーがまだ届いていないことがあるので少し待つ。 */
    private boolean waitForShizuku(long timeoutMs) {
        if (Rotator.isShizukuReady()) {
            return true;
        }
        if (!Shizuku.pingBinder()) {
            CountDownLatch latch = new CountDownLatch(1);
            Shizuku.OnBinderReceivedListener listener = latch::countDown;
            Shizuku.addBinderReceivedListenerSticky(listener);
            try {
                latch.await(timeoutMs, TimeUnit.MILLISECONDS);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            } finally {
                Shizuku.removeBinderReceivedListener(listener);
            }
        }
        return Rotator.isShizukuReady();
    }

    private void toast(String text) {
        mainHandler.post(() -> Toast.makeText(getApplicationContext(), text, Toast.LENGTH_LONG).show());
    }
}
