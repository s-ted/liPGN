;   Copyright (c)  Sylvain Tedoldi. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.
;

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
