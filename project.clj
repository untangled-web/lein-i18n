(defproject navis/untangled-lein-i18n "0.1.2"
  :description "A plugin for extracting/populating transalations for Untangled"
  :url ""
  :license {:name "MIT"
            :url  "https://opensource.org/licenses/MIT"}

  :dependencies [[org.clojure/clojure "1.7.0"]
                 [navis/untangled-spec "0.3.5" :scope "test"]
                 [leiningen-core "2.5.3"]
                 [lein-cljsbuild "1.1.2"]
                 [leiningen "2.5.3"]]

  :plugins [[com.jakemccrary/lein-test-refresh "0.14.0"]]

  :source-paths ["src"]
  :test-paths ["spec"]

  :test-refresh {:report untangled-spec.reporters.terminal/untangled-report}

  :profiles {:dev {
                   :source-paths ["src" "spec"]
                   :env          {:dev true}}})
