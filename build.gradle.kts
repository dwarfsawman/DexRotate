plugins {
    id("com.android.application") version "9.2.1" apply false
}

// Windows の MAX_PATH 対策: gradle.properties の dexrotate.shortBuildDir が設定されていれば
// ビルド出力をそこへ逃がす。短いパスに置いたプロジェクトではこの行を消してよい。
val shortBuildRoot = providers.gradleProperty("dexrotate.shortBuildDir").orNull
if (shortBuildRoot != null) {
    allprojects {
        layout.buildDirectory.set(File(shortBuildRoot, project.name))
    }
}
