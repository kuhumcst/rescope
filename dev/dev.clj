(ns dev
  (:require [shadow.cljs.devtools.api :as shadow]))

(defn start
  []
  (shadow/watch :app)
  (shadow/repl :app))
