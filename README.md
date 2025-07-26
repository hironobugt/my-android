# Glaceon Android App

AWS Glacierを使用したファイルアーカイブAndroidアプリ

## 🚀 機能

- 📁 ファイルの自動アップロード
- 🔄 ファイルの復元機能  
- 💰 使用量とコストの追跡
- 🔔 リアルタイム通知
- 🌍 多言語対応（日本語、英語、中国語、韓国語）
- 💳 Stripe決済統合

## 🛠️ 技術スタック

- **言語**: Kotlin
- **UI**: Jetpack Compose + Material Design 3
- **アーキテクチャ**: MVVM + Repository Pattern
- **ネットワーク**: Retrofit + OkHttp
- **認証**: AWS Cognito (Mock実装)
- **決済**: Stripe
- **画像処理**: Coil
- **非同期処理**: Coroutines + Flow

## 📋 セットアップ

### 1. 前提条件

- Android Studio Hedgehog | 2023.1.1 以上
- JDK 11 以上
- Android SDK API 33 以上

### 2. プロジェクトのクローン

```bash
git clone https://github.com/your-username/glaceon-android.git
cd glaceon-android
```

### 3. 設定ファイルの準備

```bash
# local.propertiesファイルを作成
cp local.properties.example local.properties

# 必要な値を設定
# - sdk.dir: Android SDKのパス
# - API_BASE_URL_DEBUG: 開発用APIエンドポイント
# - API_BASE_URL_RELEASE: 本番用APIエンドポイント
```

### 4. 署名設定（リリースビルド用）

```bash
# キーストアファイルを生成
keytool -genkey -v -keystore glaceon-release-key.keystore -alias glaceon -keyalg RSA -keysize 2048 -validity 10000

# local.propertiesに署名情報を設定
RELEASE_STORE_PASSWORD=your_actual_keystore_password
RELEASE_KEY_PASSWORD=your_actual_key_password
```

## 🏗️ ビルド

### デバッグビルド
```bash
./gradlew assembleDebug
```

### リリースビルド
```bash
./gradlew assembleRelease
```

### Android App Bundle (推奨)
```bash
./gradlew bundleRelease
```

## 🧪 テスト

```bash
# ユニットテスト
./gradlew test

# インストゥルメンテーションテスト
./gradlew connectedAndroidTest
```

## 📱 アプリ構成

### パッケージ構造
```
com.example.glaceon/
├── config/          # 設定管理
├── data/
│   ├── api/         # API通信
│   ├── model/       # データモデル
│   ├── repository/  # リポジトリ
│   └── preferences/ # ローカル設定
├── service/         # バックグラウンドサービス
├── ui/
│   ├── screen/      # Composeスクリーン
│   ├── viewmodel/   # ViewModel
│   └── theme/       # テーマ設定
└── util/            # ユーティリティ
```

### 主要スクリーン
- **ArchiveListScreen**: ファイル一覧
- **BillingScreen**: 決済・サブスクリプション管理
- **PaymentMethodScreen**: 支払い方法管理
- **UsageScreen**: 使用量確認
- **InvoiceScreen**: 請求書確認
- **AutoUploadSettingsScreen**: 自動アップロード設定

## 🌐 多言語対応

サポート言語:
- 🇺🇸 English (en)
- 🇯🇵 日本語 (ja)
- 🇰🇷 한국어 (ko)
- 🇨🇳 简体中文 (zh)
- 🇹🇼 繁體中文 (zh-rTW)

## 🔧 設定

### API エンドポイント
- **開発環境**: `http://10.0.2.2:3000/` (Android Emulator)
- **本番環境**: AWS API Gateway URL

### 環境変数
- `API_BASE_URL_DEBUG`: 開発用APIベースURL
- `API_BASE_URL_RELEASE`: 本番用APIベースURL

## 🚀 デプロイ

### Google Play Store
1. リリースビルドを作成
2. Play Console にアップロード
3. 段階的リリース実行

詳細は [play-store-listing.md](play-store-listing.md) を参照

## 🔒 セキュリティ

### 秘密情報の管理
- ❌ **コミット禁止**: API キー、署名ファイル、パスワード
- ✅ **使用推奨**: BuildConfig、local.properties、環境変数

### 権限
- `INTERNET`: API通信
- `READ_EXTERNAL_STORAGE`: ファイル読み取り
- `POST_NOTIFICATIONS`: 通知表示
- `FOREGROUND_SERVICE`: バックグラウンド処理

## 🐛 トラブルシューティング

### よくある問題

#### ビルドエラー
```bash
# Gradle キャッシュをクリア
./gradlew clean

# 依存関係を再取得
./gradlew --refresh-dependencies
```

#### API接続エラー
- ネットワーク設定を確認
- エミュレータの場合は `10.0.2.2` を使用
- 実機の場合は実際のIPアドレスを使用

## 📄 ライセンス

MIT License

## 🤝 コントリビューション

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add some amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## 📞 サポート

- Issues: [GitHub Issues](https://github.com/your-username/glaceon-android/issues)
- Email: support@your-domain.com

## 🔄 更新履歴

### v1.0.0 (2025-01-26)
- 初回リリース
- 基本的なファイルアーカイブ機能
- Stripe決済統合
- 多言語対応
- Material Design 3 UI