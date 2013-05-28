SQLSync
=======

Overview
--------
毎日指定時間に登録したSELECT文を実行してその結果をSalesforceにBulkでUpsertするツールです。  
実行するSELECT文には前回の実行日時がパラメータとして渡されるので更新日時を比較して毎日夜間にRDB上のデータをSalesforceに取り込むような使い方ができます。

Environment
-----------
このアプリケーションはPlayframework2.1で開発されており、実行のためには以下の環境が必要です。

- RDBMS
- Salesforce
- MongoDB

RDBとSalesforceはこのアプリを実行する動機となるものなので御自身でご用意ください。  
MongoDBは単純な設定情報を保存しているだけなので最低限のスペックのもので問題ありません。  

このアプリはHeroku上で動作させることを想定しており、その場合はMongoDBは無償のSandbox版でも問題なく動きます。

Install
-------
このアプリをHeroku上で動作させる場合のsetupコマンドは以下です。

    git clone git@github.com:shunjikonishi/sqlsync.git
    heroku create
    git push heroku master
    heroku addons:add mongolab:sandbox


動作させるためには以下の環境変数の設定が必要です。

- DATABASE_URL: 接続するデータベースのURL(Heroku Postgresの標準書式で設定)
- SALESFORCE_USERNAME: 接続するSalesforceユーザー名
- SALESFORCE_PASSWORD: 接続するSalesforceユーザーのパスワード
- SALESFORCE_SECRET: 接続するSalesforceユーザーのセキュリティトークン  
  - ユーザープロファイルでIP制限がかかっている場合は不要です。
- SALESFORCE_WSDL: 接続に使用するWSDLファイル
  - プロダクション環境に接続する場合は不要です。
  - Sandbox環境に接続する場合は「conf/salesforce/sandbox.wsdl」を指定してください。
- TIMEZONE: タイムゾーン。日本で使用する場合は「Asia/Tokyo」としてください。


IP制限によりSalesforceへの接続にProxyを使用する場合はさらに以下を設定してください。

- PROXY_HOST: Proxyサーバのホスト名
- PROXY_PORT: Proxyサーバのポート。省略時は80
- PROXY_USERNAME: 必要な場合Proxyサーバのユーザ名を設定
- PROXY_PASSWORD: 必要な場合Proxyサーバのパスワードを設定


セキュリティを確保するためにアプリへのアクセスを制限したい場合は以下を設定してください。

- ALLOWED_IP: 設定すると指定のIPのクライアントからしか接続できなくなります。
  - カンマ区切りで複数指定可能です。
  - 「xxx.xxx.xxx.0/24」のようなサブネット表記での指定も可能です。
- BASIC_AUTHENTICATION: 設定するとアクセスにBasic認証が要求されるようになります。

設定が正しく行われていればHeroku上でアプリの画面が正しく表示されます。

How to use
----------
アプリ画面から「使い方」を参照してください。


License
-------
MIT
