(ns app.ui
  (:require [reagent.core :as r]
            [clojure.core.async :refer [go-loop <!]]
            [app.ws-client :refer [result-ch vote]]))

(defn get-max-result [results]
  (apply max (map :count results)))

(defn ui-results [results my-choice]
  (let [max-result (get-max-result results)]
    [:div {:class "max-w-md w-full"}
     [:ul {:class "w-full space-y-3"}
      (map (fn [{:keys [choice count]}]
             ^{:key choice}
             [:li {:class "relative w-full bg-white py-3 
                          px-4 text-white font-bold rounded-lg overflow-hidden shadow-lg"}
              [:div {:class "absolute bg-gradient-to-r from-pink-600 to-pink-500 
                            h-full w-full top-0 left-0 z-10 transition duration-500"
                     :style {:transform (str "scaleX(" (/ count max-result) ")")
                             :transform-origin "0 0"}}]


              [:span {:class "relative z-20"}
               [:span  (str choice)]
               [:span {:class "z-10 ml-2 opacity-70"} (str count)]
               (if (= my-choice choice)
                 [:span {:class "ml-2 z-10 relative"} "★"])]])
           (sort-by :choice results))]
     [:div {:class "text-gray-400 text-xs uppercase mt-3"} "★ your vote"]]))

(defn ui-vote [{:keys [on-submit]}]
  [:div.space-y-2.text-gray-500
   [:h1 {:class "font-bold text-center"} "Which is the lie?"]
   [:div {:class "space-x-3"}
    (for [option ["A" "B" "C"]]
      ^{:key option}
      [:button {:on-click #(on-submit option)
                :aria-label (str "Vote for option " option)
                :class "bg-pink-500 text-3xl text-white font-bold rounded-lg shadow-lg py-5 px-6
                        hover:bg-pink-600 border-2 border-pink-300"} option])]])

(defonce state (r/atom {:result [] :submitted? false}))

(defn root []
  (go-loop []
    (let [result (<! result-ch)]
      (swap! state assoc :result result))
    (recur))

  (fn []
    [:div {:class "flex min-h-screen items-center justify-center bg-gradient-to-b from-pink-100 via-white to-purple-100"}
     (if (:submitted? @state)
       [ui-results (:result @state) (:my-choice @state)]
       [ui-vote {:on-submit #(do
                               (vote "auto-join" %1)
                               (swap! state merge {:submitted? true :my-choice %1}))}])]))