(ns app.ws-client
  (:require [taoensso.sente :as sente]
            [clojure.core.async :refer [go-loop chan >! <!]]))

(let [{:keys [send-fn ch-recv]}
      (sente/make-channel-socket-client! "/chsk" {:type :auto})]
  (def chsk-send! send-fn)
  (def ch-recv ch-recv))

(defn join [room-id]
  (js/console.log "Joining " room-id)
  (chsk-send!
   [:room/join {:room/id room-id}]))

(defn vote [room-id choice]
  (js/console.log "Sending vote: " choice " in room " room-id)
  (chsk-send!
   [:room/vote {:room/id room-id :choice choice}]))

(def result-ch (chan))

(go-loop []
  (let [{:keys [event]} (<! ch-recv)
        [event-type [event-key payload]] event]
    (js/console.log "Received event" event-type event-key payload)
    (case event-type
      :chsk/state (do
                    (js/console.log payload)
                    (if (:first-open? payload)
                      (join "auto-join")
                      (recur)))
      :chsk/recv (case event-key
                   :room/result (>! result-ch payload)
                   nil)

      nil)

    (recur)))

(comment
  (join "room-3")

  (vote "auto-join" "A"))