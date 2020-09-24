(ns acromage.evolution
  (:require 
    [acromage.cards :as cards]
		[utils.general :as utils]
		[utils.math :as math]
    [csv.csv :as csv]
    [acromage.game :as g]
  )
)

(defn score-player [player scoring]
  (let [
    score (->>  
            player
            (filter #(contains? scoring (first %)))
            (map #(* ((first %) scoring) (second %)))
            (reduce +)  
          )
    ]
    score
  )
)

(defn score-game [game player-key] 
  (let 
    [
      scoring-data (get-in game [player-key :scoring])
      scoring-data-you (:you scoring-data)
      score-you (score-player (player-key game) scoring-data-you)

      other-key (if (= player-key :player1) :player2 :player1)
      scoring-data-other (:other scoring-data)
      score-other (score-player (other-key game) scoring-data-other)
    ]
    ;;(println "Score other:" score-other "Score you:" score-you )
    ;; TODO: Include 'play again' in score calculations somehow.
    (+ score-you score-other)
  )
)

(defn get-ai-to-pick-card [game]
  (let [
    player (:turn game)
    playable-cards (filter #(g/can-play-card game %) ["1" "2" "3" "4" "5" "1d" "2d" "3d" "4d" "5d"])
    max-result (apply max-key #(score-game (g/do-resource-gains (g/do-card-turn game %) true) player) playable-cards)
  ]
    max-result
  )
)

(defn set-from-map [original to-set [key & keys]]
	(if (nil? key)
		original
		(recur (assoc original key (key to-set)) to-set keys)
	)
)

(defn evolve 
  ([parent1 parent2 parent1-keys parent2-keys mutation-chance mutation-min mutation-max]
    (let [
      child-half (set-from-map {} parent1 parent1-keys)
      child-full (set-from-map child-half parent2 parent2-keys)
      mutation-range #(+ mutation-min (* (- mutation-max mutation-min) (rand)))
      mutated-child (map 
                      #(if (< (rand) mutation-chance) 
                          [(first %) (math/clamp -1 1 (+ (mutation-range) (second %)))] 
                          %
                        ) 
                      child-full)
    ]
      (into {} mutated-child)
    )
  )
  ([{you-1 :you other-1 :other} {you-2 :you other-2 :other}]
    (let [
      you-child (evolve you-1 you-2 [:wall :tower] [:gems :bricks :beasts :magic :quarry :zoo] 0.1 -0.5 0.5)
      other-child (evolve other-1 other-2 [:wall :tower] [:gems :bricks :beasts :magic :quarry :zoo] 0.1 -0.5 0.5)
    ]
      {:you you-child :other other-child}
    )
  )
)

(defn create-score []
	{  
		:you {:wall (rand) :tower (rand) :gems (rand) :bricks (rand) :beasts (rand) :magic (rand) :quarry (rand) :zoo (rand)} 
		:other {:wall (* -1 (rand)) :tower (* -1 (rand)) :gems (* -1 (rand)) :bricks (* -1 (rand)) :beasts (* -1 (rand)) :magic (* -1 (rand)) :quarry (* -1 (rand)) :zoo (* -1 (rand))}
	}
)

(defn create-player 
	([protoplayer] (assoc protoplayer :scoring (create-score)))
	([player1 player2] (assoc player1 :scoring (evolve (:scoring player1) (:scoring player2)) :name (str (:name player1) " " (:name player2))))
)

(defn mix-players [[player1 player2 & others]]
  (if (nil? player2)
    player1
    (let [
      mixed (create-player player1 player2)
      players (concat [mixed] others)
    ]
      (recur players)
    )
    
  )
)

(defn run-game [game] 
  (let [
    winner (g/check-win-states game)
    ]
    (if (= :none winner) 
      (let 
        [new-game (g/do-turn game)]
        (recur new-game)
      )
      winner
    )
  )
)

(defn run-round [protogame player enemy]
  (if (= player enemy)
    0
    (let 
			[
				game (assoc protogame :player1 player :player2 enemy :deck (shuffle (:cards protogame)))
    		game (->> game g/fill-nil-cards)
			]
			(if (= (run-game game) :player1) 1 0)
		)
  )
)

(defn run-rounds [protogame player enemies]
  (apply + (map #(run-round protogame player %) enemies))
)

(defn print-players [players]
  (apply println (map #(:name %) players))
)

;; Evolution-array is a list of lists that dictates which players move onto the
;; next evolution. For example: [[0 1] [0 2] [0 3] [1 2] [0] [1] [2] [3]] 
;; [0 1] would merge 1st place player and second place player,
;; [0 2] would merge 1st place and 3rd place
;; [0] would move 1st place player forward
(defn run-generation [protogame players evolution-array]
  (let [
    wins (map (fn [player] (run-rounds protogame player players)) players)
    wins-with-index (map-indexed (fn [idx item] [idx item]) wins)
    wins-sorted (reverse (sort-by #(nth % 1) wins-with-index))
    players-sorted-by-wins (map (fn [[idx itm]] (nth players idx)) wins-sorted)
    players-in-evo-array  (map 
                            (fn [evo-pos] 
                              (map 
                                (fn [player-pos] 
                                  (nth players-sorted-by-wins player-pos)
                                ) 
                                evo-pos
                              )
                            ) 
                            evolution-array
                          )
    evolved-players (map #(mix-players %) players-in-evo-array)
  ] 
    evolved-players
  )
)

(defn run-generations [protogame players evolution-array gen]
  (if (= gen 0)
    players
    (recur protogame (run-generation protogame players evolution-array) evolution-array (- gen 1))
  )
)

(defn run-training [protogame protoplayer evolution-array gens]
  (let [
    players-per-group (count evolution-array)
    create-player-array (fn [name] (map-indexed (fn [idx itm] (assoc (create-player protoplayer) :name (str name idx))) (repeat players-per-group nil)))
    letters (map #(str (char (+ % (int \a)))) (map-indexed (fn [idx itm] idx) (repeat players-per-group nil)))
    all-players (map #(create-player-array %) letters)
    group-winners (map #(run-generations protogame % evolution-array gens) all-players)
    best-bots (map #(first %) group-winners)
    overall-winners (run-generations protogame best-bots evolution-array gens)
  ]
  overall-winners
  )
)

(defn train-ai [& args] 
  (let [
    game-type {:name "original", :starting-tower 50, :starting-wall 25, :start-resource 15, :start-resource-gain 2, :max-resource 100, :max-tower 100}
    starting-tower (:starting-tower game-type)
    starting-wall (:starting-wall game-type)
    starting-resources (:start-resource game-type)
    starting-gain (:start-resource-gain game-type)
    max-resource (:max-resource game-type)
    max-tower (:max-tower game-type)
    protoplayer {
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
      :damage 0
      :card-pick-function get-ai-to-pick-card
		}
    all-cards (cards/load-cards)
    protogame {
      :cards all-cards 
      :turn :player1
      :turns 0 
      :win-conditions {:max-resources max-resource :max-tower max-tower}
      :history []
    }

    winners (run-training protogame protoplayer [[0 1] [0 2] [0 3] [1 2] [0] [1] [2] [3]] 3)
    ]
    (println (:name (first winners)))
    (println (:scoring (first winners)))
		nil
  )
)