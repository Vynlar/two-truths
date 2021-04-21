(ns app.client
  (:require [app.ws-client :as ws]))

(defn init []
  #_(ws/join "the-room"))