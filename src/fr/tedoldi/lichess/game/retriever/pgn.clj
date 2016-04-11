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
  (->str [{:keys [move comments time]}]
    (let [comments (if time
                     (conj comments
                           (str time "s"))

                     ;else
                     comments)]
      (str move
           (if comments
             (str " {" (str/join " " comments) "}"))))))

(def ^:private -get-file-content
  (memoize
    (fn [file-path]
      (-> file-path
          slurp))))

(defn moves->pgn [moves
                  {:keys [white black] :as players}
                  {:keys [template-move-pair]}]
  (->> moves
       (partition 2)
       (map-indexed
         (fn [n [move-white move-black]]
           (let [template-vars {:n     (inc n)
                                :white (->str move-white)
                                :black (->str move-black)}]
             (if template-move-pair
               (clostache/render (-get-file-content template-move-pair) template-vars)
               (clostache/render-resource "templates/move-pair.mustache" template-vars)))))
       str/join))



(defn pgn->str [{:keys [moves players]
                 :as pgn}
                {:keys [template-pgn]
                 :as options}]
  (let [template-vars
        (-> pgn
            (assoc :moves (moves->pgn moves players options)))]

    (if template-pgn
      (clostache/render (-get-file-content template-pgn) template-vars)
      (clostache/render-resource "templates/pgn.mustache" template-vars))))


(defn game->pgn [{:keys [id moves timestamp players winner
                         variant speed url analysis]}
                 {:keys [with-times] :as options}]

  (let [result (case winner
                 "black" "0-1"
                 "white" "1-0"
                 nil     "1/2-1/2")
        black  (:black players)
        white  (:white players)

        date   (->> timestamp
                    c/from-long
                    (f/unparse date-formatter))

        moves  (str/split moves #"\s")

        display-move-times? (and
                              with-times
                              (= (+
                                  (count (:moveTimes white))
                                  (count (:moveTimes black)))
                                 (count moves)))

        moves   (if display-move-times?
                  (map-indexed
                    (fn [i move]
                      {:move move
                       :time (-> (if (zero? (mod i 2)) white black)
                                 :moveTimes
                                 (nth (/ i 2))
                                 (/ 10)
                                 float)})
                    moves)

                  ;else
                  moves)]
    (-> {:id      id
         :speed   speed
         :date    date
         :url     url
         :players {:white {:name       (:userId white)
                           :elo        (:rating white)
                           :moveTimes  (when display-move-times?
                                         (map #(float (/ % 10))
                                              (:moveTimes white)))}
                   :black {:name       (:userId black)
                           :elo        (:rating black)
                           :moveTimes  (when display-move-times?
                                         (map #(float (/ % 10))
                                              (:moveTimes black)))}}
         :variant variant
         :result  result

         :moves   moves}
        (cond->
          analysis (assoc :analysis
                          (clostache/render-resource "templates/game-analysis.mustache"
                                                     {:white (:analysis white)
                                                      :black (:analysis black)})))
     (pgn->str options))))
