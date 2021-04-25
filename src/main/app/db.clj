(ns app.db
  (:require [mount.core :refer [defstate]]
            [datascript.core :as d]))

(def schema
  {:user/id {:db/unique :db.unique/identity}
   :room/id {:db/unique :db.unique/identity}
   :room/members {:db/type :db.type/ref
                  :db/cardinality :db.cardinality/many}
   :room/owner {:db/type :db.type/ref
                :db/cardinality :db.cardinality/one}
   :vote/room {:db/type :db.type/ref
               :db/cardinality :db.cardinality/one}
   :vote/user {:db/type :db.type/ref
               :db/cardinality :db.cardinality/one}})

(defstate conn 
  :start (d/create-conn schema))

(defn add-user! [{:keys [user/id]}]
  (d/transact! conn [{:user/id id}]))

(defn add-room! [{:keys [user/id]}]
  (let [room-id (str (java.util.UUID/randomUUID))]
    (d/transact! conn [{:room/id room-id
                        :room/owner [:user/id id]}])
    room-id))

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

(defn get-user-state [payload]
  (let [room-id (:room/id payload)
        user-id (:user/id payload)
        owner-id (d/q '[:find ?uid .
                        :in $ ?rid
                        :where
                        [?r :room/id ?rid]
                        [?r :room/owner ?u]
                        [?u :user/id ?uid]]
                      @conn room-id)
        choice (d/q '[:find ?c .
                      :in $ ?uid ?rid
                      :where
                      [?u :user/id ?uid]
                      [?r :room/id ?rid]
                      [?v :vote/user ?u]
                      [?v :vote/room ?r]
                      [?v :vote/choice ?c]]
                    @conn user-id room-id)]
    {:choice choice
     :submitted? (not (nil? choice))
     :owner? (= user-id owner-id)}))

(defn clear-room [{:room/keys [id]}]
  (let [vote-ids (d/q '[:find [?v ...]
                        :in $ ?rid
                        :where
                        [?r :room/id ?rid]
                        [?v :vote/room ?r]]
                      @conn id)]
    (d/transact! conn
                 (vec (map (fn [vid] [:db.fn/retractEntity vid]) vote-ids)))))

(comment
  @conn

  (clear-room {:room/id "auto-join"})

  (get-members {:room/id "auto-join"})

  (get-user-state {:room/id "auto-join"
                   :user/id (first (get-members {:room/id "auto-join"}))})

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
