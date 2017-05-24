(defproject clj-helper "0.3.1-SNAPSHOT"
  :description "Helps you package Clojure projects for Debian"
  :url "https://github.com/ehashman/clj-helper"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [environ "1.1.0"]
                 [selmer "1.10.7"]]
  :plugins [[lein-cljfmt "0.5.6" :exclusions [com.google.javascript/closure-compiler
                                              org.clojure/clojurescript]]
            [jonase/eastwood "0.2.3"]
            [lein-cloverage "1.0.9"]]
  :main ^:skip-aot clj-helper.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}
             :test {:dependencies [[me.raynes/fs "1.4.6"]]}})
