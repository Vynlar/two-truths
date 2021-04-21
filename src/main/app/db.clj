(ns app.db
  (:require [mount.core :refer [defstate]]))

(def initial-db {:room/id {}
                 :user/id {}})

(defn new-db []
  (atom initial-db))

(defstate db :start (new-db))

(defn room-path
  ([id] [:room/id id])
  ([id field] [:room/id id field]))

(defn user-path
  ([id] [:user/id id])
  ([id field] [:user/id id field]))

(defn add-user* [state {:user/keys [id]}]
  (println "add-user* " id)
  (let [new-state (assoc-in state (user-path id) {:user/id id})]
    new-state))

(defn add-user! [payload]
  (println "add-user! " payload)
  (swap! db add-user* payload))

(defn add-room*
  ([state id]
   (assoc-in state (room-path id) {:room/id id
                                   :room/members #{}}))
  ([state]
   (let [id (str (java.util.UUID/randomUUID))]
     (add-room* state id))))


(defn join-room* [state payload]
  (let [user-ident (user-path (:user/id payload))
        room-ident (room-path (:room/id payload))
        room-exists? (not (nil? (get-in state room-ident)))
        members-path (room-path (:room/id payload) :room/members)]

    (if room-exists?
      (update-in state members-path
                 conj user-ident)

      (-> state
          (add-room* (:room/id payload))
          (assoc-in members-path #{user-ident})))))

(defn join-room! [payload]
  (swap! db join-room* payload))

(comment
  (reset! db initial-db)
  @db

  (defn first-room-id* [state]
    (first (keys (get-in state [:room/id]))))

  (swap! db (fn [s]
              (-> s
                  (add-room* "my-id"))))

  (swap! db join-room* {:room/id "my-id"
                        :user/id "my-uid-4"}))