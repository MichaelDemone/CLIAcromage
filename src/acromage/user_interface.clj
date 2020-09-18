(ns acromage.user-interface
  (:require 
    [acromage.cards :as cards]
    [utils.general :as utils]
    [lanterna.terminal :as t]
  )
)


;; Command line
(defn print-stats [player]
  
  (println 
    "Tower: " (player :tower) 
    "\nWall: " (player :wall) 
    "\nGems(+Magic): " (player :gems) "(+" (player :magic) "/turn)"
    "\nBricks(+Quarry): " (player :bricks) "(+" (player :quarry) "/turn)"
    "\nBeasts(+Zoo): " (player :beasts) "(+" (player :zoo) "/turn)"
  )
)

(defn get-card-string [c]
  (if (nil? c)
    "Empty"
    (str
      (c :name) 
      " (" 
      (c :cost) 
      " " 
      (utils/format-keyword (c :type))
      ")\n" (c :description)
    )  
  )
)

(defn print-cards [p]
  (println
    (str "(1)" (utils/array-string (take 12 (repeat "-")))
    "\n" (get-card-string (p :c1))
    "\n\n(2)" (utils/array-string (take 12 (repeat "-")))
    "\n" (get-card-string (p :c2))
    "\n\n(3)" (utils/array-string (take 12 (repeat "-")))
    "\n" (get-card-string (p :c3))
    "\n\n(4)" (utils/array-string (take 12 (repeat "-")))
    "\n" (get-card-string (p :c4))
    "\n\n(5)" (utils/array-string (take 12 (repeat "-")))
    "\n" (get-card-string (p :c5))
    "\n\n" (utils/array-string (take 15 (repeat "-"))))
  )  
)

(defn get-game-change [old-game new-game player-key resource-key]
  (let [
    old (get-in old-game [player-key resource-key])
    new (get-in new-game [player-key resource-key])
    ]
    (if (= old new) "" (str "\t" (utils/format-keyword resource-key) " " old "->" new " (" (if (< 0 (- new old)) "+" "") (- new old) ")\n"))
  )
)

(defn get-turn-change-text [old-game new-game]
  (let [
    players [:player1 :player2]
    resources [:tower :wall :gems :bricks :beasts :magic :quarry :zoo]
    resource-changes (map 
                        (fn [player-key] 
                          (apply str
                            (str (utils/format-keyword player-key) "\n") 
                            (apply str (map 
                              (fn [resource-key] 
                                (get-game-change old-game new-game player-key resource-key)
                              ) 
                              resources
                            ))
                          )
                        ) 
                        players)
  ]
    (apply str resource-changes)
  )
)

(defn print-game-state [game]
  (let [
    p1 (game :player1)
    p2 (game :player2)
    ]

    (println "\nPlayer 2s state:")
    (print-stats p2)

    (println "\n\nIt's player 1s turn!")
    (println "Your current stats:")
    (print-stats p1)
    (println "Your current cards:")
    (print-cards p1)
  )
)
;; End command line

;; Laterna Functions
(defn display-player-info [player position-x position-y term]

  (t/move-cursor term position-x position-y)
  (t/set-bg-color term :blue)
  (t/put-string term (str (get player :gems) "(+ " (get player :magic) "/turn)"))

  (t/move-cursor term position-x (+ position-y 1))
  (t/set-bg-color term :red)
  (t/put-string term (str (get player :bricks) "(+ " (get player :quarry) "/turn)"))

  (t/move-cursor term position-x (+ position-y 2))
  (t/set-bg-color term :green)
  (t/put-string term (str (get player :beasts) "(+ " (get player :zoo) "/turn)"))

  (t/set-bg-color term :default)

  (t/move-cursor term position-x (+ position-y 4))
  (t/put-string term (str (get player :tower)))
 
  (t/move-cursor term position-x (+ position-y 5))
  (t/put-string term "Tower")

  (t/move-cursor term (+ position-x 7) (+ position-y 4))
  (t/put-string term (str (get player :wall)))
 
  (t/move-cursor term (+ position-x 7) (+ position-y 5))
  (t/put-string term "Wall")
)

