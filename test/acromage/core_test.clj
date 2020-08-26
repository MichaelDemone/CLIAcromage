(ns acromage.core-test
  (:require [clojure.test :refer :all]
            [acromage.core :refer :all]
            [acromage.cards :refer :all]
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
      effect (get-effect {:effected :you :resource :quarry :amount [< [:you :quarry] [:other :quarry] 2 1]})
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
      effect (get-effect {:effected :you :resource :quarry :amount [< [:you :quarry] [:other :quarry] 2 1]})
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
        effect (get-effect {:effected :you :resource :quarry :amount [< [:you :quarry] [:other :quarry] [:other :quarry] [:you :quarry]] :set true})
        post-effect-game (effect game)
        correct-response 6
        response (get-in post-effect-game [:player1 :quarry])
        ]
        (is (= correct-response response))
      )
      (let [
        effect (get-effect {:effected :you :resource :quarry :amount [< [:you :quarry] [:other :quarry] [:other :quarry] [:you :quarry]]})
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
      effect (get-effect {:effected :other :resource [> [:other :wall] 10 :tower :wall] :amount -6})
      post-effect-game (effect game)
      ]
        (is (= 4 (get-in post-effect-game [:player2 :tower])))
        (is (= 40 (get-in post-effect-game [:player2 :wall])))
    )
  )
)

(deftest loading-card
  (testing "Loading cards"
    (let [
      cards (load-cards)
    ]
      (println cards)
    )
  )
)