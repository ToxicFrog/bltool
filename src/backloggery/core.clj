(ns backloggery.core
  (:require [backloggery.flags :refer :all])
  (:require [backloggery.from.steam :as steam])
  (:require [backloggery.from.backloggery :as backloggery])
  (:gen-class))

; GET /ajax_moregames.php? temp_sys=ZZZ &total=0 &own= &wish= &search= &region_u=0 &user=ToxicFrog
; &unplayed= &alpha= &region= &console= &status= &rating= &aj_id=0 &id=1 comments= HTTP/1.1

(def command-help "
 Command    Desc
 -------    ----
 add        Add games to Backloggery, or write game list to a file.
 edit       Edit game entries already on Backloggery, or delete game entries.
 ")

(defn- add
  "Add games to Backloggery, or write game list to a file."
  [] nil)

(defn- edit
  "Edit game entries already on Backloggery, or delete game entries."
  [] nil)

(defn- show-help
  [help command]
  (println "Unknown command:" command)
  (println "Usage:\n backloggery (add|edit) [flags]")
  (println command-help)
  (println help))

(defn -main
  [& args]
  (let [[opts args help] (getopts args)
        command (first args)]
    (binding [*opts* opts]
      (case command
        "add" (add)
        "edit" (edit)
        (show-help help command)))))
