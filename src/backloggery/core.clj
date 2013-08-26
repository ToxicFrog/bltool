(ns backloggery.core
  (:require [backloggery.flags :refer :all])
  (:require [backloggery.from.steam :as steam])
  (:require [backloggery.from.backloggery :as backloggery])
  (:gen-class))

; GET /ajax_moregames.php? temp_sys=ZZZ &total=0 &own= &wish= &search= &region_u=0 &user=ToxicFrog
; &unplayed= &alpha= &region= &console= &status= &rating= &aj_id=0 &id=1 comments= HTTP/1.1

(register-flags ["--from" "What type of data source to read the games/edits from."])

(def command-help "
 Command    Desc
 -------    ----
 add        Add games to Backloggery, or write game list to a file.
 edit       Edit game entries already on Backloggery, or delete game entries.
 ")

(defn- show-help
  [help command]
  (println "Unknown command:" command)
  (println "Usage:\n backloggery (add|edit) [flags]")
  (println command-help)
  (println help))

(defn- filter-games [bl-games new-games]
  (let [same-name? (fn [x y] (= (:name x) (:name y)))
        new-game? (fn [game] (not-any? (partial same-name? game) bl-games))]
    (filter new-game? new-games)))

(defn- get-games-from [_] (steam/games))

(def commands
  {"add"
   (fn []
     (if-let [from (:from *opts*)]
       ; do stuff
       (let [bl-games (backloggery/games)
             new-games (filter-games bl-games (get-games-from from))]
         (println "BL " (count bl-games))
         (println "NEW" (count new-games))
         (dorun (map println new-games)))
       ; die
       (println "add requires a data source - try '--from (steam|text|edn)'")))
   "edit"
   (fn [] nil)
   })

(defn -main
  [& args]
  (let [[opts args help] (getopts args)
        command (first args)]
    (binding [*opts* opts]
      (if-let [command-fn (commands command)]
              (command-fn)
              (show-help help command)))))
