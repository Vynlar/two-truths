(ns development
  (:require [mount.core :as mount]
            [clojure.tools.namespace.repl :as tools-ns]
            app.server))

(defn start []
  (mount/start))

(defn stop []
  (mount/stop))

(defn restart []
  (stop)
  (tools-ns/refresh :after 'development/start))

(comment
  (start)
  (stop)
  (restart)
  )