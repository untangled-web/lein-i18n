(defproject untangled-lein-i18n "0.1.2"
  :description "A plugin for extracting/populating transalations for Untangled"
  :url ""
  :license {:name "NAVIS"
            :url  "http://www.thenavisway.com"}

  :dependencies [[org.clojure/clojure "1.7.0"]
                 [untangled-spec "0.3.1" :scope "test"]
                 [leiningen-core "2.5.3"]
                 [lein-cljsbuild "1.1.0"]
                 [leiningen "2.5.3"]
                 ]

  :repositories [["releases" "https://artifacts.buehner-fry.com/artifactory/release"]
                 #_["snapshot" "https://artifacts.buehner-fry.com/artifactory/internal-snapshots"]
                 #_["third-party" "https://artifacts.buehner-fry.com/artifactory/internal-3rdparty"]]

  :deploy-repositories [["releases" {:id            "central"
                                     :url           "https://artifacts.buehner-fry.com/artifactory/navis-maven-release"
                                     :snapshots     false
                                     :sign-releases false}]
                        ["snapshots" {:id            "snapshots"
                                      :url           "https://artifacts.buehner-fry.com/artifactory/navis-maven-snapshot"
                                      :sign-releases false}]]

  :source-paths ["src"]
  :test-paths ["spec"]

  :test-refresh {:report untangled-spec.reporters.terminal/untangled-report}

  :profiles {
             :dev {
                   :source-paths ["src" "spec"]
                   :env          {:dev true}
                   }
             }
)
