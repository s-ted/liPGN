(defproject fr.tedoldi/liPGN "1.0.0-SNAPSHOT"
  :description  "Generate PGN file from lichess.org"
  :url          "https://github.com/s-ted/liPGN"
  :author       "Sylvain Tedoldi"
  :license      {:name "Attribution-NonCommercial-ShareAlike 4.0 International"
                 :url  "http://creativecommons.org/licenses/by-sa/4.0/"}
  :dependencies [[org.clojure/clojure "1.9.0"]

                 [cheshire "5.8.0"]
                 [org.clojure/data.json "0.2.6"]

                 [de.ubercode.clostache/clostache "1.4.0"]

                 [org.clojure/tools.cli "0.3.5"]
                 [clojure-term-colors "0.1.0"]

                 [com.taoensso/timbre "4.10.0"]

                 [clj-http "3.8.0"]

                 [clj-time "0.14.2"]

                 [environ "1.0.2"]

                 [com.orientechnologies/orientdb-core   "2.2.33"]
                 [com.orientechnologies/orientdb-server "2.2.33"]]

  :plugins      [[lein-environ "1.0.2"]]

  :global-vars  {*warn-on-reflection* false
                 *assert* false}

  :source-paths ["src" "resources"]
  :test-paths   ["test" "test-resources"]
  :main         fr.tedoldi.lichess.game.retriever.main
  :omit-source true

  :uberjar-name "liPGN.jar"

  :profiles     {:dev
                 {:global-vars  {*warn-on-reflection* false
                                 *assert*             true}
                  :dependencies [[midje "1.9.1"]]
                  :plugins      [[lein-midje "3.2"]]
                  :injections   [(use 'midje.repl)]
                  :env          {:dev "true"}}

                 :uberjar
                 {:aot :all}})
