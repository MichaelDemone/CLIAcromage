(ns acromage.game
  (:require 
    [acromage.cards :as cards]
    [utils.general :as utils]
  )
)

(defn change-player [game]
  (let [
    game (assoc game :turns (inc (game :turns)))
    ]
    (if (= (game :turn) :player1)
      (assoc game :turn :player2)
      (assoc game :turn :player1)
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

(defn can-play-card [game card]
  (let [
    player (game (:turn game))
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



(defn fill-deck [game deck]
  ; Fill the deck by checking
  ; Putting all cards into deck that aren't in players' hands.
  (if (= (count deck) 0)
    (do 
      (println "Reshuffling cards")
      (->> 
        (:cards game)
        (filter 
          #(and 
            (not (utils/contains-value? % (:player1 game)))
            (not (utils/contains-value? % (:player2 game)))
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

(defn do-resource-gains [game & no-check]
  (if (or no-check (>= (game :turns) 2))
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

(defn play-card [game player card-num discard card-def]
  (let [
    player-key (:turn game)
		new-player (remove-card player card-num)
		new-game (assoc game player-key new-player)

		history-entry {:player player-key :type (if discard :discarded :played) :def card-def}
		new-history (conj (:history game) history-entry)
		new-game (assoc new-game :history new-history)	
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

(defn do-card-turn [game card]
  (let [
    player (game (:turn game))
    card-num (subs card 0 1)
    discard (clojure.string/includes? card "d")
    card-def (player (keyword (str "c" card-num)))
    new-game (play-card game player card-num discard card-def)
    ]
    new-game
  )
)

(defn do-turn 
  [game]
  (let [
    current-player-key (:turn game)
    pick-card-func (get-in game [current-player-key :card-pick-function])
		picked-card (pick-card-func game)
		post-turn-game (do-card-turn game picked-card)
    ]
		(->> post-turn-game fill-nil-cards do-resource-gains)
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
    (if tie :all (if p2-win :player2 (if p1-win :player1 :none)))
  )
)