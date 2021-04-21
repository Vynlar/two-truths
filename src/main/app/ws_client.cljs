(ns app.ws-client
  (:require [taoensso.sente :as sente]
            [clojure.core.async :refer [go-loop <!]]))

(let [{:keys [send-fn ch-recv]}
      (sente/make-channel-socket-client! "/chsk" {:type :auto})]
  (def chsk-send! send-fn)
  (def ch-recv ch-recv))

(defn join [room-id]
  (js/console.log "Joining " room-id)
  (chsk-send!
   [:room/join {:room/id room-id}]))

(go-loop []
  (let [{:keys [event]} (<! ch-recv)
        [event-key [_ payload]] event]
        (js/console.log event event-key)
    (case event-key
      :chsk/state (do
                    (js/console.log payload)
                    (if (:first-open? payload)
                      (join "auto-join")
                      (recur)))
      (recur))))


(comment
  (join "room-3"))