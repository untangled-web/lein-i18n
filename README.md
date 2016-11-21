# Untangled Internationalization

<img src="https://img.shields.io/clojars/v/navis/untangled-lein-i18n.svg">

Release: <img src="https://api.travis-ci.org/untangled-web/untangled-lein-i18n.svg?branch=master">
Snapshot: <img src="https://api.travis-ci.org/untangled-web/untangled-lein-i18n.svg?branch=develop">

WARNING: This plugin is in progress. It mostly works, but the following bits need a bit of work:

- Safari support requires a polyfill

The internationalization support in Untangled is based on a number of tools to give a fully-functional, bi-directional
localization and internationalization solution that includes:

- GNU Gettext message support
   - Use plain strings in your UI
   - The plain strings are extracted
   - Standard GNU utilities are used to build translation files
   - Translations are complied to cljs dependencies
- Extensions to Gettext support:
   - Formatted messages
     - Output of localized numbers, dates, currency symbols
- Support for output *and* input of dates, numbers, and currency strings
   - E.g. User can type 4.211,44 in one field and 03.11.2011 in another with German as the locale and you can easily
   convert that to the javascript number 4211.44 and a Date object with November 3, 2011 in it.
- Yahoo's FormatJS for output.
   - Augmented parsing to get the same for input


## Translating Messages (including numbers/dates/currencies)

Your component render can simply use one of the various output translation functions on strings. String constructions
that involve variables should be constructed with `trf`.


     (ns mine
        (:require untangled.i18n :refer-macros [tr trc trf])
        )

     (tr "Some message")
     (trf "You owe {amount, number, usd}" :amount 122.34)
     (let [filter "completed"]
       (trf "Current filter: {filter, select, all {All} completed {Completed}}" :filter filter))

Internally, formatting uses the standard ICU message syntax provided by FormatJS.  This library includes number
formatters for the following currencies: usd, euro, yen. These are easily extendable via TODO...

## Translating Date objects and raw numbers

You can use `trf` to format these; however, if you're just wanting a nice standard string form of a date or number
without any fuss it may be more convenient to use these:


     (i18n/format-date :short date-object) ; 3/4/1998, 4.3.1998, etc
     (i18n/format-date :medium date-object) ; Mar 4, 1998
     (i18n/format-date :long date-object) ; March 4, 1998

     (i18n/format-number 44.6978) ; "44.6978" "44,6978", etc.

     (i18n/format-currency 44.6978) ; Truncates by locale: US: "$44.69" Japan: "¥44", etc.
     (i18n/format-rounded-currency 44.6978) ; Rounds by locale: US: "44.70" Japan: "¥45", etc.

## Parsing dates and numbers

At the time of this writing, HTML INPUT does not support internationalized input in any consistent, usable form. As
a result it is necessary to normally just take text input and parse that string as a separate step. The Untangled
framework includes components for forms that can handle this complexity for you:


     ; app-state
       :comp
          :n (make-number-input)
          :start-date (make-date-input)

     ; rendering
     [data context] ; args to the render of :comp
     ; render the components with :changed event handlers to deal with the data as it changes
     (u/number-input :n context {:changed (fn [] (u/get-value (:n data))) })
     (u/date-input :start-date context { :changed (fn [] (u/get-value (:start-date data))) })

## Translation extraction and deployment

### Plugin Installation and Setup
Untangled ships with a leiningen plugin that conveniently:

- extracts strings into a messages template file (messages.pot)
- merges new strings from the template into existing locale-specific translation files (eg: ja_JP.po)
- generates cljs files from locale-specific translations and installs them into your project

The plugin is only supported on Unix/Linux systems.

### Install Dependencies

The i18n plugin requires that gettext tools are installed and available in your $PATH.
On MAC OS install via homebrew:

`brew install gettext`

`brew link --force gettext`

The link command is required because some software get confused if two of the same utilities are in the library path.

Also, make sure that the project you are going to leverage the gettext against has a path 'i18n/msgs`

### Configure Plugin

