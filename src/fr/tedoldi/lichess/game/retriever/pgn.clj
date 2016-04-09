(ns fr.tedoldi.lichess.game.retriever.pgn
  (:require
    [environ.core :as env]
    [clojure.string :as str]

    [clostache.parser :as clostache]

    [clj-time.coerce :as c]
    [clj-time.format :as f]

    [fr.tedoldi.lichess.game.retriever.lichess :as lichess]
    [fr.tedoldi.lichess.game.retriever.orientdb :as dal]))


(def ^:private date-formatter (f/formatter "yyyy.MM.dd"))

(defprotocol Move*
  (->str [this]))

(extend-protocol Move*
  String
  (->str [this]
    (str this))

  clojure.lang.IPersistentMap
  (->str [{:keys [move comments]}]
    (str move
         (->> comments
              (map #(str " {" % "}"))
              str/join))))


(defn moves->pgn [moves]
  (->> moves
       (partition 2)
       (reduce
         (fn [[pgn n] [white black]]
           [(str pgn n ". " (->str white) " " (->str black) " ")
            (inc n)])
         ["" 1])
       first))



(defn pgn->str [{:keys [event site timestamp round players
                        variant result moves]
                 :as pgn}]
  (let [black  (:black players)
        white  (:white players)

        date (->> timestamp
                  c/from-long
                  (f/unparse date-formatter))

        template-vars
        (-> pgn
            (select-keys [:event :site :round :variant :result
                          :players])
            (assoc :date  date
                   :moves (moves->pgn moves)))]

    (clostache/render-resource "templates/pgn.mustache" template-vars)))


(defn game->pgn [{:keys [id moves timestamp players winner
                         variant speed url analysis]}]

  (let [result (case winner
                 "black" "0-1"
                 "white" "1-0"
                 nil     "1/2-1/2")
        black  (:black players)
        white  (:white players)

        date (->> timestamp
                  c/from-long
                  (f/unparse date-formatter))]

    (-> {:event id
         :site speed
         :date date
         :round url
         :players {:white {:name (:userId white)
                           :elo  (:rating white)}
                   :black {:name (:userId black)
                           :elo  (:rating black)}}
         :variant variant
         :result  result

         :moves   (str/split moves #"\s")}
        (cond->
          analysis (assoc :analysis
                          (clostache/render-resource "templates/game-analysis.mustache"
                                                     {:white (:analysis white)
                                                      :black (:analysis black)})))
     pgn->str)))
