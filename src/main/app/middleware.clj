(ns app.middleware
  (:require [compojure.core :refer [defroutes GET POST]]
            [compojure.route :as route]
            [taoensso.sente :as sente]
            [mount.core :refer [defstate]]
            [ring.middleware.keyword-params :refer [wrap-keyword-params]]
            [clojure.core.async :refer [go go-loop <! <!! >! >!! chan alt!]]
            [ring.middleware.params :refer [wrap-params]]
            [ring.middleware.session :refer [wrap-session]]
            [ring.middleware.resource :refer [wrap-resource]]
            [app.db :as db]
            [taoensso.sente.server-adapters.http-kit :refer (get-sch-adapter)]))

(let [{:keys [ch-recv send-fn connected-uids
              ajax-post-fn ajax-get-or-ws-handshake-fn]}
      (sente/make-channel-socket! (get-sch-adapter)
                                  {:csrf-token-fn nil})]
  (def ring-ajax-post ajax-post-fn)
  (def ring-ajax-get-or-ws-handshake ajax-get-or-ws-handshake-fn)
  (def ch-chsk ch-recv)
  (def chsk-send! send-fn)
  (def connected-uids connected-uids))

(defroutes app-routes
  (GET "/" [] "WOW")
  (GET "/chsk" req (ring-ajax-get-or-ws-handshake req))
  (POST "/chsk" req (ring-ajax-post req))
  (route/not-found "not found!"))

(defn wrap-add-uid [handler]
  (fn [request]
    (let [response (handler request)
          old-uid (get-in request [:session :uid])]

      (if old-uid
        response
        (assoc-in response [:session] {:uid (str (java.util.UUID/randomUUID))})))))

(defstate middleware
  :start
  (-> app-routes
      (wrap-resource "public")
      wrap-add-uid
      wrap-keyword-params
      wrap-params
      wrap-session))

(defstate cancel-ch
  :start (chan))

(defstate vote-ch
  "Push events onto this channel to vote.

   Required keys: #{:room/id :user/id :choice}"
  :start
  (chan))

(defstate broadcast-ch
  :start
  (chan))

(defn broadcast! [uids payload]
  (println "Broadcasting" payload "to" uids)
  (doseq [uid uids]
    (println "Sending to " uid payload)
    (chsk-send! uid payload)))

(defstate broadcast-processor
  :start
  (go-loop []
    (alt!
      cancel-ch :noop
      broadcast-ch ([payload]
                    (println "Broadcasting" payload)
                    (broadcast! (db/get-members payload)
                                [:room/result (db/get-results payload)])
                    (recur)))))

(defstate event-processor
  :start
  (go-loop []
    (alt!
      cancel-ch :noop
      ch-chsk ([{:keys [event uid ?reply-fn]}]
               (let [[event-key event-payload] event
                     room-id (:room/id event-payload)]
                 (println "Processing event: " event-key)
                 (case event-key
                   :room/join
                   (do
                     (db/join-room! {:room/id room-id
                                     :user/id uid})
                     (when ?reply-fn
                       (?reply-fn {:user-state (db/get-user-state {:user/id uid
                                                                   :room/id room-id})}))
                     (>! broadcast-ch {:room/id room-id}))


                   :room/vote
                   (let [payload {:user/id uid
                                  :room/id room-id
                                  :choice (:choice event-payload)}]
                     (db/vote! payload)
                     (>! broadcast-ch payload)
                     (when ?reply-fn
                       (?reply-fn {:user-state (db/get-user-state {:user/id uid
                                                                   :room/id room-id})})))



                   :chsk/uidport-open
                   (db/add-user! {:user/id uid})

                   (println "Skipped: " event-key))
                 (recur))))))


(comment @connected-uids

         (go-loop []
           (alt!
             cancel-ch :noop
             ch-chsk ([event]
                      (let [{:keys [event]} event]
                        (println "loopin:" event)
                        (recur)))))



         (>!! cancel-ch {})
         :stop
         (>!! cancel-ch {})


         (reset! db/db {:wow 3})
         @db/db

         ring-ajax-get-or-ws-handshake)