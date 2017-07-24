;   Copyright (c)  Sylvain Tedoldi. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.
;

(ns fr.tedoldi.lichess.game.retriever.lichess
  (:require
    [clj-http.client :as client]
    [cheshire.core :as json]
    [clojure.walk :refer [keywordize-keys]]

    [taoensso.timbre :refer [spy]]

    [clojure.term.colors :as color]
    [fr.tedoldi.lichess.game.retriever.console :as console]))

(def ^:private nice-downloader-waiting-time 1000)
(def ^:private even-nicer-downloader-waiting-time 120000)

(defn username->user [url username user-agent]
  (let [url (str url "/user/" username)]
    (try
      (-> url
          (client/get {:client-params {"http.useragent" user-agent}})

          :body

          json/parse-string
          keywordize-keys)
      (catch Exception e
        (throw
          (if (= 404 (:status (ex-data e)))
            (Exception. (str "No such user " username " (error 404)"))
            ;else
            e))))))


(defn- -games-url->games [url user-agent max-per-page page total-pages]
  (-> (str "Downloading page " (inc page) "/" total-pages ".\n")
      color/cyan
      console/print-err)

  (when (pos? page)
    (Thread/sleep nice-downloader-waiting-time))

  (try
    (-> url
        (client/get {:client-params {"http.useragent" user-agent}
                     :query-params
                     {:nb             max-per-page
                      :page           (inc page)
                      :with_moves     1
                      :with_movetimes 1
                      :with_opening   1
                      :with_analysis  1}})

        :body

        json/parse-string
        keywordize-keys

        :currentPageResults)
    (catch Exception e
      (if (= 429 (:status (ex-data e)))
        (do
          (-> (str "Lichess is rate-limiting us, waiting "
                   (/ even-nicer-downloader-waiting-time
                      1000)
                   "s...\n")
              color/cyan
              console/print-err)
          (Thread/sleep even-nicer-downloader-waiting-time)
          (-games-url->games url user-agent max-per-page page total-pages))

        ; else
        (throw e)))))

(defn username->games [url username user-agent since-createdAt updater!]
  (let [max-per-page 100
        url          (str url "/user/" username "/games")

        nb-results   (-> url
                         (client/get {:client-params {"http.useragent" user-agent}
                                      :query-params
                                      {:nb   1
                                       :page 1}})

                         :body

                         json/parse-string
                         keywordize-keys

                         :nbResults)

        total-pages  (-> nb-results
                         (/ max-per-page)
                         Math/ceil
                         int)]

    (-> (str "Found " nb-results " games ~ " total-pages " pages to sync.\n")
        color/cyan
        console/print-err)

    (let [page-nb->game (comp

                          (filter (fn [{:keys [createdAt]}]
                                    (or
                                      (nil? since-createdAt)
                                      (and createdAt
                                           (> createdAt since-createdAt)))))

                          (map #(-games-url->games url user-agent max-per-page % total-pages))

                          (map #(map updater! %)))]
      (flatten
        (lazy-seq
          (sequence page-nb->game
                    (range total-pages)))))))
