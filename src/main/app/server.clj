(ns app.server
  (:require [mount.core :as mount :refer [defstate]]
            [org.httpkit.server :as http-kit]
            [app.middleware :refer [middleware]]))

(defstate http-server
  :start
  (let [cfg {:port (Integer/parseInt (or (System/getenv "PORT") "8080"))}]
    (println "starting server on port " (:port cfg))
    (http-kit/run-server middleware cfg))
  :stop
  (http-server))