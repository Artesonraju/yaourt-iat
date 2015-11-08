(defproject yaourt-iat "0.1.0-SNAPSHOT"
  :description "Yaourt IAT"
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [org.clojure/clojurescript "1.7.170"]
                 [org.clojure/core.async "0.2.371"]
                 [org.omcljs/om "1.0.0-alpha18"]
                 [figwheel-sidecar "0.5.0-SNAPSHOT"
                  :scope "provided"
                  :exclusions [org.apache.httpcomponents/httpclient
                               org.apache.httpcomponents/httpcore]]
                 [bidi "1.20.3"]
                 [ring/ring "1.4.0"]
                 [org.clojure/data.csv "0.1.3"]
                 [com.cognitect/transit-clj "0.8.281"]
                 [com.cognitect/transit-cljs "0.8.225"]
                 [cljs-http "0.1.30" :exclusions
                  [org.clojure/clojure org.clojure/clojurescript
                   com.cognitect/transit-cljs]]
                 [http-kit "2.1.19"]
                 [amazonica "0.3.39"
                  :exclusions [com.amazonaws/amazon-kinesis-client
                               com.amazonaws/aws-java-sdk-autoscaling
                               com.amazonaws/aws-java-sdk-cloudformation
                               com.amazonaws/aws-java-sdk-cloudfront
                               com.amazonaws/aws-java-sdk-cloudhsm
                               com.amazonaws/aws-java-sdk-cloudtrail
                               com.amazonaws/aws-java-sdk-cloudwatch
                               com.amazonaws/aws-java-sdk-cloudwatchmetrics
                               com.amazonaws/aws-java-sdk-codecommit
                               com.amazonaws/aws-java-sdk-codedeploy
                               com.amazonaws/aws-java-sdk-codepipeline
                               com.amazonaws/aws-java-sdk-cognitoidentity
                               com.amazonaws/aws-java-sdk-cognitosync
                               com.amazonaws/aws-java-sdk-datapipeline
                               com.amazonaws/aws-java-sdk-devicefarm
                               com.amazonaws/aws-java-sdk-directconnect
                               com.amazonaws/aws-java-sdk-directory
                               com.amazonaws/aws-java-sdk-dynamodb
                               com.amazonaws/aws-java-sdk-ec2
                               com.amazonaws/aws-java-sdk-ecs
                               com.amazonaws/aws-java-sdk-efs
                               com.amazonaws/aws-java-sdk-elasticache
                               com.amazonaws/aws-java-sdk-elasticbeanstalk
                               com.amazonaws/aws-java-sdk-elasticloadbalancing
                               com.amazonaws/aws-java-sdk-elasticsearch
                               com.amazonaws/aws-java-sdk-elastictranscoder
                               com.amazonaws/aws-java-sdk-emr
                               com.amazonaws/aws-java-sdk-glacier
                               com.amazonaws/aws-java-sdk-iam
                               com.amazonaws/aws-java-sdk-importexport
                               com.amazonaws/aws-java-sdk-inspector
                               com.amazonaws/aws-java-sdk-iot
                               com.amazonaws/aws-java-sdk-kinesis
                               com.amazonaws/aws-java-sdk-logs
                               com.amazonaws/aws-java-sdk-machinelearning
                               com.amazonaws/aws-java-sdk-marketplacecommerceanalytics
                               com.amazonaws/aws-java-sdk-opsworks
                               com.amazonaws/aws-java-sdk-rds
                               com.amazonaws/aws-java-sdk-redshift
                               com.amazonaws/aws-java-sdk-route53
                               com.amazonaws/aws-java-sdk-ses
                               com.amazonaws/aws-java-sdk-simpledb
                               com.amazonaws/aws-java-sdk-simpleworkflow
                               com.amazonaws/aws-java-sdk-sns
                               com.amazonaws/aws-java-sdk-sqs
                               com.amazonaws/aws-java-sdk-ssm
                               com.amazonaws/aws-java-sdk-storagegateway
                               com.amazonaws/aws-java-sdk-sts
                               com.amazonaws/aws-java-sdk-support
                               com.amazonaws/aws-java-sdk-swf-libraries
                               com.amazonaws/aws-java-sdk-waf
                               com.amazonaws/aws-java-sdk-workspaces]]]
  :source-paths ["src"]
  :plugins [[lein-cljsbuild "1.1.1"]]
  :hooks [leiningen.cljsbuild]
  :omit-source true
  :aot :all
  :main yaourt-iat.server
  :cljsbuild {
    :builds [{
          :source-paths ["src"]
          :compiler {
            :output-to "resources/public/js/main.js"
            :optimizations :simple
            :pretty-print false}}]})