(def buildmapdev
  [{; The path to the top-level ClojureScript source directory:
    :source-paths ["src/cljs"]
    ; The standard ClojureScript compiler options:
    ; (See the ClojureScript compiler documentation for details.)
    :compiler {:main 'xyloobservations.advanced
               :output-to "target/cljsbuild/public/js/advanced.js"
               :optimizations :whitespace
               :pretty-print true}}
    {; The path to the top-level ClojureScript source directory:
     :source-paths ["src/cljs"]
     ; The standard ClojureScript compiler options:
     ; (See the ClojureScript compiler documentation for details.)
     :compiler {:main 'xyloobservations.progress
                :output-to "target/cljsbuild/public/js/progress.js"
                :optimizations :whitespace
                :pretty-print true}}])

;; prod version of the build map, with optimizations turned on
(def buildmapprod
  (map (fn [build] (-> build
                       (assoc-in [:compiler :optimizations] :advanced)
                       (assoc-in [:compiler :pretty-print]  false))) buildmapdev))

(defproject xyloobservations "0.1.0-SNAPSHOT"

  :description "A photo gallery app in Clojure with tag-based organization"
  :url "https://codeberg.org/xylon/xyloobservations"

  :dependencies [[clojure.java-time "1.4.3"]
                 [conman "0.9.6"]
                 [cprop "0.1.20"]
                 [expound "0.9.0"]
                 [funcool/struct "1.4.0"]
                 [json-html "0.4.7"]
                 [luminus-immutant "0.2.5"]
                 [luminus-migrations "0.7.5"]
                 [luminus-transit "0.1.6"]
                 [luminus-log4j "0.1.7"]
                 [markdown-clj "1.12.4"]
                 [metosin/muuntaja "0.6.11"]
                 [metosin/reitit "0.9.1"]
                 [metosin/ring-http-response "0.9.5"]
                 [mount "0.1.23"]
                 [nrepl "1.3.1"]
                 [org.clojure/clojure "1.12.0"]
                 [org.clojure/tools.cli "1.1.230"]
                 [org.clojure/tools.logging "1.3.0"]
                 [org.postgresql/postgresql "42.7.7"]
                 [org.webjars.npm/bulma "1.0.4"]
                 [org.webjars.npm/material-icons "1.13.2"]
                 [org.webjars/webjars-locator "0.52"]
                 [org.webjars/webjars-locator-jboss-vfs "0.1.0"]
                 [ring-webjars "0.3.0"]
                 [ring/ring-core "1.14.2"]
                 [ring/ring-defaults "0.6.0"]
                 [selmer "1.12.62"]
                 [cheshire "6.0.0"]
                 [buddy/buddy-hashers "2.0.167"]
                 [amazonica "0.3.168" :exclusions [com.amazonaws/aws-java-sdk
                                                   com.amazonaws/amazon-kinesis-client
                                                   com.amazonaws/dynamodb-streams-kinesis-adapter]]
                 [com.amazonaws/aws-java-sdk-core "1.12.788"]
                 [com.amazonaws/aws-java-sdk-s3 "1.12.788"]
                 [com.novemberain/langohr "5.5.0"]
                 [org.clojure/data.fressian "1.1.0"]
                 [clj-http "3.13.1"]
                 [org.clojure/clojurescript "1.11.132"]
                 [cljs-ajax "0.8.4"]
                 [cljsjs/react "18.3.1-1"]
                 [cljsjs/react-dom "18.3.1-1"]
                 [reagent "1.3.0"]
                 [org.clojars.quoll/clormat "0.0.1"]]

  :min-lein-version "2.0.0"
  
  :source-paths ["src/clj"]
  :test-paths ["test/clj"]
  :resource-paths ["resources" "target/cljsbuild"]
  :target-path "target/%s/"
  :main ^:skip-aot xyloobservations.core

  :plugins [[lein-cljsbuild "1.1.8"]]

  :hooks [leiningen.cljsbuild]

  :profiles
  {:uberjar {:omit-source true
             :aot :all
             :uberjar-name "xyloobservations.jar"
             :source-paths ["env/prod/clj" ]
             :resource-paths ["env/prod/resources"]
             :cljsbuild {:builds ~buildmapprod}}

   :dev           [:project/dev :profiles/dev]
   :test          [:project/dev :project/test :profiles/test]

   :project/dev  {:jvm-opts ["-Dconf=dev-config.edn" ]
                  :dependencies [[org.clojure/tools.namespace "1.5.0"]
                                 [pjstadig/humane-test-output "0.11.0"]
                                 [prone "2021-04-23"]
                                 [ring/ring-devel "1.14.2"]
                                 [ring/ring-mock "0.6.1"]]
                  :plugins      [[com.jakemccrary/lein-test-refresh "0.24.1"]
                                 [jonase/eastwood "0.3.5"]
                                 [cider/cider-nrepl "0.26.0"]] 
                  
                  :source-paths ["env/dev/clj" ]
                  :resource-paths ["env/dev/resources"]
                  :repl-options {:init-ns user
                                 :timeout 120000}
                  :injections [(require 'pjstadig.humane-test-output)
                               (pjstadig.humane-test-output/activate!)]
                  :cljsbuild {:builds ~buildmapdev}}
   :project/test {:jvm-opts ["-Dconf=test-config.edn" ]
                  :resource-paths ["env/test/resources"]
                  :cljsbuild {:builds ~buildmapdev}}
   :profiles/dev {}
   :profiles/test {}})
