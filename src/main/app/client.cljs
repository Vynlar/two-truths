(ns app.client
  (:require [app.ws-client :as ws]
            [app.ui :as ui]
            [reagent.dom :as dom]))

(defn ^:dev/after-load start []
  (dom/render [ui/root {:x (js/Date.now)}] (.getElementById js/document "root"))
  #_(ws/join "the-room"))

(defn init []
  (start))