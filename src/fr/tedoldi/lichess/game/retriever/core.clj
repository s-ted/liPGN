;   Copyright (c)  Sylvain Tedoldi. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.
;

(ns fr.tedoldi.lichess.game.retriever.core
  (:require
    [environ.core :as env]
    [clojure.string :as str]

    [clojure.term.colors :as color]

    [fr.tedoldi.lichess.game.retriever.console :as console]
    [fr.tedoldi.lichess.game.retriever.lichess :as lichess]
    [fr.tedoldi.lichess.game.retriever.game :as game]
    [fr.tedoldi.lichess.game.retriever.pgn :as pgn]
    [fr.tedoldi.lichess.game.retriever.orientdb :as dal]))


(defn- -parse-csv-param [param]
  (->> (str/split (str param) #",")
       (map str/trim)
       (remove str/blank?)
       (map str/lower-case)
       (into #{})))

(defn export! [{:keys [quiet url casual variant speed
                       output store no-sync username
                       refresh-all color user-agent]
                :as options}]
  (if (str/blank? username)
    (throw (Exception. "You must provide a username!"))

    ;else
    (let [dal (dal/->Dal
                {:store           store
                 :empty-at-start? (env/env :empty-db-at-start)})
          out (if output
                (if (= "-" output)
                  *out*
                  output)
                (str username ".pgn"))]
      (dal/init dal)

      (if no-sync
        (-> "By-passing server sync\n" color/yellow console/print-err)
        (game/update-user dal url username user-agent refresh-all))


      (-> (str "Crunching data, this may take a while...\n")
          color/blue
          console/print-err)

      (let [variants (-parse-csv-param variant)
            speeds   (-parse-csv-param speed)

            games (game/username->games
                    dal
                    username
                    #(and
                       (if color
                         (= (str/lower-case (or username ""))
                            (str/lower-case
                              (or
                                (get-in % [:players (keyword color) :userId])
                                "")))
                         true)

                       (if casual
                         true
                         (:rated %))

                       (if (empty? speeds)
                         true
                         (contains? speeds (str/lower-case (:speed %))))

                       (if (empty? variants)
                         (not= "fromPosition" (:variant %))
                         (contains? variants (str/lower-case (:variant %))))))]

        (-> (str "Parsing " (count games) " games...\n")
          color/blue
          console/print-err)

        (->> games
             (map #(pgn/game->pgn %
                                  (select-keys options
                                               [:with-times
                                                :template-pgn
                                                :template-move-pair])))
             clojure.string/join
             (spit out))

        (-> (str "Sent " (count games) " games"
                 " to " (if (= *out* out)
                          "stdout"
                          out) ".\n")

            color/green
            console/print-err)
        games))))
