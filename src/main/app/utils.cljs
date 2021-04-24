(ns app.utils
  (:require
   [clojure.string :as string]))

(defn get-room-name []
  (string/replace
   (.. js/window -location -pathname)
   #"/room/" ""))