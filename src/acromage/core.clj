(ns acromage.core
  (:gen-class)
  (:require 
    [acromage.cards :as cards]
    [utils.general :as utils]
    [lanterna.terminal :as t]
    [acromage.user-interface :as ui]
    [csv.csv :as csv]
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
    card-string (interleave cards commas)
    card-string (apply str (drop-last card-string)) ;; Remove ending commas
    discard-string (interleave discard-cards commas)
    discard-string (apply str (drop-last discard-string)) ;; Remove ending commas
    user-text (str "Please select a card you wish to play (" card-string ") or discard (" discard-string ")")
    card (ui/get-user-input term user-text playable)
  ]
    (do-card-turn game card)
  )
)

(defn contains-value? [val col]
  (if (some #(= (second %) val) col) true false)
)

(defn fill-deck [game deck]
  ; Fill the deck by checking
  ; Putting all cards into deck that aren't in players' hands.
  (println "deck size" (count deck))
  (if (= (count deck) 0)
    (do 
      (println "Reshuffling cards")
      (->> 
        (:cards game)
        (filter 
          #(and 
            (not (contains-value? % (:player1 game)))
            (not (contains-value? % (:player2 game)))
          )
        )
        (shuffle)
      )
    )
    deck
  )
)

(defn fill-nil-player [player deck card-keyword game]
  (if (= (player card-keyword) nil)
    [(assoc player card-keyword (first deck)) (fill-deck game (drop 1 deck))]
    [player deck]
  )  
)

(defn fill-nil [game player-keyword card-keyword]
  (let [ 
    res (fill-nil-player (game player-keyword) (game :deck) card-keyword game)
    game1 (assoc game player-keyword (first res))
    deck (second res)
    ]
    (assoc game1 :deck deck)
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

(defn player-lose [player]
  (or 
    false
    (<= (player :tower) 0)
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
    p1-win (or (player-win (game :player1) res-max tower-max) (player-lose (game :player2)))
    p2-win (or (player-win (game :player2) res-max tower-max) (player-lose (game :player1)))
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
      winner
    )
  )
)

(defn pick-game-type [term]
  
  (let [
    game-types (->> 
                  (csv/load-csv-file "resources/Games.csv")
                  (map #(utils/parse-map % [:starting-tower :starting-wall :start-resource :start-resource-gain :max-resource :max-tower]))
                )
    names (map #(:name %) game-types)
    response (ui/get-user-input term (str "Please select your game type! (" (clojure.string/join ", " names) ")") names)
  ]
    (first (filter #(= (:name %) response) game-types))
  )
)

(defn -main
  "Play a game of achromage!"
  [& args]

  (let [
    term (t/get-terminal :swing {:cols 120 :rows 30})
    res (t/start term)
    res (ui/get-user-input term "Welcome to achromage (Command line edition)! <Enter to continue>" [""])
    game-type (pick-game-type term)
    tmp (println game-type)
    user-input (ui/get-user-input term "We must find out who goes first! Heads (H) or tails (T)?" ["H" "T"])
    flip-result (rand-nth ["H" "T"])
    next-player (if (= user-input flip-result) 0 1)
    starting-tower (:starting-tower game-type)
    starting-wall (:starting-wall game-type)
    starting-resources (:start-resource game-type)
    starting-gain (:start-resource-gain game-type)
    max-resource (:max-resource game-type)
    max-tower (:max-tower game-type)
    player {
      :tower starting-tower 
      :wall starting-wall 
      :gems starting-resources 
      :beasts starting-resources 
      :bricks starting-resources 
      :magic starting-gain
      :zoo starting-gain
      :quarry starting-gain 
      :c1 nil 
      :c2 nil 
      :c3 nil 
      :c4 nil 
      :c5 nil 
      :damage 0}
    all-cards (cards/load-cards)
    deck (shuffle all-cards)
    game {:cards all-cards :player1 player :player2 player :turn next-player :deck deck :turns 0 :win-conditions {:max-resources max-resource :max-tower max-tower}}
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
    (t/stop term)
  )
)