(defn clear-space [term start-x start-y end-x end-y]
  (if (<= start-y end-y)
    (do 
      (t/move-cursor term start-x start-y)
      (t/put-string term (apply str (take (- end-x start-x) (repeat " "))))
      (clear-space term start-x (+ start-y 1) end-x end-y)
    )
    nil
  )
)

(defn display-card-info [card position-x position-y term card-num]
  (let [
    color-map {:bricks :red :gems :blue :beasts :green}
    ]
    (clear-space term position-x position-y (+ position-x 20) (+ position-y 9))

    (t/set-fg-color term (get color-map (get card :type)))
    (t/move-cursor term position-x position-y)
    (t/put-string term (str (apply str (take 10 (repeat "-"))) card-num (apply str (take 10 (repeat "-")))))

    (t/move-cursor term position-x (+ position-y 1))
    (t/put-string term (get card :name))

    (t/move-cursor term position-x (+ position-y 3))
    (t/put-string term (apply str (take 20 (drop 0 (get card :description)))))
    (t/move-cursor term position-x (+ position-y 4))
    (t/put-string term (apply str (take 20 (drop 20 (get card :description)))))
    (t/move-cursor term position-x (+ position-y 5))
    (t/put-string term (apply str (take 20 (drop 40 (get card :description)))))
    (t/move-cursor term position-x (+ position-y 6))
    (t/put-string term (apply str (take 20 (drop 60 (get card :description)))))
    (t/move-cursor term position-x (+ position-y 7))
    (t/put-string term (apply str (take 20 (drop 80 (get card :description)))))

    (t/move-cursor term position-x (+ position-y 9))
    (t/put-string term (str "Costs: " (get card :cost) " " (utils/format-keyword (get card :type))))
  )
  
  (t/set-fg-color term :default)
)

(defn display-game [term game]
  (let [
    player-start 18
    enemy-start 50
    card-start-y 11
    ]

    (clear-space term 0 0 67 7)

    (t/move-cursor term 0 1)
    (t/set-bg-color term :blue)
    (t/put-string term "Gems (+ Magic)")
    (t/move-cursor term 0 2)
    (t/set-bg-color term :red)
    (t/put-string term "Bricks (+ Quarry)")
    (t/move-cursor term 0 3)
    (t/set-bg-color term :green)
    (t/put-string term "Beasts (+ Zoo)")
    
    (t/set-bg-color term :default)

    (t/move-cursor term player-start 0)
    (t/put-string term "Player")

    (t/move-cursor term enemy-start 0)
    (t/put-string term "Enemy")

    ;; Set player text
    (t/set-bg-color term :default)
    (display-player-info (get game :player1) player-start 1 term)
    (display-player-info (get game :player2) enemy-start 1 term)

    (display-card-info (get-in game [:player1 :c1]) 0 card-start-y term 1)
    (display-card-info (get-in game [:player1 :c2]) 22 card-start-y term 2)
    (display-card-info (get-in game [:player1 :c3]) 44 card-start-y term 3)
    (display-card-info (get-in game [:player1 :c4]) 66 card-start-y term 4)
    (display-card-info (get-in game [:player1 :c5]) 88 card-start-y term 5)
  )
)

(defn display-enemy-card [term card play-or-discard]
  (clear-space term 75 0 120 9)
  (display-card-info card 75 0 term (str "Enemy's " play-or-discard " Card"))
)

(defn put-info-text [term text]
  (clear-space term 0 120 24 29)
	(t/move-cursor term 0 24)
  (t/put-string term "                                                                                   ")
  (t/move-cursor term 0 24)
  (t/put-string term text)
)

(defn wait-for-input [term x-pos y-pos s]
  ;; clear line
  (t/move-cursor term x-pos y-pos)
  (t/put-string term s)

  (t/move-cursor term x-pos y-pos)
  (let [pressed-key (t/get-key-blocking term)]
    (if (= pressed-key :enter) 
      s
      (wait-for-input term x-pos y-pos (str s pressed-key))
    )  
  )
)

(defn get-user-input [term text accepted-responses]
  (put-info-text term text)
  (clear-space term 0 25 120 29)

  (let [
    response (wait-for-input term 0 25 "")
    ]
    (if (utils/in? response accepted-responses)
      response
      (recur term text accepted-responses)
    )
  )
)