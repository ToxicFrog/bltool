(ns bltool.core
  (:require [bltool.data :as data])
  (:require [bltool.flags :refer :all])
  (:require [clojure.java.io :as io])
  (:require [clojure.core.typed :as t :refer [fn> ann]])
  (:require [slingshot.slingshot :refer [try+]])
  (:gen-class))

; GET /ajax_moregames.php? temp_sys=ZZZ &total=0 &own= &wish= &search= &region_u=0 &user=ToxicFrog
; &unplayed= &alpha= &region= &console= &status= &rating= &aj_id=0 &id=1 comments= HTTP/1.1

(register-flags ["--help"
                 "Show detailed help. Try 'bltool --help (formats|usage)'"]
                ["--[no-]filter"
                 "Filter out games already on Backloggery. Requires loading the game list from Backloggery."
                 :default false]
                ["--input"
                 "For file-based formats, read input from this file. '-' means stdin."
                 :default "-"]
                ["--output"
                 "For file-based formats, write output to this file. '-' means stdout."
                 :default "-"]
                ["--name"
                 "Include only games where the name contains this string."])



(t/ann help (t/Map String String))
(def help
  {"formats"
   "    This tool can read and write a variety of sources. The --from and --to
    options control what formats it reads and write; the --input and --output
    options control what file it writes to or reads from, for formats that are
    stored in files.
   
    Format       RW  Desc
    ------       --  ----
    backloggery  RW  Backloggery game library. When writing, equivalent to bl-add.
    bl-wishlist  R   Backloggery game wishlist.
    bl-add        W  Add new games to Backloggery.
   *bl-edit      RW  Edit existing games, overwriting current information.
                     In read mode, gets all game info, not just basic info (slow!)
    bl-delete     W  Delete all listed games; all properties except ID are ignored.
    steam        R   Game list from Steam Community
   *html          W  HTML file that can submit changes to Backloggery
    text         RW  User-editable plain text
    edn          RW  Machine-readable EDN
   
   * Not yet implemented"
    
   "usage"
   (str "Usage: bltool <command> [<args>]\n\n" (last (getopts [])))
   })

(t/def-alias BLGame '{:id String :name String :platform String :progress String})
(t/def-alias BLGameList (t/Vec BLGame))

(t/ann filter-games [BLGameList -> BLGameList])
(defn- filter-games [bl-games new-games]
  (let [same-name? (fn [x y] (= (:name x) (:name y)))
        new-game? (fn [game] (not-any? (partial same-name? game) bl-games))
        name-ok? (fn [game] (.contains (:name game) (:name *opts*)))]
    (cond->> new-games
             true (filter new-game?)
             (:name *opts*) (filter name-ok?))))

; The actual work happens here
(t/ann execute [Nothing -> Nothing])
(defn- execute []
  (let [in-games (data/read-games (:from *opts*))
        bl-games (cond
                   (not (:filter *opts*)) []
                   (= "backloggery" (:from *opts*)) in-games
                   :else (data/read-games "backloggery"))
        out-games (filter-games bl-games in-games)]
    (printf "Read %d games in %s format.\n" (count in-games) (:from *opts*))
    (if (:filter *opts*)
      (printf "After filtering, %d remain.\n" (count out-games)))
    (printf "Writing game list to %s.\n" (:to *opts*))
    (data/write-games (:to *opts*) out-games)))

(t/ann resolve-io [t/Map -> t/Map])
(defn- resolve-io
  "Turn the results of --input and --output flags into actual streams."
  [opts]
  (conj opts
        {:input (if (= "-" (:input opts))
                  *in*
                  (io/reader (:input opts)))
         :output (if (= "-" (:output opts))
                   *out*
                   (io/writer (:output opts)))}))

(t/ann show-help [String -> Nothing])
(defn- show-help [section]
  (let [text (or (help section) (help "usage"))]
    (println text)))

(t/ann -main [(t/Vec String) -> Nothing])
(defn -main
  [& argv]
  (let [[opts args _] (getopts argv)]
    (binding [*opts* (resolve-io opts)]
      (try+
        (if (contains? opts :help)
          (show-help (:help opts))
          (execute))
        (catch String _
          (println (:object &throw-context)))
        (catch Object _
          (println (:message &throw-context))
          (dorun (map println (:stack-trace &throw-context)))))
      (.flush (:output *opts*)))))
