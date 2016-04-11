(ns fr.tedoldi.lichess.game.retriever.lichess
  (:require
    [clj-http.client :as client]
    [cheshire.core :as json]
    [clojure.walk :refer [keywordize-keys]]

    [taoensso.timbre :refer [spy]]

    [clojure.term.colors :as color]
    [fr.tedoldi.lichess.game.retriever.console :as console]))

(def ^:private nice-downloader-waiting-time 300)
(def ^:private even-nicer-downloader-waiting-time 30000)

(defn username->user [url username]
  (let [url (str url "/user/" username)]
    (try
      (-> url
          client/get

          :body

          json/parse-string
          keywordize-keys)
      (catch Exception e
        (throw
          (if (= 404 (:status (ex-data e)))
            (Exception. (str "No such user " username " (error 404)"))
            ;else
            e))))))


(defn- -games-url->games [url max-per-page page total-pages]
  (-> (str "Downloading page " (inc page) "/" total-pages ".\n")
      color/cyan
      console/print-err)

  (when (pos? page)
    (Thread/sleep nice-downloader-waiting-time))

  (try
    (-> url
        (client/get {:query-params
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
          (-> (str "Lichess is rate-limiting us, waiting 30s...\n")
              color/cyan
              console/print-err)
          (Thread/sleep even-nicer-downloader-waiting-time)
          (-games-url->games url max-per-page page total-pages))

        ; else
        (throw e)))))

(defn username->games [url username since-timestamp]
  (let [max-per-page 100
        url          (str url "/user/" username "/games")

        nb-results   (-> url
                         (client/get {:query-params
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

                          (filter (fn [{:keys [timestamp]}]
                                    (or
                                      (nil? since-timestamp)
                                      (and timestamp
                                           (> timestamp since-timestamp)))))

                          (map #(-games-url->games url max-per-page % total-pages)))]
      (lazy-cat
        (sequence page-nb->game
                  (range total-pages))))))
