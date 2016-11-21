(ns leiningen.i18n.util
  (:require [clojure.java.shell :refer [sh]]
            [clojure.string :as str]
            [leiningen.cljsbuild :refer [cljsbuild]]
            [leiningen.core.main :as lmain])
  (:import (java.io File)))

(defn puke [& msgs]
  (apply lmain/warn msgs)
  (lmain/abort))

(defn po-path [settings po-file] (.getAbsolutePath (new File (:po-dir settings) po-file)))

(defn cljs-output-dir [src-base namespace]
  (let [path-from-namespace (-> (str namespace)
                                (str/replace #"\." "/")
                                (str/replace #"-" "_"))]
    (str src-base "/" path-from-namespace)))

(defn find-po-files [msgs-dir-path]
  (filter #(.endsWith % ".po")
          (clojure.string/split-lines
            (:out (sh "ls" msgs-dir-path)))))

(defn gettext-missing? []
  (let [xgettext (:exit (sh "which" "xgettext"))
        msgcat (:exit (sh "which" "msgcat"))]
    (or (> xgettext 0) (> msgcat 0))))

(defn cljs-build?
  [build target]
  (if (= (:id build) target) build false))

(defn run
  "Run a shell command and logging the command and result."
  [& args]
  (lmain/info "Running: " (str/join " " args))
  (let [result (:exit (apply sh args))]
    (when (not= 0 result)
      (puke "FAILED!"))))

(defn build
  "Run a cljsbuild with logging output, but die if it fails (with a helpful message)"
  [project build-name]
  (lmain/info "Running: cljsbuild once " build-name)
  (cljsbuild project "once" build-name))

(defn get-cljsbuild [builds target]
  (some #(cljs-build? % target)
        builds))

(defn clojure-ize-locale [po-filename]
  (-> po-filename
      (str/replace #"^([a-z]+_*[A-Z]*).po$" "$1")
      (str/replace #"_" "-")))

(defn target-build [project]
  (if-let [target (get-in project [:untangled-i18n :target-build])]
    target
    (do
      (lmain/warn "No production target build specified! Assuming 'production'")
      "production")))

(defn target-build [project]
  (if-let [target (get-in project [:untangled-i18n :target-build])]
    target
    (do
      (lmain/warn "No production target build specified! Assuming 'production'")
      "production")))

