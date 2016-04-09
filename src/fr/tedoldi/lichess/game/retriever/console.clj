(ns fr.tedoldi.lichess.game.retriever.console
  (:require [clojure.term.colors :as color]))

(def ^:dynamic *not-quiet* true)

(defn print-err
  "Print a message to stderr"
  {:added "1.0"}

  ([msg]
   (print-err msg false))

  ([msg force?]
   (when (or force?
             *not-quiet*)
     (binding [*out* *err*]
       (print msg)
       (flush)))))

(defn exit
  "Exit the program with the given status code and message"
  {:added "1.0"}
  [status msg]
  (do
    (print-err (color/red msg \newline) true)
    (System/exit status)))
