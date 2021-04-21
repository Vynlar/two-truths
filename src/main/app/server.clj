(ns app.server
  (:require [mount.core :as mount :refer [defstate]]
            [org.httpkit.server :as http-kit]
            [app.middleware :refer [app]]))

(defstate http-server
  :start
  (let [cfg {:port 8080}]
    (println "starting server!")
    (http-kit/run-server app cfg))
  :stop
  (http-server))