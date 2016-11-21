(ns leiningen.i18n
  (:require [clojure.java.shell :refer [sh]]
            [leiningen.core.main :as lmain]
            [clojure.string :as string]
            [clojure.string :as str]
            [clojure.java.io :as io]
            [leiningen.i18n.code-gen :as cg]
            [leiningen.i18n.parse-po :as parse]
            [leiningen.i18n.util :as util :refer [puke po-path]]
            [clojure.pprint :as pp])
  (:import (java.io File)))

(def i18n-defaults
  {:default-locale        "en"
   :translation-namespace "untangled.translations"
   :source-folder         nil
   :translation-build     "i18n"
   :production-build      "production"
   :po-files              "i18n/msgs"})

(defn check-i18n-build! [nm build]
  (when (nil? build)
    (puke "The specified translation build (" nm ") does not exist in the project file."))
  (when (nil? (get-in build [:compiler :output-to]))
    (puke "The translation build (" nm ") has no :output-to setting."))
  (when-not (#{:whitespace :simple} (get-in build [:compiler :optimizations]))
    (puke "The translation build (" nm ") should specify optimizations of :whitespace or :simple")))

(defn check-production-build! [nm build]
  (when (nil? build)
    (puke "The specified production build (" nm ") does not exist in the project file."))
  (when (nil? (get-in build [:compiler :output-to]))
    (puke "The production build (" nm ") has no :output-to setting."))
  (when-not (#{:whitespace :simple :advanced} (get-in build [:compiler :optimizations]))
    (puke "The production build (" nm ") should specify optimizations of :whitespace, :simple, or :advanced"))
  (when-not (get-in build [:compiler :modules])
    (lmain/warn "The production build (" nm ") does not specify modules. Your program will have to require all translations at the top level to compile them all in.")))

(defn setup-environment!
  "Check and setup the environment. Returns a subset of settings pertaining to the filesystem:

  :messages-pot The full path to the messages.pot (generated) file
  :po-dir a File object representing the po directory for translation files."
  [settings]
  (when (or (nil? (:source-folder settings))
            (not (.exists (io/as-file (:source-folder settings)))))
    (puke "The configured target folder [:untangled-i18n :source-folder] for translation cljs sources ("
          (:source-folder settings) ") is not set or does not exist."))
  (when (util/gettext-missing?)
    (puke "The xgettext and msgmerge commands are not installed, or are not on your $PATH."))
  (let [po-files (:po-files settings)
        po-dir ^File (io/as-file po-files)]
    (when (not (.exists po-dir))
      (lmain/warn "Creating missing PO directory: " po-files)
      (.mkdirs po-dir))
    (when (not (.isDirectory po-dir))
      (puke po-files " is NOT a directory!"))
    {:po-dir       po-dir
     :messages-pot (.getAbsolutePath (new File po-dir "messages.pot"))}))

(defn check-settings!
  "Verify that the project configured settings are all present, or that the defaults make sense. Returns settings
  with various additional things set from the environment and project file:

  :translation-js will be the path to the Javascript output of the i18n build
  "
  [settings builds]
  (let [i18n-build-name (:translation-build settings)
        prod-build-name (:production-build settings)
        i18n-build (util/get-cljsbuild builds i18n-build-name)
        production-build (util/get-cljsbuild builds prod-build-name)
        module-basepath (or (-> production-build :compiler :asset-path) "/")
        trans-ns (:translation-namespace settings)
        src-base (:source-folder settings)
        output-dir (util/cljs-output-dir src-base trans-ns)
        outdir ^File (io/as-file output-dir)
        updated-settings (merge settings
                                {:translation-target output-dir
                                 :module-basepath    module-basepath
                                 :translation-js     (get-in i18n-build [:compiler :output-to])}
                                (setup-environment! settings))]
    (when (not (.exists outdir))
      (lmain/info "Making missing source folder " outdir)
      (.mkdirs outdir))
    (lmain/info "Locale modules will be build to load relative to asset path: " module-basepath)
    (lmain/info "Translation build: " i18n-build-name)
    (check-i18n-build! i18n-build-name i18n-build)
    (check-production-build! prod-build-name production-build)
    (lmain/info "Settings for i18n: " updated-settings)
    updated-settings))

(defn extract-strings
  "This subtask extracts strings from your cljs files that should be translated."
  [project]
  (let [explicit-settings (get project :untangled-i18n {})
        builds (get-in project [:cljsbuild :builds])
        settings-with-defaults (merge i18n-defaults explicit-settings)
        settings (check-settings! settings-with-defaults builds)
        messages-pot-path (:messages-pot settings)
        js-path (:translation-js settings)
        po-files-to-merge (util/find-po-files (:po-files settings))]
    (util/build project (:translation-build settings))
    (util/run "xgettext" "--from-code=UTF-8" "--debug" "-k" "-ktr:1" "-ktrc:1c,2" "-ktrf:1" "-o" messages-pot-path js-path)
    (doseq [po po-files-to-merge]
      (when (.exists (io/as-file (po-path settings po)))
        (lmain/info "Merging new template to existing translations for " po)
        (util/run "msgmerge" "--force-po" "--no-wrap" "-U" (po-path settings po) messages-pot-path)))))

(defn deploy-translations
  "This subtask converts translated .po files into locale-specific .cljs files for runtime string translation."
  [project]
  (let [explicit-settings (get project :untangled-i18n {})
        builds (get-in project [:cljsbuild :builds])
        settings-with-defaults (merge i18n-defaults explicit-settings)
        settings (check-settings! settings-with-defaults builds)
        replace-hyphen #(str/replace % #"-" "_")
        trans-ns (:translation-namespace settings)
        output-dir (:translation-target settings)
        po-files (util/find-po-files (:po-files settings))
        default-lc (:default-locale settings)
        locales (map util/clojure-ize-locale po-files)
        locales-inc-default (conj locales default-lc)
        default-lc-translation-path (str output-dir "/" (replace-hyphen default-lc) ".cljs")
        default-lc-translations (cg/wrap-with-swap :namespace trans-ns :locale default-lc :translation {})
        locales-code-string (cg/gen-locales-ns settings locales)
        locales-path (str output-dir "/locales.cljs")
        default-locale-code-string (cg/gen-default-locale-ns trans-ns default-lc)
        default-locale-path (str output-dir "/default_locale.cljs")]

    (cg/write-cljs-translation-file default-locale-path default-locale-code-string)
    (if (some #{default-lc} locales)
      (cg/write-cljs-translation-file locales-path locales-code-string)
      (let [locales-code-string (cg/gen-locales-ns project locales-inc-default)]
        (cg/write-cljs-translation-file locales-path locales-code-string)
        (cg/write-cljs-translation-file default-lc-translation-path default-lc-translations)))
    (lmain/warn "Configured project for default locale:" default-lc)

    (doseq [po po-files]
      (let [locale (util/clojure-ize-locale po)
            translation-map (parse/map-translations (po-path settings po))
            cljs-translations (cg/wrap-with-swap
                                :namespace trans-ns :locale locale :translation translation-map)
            cljs-trans-path (str output-dir "/" (replace-hyphen locale) ".cljs")]
        (cg/write-cljs-translation-file cljs-trans-path cljs-translations)))

    (lmain/info "Deployed translations for the following locales:" locales)))

(defn i18n
  "A plugin which automates your i18n string translation workflow"
  {:subtasks [#'extract-strings #'deploy-translations]}
  ([project]
   (puke "Usage: lein i18n (extract-strings | deploy-translations)"))
  ([project subtask]
   (case subtask
     "extract-strings" (extract-strings project)
     "deploy-translations" (deploy-translations project)
     (puke "Unrecognized subtask:" subtask "\n Use 'extract-strings' or 'deploy-translations'."))))
