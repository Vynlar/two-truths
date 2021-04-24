(ns app.server-main
  (:require app.server
            [mount.core :as mount]))

(defn -main [& args]
  (mount/start))