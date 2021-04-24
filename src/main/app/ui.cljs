(ns app.ui
  (:require [reagent.core :as r]
            [clojure.core.async :refer [go-loop alt!]]
            [app.utils :refer [get-room-name]]
            [app.ws-client :refer [result-ch user-state-ch vote clear-room]]))

(def options ["A" "B" "C"])

(defn get-max-result [results]
  (apply max (map :count results)))

(defn ui-results [results my-choice]
  (let [max-result (or (get-max-result results) 1)]
    [:div {:class "px-4 max-w-md w-full"}
     [:ul {:class "w-full space-y-3"}
      (for [option options]
        (let [count (or (:count (first (filter
                                        (fn [result] (= (:choice result) option))
                                        results)))
                        0)
              percent (* 100 (/ count max-result))]
          ^{:key option}
          [:li {:class "relative w-full bg-white h-12 flex items-center
                          px-4 text-white font-bold rounded-lg overflow-hidden shadow-lg"}
           [:div {:class "absolute bg-gradient-to-r from-pink-600 to-pink-500 flex items-center
                            h-full w-full top-0 left-0 z-20 transition-all duration-500 px-4"
                  :style {:clip-path (str "polygon(0% 0%, " percent "% 0%, " percent "% 100%, 0% 100%)")}}
            [:span {:class "absolute z-30"}
             [:span  (str option)]
             [:span {:class "z-10 ml-2 opacity-70"} (str count)]
             (when (= my-choice option)
               [:span {:class "ml-2 relative"} "★"])]]

           [:span {:class "absolute z-10 text-black"}
            [:span  (str option)]
            [:span {:class "z-10 ml-2 opacity-70"} (str count)]
            (when (= my-choice option)
              [:span {:class "ml-2 z-10 relative"} "★"])]]))]
     [:div {:class "flex justify-between items-center mt-3"}
      [:div {:class "text-gray-400 text-xs uppercase"} "★ your vote"]
      [:button {:class "uppercase font-bold text-gray-500 text-sm hover:text-red-500"
                :on-click #(clear-room (get-room-name))} "Reset"]]]))

(defn ui-vote [{:keys [on-submit]}]
  [:div.space-y-2.text-gray-500
   [:h1 {:class "font-bold text-center"} "Which is the lie?"]
   [:div {:class "space-x-3"}
    (for [option options]
      ^{:key option}
      [:button {:on-click #(on-submit option)
                :aria-label (str "Vote for option " option)
                :class "bg-pink-500 text-3xl text-white font-bold rounded-lg shadow-lg py-5 px-6
                        hover:bg-pink-600 border-2 border-pink-300"} option])]])

(defonce state (r/atom {:result [] :loaded? false}))

(defn root []
  (go-loop []
    (alt!
      result-ch ([result]
                 (swap! state assoc :result result))
      user-state-ch ([{:keys [user-state]}]
                     (swap! state (fn [s] (-> s
                                              (merge user-state)
                                              (assoc :loaded? true))))))
    (recur))

  (fn []
    [:div {:class "flex min-h-screen items-center justify-center bg-gradient-to-b from-pink-100 via-white to-purple-100"}
    (if-not (:loaded? @state)
      [:div]
      (if (:submitted? @state)
        [ui-results (:result @state) (:choice @state)]
        [ui-vote {:on-submit #(do
                                (vote (get-room-name) %1))}]))]))