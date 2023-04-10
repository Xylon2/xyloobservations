(defproject xyloobservations "0.1.0-SNAPSHOT"

  :description "FIXME: write description"
  :url "http://example.com/FIXME"

  :dependencies [[clojure.java-time "1.2.0"]
                 [conman "0.9.6"]
                 [cprop "0.1.19"]
                 [expound "0.9.0"]
                 [funcool/struct "1.4.0"]
                 [json-html "0.4.7"]
                 [luminus-immutant "0.2.5"]
                 [luminus-migrations "0.7.5"]
                 [luminus-transit "0.1.6"]
                 [markdown-clj "1.11.4"]
                 [metosin/muuntaja "0.6.8"]
                 [metosin/reitit "0.6.0"]
                 [metosin/ring-http-response "0.9.3"]
                 [mount "0.1.17"]
                 [nrepl "1.0.0"]
                 [org.clojure/clojure "1.11.1"]
                 [org.clojure/tools.cli "1.0.214"]
                 [org.clojure/tools.logging "1.2.4"]
                 [org.postgresql/postgresql "42.6.0"]
                 [org.webjars.npm/bulma "0.9.4"]
                 [org.webjars.npm/material-icons "1.13.2"]
                 [org.webjars/webjars-locator "0.46"]
                 [org.webjars/webjars-locator-jboss-vfs "0.1.0"]
                 [ring-webjars "0.2.0"]
                 [ring/ring-core "1.10.0"]
                 [ring/ring-defaults "0.3.4"]
                 [selmer "1.12.58"]
                 [cheshire "5.11.0"]
                 [buddy/buddy-hashers "1.8.158"]
                 [amazonica "0.3.163" :exclusions [com.amazonaws/aws-java-sdk
                                                   com.amazonaws/amazon-kinesis-client
                                                   com.amazonaws/dynamodb-streams-kinesis-adapter]]
                 [com.amazonaws/aws-java-sdk-core "1.12.442"]
                 [com.amazonaws/aws-java-sdk-s3 "1.12.442"]
                 [com.novemberain/langohr "5.4.0"]
                 [com.taoensso/nippy "3.2.0"]
                 [clj-http "3.12.3"]
                 [org.clojure/clojurescript "1.11.60"]
                 [cljs-ajax "0.8.4"]]

  :min-lein-version "2.0.0"
  
  :source-paths ["src/clj"]
  :test-paths ["test/clj"]
  :resource-paths ["resources" "target/cljsbuild"]
  :target-path "target/%s/"
  :main ^:skip-aot xyloobservations.core

  :plugins [[lein-cljsbuild "1.1.8"]
            [lein-immutant "2.1.0"]]

  :cljsbuild
  {:builds
   [{; The path to the top-level ClojureScript source directory:
     :source-paths ["src/cljs"]
     ; The standard ClojureScript compiler options:
     ; (See the ClojureScript compiler documentation for details.)
     :compiler {:main xyloobservations.advanced
                :output-to "target/cljsbuild/public/js/advanced.js"
                :optimizations :whitespace
                :pretty-print true}}
    {; The path to the top-level ClojureScript source directory:
     :source-paths ["src/cljs"]
     ; The standard ClojureScript compiler options:
     ; (See the ClojureScript compiler documentation for details.)
     :compiler {:main xyloobservations.progress
                :output-to "target/cljsbuild/public/js/progress.js"
                :optimizations :whitespace
                :pretty-print true}}]}

  :profiles
  {:uberjar {:omit-source true
             :aot :all
             :uberjar-name "xyloobservations.jar"
             :source-paths ["env/prod/clj" ]
             :resource-paths ["env/prod/resources"]}

   :dev           [:project/dev :profiles/dev]
   :test          [:project/dev :project/test :profiles/test]

   :project/dev  {:jvm-opts ["-Dconf=dev-config.edn" ]
                  :dependencies [[org.clojure/tools.namespace "1.4.4"]
                                 [pjstadig/humane-test-output "0.11.0"]
                                 [prone "2021-04-23"]
                                 [ring/ring-devel "1.10.0"]
                                 [ring/ring-mock "0.4.0"]]
                  :plugins      [[com.jakemccrary/lein-test-refresh "0.24.1"]
                                 [jonase/eastwood "0.3.5"]
                                 [cider/cider-nrepl "0.26.0"]] 
                  
                  :source-paths ["env/dev/clj" ]
                  :resource-paths ["env/dev/resources"]
                  :repl-options {:init-ns user
                                 :timeout 120000}
                  :injections [(require 'pjstadig.humane-test-output)
                               (pjstadig.humane-test-output/activate!)]}
   :project/test {:jvm-opts ["-Dconf=test-config.edn" ]
                  :resource-paths ["env/test/resources"] }
   :profiles/dev {}
   :profiles/test {}})
