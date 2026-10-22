package jp.ginji.dexrotate;

import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.display.DisplayManager;
import android.view.Display;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Method;

import rikka.shizuku.Shizuku;

/**
 * Shizuku 経由で `cmd window user-rotation -d <displayId> lock <rotation>` を実行し、
 * DeX (外部ディスプレイ) の向きを固定する。
 *
 * Settings.System.USER_ROTATION はデフォルトディスプレイ専用で、DeX 画面には効かない。
 * DeX 設定の「ディスプレイの回転」も内部的には IWindowManager.freezeDisplayRotation を
 * 直接呼んでいるだけなので、shell 権限 (= Shizuku) で同じコマンドを叩くのが唯一の経路。
 */
public final class Rotator {

    public static final int ROTATION_0 = 0;
    public static final int ROTATION_90 = 1;
    public static final int ROTATION_180 = 2;
    public static final int ROTATION_270 = 3;

    private Rotator() {
    }

    /** 回転対象のディスプレイ ID を決める。 */
    public static int findTargetDisplayId(Context context) {
        // 1. 自分が DeX 画面上で動いているなら、その画面を回す
        try {
            Display own = context.getDisplay();
            if (own != null && own.getDisplayId() != Display.DEFAULT_DISPLAY) {
                return own.getDisplayId();
            }
        } catch (RuntimeException ignored) {
            // UI コンテキストでない場合など
        }
        // 2. 本体画面から呼ばれた場合は、外部 (プレゼンテーション) ディスプレイを探す
        DisplayManager dm = context.getSystemService(DisplayManager.class);
        if (dm != null) {
            for (Display d : dm.getDisplays(DisplayManager.DISPLAY_CATEGORY_PRESENTATION)) {
                if ((d.getFlags() & Display.FLAG_PRESENTATION) != 0) {
                    return d.getDisplayId();
                }
            }
        }
        // 3. 何もなければ本体画面
        return Display.DEFAULT_DISPLAY;
    }

    public static String displayName(Context context, int displayId) {
        DisplayManager dm = context.getSystemService(DisplayManager.class);
        Display d = dm == null ? null : dm.getDisplay(displayId);
        if (d == null) {
            return "display " + displayId;
        }
        return d.getName() + " (id " + displayId + ")";
    }

    public static boolean isShizukuReady() {
        try {
            return Shizuku.pingBinder()
                    && !Shizuku.isPreV11()
                    && Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED;
        } catch (RuntimeException e) {
            return false;
        }
    }

    public static void setRotation(int displayId, int rotation) throws Exception {
        runShell("cmd", "window", "user-rotation",
                "-d", String.valueOf(displayId), "lock", String.valueOf(rotation));
    }

    /** 現在の回転 (0..3)。取得できなければ -1。 */
    public static int getRotation(int displayId) {
        try {
            String out = runShell("cmd", "window", "user-rotation", "-d", String.valueOf(displayId)).trim();
            // 出力例: "lock 1" / "free"
            String[] parts = out.split("\\s+");
            if (parts.length >= 2) {
                return Integer.parseInt(parts[parts.length - 1]);
            }
        } catch (Exception ignored) {
        }
        return -1;
    }

    /** Shizuku の newProcess (非公開 API) で shell 権限のプロセスを起動する。 */
    public static String runShell(String... cmd) throws Exception {
        Method newProcess = Shizuku.class.getDeclaredMethod(
                "newProcess", String[].class, String[].class, String.class);
        newProcess.setAccessible(true);
        Process process = (Process) newProcess.invoke(null, (Object) cmd, null, null);
        if (process == null) {
            throw new IOException("Shizuku.newProcess returned null");
        }
        String stdout = readAll(process.getInputStream());
        String stderr = readAll(process.getErrorStream());
        int code = process.waitFor();
        if (code != 0) {
            throw new IOException("exit " + code + ": " + (stderr.isEmpty() ? stdout : stderr));
        }
        return stdout;
    }

    private static String readAll(InputStream in) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader r = new BufferedReader(new InputStreamReader(in))) {
            String line;
            while ((line = r.readLine()) != null) {
                sb.append(line).append('\n');
            }
        }
        return sb.toString();
    }
}
