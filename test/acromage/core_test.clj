(ns acromage.core-test
  (:require [clojure.test :refer :all]
            [acromage.core :refer :all]
            [acromage.cards :refer :all]
            [acromage.user-interface :refer :all]
            [acromage.game :refer :all]
  )
)

(deftest card-effect-test
  (testing "Player plain card effect"
    (let [
      player {:quarry 2}
      enemy {:quarry 5}
      game {:player1 player :player2 enemy :turn 0}
      effect (get-effect {:effected :you :resource :quarry :amount 1})
      post-effect-game (effect game)
      correct-response 3
      response (get-in post-effect-game [:player1 :quarry])
      ]
      (is (= correct-response response))
    )
  )
)

(deftest card-effect-test2
  (testing "Enemy plain card effect"
    (let [
      player {:quarry 2}
      enemy {:quarry 5}
      game {:player1 player :player2 enemy :turn 1}
      effect (get-effect {:effected :you :resource :quarry :amount 1})
      post-effect-game (effect game)
      correct-response 6
      response (get-in post-effect-game [:player2 :quarry])
      ]
      (is (= correct-response response))
    )
  )
)

(deftest card-effect-test3
  (testing "Changing amount card effect"
    (let [
      player {:quarry 2}
      enemy {:quarry 5}
      game {:player1 player :player2 enemy :turn 0}
      effect (get-effect {:effected :you :resource :quarry :amount [(symbol "<") [:you :quarry] [:other :quarry] 2 1]})
      post-effect-game (effect game)
      correct-response 4
      response (get-in post-effect-game [:player1 :quarry])
      ]
      (is (= correct-response response))
    )
  )
)

(deftest card-effect-test4
  (testing "Changing amount card effect"
    (let [
      player {:quarry 2}
      enemy {:quarry 2}
      game {:player1 player :player2 enemy :turn 0}
      effect (get-effect {:effected :you :resource :quarry :amount [(symbol "<") [:you :quarry] [:other :quarry] 2 1]})
      post-effect-game (effect game)
      correct-response 3
      response (get-in post-effect-game [:player1 :quarry])
      ]
      (is (= correct-response response))
    )
  )
)

(deftest card-effect-test5
  (testing "Testing return values being collections and testing set"
    (let [
      player {:quarry 2}
      enemy {:quarry 6}
      game {:player1 player :player2 enemy :turn 0}
      ]
      (let [
        effect (get-effect {:effected :you :resource :quarry :amount [(symbol "<") [:you :quarry] [:other :quarry] [:other :quarry] [:you :quarry]] :set true})
        post-effect-game (effect game)
        correct-response 6
        response (get-in post-effect-game [:player1 :quarry])
        ]
        (is (= correct-response response))
      )
      (let [
        effect (get-effect {:effected :you :resource :quarry :amount [(symbol "<") [:you :quarry] [:other :quarry] [:other :quarry] [:you :quarry]]})
        post-effect-game (effect game)
        correct-response 8
        response (get-in post-effect-game [:player1 :quarry])
        ]
        (is (= correct-response response))
      )
    )
  )
)

(deftest card-effect-test6
  (testing "Testing resource"
    (let [
      player {}
      enemy {:wall 40 :tower 10}
      game {:player1 player :player2 enemy :turn 0}
      effect (get-effect {:effected :other :resource [(symbol ">") [:other :wall] 10 :tower :wall] :amount -6})
      post-effect-game (effect game)
      ]
        (is (= 4 (get-in post-effect-game [:player2 :tower])))
        (is (= 40 (get-in post-effect-game [:player2 :wall])))
    )
  )
)

(deftest all-card-effect-test
  (testing "Testing if applying an effect to a game causes an errors (format validation of configs)"
    (let [
      player {:tower 50 :wall 25 :gems 15 :beasts 15 :bricks 15 :magic 2 :zoo 2 :quarry 2 :c1 nil :c2 nil :c3 nil :c4 nil :c5 nil :damage 0}
      enemy {:tower 50 :wall 25 :gems 15 :beasts 15 :bricks 15 :magic 2 :zoo 2 :quarry 2 :c1 nil :c2 nil :c3 nil :c4 nil :c5 nil :damage 0}
      all-cards (load-cards)
      deck (shuffle all-cards)
      game {:player1 player :player2 enemy}
      ]
      (println (map 
        #(apply-effects game %) 
        (map 
          #(do 
            (println "Getting effect from " (:name %)) 
            (flush) 
            (:effects %)
          ) 
          all-cards
        )
      ))
    )
  )
)

(deftest loading-card
  (testing "Loading cards"
    (let [
      cards (load-cards)
    ]
      ;;(println cards)
    )
  )
)

(deftest game-changes
  (testing "Displaying game changes"
    (let [
      old-player {:tower 50 :wall 25 :gems 15 :beasts 15 :bricks 15 :magic 2 :zoo 2 :quarry 2 :c1 nil :c2 nil :c3 nil :c4 nil :c5 nil}
      old-enemy {:tower 50 :wall 25 :gems 15 :beasts 15 :bricks 15 :magic 2 :zoo 2 :quarry 2 :c1 nil :c2 nil :c3 nil :c4 nil :c5 nil}
      old-game {:player1 old-player :player2 old-enemy}
      new-player {:tower 60 :wall 20 :gems 15 :beasts 15 :bricks 15 :magic 2 :zoo 2 :quarry 2 :c1 nil :c2 nil :c3 nil :c4 nil :c5 nil}
      new-enemy {:tower 50 :wall 25 :gems 10 :beasts 15 :bricks 15 :magic 1 :zoo 2 :quarry 2 :c1 nil :c2 nil :c3 nil :c4 nil :c5 nil}
      new-game {:player1 new-player :player2 new-enemy}
      turn-change-text (get-turn-change-text old-game new-game)
    ]
      (is (= "Player1\n\tTower 50->60 (+10)\n\tWall 25->20 (-5)\nPlayer2\n\tGems 15->10 (-5)\n\tMagic 2->1 (-1)\n" turn-change-text))
    )
  )
)


(deftest dealing-with-damage
  (testing "If damage works on wall"
    (let [
      old-player {:tower 50 :wall 25 :gems 15 :beasts 15 :bricks 15 :magic 2 :zoo 2 :quarry 2 :c1 nil :c2 nil :c3 nil :c4 nil :c5 nil}
      old-enemy {:tower 50 :wall 25 :gems 15 :beasts 15 :bricks 15 :magic 2 :zoo 2 :quarry 2 :c1 nil :c2 nil :c3 nil :c4 nil :c5 nil :damage 10}
      old-game {:player1 old-player :player2 old-enemy}
      new-player {:tower 50 :wall 25 :gems 15 :beasts 15 :bricks 15 :magic 2 :zoo 2 :quarry 2 :c1 nil :c2 nil :c3 nil :c4 nil :c5 nil}
      new-enemy {:tower 50 :wall 15 :gems 15 :beasts 15 :bricks 15 :magic 2 :zoo 2 :quarry 2 :c1 nil :c2 nil :c3 nil :c4 nil :c5 nil :damage 0}
      new-game {:player1 new-player :player2 new-enemy}
      damage-game (deal-with-damage old-game)
    ]
      (is (= damage-game new-game))
    )
  )
)