package jp.ginji.dexrotate;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;

/**
 * ランチャーの矢印アイコン (activity-alias) やピン留めショートカットから呼ばれる入口。
 * Theme.NoDisplay なので画面は一切出さず、RotateService に処理を渡して即 finish() する。
 */
public class RotateActivity extends Activity {

    public static final String META_ROTATION = "jp.ginji.dexrotate.ROTATION";
    public static final String EXTRA_ROTATION = "rotation";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        int rotation = resolveRotation();
        if (rotation < 0 || rotation > 3) {
            Intent main = new Intent(this, MainActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(main);
        } else {
            Intent work = new Intent(this, RotateService.class)
                    .putExtra(RotateService.EXTRA_ROTATION, rotation)
                    .putExtra(RotateService.EXTRA_DISPLAY_ID, Rotator.findTargetDisplayId(this));
            startService(work);
        }
        finish();
    }

    private int resolveRotation() {
        Intent intent = getIntent();
        if (intent != null && intent.hasExtra(EXTRA_ROTATION)) {
            return intent.getIntExtra(EXTRA_ROTATION, -1);
        }
        try {
            ActivityInfo info = getPackageManager()
                    .getActivityInfo(getComponentName(), PackageManager.GET_META_DATA);
            if (info.metaData != null) {
                Object v = info.metaData.get(META_ROTATION);
                if (v != null) {
                    return Integer.parseInt(String.valueOf(v).trim());
                }
            }
        } catch (PackageManager.NameNotFoundException | NumberFormatException ignored) {
        }
        return -1;
    }
}
