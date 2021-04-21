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
                                  {:csrf-token-fn nil
                                   :user-id-fn (fn [_] (java.util.UUID/randomUUID))})]
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

(def app
  (-> app-routes
      wrap-keyword-params
      wrap-params
      wrap-session
      (wrap-resource "public")))


(defstate cancel-ch
  :start (chan))

(defstate event-processor
  :start
  (go-loop []
    (alt!
      cancel-ch :noop
      ch-chsk ([{:keys [event uid]}]
               (println "Event received")
               (let [[event-key event-payload] event]
                 (case event-key
                   :room/join (do
                                (println "Processing" event-key)
                                (db/join-room! {:room/id (:room/id event-payload)
                                                :user/id uid}))

                   :chsk/uidport-open (do (println "go-loop :chsk/uidport-open" uid)
                                          (db/add-user! {:user/id uid}))


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