(ns app.db
  (:require [mount.core :refer [defstate]]
            [datascript.core :as d]))

(def schema
  {:user/id {:db/unique :db.unique/identity}
   :room/id {:db/unique :db.unique/identity}
   :room/members {:db/type :db.type/ref
                  :db/cardinality :db.cardinality/many}
   :vote/room {:db/type :db.type/ref
               :db/cardinality :db.cardinality/one}
   :vote/user {:db/type :db.typf
               :db/cardinality :db.cardinality/one}})

(defstate conn 
  :start (d/create-conn schema))

(defn add-user! [{:keys [user/id]}]
  (d/transact! conn [{:user/id id}]))

(defn add-room! []
  (d/transact! conn [{:room/id (str (java.util.UUID/randomUUID))}]))

(defn join-room! [payload]
  (let [room-id (:room/id payload)
        user-id (:user/id payload)]
    (d/transact! conn [{:room/id room-id
                       :room/members [[:user/id user-id]]}])))

(defn vote! [payload]
  (let [room-id (:room/id payload)
        user-id (:user/id payload)
        choice (:choice payload)]
    (d/transact! conn [{:db/id -1
                       :vote/room [:room/id room-id]
                       :vote/choice choice
                       :vote/user [:user/id user-id]}])))

(defn get-results [{:room/keys [id]}]
  (vec
   (d/q '[:find ?c (count ?uid)
          :keys choice count
          :in $ ?rid
          :where
          [?v :vote/room ?rid]
          [?v :vote/choice ?c]
          [?v :vote/user ?uid]]
        @conn [:room/id id])))

(defn get-members [{:room/keys [id]}]
  (d/q '[:find [?uid ...]
         :in $ ?rid
         :where
         [?r :room/id ?rid]
         [?r :room/members ?u]
         [?u :user/id ?uid]]
       @conn id))

(comment
  @conn

  (get-members {:room/id "auto-join"})

  (d/transact! conn [{:user/id "user-id"}
                     {:room/id "room-id"
                      :room/members [[:user/id "user-id"]]}])

  (d/transact! conn [{:user/id "user-id-2"}])

  (do
    (add-room!)
    (add-user! {:user/id "adrian"})
    (add-user! {:user/id "jack"})
    (def room-id (d/q '[:find ?rid .
                        :where
                        [?r :room/id ?rid]]
                      @conn))
    (join-room! {:room/id  room-id
                 :user/id "adrian"})
    (join-room! {:room/id  room-id
                 :user/id "jack"}))

    (vote! {:user/id "adrian" :room/id room-id :choice "C"})
    (vote! {:user/id "jack" :room/id room-id :choice "A"})
    (get-results {:room-id "auto-join"})

  (d/q '[:find [(pull ?r [* {:room/members [*]}]) ...]
         :where
         [?r :room/id _]]
       @conn))
