# Glaceon Android App - セットアップガイド

## 🚀 クイックスタート

### 1. リポジトリのクローン
```bash
git clone https://github.com/your-username/glaceon-android.git
cd glaceon-android
```

### 2. 設定ファイルの準備
```bash
# local.propertiesファイルを作成
cp local.properties.example local.properties

# Android SDKパスを設定
echo "sdk.dir=/path/to/your/Android/Sdk" >> local.properties

# APIエンドポイントを設定
echo "API_BASE_URL_DEBUG=http://10.0.2.2:3000/" >> local.properties
echo "API_BASE_URL_RELEASE=https://your-api-url.amazonaws.com/dev/" >> local.properties
```

### 3. Android Studioでプロジェクトを開く
1. Android Studio を起動
2. "Open an existing project" を選択
3. クローンしたフォルダを選択
4. Gradle sync を実行

### 4. ビルドとテスト
```bash
# デバッグビルド
./gradlew assembleDebug

# テスト実行
./gradlew test
```

## 🔧 開発環境の設定

### 必要なツール
- Android Studio Hedgehog | 2023.1.1+
- JDK 11+
- Android SDK API 33+

### 推奨設定
- Kotlin plugin 最新版
- Android Gradle Plugin 8.0+
- Compose Compiler 最新版

## 🔐 セキュリティ設定

### 秘密情報の管理
```bash
# 署名用キーストアの生成
keytool -genkey -v -keystore glaceon-release-key.keystore -alias glaceon -keyalg RSA -keysize 2048 -validity 10000

# local.propertiesに署名情報を追加
echo "RELEASE_STORE_FILE=glaceon-release-key.keystore" >> local.properties
echo "RELEASE_STORE_PASSWORD=your_keystore_password" >> local.properties
echo "RELEASE_KEY_ALIAS=glaceon" >> local.properties
echo "RELEASE_KEY_PASSWORD=your_key_password" >> local.properties
```

### ⚠️ 重要な注意事項
- `local.properties` は絶対にコミットしない
- キーストアファイルは安全な場所に保管
- パスワードは環境変数や秘密管理ツールを使用

## 🌐 API設定

### バックエンドAPI
- **開発環境**: `http://10.0.2.2:3000/` (Android Emulator用)
- **本番環境**: AWS API Gateway URL

### 認証
- 現在はモック認証を使用
- 将来的にAWS Cognitoに移行予定

## 📱 テスト

### エミュレータ設定
- API Level 33以上
- Google Play Services有効
- インターネット接続確認

### 実機テスト
- 開発者オプション有効
- USBデバッグ有効
- 適切な権限設定

## 🚀 デプロイ

### Google Play Store
1. リリースビルド作成
2. Play Console設定
3. 段階的リリース

詳細は [play-store-listing.md](play-store-listing.md) を参照

## 🤝 コントリビューション

### ブランチ戦略
- `main`: 本番リリース用
- `develop`: 開発用
- `feature/*`: 機能開発用

### コミットメッセージ
```
feat: 新機能追加
fix: バグ修正
docs: ドキュメント更新
style: コードスタイル修正
refactor: リファクタリング
test: テスト追加・修正
```

## 📞 サポート

- Issues: GitHub Issues
- Email: support@your-domain.com
- Wiki: プロジェクトWiki