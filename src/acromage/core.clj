(ns acromage.core
  (:gen-class)
  (:require 
    [acromage.cards :as cards]
    [utils.general :as utils]
    [lanterna.terminal :as t]
    [acromage.user-interface :as ui]
  )
)

(defn reload [] 
  (require 'acromage.core 'acromage.cards 'utils.general :reload))

(def PLAYER_PLAYING 0)
(def AI_PLAYING 1)

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
        (println player-key "played:" (get card-def :name) "Which changed:\n" (ui/get-turn-change-text game post-effect-game))
        (if (card-def :play-again) 
          post-effect-game 
          (change-player post-effect-game)
        )
      )
    )
  )
  
)

(defn can-play-card [game card]
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
    valid
  )
)

(defn do-card-turn [game card]
  (let [
    player (game (key-from-turn game))
    card-num (subs card 0 1)
    discard (clojure.string/includes? card "d")
    card-def (player (keyword (str "c" card-num)))
    new-game (play-card game player card-num discard card-def)
    ]
    new-game
  )
)

(defn do-enemy [game term]
  (let [
    card (rand-nth (filter #(can-play-card game %) ["1" "2" "3" "4" "5" "1d" "2d" "3d" "4d" "5d"]))
    card-num (clojure.string/replace card "d" "")
    card-keyword (keyword (str "c" card-num))
    player-card (get-in game [:player2 card-keyword])
    discard (clojure.string/includes? card "d")
    card-def (get-in game [:player2 (keyword (str "c" card-num))])
  ]
    (ui/display-enemy-card term card-def (if discard "Discarded" "Played"))
    (do-card-turn game card)
  )
)

(defn pick-card [game term]
  (let [
    cards (filter #(can-play-card game %) ["1" "2" "3" "4" "5"])
    discard-cards (filter #(can-play-card game %) ["1d" "2d" "3d" "4d" "5d"])
    commas (repeat ", ")
    playable (concat cards discard-cards)
    card-string (apply str (interleave cards commas))
    discard-string (apply str (interleave discard-cards commas))
    user-text (str "Please select a card you wish to play (" card-string ") or discard (" discard-string ")")
    card (ui/get-user-input term user-text playable)
  ]
    (do-card-turn game card)
  )
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
    deck (second res)
    deck  (if (= (count deck) 0)
            () 
            deck
          )
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

(defn do-turn 
  [game term]
  (let [updated-game (->> game fill-nil-cards do-resource-gains)]
    (ui/display-game term updated-game)
    (if (= (updated-game :turn) PLAYER_PLAYING)
      (pick-card updated-game term)
      (do-enemy updated-game term)
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

(defn do-game [term game] 
  (let [
    winner (check-win-states game)
    ]
    (if (= -1 winner) 
      (do-game term (do-turn game term))
      (winner)
    )
  )
)

(defn -main
  "Play a game of achromage!"
  [& args]

  (let [
    term (t/get-terminal :swing {:cols 120 :rows 30})
    res (t/start term)
    res (ui/get-user-input term "Welcome to achromage (Command line edition)! <Enter to continue>" [""])
    user-input (ui/get-user-input term "We must find out who goes first! Heads (H) or tails (T)?" ["H" "T"])
    flip-result (rand-nth ["H" "T"])
    next-player (if (= user-input flip-result) 0 1)
    player {:tower 50 :wall 25 :gems 15 :beasts 15 :bricks 15 :magic 2 :zoo 2 :quarry 2 :c1 nil :c2 nil :c3 nil :c4 nil :c5 nil :damage 0}
    enemy {:tower 50 :wall 25 :gems 15 :beasts 15 :bricks 15 :magic 2 :zoo 2 :quarry 2 :c1 nil :c2 nil :c3 nil :c4 nil :c5 nil :damage 0}
    all-cards (cards/load-cards)
    deck (shuffle all-cards)
    game {:player1 player :player2 enemy :turn next-player :deck deck :turns 0 :win-conditions {:max-resources 100 :max-tower 100}}
    ]
    (ui/put-info-text term "Flip was")
    (Thread/sleep 500)
    (ui/put-info-text term "Flip was.")
    (Thread/sleep 500)
    (ui/put-info-text term "Flip was..")
    (Thread/sleep 500)
    (ui/put-info-text term "Flip was...")
    (Thread/sleep 400)
    (ui/get-user-input term (str "Flip was... " (if (= flip-result "H") "Heads!" "Tails!") "<Enter to continue>") [""])

    (let [winner (do-game term game)]
      (if (= winner 0)
        (println "You've won!")
        (println "You lost, try again soon!")
      )
    )
  )
)

(defn test-display []
  
  (let [
    next-player 0
    player {:tower 50 :wall 25 :gems 15 :beasts 15 :bricks 15 :magic 2 :zoo 2 :quarry 2 :c1 nil :c2 nil :c3 nil :c4 nil :c5 nil :damage 0}
    enemy {:tower 50 :wall 25 :gems 15 :beasts 15 :bricks 15 :magic 2 :zoo 2 :quarry 2 :c1 nil :c2 nil :c3 nil :c4 nil :c5 nil :damage 0}
    all-cards (cards/load-cards)
    deck (shuffle all-cards)
    game {:player1 player :player2 enemy :turn next-player :deck deck :turns 0 :win-conditions {:max-resources 100 :max-tower 100}}
    game (fill-nil-cards game)
    term (t/get-terminal :swing {:cols 120 :rows 30})
    ]
    (ui/display-game term game)
    (t/move-cursor term 0 24)
    (t/put-string term "Which card would you like to play?")

    (ui/wait-for-input term 0 25 "")
    (t/stop term)
  )
)