Add `[navis/untangled-lein-i18n "0.2.0"]` to the `:plugins` list in `project.clj`.

The i18n plugin will look for configuration options at the `:untangled-i18n` key in your `project.clj`:

    :plugins [navis/untangled-lein-i18n \"0.2.0\"]

    :untangled-i18n {:default-locale        \"en\" ;; the default locale of your app
                     :translation-namespace \"app.i18n\" ;; the namespace for generating cljs translations
                     :source-folder         \"src\" ;; the target source folder for generated code
                     :translation-build     \"i18n\" ;; The name of the cljsbuild to compile your code that has tr calls
                     :po-files              \"msgs\" ;; The folder where you want to store gettext files  (.po/.pot)
                     :production-build      \"prod\"} ;; The name of your production build

    ; You need to have a build for generating an i18n source file for string extraction, and one for generating the
    ; final production application. You cannot use :advanced optimizations for the i18n step, but must at least use :whitespace
    ; so you get a single file. See the Developer's Guide for more details on Internationalization configuration.
    :cljsbuild {:builds [{:id           \"i18n\"
                          :source-paths [\"src\"]
                          :compiler     {:output-to     \"i18n/out/compiled.js\"
                                         :main          entry-point
                                         :optimizations :whitespace}}
                         {:id \"prod\"
                          :source-paths [\"src\"]
                          :compiler {:asset-path    \"js\"
                                     :output-dir \"resources/public/js\"
                                     :optimizations :advanced
                                     :source-map    true
                                     :modules       {;; The main program
                                                     :cljs-base {:output-to \"resources/public/js/main.js \"}
                                                     ;; One entry for each locale
                                                     :de        {:output-to \"resources/public/js/de.js \" :entries #{\"app.i18n.de \"}}
                                                     :es        {:output-to \"resources/public/js/es.js \" :entries #{\"app.i18n.es \"}}}}}]})

So, the above configuration (when used) will generate the following files:

```
src/
└── app
    ├── i18n
    │   ├── default_locale.cljs   ; The default locale translations
    │   ├── en_US.cljs            ; A file per locale (used for module loading of translations)
    │   ├── ...
    │   └── locales.cljs          ; A file that will load all locales in bulk (require this if not using modules)
```

### Plugin Usage and Translator Workflow

Suppose that you have just finished an awesome new feature in your project. This feature has added new untranslated
strings to your UI, and you would like to have the new parts of your UI translated for international users. To extract
your new strings for translation, run this command from the root of your project.

`lein i18n extract-strings`

WARNING: RUN cljsbuild on i18n FIRST TO MAKE SURE IT IS WORKING! If you have duplicate externs it might fail.

This will generate a new `messages.pot` in the `i18n/msgs` directory of your project. If you have existing translation
files in your project (eg: `i18n/msgs/fr_CA.po`), these files will be updated with your new untranslated strings. Any
existing translations in `fr_CA.po` will be preserved!

The updated `fr_CA.po` file now needs to be sent off to your human translator, who will see the new untranslated
strings in the file and produce the required translations. The translator will then send `fr_CA.po` file back to you,
and you will need to replace `i18n/msgs/fr_CA.po` with the new version provided by the translator. If you need to add a
new locale to the project (eg, we now want to add support for German), you will send the `i18n/msgs/messages.pot` file
to the German translator, and they will provide you with a `de.po` file which you will add to the `i18n/msgs` directory.

Now would be a good time to commit your new `*.po` files to version control.

We now want to convert `*.po` translation files into a format that your project can load at runtime when a user needs to
see translations in the UI. Run the following command from the root of your project to deploy new translations into your
project:

`lein i18n deploy-translations`

You now should be able to see the new translations in your app!

### Using the Translations

The generated code will include a `locales` namespace. Just require that and use the generated
`set-locale` function (e.g. in your UI).

```
(ns some.ui
 (:require [app.i18n.locales :as l]))

...
(l/set-locale \"es\") ; change the UI locale, possibly triggering a dynamic module load.
```

See the Untangled Developer's Guide for more information.
