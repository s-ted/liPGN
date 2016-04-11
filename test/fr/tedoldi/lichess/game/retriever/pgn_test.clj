(ns fr.tedoldi.lichess.game.retriever.pgn-test
  (:require
    [fr.tedoldi.lichess.game.retriever.pgn :refer :all]

    [clojure.string :as str]
    [clojure.java.io :as io]

    [midje.sweet :refer :all]))

(extend-protocol Move*
  midje.data.metaconstant.Metaconstant
  (->str [this]
    (str this)))

(facts "PGN generation"

      (fact "Basic PGN"
            (pgn->str {:id      ..event..
                       :moves   [..m-w-1.. ..m-b-1..]
                       :speed   ..site..
                       :date    ..date..
                       :url     ..round..
                       :players {:white {:name ..white-player-name..
                                         :elo  ..white-player-elo..}
                                 :black {:name ..black-player-name..
                                         :elo  ..black-player-elo..}}
                       :result  ..result..
                       :variant ..variant..}
                      nil)
            => (slurp (io/resource "basic.pgn")))

      (fact "PGN with comments"
            (pgn->str {:id      ..event..
                       :moves   [{:move     ..m-w-1..
                                  :comments [..comment-m-w-1-1..
                                             ..comment-m-w-1-2..]
                                  :time     10.0}
                                 {:move     ..m-b-1..
                                  :comments [..comment-m-b-1-1..]}]
                       :speed   ..site..
                       :date    ..date..
                       :url     ..round..
                       :players {:white {:name ..white-player-name..
                                         :elo  ..white-player-elo..}
                                 :black {:name ..black-player-name..
                                         :elo  ..black-player-elo..}}
                       :result  ..result..
                       :variant ..variant..}
                      nil)
            => (slurp (io/resource "commented.pgn"))))
