(ns fr.tedoldi.lichess.game.retriever.main
  (:require
    [fr.tedoldi.lichess.game.retriever.core :as core]
    [fr.tedoldi.lichess.game.retriever.console :as console]

    [environ.core :as env]

    [clojure.tools.cli :as cli]
    [clojure.term.colors :as color])
  (:gen-class))



(defn- usage [options-summary]
  (clojure.string/join
    \newline
    ["This program is uses the lichess REST API to retrieve all the games of a user and transform them into a big PGN file"
     ""
     "Usage: java -jar libPGN.jar [options]"
     ""
     "Options:"
     options-summary
     ""
     "Please refer to the manual page for more information."]))

(defn- error-msg [errors]
  (str "The following errors occurred while parsing your command:\n\n"
       (clojure.string/join \newline errors)))

(def ^:private cli-options
  [["-q" "--quiet"               "Don't output messages on console"                                                :default false]
   ["-C" "--casual"              "Also handle casual games"                                                        :default false]
   ["-v" "--variant <variant>"   "Handle games for the given variant (standard/chess960/kinfOfTheHill/threeCheck)" :default "standard"]
   ["-p" "--speed <speed>"       "Handle games for the given speed (bullet/blitz/classical/unlimited)"]
   ["-U" "--url <URL>"           "URL of the API"                                                                  :default "http://en.lichess.org/api/"]
   ["-s" "--store <store>"       "The store to use for keeping the data (use 'memory:tmp' for a transient run)"    :default "plocal:db"]
   ["-c" "--color <color>"       "Handle games for the given color (white/black)"]
   ["-S" "--no-sync"             "Don't synchronize the games with the server"                                     :default false]
   ["-u" "--username <username>" "The username for whom to retrieve the games"]
   ["-o" "--output <file>"       "The file to output, use '-' for stdout. By default, output to '<username>.pgn'"]
   ["-h" "--help"                "Print this help"]])

(defn run [options]
  (->> [:quiet :url :casual :variant :output
        :speed :store :no-sync :username
        :color]
       (select-keys options)
       core/export!))

(defn -main [& args]
  (try

    (System/setProperty "log.console.level" (if (env/env :dev) "FINE" "SEVERE"))

    (let [{:keys [options arguments errors summary]}
          (cli/parse-opts args cli-options)]

      (cond
        (:help options) (console/exit  0 (-> summary usage color/white))
        errors          (console/exit -2 (error-msg errors)))

      (when (:quiet options)
        (alter-var-root #'console/*not-quiet* (constantly false)))

      (run options))

    (catch Exception e
      (when (env/env :dev)
        (.printStackTrace e))
      (console/exit -1 (str "\nA fatal error occured! (" (.getMessage e) ")")))

    (finally
      (shutdown-agents))))
