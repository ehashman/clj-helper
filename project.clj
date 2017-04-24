(defproject clj-helper "0.1.0-SNAPSHOT"
  :description "Helps you package Clojure projects for Debian"
  :url "https://github.com/ehashman/clj-helper"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [environ "1.1.0"]
                 [selmer "1.10.7"]]
  :main ^:skip-aot clj-helper.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
