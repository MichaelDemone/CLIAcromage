(ns acromage.core
  (:gen-class)
  (:require 
    [acromage.cards :as cards]
    [utils.general :as utils]
  )
)

(defn reload [] 
  (require 'acromage.core 'acromage.cards 'utils.general :reload))

(def PLAYER_PLAYING 0)
(def AI_PLAYING 1)

(defn print-stats [p]
  (println 
    "Tower: " (p :tower) 
    "\nWall: " (p :wall) 
    "\nGems(+Magic): " (p :gems) "(+" (p :magic) "/turn)"
    "\nBricks(+Quarry): " (p :bricks) "(+" (p :quarry) "/turn)"
    "\nBeasts(+Zoo): " (p :beasts) "(+" (p :zoo) "/turn)"
  )
)

(defn format-keyword [word]
  (clojure.string/capitalize (clojure.string/replace (str word) #":" "")))

(defn get-card-string [c]
  (if (nil? c)
    "Empty"
    (str
      (c :name) 
      " (" 
      (c :cost) 
      " " 
      (format-keyword (c :type))
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

(defn change-player [game]
  (let [
    game (assoc game :turns (inc (game :turns)))
    ]
    (if (= (game :turn) 0)
      (assoc game :turn 1)
      (assoc game :turn 0)
    )
  )
)

(defn remove-card [player card-num]
  (assoc player (keyword (str "c" card-num)) nil)
)

(defn purchase-card [player card-def]
    (let [
      new-resource-amt (- (player (card-def :type)) (card-def :cost))
      ]
      (assoc player (card-def :type) new-resource-amt)
    )
)

(defn key-from-turn [game]
  (if (= (game :turn) PLAYER_PLAYING) :player1 :player2))


(defn deal-with-damage 
  ([game [player-key & others]]
    (if (nil? player-key)
      game
      (let [
        player (get game player-key)
      ]
        (if (nil? (get player :damage))
          (deal-with-damage game others)
          (let [
            dmg (get player :damage)
            wall (get player :wall)
            tower (get player :tower)
            new-wall (if (> wall dmg) (- wall dmg) 0)
            new-dmg (if (> wall dmg) 0 (- dmg wall))
            new-tower (- tower new-dmg)
            new-player (assoc player :wall new-wall :tower new-tower :damage 0)
            new-game (assoc game player-key new-player)
          ]
            (deal-with-damage new-game others)
          )
        )
      )
    )
  )
  ([game]
    (deal-with-damage game [:player1 :player2])
  )
)

(defn apply-effects [game effects]
  ;; TODO: Find out if this is how local functions should be done in clojure.
  (let [
    apply-effects-recur (fn [game [effect & other-effects]]
                          (if (nil? effect)
                            game
                            (recur ((cards/get-effect effect) game) other-effects)
                          ))
  ]
    (deal-with-damage (apply-effects-recur game effects))
  )
)

(defn play-card [game player card-num discard card-def]
  (let [
    player-key (key-from-turn game)
    new-player (remove-card player card-num)
    new-game (assoc game player-key new-player)
    ] 
    (if discard
      (change-player new-game)
      (let [
        purchased-player (purchase-card new-player card-def)
        turn-over-game (assoc new-game player-key purchased-player)
        post-effect-game (apply-effects turn-over-game (card-def :effects))
        ]
  
        (if (card-def :play-again) 
          post-effect-game 
          (change-player post-effect-game)
        )
      )
    )
  )
  
)

(defn do-card-turn [game card on-fail]
  (let [
    player (game (key-from-turn game))
    card-num (subs card 0 1)
    discard (clojure.string/includes? card "d")
    card-def (player (keyword (str "c" card-num)))
    can-discard (not (and discard (not (card-def :discardable))))
    can-afford (or discard (>= (player (card-def :type)) (card-def :cost)))
    valid (and 
      can-discard
      can-afford
    )
    ]
    (if valid
      (play-card game player card-num discard card-def)
      (do 
        (let [issue (if can-discard "Can't afford that card" "Can't discard that card")]
          (println issue)
        )
        (on-fail)
      )
    )
  )
)

(defn do-enemy [game]
  (if-not (= (game :turn) AI_PLAYING)
    (println "do-enemy called but its players turn!!"))

  (let [
    card (rand-nth ["1" "2" "3" "4" "5" "1d" "2d" "3d" "4d" "5d"])
    card-num (clojure.string/replace card "d" "")
    card-keyword (keyword (str "c" card-num))
    player-card (get-in game [:player2 card-keyword])
    discard (clojure.string/includes? card "d")
  ]
    (println "Enemy" (if discard "discarded" "played") "a card!" (get-card-string player-card))
    (do-card-turn game card #((do-enemy game)))
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

(defn pick-card [game]
  (println "Please select a card you wish to play (1, 2, 3, 4, 5) or discard (1d, 2d, 3d, 4d, 5d)")
  (let [
    card (utils/get-input ["1" "2" "3" "4" "5" "1d" "2d" "3d" "4d" "5d"])
  ]
    (do-card-turn game card #((pick-card game)))
  )
)

(defn do-prompt [game]
  (if-not (= (:turn game) PLAYER_PLAYING)
    (println "do-prompt called but its enemies turn!!"))

  (print-game-state game)

  (pick-card game)
)

(defn fill-nil-player [player deck card-keyword]
  (if (= (player card-keyword) nil)
    [(assoc player card-keyword (first deck)) (drop 1 deck)]
    [player deck]
  )  
)

(defn fill-nil [game player-keyword card-keyword]
  (let [ 
    res (fill-nil-player (game player-keyword) (game :deck) card-keyword)
    game1 (assoc game player-keyword (first res))
    ]
    (assoc game1 :deck (second res))
  )
)

(defn fill-nil-cards 
  [game]
  (let [
    game1 (fill-nil game :player1 :c1)
    game2 (fill-nil game1 :player1 :c2)
    game3 (fill-nil game2 :player1 :c3)
    game4 (fill-nil game3 :player1 :c4)
    game5 (fill-nil game4 :player1 :c5)
    game6 (fill-nil game5 :player2 :c1)
    game7 (fill-nil game6 :player2 :c2)
    game8 (fill-nil game7 :player2 :c3)
    game9 (fill-nil game8 :player2 :c4)
    game10 (fill-nil game9 :player2 :c5)
  ]
    game10
  )
)

(defn resource-gain [game player-key resource-key resource-gain-key]
  (let [new-res (+ (get-in game [player-key resource-gain-key]) (get-in game [player-key resource-key]))]
    (assoc-in game [player-key resource-key] new-res)
  )
)

(defn do-resource-gains [game]
  (if (>= (game :turns) 2)
    (do 
      (let[
        game1 (resource-gain game :player1 :gems :magic)
        game2 (resource-gain game1 :player1 :beasts :zoo)
        game3 (resource-gain game2 :player1 :bricks :quarry)
        game4 (resource-gain game3 :player2 :gems :magic)
        game5 (resource-gain game4 :player2 :beasts :zoo)
        game6 (resource-gain game5 :player2 :bricks :quarry)
      ] 
        (assoc game6 :turns 0)
      )
    )
    game
  )  
)

(defn get-game-change [old-game new-game player-key resource-key]
  (let [
    old (get-in old-game [player-key resource-key])
    new (get-in new-game [player-key resource-key])
    ]
    (if (= old new) "" (str "\t" (format-keyword resource-key) " " old "->" new " (" (if (< 0 (- new old)) "+" "") (- new old) ")\n"))
  )
)

(defn get-turn-change-text [old-game new-game]
  (let [
    players [:player1 :player2]
    resources [:tower :wall :gems :bricks :beasts :magic :quarry :zoo]
    resource-changes (map 
                        (fn [player-key] 
                          (apply str
                            (str (format-keyword player-key) "\n") 
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

(defn do-turn 
  [game]
  (let [updated-game (->> game fill-nil-cards do-resource-gains)]
    
    (if (= (updated-game :turn) PLAYER_PLAYING)
      (do (println "Player's turn!") (do-prompt updated-game))
      (do (println "Enemy's turn!") (do-enemy updated-game))
    )
  )
)

(defn player-win [player res-max tower-max]
  (or 
    (>= (player :gems) res-max)
    (>= (player :beasts) res-max)
    (>= (player :bricks) res-max)
    (>= (player :tower) tower-max)
  )
)

(defn check-win-states [game]
  (let [
    res-max (get-in game [:win-conditions :max-resources])
    tower-max (get-in game [:win-conditions :max-tower])
    p1-win (player-win (game :player1) res-max tower-max)
    p2-win (player-win (game :player2) res-max tower-max)
    tie (and p1-win p2-win)
    ]
    (if tie 2 (if p2-win 1 (if p1-win 0 -1)))
  )
)

(defn do-game [game] 
  (let [winner (check-win-states game)]
    (if (= -1 winner) 
      (do-game (do-turn game))
      (winner)
    )
  )
)

(defn -main
  "Play a game of achromage!"
  [& args]

  (println "Welcome to achromage (Command line edition)!")
  (println "We must find out who goes first! Heads (H) or tails (T)?")

  (let [
    user-input (utils/get-input ["H" "T"])
    flip-result (rand-nth ["H" "T"])
    next-player (if (= user-input flip-result) 0 1)
    player {:tower 50 :wall 25 :gems 15 :beasts 15 :bricks 15 :magic 2 :zoo 2 :quarry 2 :c1 nil :c2 nil :c3 nil :c4 nil :c5 nil}
    enemy {:tower 50 :wall 25 :gems 15 :beasts 15 :bricks 15 :magic 2 :zoo 2 :quarry 2 :c1 nil :c2 nil :c3 nil :c4 nil :c5 nil}
    all-cards (cards/load-cards)
    deck (shuffle all-cards)
    game {:player1 player :player2 enemy :turn next-player :deck deck :turns 0 :win-conditions {:max-resources 100 :max-tower 100}}
    ]
    (println "First turn:" 
      (if (= next-player 0)
        "You"
        "Enemy")
    )
    (let [winner (do-game game)]
      (if (= winner 0)
        (println "You've won!")
        (println "You lost, try again soon!")
      )
    )
  )
)