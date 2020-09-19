(ns acromage.core
  (:gen-class)
  (:require 
    [acromage.cards :as cards]
    [utils.general :as utils]
    [lanterna.terminal :as t]
    [acromage.user-interface :as ui]
    [csv.csv :as csv]
    [acromage.game :as g]
  )
)

(defn reload [] 
  (require 'acromage.core 'acromage.cards 'utils.general 'acromage.game :reload))

(def DEBUG 1)

(defn get-user-to-pick-card [term game]
  (ui/display-game term game)
  (let [
    cards (filter #(g/can-play-card game %) ["1" "2" "3" "4" "5"])
    discard-cards (filter #(g/can-play-card game %) ["1d" "2d" "3d" "4d" "5d"])
    playable (concat cards discard-cards)
    card-string (clojure.string/join ", " cards)
    discard-string (clojure.string/join ", " discard-cards)
    user-text (str "Please select a card you wish to play (" card-string ") or discard (" discard-string ")")
    card (ui/get-user-input term user-text playable)
  ]
    card
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
    (println "Score other:" score-other "Score you:" score-you )
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

(defn do-game [game] 
  (let [
    winner (g/check-win-states game)
    ]
    (if (= :none winner) 
      (let 
        [new-game (g/do-turn game)]
        (println "Game changed\n" (ui/get-turn-change-text game new-game))
        (do-game new-game)
      )
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
  "Jump into a game of acromage"
  [& args]
  (let [
    term (t/get-terminal :swing {:cols 120 :rows 30})
    res (t/start term)
    game-type {:name "original", :starting-tower 50, :starting-wall 25, :start-resource 15, :start-resource-gain 2, :max-resource 100, :max-tower 100}
    next-player :player1
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
      :damage 0
      :card-pick-function (partial get-user-to-pick-card term)}
    player2 (assoc player :card-pick-function get-ai-to-pick-card)
    player2 (assoc player2 :scoring {
                                      :you {:wall 1 :tower 1 :gems 1 :bricks 1 :beasts 1 :magic 1 :quarry 1 :zoo 1} 
                                      :other {:wall -1 :tower -1 :gems -1 :bricks -1 :beasts -1 :magic -1 :quarry -1 :zoo -1}})
    all-cards (cards/load-cards)
    deck (shuffle all-cards)
    game {
      :cards all-cards 
      :player1 player
      :player2 player2
      :turn next-player 
      :deck deck 
      :turns 0 
      :win-conditions {:max-resources max-resource :max-tower max-tower}
      :history []
      }
    game (->> game g/fill-nil-cards)
    ]
    (let [winner (do-game game)]
      (t/clear term)
      (if (= winner :all)
        (ui/get-user-input term "A tie has occured! <Enter to quit>" [""])
        (if (= winner :player1) 
          (ui/get-user-input term "Player 1 has won! <Enter to quit>" [""])
          (ui/get-user-input term "Player 2 has won! <Enter to quit>" [""])
        )
      )
    )
    
    (t/stop term)
  )
)

(defn -main2
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
    next-player (if (= user-input flip-result) :player1 :player2)
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
      :damage 0
      :card-pick-function (partial get-user-to-pick-card term)}
    player2 (assoc player :card-pick-function get-ai-to-pick-card)
    player2 (assoc player2 :scoring {
                                      :you {:wall 1 :tower 1 :gems 1 :bricks 1 :beasts 1 :magic 1 :quarry 1 :zoo 1} 
                                      :other {:wall -1 :tower -1 :gems -1 :bricks -1 :beasts -1 :magic -1 :quarry -1 :zoo -1}})
    all-cards (cards/load-cards)
    deck (shuffle all-cards)
    game {
      :cards all-cards 
      :player1 player
      :player2 player2
      :turn next-player 
      :deck deck 
      :turns 0 
      :win-conditions {:max-resources max-resource :max-tower max-tower}
      :history []
      }
    game (->> game g/fill-nil-cards)
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

    (let [winner (do-game game)]
      (t/clear term)
      (if (= winner :all)
        (ui/get-user-input term "A tie has occured! <Enter to quit>" [""])
        (if (= winner :player1) 
          (ui/get-user-input term "Player 1 has won! <Enter to quit>" [""])
          (ui/get-user-input term "Player 2 has won! <Enter to quit>" [""])
        )
      )
    )
    
    (t/stop term)
  )
)