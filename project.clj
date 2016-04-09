(defproject fr.tedoldi/liPng "1.0.0"
  :description  "Generate PNG file from lichess.org"
  :url          "https://github.com/s-ted/liPng"
  :license      {:name "Attribution-NonCommercial-ShareAlike 4.0 International"
                 :url  "http://creativecommons.org/licenses/by-sa/4.0/"}
  :dependencies [[org.clojure/clojure "1.8.0"]

                 [cheshire "5.5.0"]
                 [org.clojure/data.json "0.2.6"]

                 [de.ubercode.clostache/clostache "1.4.0"]

                 [org.clojure/tools.cli "0.3.1"]
                 [clojure-term-colors "0.1.0-SNAPSHOT"]

                 [com.taoensso/timbre "4.3.1"]

                 [clj-http "2.1.0"]

                 [clj-time "0.11.0"]

                 [environ "1.0.2"]

                 [com.orientechnologies/orientdb-core   "2.1.13"]
                 [com.orientechnologies/orientdb-server "2.1.13"]]

  :plugins      [[lein-environ "1.0.2"]]

  :global-vars  {*assert* false}

  :source-paths ["src" "resources"]
  :test-paths   ["test" "test-resources"]
  :main         fr.tedoldi.lichess.game.retriever.main
  :omit-source true

  :uberjar-name "libPng.jar"

  :profiles     {:dev
                 {:global-vars  {*warn-on-reflection* false
                                 *assert*             true}
                  :dependencies [[midje "1.8.3"]]
                  :plugins      [[lein-midje "3.2"]]
                  :injections   [(use 'midje.repl)]
                  :env          {:dev "true"}}

                 :uberjar
                 {:aot :all}})
