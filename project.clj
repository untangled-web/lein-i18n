(defproject untangled-lein-i18n "0.1.0-SNAPSHOT"
  :description "A plugin for extracting/populating transalations for Untangled"
  :url ""
  :license {:name "NAVIS"
            :url  "http://www.thenavisway.com"}

  :dependencies [[org.clojure/clojure "1.7.0"]
                 [untangled-spec "0.1.1"]
                 [leiningen-core "2.5.3"]
                 [lein-cljsbuild "1.1.0"]
                 [leiningen "2.5.3"]
                 ]

  :source-paths ["src"]
  :test-paths ["spec"]

  :test-refresh {
                 :report untangled-spec.report/untangled-report
                 }

  :profiles {
             :dev {
                   :source-paths ["src" "spec"]
                   :env          {:dev true}
                   }
             }
)
