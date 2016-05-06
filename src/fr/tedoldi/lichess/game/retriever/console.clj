;   Copyright (c)  Sylvain Tedoldi. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.
;

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
