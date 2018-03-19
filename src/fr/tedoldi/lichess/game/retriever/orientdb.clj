;   Copyright (c)  Sylvain Tedoldi. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.
;

(ns fr.tedoldi.lichess.game.retriever.orientdb
  (:require
    [clj-time.coerce :as coerce]
    [clj-time.core :as t]
    clojure.data
    [clojure.string :as str]
    [clojure.data.json :as json])
  (:import (java.io PrintWriter)

           (com.orientechnologies.orient.core.sql OCommandSQL)
           (com.orientechnologies.orient.core.record.impl ODocument)
           (com.orientechnologies.orient.core.db.document ODatabaseDocumentPool ODatabaseDocumentTx)
           (com.orientechnologies.orient.core.id ORecordId)
           (com.orientechnologies.orient.core.query OQuery)
           (com.orientechnologies.orient.core.sql.query OSQLSynchQuery)))



(defprotocol LichessDal

  (username->games [this username])
  (username->last-game [this username]))






(defprotocol ReadDal

  (init
    [this]
    "Initialize the db")

  (find-all
    [this collection]
    [this collection request]
    "Retrieves all item ids")

  (find-all-deep
    [this collection]
    [this collection request]
    "Retrieves all items")

  (find-by-id
    [this collection id]
    "Retrieves an item by its ID")

  (existed?
    [this collection id]
    "Returns whether an item by this ID ever existed")

  (new?
    [this collection id]
    "Returns whether an item by this ID is newly created"))


(defprotocol CrudDal

  (create-with-id!
    [this collection id item]
    "Stores a new item and return its ID")

  (create
    [this collection item]
    "Stores a new item (generating its ID) and return its ID")

  (delete!
    [this collection id]
    "Deletes an item by its ID")

  (update!
    [this collection id item]
    "Updates an item by its ID")

  (patch!
    [this collection id item]
    "Patches an item by its ID"))
























(defn pool->db
  ([]
   (pool->db nil))

  ([{:keys [store login password]
     :or {store    "memory:dev"
          login    "admin"
          password "admin"}}]
   (pool->db store login password))

  ([store login password]
   (-> (ODatabaseDocumentPool/global)
       (.acquire store login password))))


(declare prop-in prop-out)
(declare document?)

(def kw->oclass-name
  (memoize
    (fn [k]
      (if (string? k)
        k
        (str (if-let [n (namespace k)] (str n "_"))
             (name k))))))

(def oclass-name->kw (memoize (fn [o] (keyword (.replace o "_" "/")))))


(deftype CljODoc [^ODocument odoc]
  clojure.lang.IPersistentMap
  (assoc [_ k v] (.field odoc (name k) (prop-in v)) _)
  (assocEx [_ k v] (if (.containsKey _ k)
                     (throw (Exception. "Key already present."))
                     (do (.assoc odoc k v) _)))
  (without [_ k] (.removeField odoc (name k)) _)

  java.lang.Iterable
  (iterator [_] (.iterator (.seq _)))

  clojure.lang.Associative
  (containsKey [_ k] (.containsField odoc (name k)))
  (entryAt [_ k] (if-let [v (.valAt _ k)] (clojure.lang.MapEntry. k v)))

  clojure.lang.IPersistentCollection
  (count [_] (count (.fieldNames odoc)))
  (cons [self o] (doseq [[k v] o] (.assoc self k v)) self)
  (empty [_] (with-meta (CljODoc. (ODocument.)) (meta _)))
  (equiv [_ o] (= odoc (if (instance? CljODoc o) (.-odoc o) o)))

  clojure.lang.Seqable
  (seq [_] (for [k (.fieldNames odoc)]
             (clojure.lang.MapEntry. (keyword k)
                                     (prop-out (.field odoc k)))))

  clojure.lang.ILookup
  (valAt [_ k not-found]
    (or (prop-out (.field odoc (name k)))
        (case k
          :#rid     (.getIdentity odoc)
          :#class   (oclass-name->kw (.field odoc "@class"))
          :#version (.field odoc "@version")
          :#size    (.field odoc "@size")
          :#type    (.field odoc "@type")
          nil)
        not-found))
  (valAt [_ k] (.valAt _ k nil))

  clojure.lang.IFn
  (invoke [_ k] (.valAt _ k))
  (invoke [_ k nf] (.valAt _ k nf))

  clojure.lang.IObj
  (meta [self] (prop-out (.field odoc "__meta__")))
  (withMeta [self new-meta]
    {:pre [(map? new-meta)]}
    (.field odoc "__meta__" (prop-in new-meta)) self)

  java.lang.Object
  (equals [self o] (= (dissoc odoc "__meta__") (if (instance? CljODoc o) (dissoc (.-odoc o) "__meta__") o))))

