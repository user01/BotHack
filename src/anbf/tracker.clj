; tracking and pairing monsters frame-to-frame

(ns anbf.tracker
  (:require [anbf.position :refer :all]
            [anbf.dungeon :refer :all]
            [clojure.tools.logging :as log]))

(defn- track-transfer [game old-monster monster]
  (log/debug "transfer:" \newline old-monster "to" \newline monster)
  (update-curlvl-monster game monster into ; TODO type
                         (select-keys old-monster [:peaceful :cancelled])))

(defn track-monsters
  "Try to transfer monster properties from the old game snapshot to the new, even if the monsters moved slightly."
  [new-game old-game]
  (if (not= (-> old-game :dungeon :dlvl)
            (-> new-game :dungeon :dlvl))
    new-game ; TODO track stair followers?
    (let [old-monsters (vals (-> old-game :dungeon curlvl :monsters))]
      (loop [res new-game
             new-monsters (vals (-> new-game :dungeon curlvl :monsters))
             dist 0
             ignored-new #{}
             ignored-old #{}]
        (if (> 3 dist)
          (if-let [m (first (remove (comp ignored-new position) new-monsters))]
            (if-let [candidates (seq (->> old-monsters
                                          (remove (comp ignored-old position))
                                          (filter
                                            (fn candidate? [n]
                                              (and (= (:glyph m) (:glyph n))
                                                   (= (:color m) (:color n))
                                                   (= dist (distance m n)))))))]
              (if (next candidates) ; ignore ambiguous cases
                (recur res (rest new-monsters) dist
                       (conj ignored-new (position m)) ignored-old)
                (recur (track-transfer res (first candidates) m)
                       (rest new-monsters) dist
                       (conj ignored-new (position m))
                       (conj ignored-old (position (first candidates)))))
              (recur res (rest new-monsters) dist ignored-new ignored-old))
            (recur res (vals (-> new-game :dungeon curlvl :monsters))
                   (inc dist) ignored-new ignored-old))
          res))))) ; TODO remember/unremember unignored old/new
