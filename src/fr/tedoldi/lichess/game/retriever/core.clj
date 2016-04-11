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
                       color]
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
        (-> "By-passing sever sync\n" color/yellow console/print-err)
        (game/update-user dal url username))


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
                         (= username (get-in % [:players (keyword color) :userId]))
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
             (pmap #(pgn/game->pgn %
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
