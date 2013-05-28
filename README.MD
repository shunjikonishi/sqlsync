SQLSync
=======

Overview
--------
毎日指定時間に登録したSELECT文を実行してその結果をSalesforceにBulkでUpsertするツールです。  
実行するSELECT文には前回の実行日時がパラメータとして渡されるので更新日時を比較して毎日夜間にRDB上のデータをSalesforceに取り込むような使い方ができます。

Install
-------
このアプリケーションはPlayframework2で開発されており、実行のためには以下の環境が必要です。

- RDBMS(同期対象のデータが保存されているRDBMS)
- Salesforce(同期対象のSalesforce)
- MongoDB(設定情報の保存用)

    git clone git@github.com:shunjikonishi/sqlsync.git
    heroku create
    git push heroku master
    heroku config:add D=<your aws accessKey> 
    heroku config:add S3_SECRETKEY=<your aws secretKey> 
    heroku config:add PAPERTRAIL_ARCHIVE_APP1=<your S3 bucket>/papertrail/logs
    heroku config:add TIMEZONE=Asia/Tokyo


ALLOWED_IP:                218.219.152.9
DATABASE_URL:              postgres://u504tcjsfdver5:p1elq70o402phu13v95orev9htq@ec2-204-236-238-228.compute-1.amazonaws.com:5462/d4h9dvo8fs294
HEROKU_POSTGRESQL_RED_URL: postgres://bjxjyzdhappvbp:7q5a7zvUjAaQjnqQzhhXGRkg_m@ec2-54-235-152-226.compute-1.amazonaws.com:5432/d3npgjd09gcr9k
JAVA_OPTS:                 -Xmx384m -Xss512k -XX:+UseCompressedOops
MONGOLAB_URI:              mongodb://heroku_app15396863:rgjq92po8du6ivevnes96gbp77@ds061767.mongolab.com:61767/heroku_app15396863
PAPERTRAIL_API_TOKEN:      PB2q3JbLcR3YYT9YaTF
PATH:                      .jdk/bin:.sbt_home/bin:/usr/local/bin:/usr/bin:/bin
REPO:                      /app/.sbt_home/.ivy2/cache
SALESFORCE_PASSWORD:       flect2090!
SALESFORCE_SECRET:         hmiTy3WA9shJeffdgP7C4xoeh
SALESFORCE_USERNAME:       k-shunji@flect.co.jp
SALESFORCE_WSDL:           conf/salesforce/partner.wsdl
SBT_OPTS:                  -Xmx384m -Xss512k -XX:+UseCompressedOops
SCHEDULE_TIME:             01:00

License
-------
MIT
