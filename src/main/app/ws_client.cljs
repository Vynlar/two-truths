(ns app.ws-client
  (:require [taoensso.sente :as sente]
            [clojure.core.async :refer [go-loop go chan >! <!]]))

(let [{:keys [send-fn ch-recv]}
      (sente/make-channel-socket-client! "/chsk" {:type :auto})]
  (def chsk-send! send-fn)
  (def ch-recv ch-recv))

(def result-ch (chan))
(def user-state-ch (chan))

(defn handle-user-state-reply [reply]
  (when (sente/cb-success? reply)
    (js/console.log "Got user state" reply)
    (go (>! user-state-ch reply))))

(defn join [room-id]
  (js/console.log "Joining " room-id)
  (chsk-send!
   [:room/join {:room/id room-id}]
   5000
   handle-user-state-reply))

(defn vote [room-id choice]
  (js/console.log "Sending vote: " choice " in room " room-id)
  (chsk-send!
   [:room/vote {:room/id room-id :choice choice}]
   5000
   handle-user-state-reply))


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