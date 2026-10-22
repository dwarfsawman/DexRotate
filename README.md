# DexRotate

Shizuku を使って、矢印アイコン 4 つで Samsung DeX の画面を 4 方向 (0° / 90° / 180° / 270°) に回転させる Android アプリ。

## 使い方

1. [Shizuku](https://shizuku.rikka.app/) を入れて起動する。
2. 「DeX回転」を開き、Shizuku の権限を許可する。
3. アプリ一覧に増えた「回転 0° / 90° / 180° / 270°」のアイコンを DeX のデスクトップに置いてタップする。
   モニターの通常の横向きが 0° (↓)、90° は ←、180° は ↑、270° は →。

インストール直後に「Shizuku が起動していません」と出たら、アプリを一度完全終了して起動し直す。

## 仕組み

DeX 画面の向きは `Settings.System.USER_ROTATION` では変えられないため、Shizuku (shell 権限) で
`cmd window user-rotation -d <displayId> lock <0..3>` を実行している。対象ディスプレイはアイコンをタップした画面を優先し、DeX の ID が変わっても追従する。

## ビルド

```bash
./gradlew assembleDebug
```