(defn wrap-odoc "Wraps an ODocument object inside a CljODoc object."
  [odoc]
  (with-meta
    (CljODoc. odoc)
    {:orid     (.getIdentity odoc)
     :oclass   (oclass-name->kw (.field odoc "@class"))
     :oversion (.field odoc "@version")
     :osize    (.field odoc "@size")
     :otype    (.field odoc "@type")}))

(defn prop-in ; Prepares a property when inserting it on a document.
  [x]
  (cond
    (keyword? x) (str x)
    (set? x) (->> x (map prop-in) java.util.HashSet.)
    (map? x) (apply hash-map (mapcat (fn [[k v]] [(str k) (prop-in v)]) x))
    (coll? x) (map prop-in x)
    (document? x) (.-odoc x)
    :else x))

(defn prop-out ; Prepares a property when extracting it from a document.
  [x]
  (cond
    (and (string? x) (.startsWith x ":")) (keyword (.substring x 1))
    (instance? java.util.Set x) (->> x (map prop-out) set)
    (instance? java.util.Map x) (->> x (into {}) (mapcat (fn [[k v]] [(prop-out k) (prop-out v)])) (apply hash-map))
    (instance? java.util.List x) (->> x (map prop-out))
    :else x))



(defn document? [x] (instance? CljODoc x))




(extend-protocol json/JSONWriter

  org.joda.time.DateTime
  (-write [in ^PrintWriter out]
    (.print out (str "\"" (coerce/to-string in) "\"")))

  java.util.UUID
  (-write [in ^PrintWriter out]
    (.print out (str "\"" (str in) "\"")))

  CljODoc
  (-write [in ^PrintWriter out]
    (.print out (.toJSON (.-odoc in)))))




