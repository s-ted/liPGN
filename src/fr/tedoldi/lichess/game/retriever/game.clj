;   Copyright (c)  Sylvain Tedoldi. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.
;

(ns fr.tedoldi.lichess.game.retriever.game
  (:require
    [environ.core :as env]
    [clojure.string :as str]

    [clojure.term.colors :as color]

    [clj-time.coerce :as c]
    [clj-time.format :as f]

    [fr.tedoldi.lichess.game.retriever.console :as console]
    [fr.tedoldi.lichess.game.retriever.lichess :as lichess]
    [fr.tedoldi.lichess.game.retriever.orientdb :as dal]))

(declare username->games)

(defn last-game-timestamp [dal username]
  (:timestamp (dal/username->last-game dal username)))

(defn update-user [dal url username]
  (-> (str "Updating user " username " from server.\n")
      color/blue
      console/print-err)

  (let [f (if (dal/find-by-id dal "user" username)
            dal/update!
            dal/create-with-id!)]

    (->> username
        (lichess/username->user url)
        (f dal "user" username))

    (let [last-game-timestamp (last-game-timestamp dal
                                                   username)]
      (-> (str "Retrieving user " username " games from server since "
               (if last-game-timestamp
                 (->> last-game-timestamp
                      c/from-long
                      (f/unparse
                        (f/formatters :basic-date-time)))
                 "day one")
               ".\n")
          color/blue
          console/print-err)

      (let [games (lichess/username->games url
                                           username
                                           last-game-timestamp)]
        (-> (str "Found " (count games) " new games.\n")
            color/blue
            console/print-err)

        (doseq [{:keys [id] :as game} games]
          (let [f (if (dal/find-by-id dal "game" id)
                    dal/update!
                    dal/create-with-id!)]
            (f dal "game" id (assoc game
                                    :userId username))))))))

(defn username->user [dal username]
  (dal/find-by-id dal "user" username))

(defn username->games
  ([dal username]
   (dal/username->games dal username))

  ([dal username custom-filter]
   (->> (username->games dal username)
        (filter custom-filter))))
