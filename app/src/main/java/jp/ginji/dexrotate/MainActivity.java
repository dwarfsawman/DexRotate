package jp.ginji.dexrotate;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.hardware.display.DisplayManager;
import android.os.Bundle;
import android.view.Display;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import rikka.shizuku.Shizuku;

public class MainActivity extends Activity {

    private static final int REQUEST_SHIZUKU = 1;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private TextView status;
    private TextView target;
    private Button requestButton;

    private final Shizuku.OnRequestPermissionResultListener permissionListener =
            (requestCode, grantResult) -> runOnUiThread(this::refreshStatus);
    private final Shizuku.OnBinderReceivedListener binderReceivedListener =
            () -> runOnUiThread(this::refreshStatus);
    private final Shizuku.OnBinderDeadListener binderDeadListener =
            () -> runOnUiThread(this::refreshStatus);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        status = findViewById(R.id.status);
        target = findViewById(R.id.target);
        requestButton = findViewById(R.id.request_permission);
        requestButton.setOnClickListener(v -> requestShizuku());

        bind(R.id.rotate_0, Rotator.ROTATION_0);
        bind(R.id.rotate_90, Rotator.ROTATION_90);
        bind(R.id.rotate_180, Rotator.ROTATION_180);
        bind(R.id.rotate_270, Rotator.ROTATION_270);

        Shizuku.addRequestPermissionResultListener(permissionListener);
        Shizuku.addBinderReceivedListenerSticky(binderReceivedListener);
        Shizuku.addBinderDeadListener(binderDeadListener);
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshStatus();
    }

    @Override
    protected void onDestroy() {
        Shizuku.removeRequestPermissionResultListener(permissionListener);
        Shizuku.removeBinderReceivedListener(binderReceivedListener);
        Shizuku.removeBinderDeadListener(binderDeadListener);
        executor.shutdownNow();
        super.onDestroy();
    }

    private void bind(int viewId, int rotation) {
        View v = findViewById(viewId);
        v.setOnClickListener(x -> rotate(rotation));
    }

    private void rotate(int rotation) {
        if (!Rotator.isShizukuReady()) {
            refreshStatus();
            requestShizuku();
            return;
        }
        int displayId = Rotator.findTargetDisplayId(this);
        executor.execute(() -> {
            try {
                Rotator.setRotation(displayId, rotation);
                runOnUiThread(this::refreshStatus);
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this,
                        getString(R.string.error_rotate, e.getMessage()), Toast.LENGTH_LONG).show());
            }
        });
    }

    private void requestShizuku() {
        if (!Shizuku.pingBinder()) {
            Toast.makeText(this, R.string.shizuku_not_running, Toast.LENGTH_LONG).show();
            return;
        }
        if (Shizuku.isPreV11()) {
            Toast.makeText(this, R.string.shizuku_too_old, Toast.LENGTH_LONG).show();
            return;
        }
        if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
            refreshStatus();
            return;
        }
        if (Shizuku.shouldShowRequestPermissionRationale()) {
            Toast.makeText(this, R.string.shizuku_denied_before, Toast.LENGTH_LONG).show();
        }
        Shizuku.requestPermission(REQUEST_SHIZUKU);
    }

    /** 対象ディスプレイの現在の回転 (0..3)。取れなければ -1。Shizuku 不要の公開 API。 */
    private int currentRotation(int displayId) {
        DisplayManager dm = getSystemService(DisplayManager.class);
        Display d = dm == null ? null : dm.getDisplay(displayId);
        return d == null ? -1 : d.getRotation();
    }

    private void refreshStatus() {
        boolean running = Shizuku.pingBinder();
        boolean granted = running && !Shizuku.isPreV11()
                && Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED;
        if (!running) {
            status.setText(R.string.status_not_running);
        } else if (!granted) {
            status.setText(R.string.status_no_permission);
        } else {
            status.setText(R.string.status_ready);
        }
        requestButton.setVisibility(granted ? View.GONE : View.VISIBLE);

        int displayId = Rotator.findTargetDisplayId(this);
        int current = currentRotation(displayId);
        String name = Rotator.displayName(this, displayId);
        target.setText(getString(R.string.target_display_rotation,
                name, current < 0 ? "?" : String.valueOf(current * 90)));
    }
}
