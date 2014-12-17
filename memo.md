# 環境や構成に関するメモ

## conf関連
### application.conf
DATABASE_URIとMONGOLAB_URIのみapplication.confで設定
それ以外の環境変数は各モジュールで直接sys.envで読んでいる

### messages
- messages
- messages.ja
- messages.origin

とあるが編集するのは「messages.origin」のみ

### salesforceフォルダ
production用とsandbox用のWSDLが置いてある  
sandboxを使用する場合は環境変数で切り替える  
APIのバージョンは27だが特に変える必要はない

## Assets(javascripts/css)
public以下のものを直接使用(未加工)
主なスクリプトは以下の2つ

- sqlsync.js - アプリ本体
- sqlgrid.js - 任意のSQL実行結果をjqGridで表示するライブラリ(SelectTool.scalaと連動)

## Scalaコード
そんなに多くないので全ファイルの役割を解説

### controllers
#### Application.scala
メインのController。主要な処理は全部ここ

#### I18N.scala
messages.jsを生成するController。国際化対応アプリ開発時の定番

#### MySelectTool.scala 
SelectToolにAccessControlによるアクセス制限と環境変数による変数の置換を付け加えたもの

### models
#### Salesforce.scala
同期処理の実行。
実際の同期処理はflectSalesforceのAPIを実行しているだけ

#### Schedule.scala
スケジュール管理
次のスケジュール実行時間を計算してAkkaのschedulerに登録している

#### StorageManager.scala
設定情報の保存
traitになっていて現在はMongoStorageManagerだけが実装されている

### utils
#### AccessControl.scala
IP制限、ベーシック認証によるアクセス制限

#### JqGrid.scala
JqGrid用のユーティリティ

#### RequestUtils.scala
Requestハンドリング用のユーティリティ

#### SelectTool.scala
jqGridに任意のSQLを表示するためのController

### Global.scala
messagesの自動生成とherokuに対するポーリング

## 問題点
終了時に

> Can't get ClosableLazy value after it has been closed

というExceptionが発生しているが多分気にする必要はない