(defn- -execute [db command & args]
  (->> command
       OCommandSQL.
       (.command db)
       (#(.execute % args))))



(defn- initialize-blank-db [Dal]
  (let [db (pool->db (:config Dal))]
    (try
      (-execute db "CREATE CLASS user")
      (-execute db "CREATE PROPERTY user._id STRING")
      (-execute db "CREATE PROPERTY user.username STRING")
      (create-with-id!
        Dal
        "user"
        "dummy"
        {:username "dummy"})
      (-execute db "CREATE INDEX user._id ON user (_id) UNIQUE")

      (-execute db "CREATE CLASS game")
      (-execute db "CREATE PROPERTY game._id STRING")
      (-execute db "CREATE PROPERTY game.username STRING")
      (create-with-id!
        Dal
        "game"
        "dummy"
        {:username "dummy"})
      (-execute db "CREATE INDEX game._id ON game (_id) UNIQUE")

      (finally
        (.close db)))))

(defn- -update-db [Dal]
  (let [db (pool->db (:config Dal))]
    (try
      (-execute db "DROP INDEX user.username")
      (-execute db "DROP INDEX game.username")

      (finally
        (.close db)))))





(defn- ODocument->map [^ODocument odoc]
  (with-meta
    (->> odoc
         wrap-odoc
         (into {})
         (#(dissoc % :__meta__))
         clojure.walk/keywordize-keys)
    {:orid     (.getIdentity odoc)
     :oclass   (oclass-name->kw (.field odoc "@class"))
     :oversion (.field odoc "@version")
     :osize    (.field odoc "@size")}))



(defn- find-map-by-id [Dal collection id]
  (let [query (OSQLSynchQuery. (str "SELECT FROM " (kw->oclass-name collection)
                                    " WHERE _id = '" id "'")
                               1)
        db    (pool->db (:config Dal))]
    (try

      (-> db
          .activateOnCurrentThread
          (.query ^OSQLSynchQuery query (to-array nil))
          first
          (#(when %
              (when-not (:sentinel %)
                (ODocument->map %)))))

      (catch java.lang.IllegalArgumentException e nil)

      (finally
        (.close db)))))


(def ^:private undefined-ORecordId
  (ORecordId. -1 -1))

(defn- -username->games-paginated
  ([dal username] (-username->games-paginated dal
                                              username
                                              undefined-ORecordId))

  ([dal username lower-rid]
   (let [page-size 100
         query     (OSQLSynchQuery. (str "SELECT FROM game"
                                         " WHERE "
                                         "       userId.toLowerCase() = ?"
                                         "   AND @rid > " lower-rid
                                         " LIMIT " page-size)
                                    page-size)
         db        (pool->db (:config dal))]
     (try

       (let [items (seq
                     (-> db
                         .activateOnCurrentThread

                         (.query ^OSQLSynchQuery query (to-array [(str/lower-case username)]))))]
         (when items
           (cons items
                 (-username->games-paginated
                   dal
                   username
                   (.getIdentity (last items))))))

       (finally
         (.close db))))))

(defn- -find-all-deep-paginated
  ([dal collection] (-find-all-deep-paginated dal
                                              collection
                                              undefined-ORecordId))

  ([dal collection lower-rid]
   (let [page-size 100
         query     (OSQLSynchQuery. (str "SELECT FROM " (kw->oclass-name collection)
                                         " WHERE @rid > " lower-rid
                                         " LIMIT " page-size)
                                    page-size)
         db        (pool->db (:config dal))]
     (try

       (let [items (seq
                     (-> db
                         .activateOnCurrentThread

                         (.query ^OSQLSynchQuery query nil)))]
         (when items
           (cons items
                 (-find-all-deep-paginated
                   dal
                   collection
                   (.getIdentity (last items))))))

       (catch java.lang.IllegalArgumentException e nil)

       (finally
         (.close db))))))







(defrecord Dal [config]
  ReadDal

  (init [this]
    (let [{:keys [store login password]
           :or {store    "memory:dev"
                login    "admin"
                password "admin"}}
          config]

      (if (.exists (ODatabaseDocumentTx. store))
        (when (:empty-at-start? config)
          (doto (ODatabaseDocumentTx. store)
            (.open login password)
            .drop
            .create)
          (initialize-blank-db this))

        ; else
        (do
          (.create (ODatabaseDocumentTx. store))
          (initialize-blank-db this))))

    (-update-db this)

    this)

  (find-all [this collection request]
    (vals
      (map :_id
           (let [db (pool->db config)]
             (try

               (.activateOnCurrentThread db)

               (->> collection
                    kw->oclass-name

                    (.browseClass db)
                    iterator-seq

                    (map ODocument->map)

                    (filter #(not (second (clojure.data/diff % request))))
                    (remove :sentinel)

                    (into []))

               (catch java.lang.IllegalArgumentException e [])

               (finally
                 (.close db)))))))

  (find-all-deep
    [this collection]
    (find-all-deep this collection nil))

  (find-all-deep
    [this collection request]


    (flatten
      (lazy-cat
        (map
          (fn [raw-items]
            (->> raw-items
                 (map ODocument->map)
                 (remove :sentinel)

                 (filter #(not (second (clojure.data/diff % request))))))
          (-find-all-deep-paginated this collection)))))


  (find-by-id
    [this collection id]
    (when id
      (when-let [result (find-map-by-id this collection id)]
        (when-not (:sentinel result)
          result))))

  (existed?
    [this collection id]
    (:sentinel (find-map-by-id this collection id)))

  (new?
    [this collection id]
    (nil? (find-by-id this collection id)))

  CrudDal

  (create-with-id!
    [this collection id item]
    (let [id        id
          now       (t/now)
          new-item  (merge
                      {:createdAt now}
                      item
                      {:_id       id
                       :updatedAt now})]

      (let [db (pool->db config)]
        (try

          (.activateOnCurrentThread db)

          (-> collection
              kw->oclass-name
              ODocument.

              wrap-odoc

              (merge (clojure.walk/stringify-keys new-item))

              .-odoc
              .save

              wrap-odoc)

          (finally
            (.close db)))
        {:_id id})))

  (create
    [this collection item]
    (create-with-id! this collection (str (java.util.UUID/randomUUID)) item))

  (delete!
    [this collection id]
    (patch! this collection id {:sentinel true}))

  (update!
    [this collection id item]
    (let [now       (t/now)
          new-item  (-> item
                        (merge {:updatedAt now})
                        (dissoc :_id))]

      (let [document (find-by-id this collection id)

            {:keys [orid]}
            (meta document)

            db       (pool->db config)]
        (try

          (doto db
            .activateOnCurrentThread
            .begin
            (.delete orid))

          (create-with-id! this collection id new-item)

          (.commit db)

          (finally
            (.close db))))))

  (patch!
    [this collection id item]
    (let [document (find-by-id this collection id)]
      (update! this collection id (merge document item))))

  LichessDal

  (username->games
    [this username]

    (flatten
      (lazy-cat
        (map
          (fn [raw-items]
            (->> raw-items
                 (map ODocument->map)
                 (remove :sentinel)))
          (-username->games-paginated this username)))))

  (username->last-game
    [this username]

    (let [query     (OSQLSynchQuery. (str "SELECT FROM game"
                                          " WHERE userId.toLowerCase() = ?"
                                          " ORDER BY createdAt DESC"
                                          " LIMIT " 1)
                                     1)
          db        (pool->db config)]
      (try
        (-> db
            .activateOnCurrentThread

            (.query ^OSQLSynchQuery query (to-array [(str/lower-case username)]))
            seq
            first
            (#(when %
                (when-not (:sentinel %)
                  (ODocument->map %)))))

        (catch java.lang.IllegalArgumentException e nil)

        (finally
          (.close db))))))
