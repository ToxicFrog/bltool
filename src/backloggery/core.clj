(ns backloggery.core
  (:require [backloggery.flags :refer :all])
  (:require [backloggery.data :as data])
  (:require [slingshot.slingshot :refer [try+]])
  (:gen-class))

; GET /ajax_moregames.php? temp_sys=ZZZ &total=0 &own= &wish= &search= &region_u=0 &user=ToxicFrog
; &unplayed= &alpha= &region= &console= &status= &rating= &aj_id=0 &id=1 comments= HTTP/1.1

(register-flags ["--help"
                 "Show detailed help. Try 'backloggery --help (commands|formats|usage)"]
                ["--[no-]filter"
                 "When adding, filter out games already on Backloggery."
                 :default true])

(def help
  {"commands"
   "Command    Desc
    -------    ----
    add        Add games to Backloggery, or write game list to a file.
    edit       Edit game entries already on Backloggery, or delete game entries."   
    
   "formats"
   "This tool can read and write a variety of sources. The --from and --to
    options control what formats it reads and write; the --input and --output
    options control what file it writes to or reads from, for formats that are
    stored in files.
   
    Format       RW  Desc
    ------       --  ----
    backloggery  RW  Backloggery game library
    steam        R   Game list from Steam Community
    html          W  HTML file that can submit to Backloggery
    text         RW  User-editable plain text
    edn          RW  Machine-readable EDN"
    
   "usage"
   (str "Usage: backloggery <command> [<args>]\n\n" (last (getopts [])))
   })

(defn- filter-games [bl-games new-games]
  (let [same-name? (fn [x y] (= (:name x) (:name y)))
        new-game? (fn [game] (not-any? (partial same-name? game) bl-games))]
    (filter new-game? new-games)))

(def commands
  {"add"
   (fn []
     (let [other-games (data/read-games (:from *opts*))
           bl-games (data/read-games "backloggery")
           new-games (filter-games bl-games other-games)]
       (println "BL " (count bl-games))
       (println "NEW" (count new-games))
       (dorun (map println new-games))))
   "edit"
   (fn [] nil)
   "help"
   (fn []
     (let [section (:help *opts*)
           text (or (help section) (help "usage"))]
       (println text)))
   })

(defn -main
  [& args]
  (let [[opts args _] (getopts args)
        command (if (:help opts) "help" (first args))]
    (binding [*opts* opts]
      (if-let [command-fn (commands command)]
        (try+
          (command-fn)
          (catch Object _
            (println (:object &throw-context))))
        (println (help "usage"))))))
