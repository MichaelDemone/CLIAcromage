(ns acromage.cards
	(:require 
		[csv.csv :as csv]
		[clojure.string :as str]
		[utils.general :as utils]
	)
)

(defn get-nested-value [game player-key enemy-key value]
	(if (coll? value) 
		(do 
			(let [effect (first value) resource (second value)]
				(if (= effect :you)
					(get-in game [player-key resource])
					(get-in game [enemy-key resource])
				)
			)
		)
		value 
	)
)

(defn get-value [game player-key enemy-key [func param1 param2 value-if-true value-if-false]]
	(let [
		eval-param1 (get-nested-value game player-key enemy-key param1)
		eval-param2 (get-nested-value game player-key enemy-key param2)
		]
		(if (func eval-param1 eval-param2)
			(get-nested-value game player-key enemy-key value-if-true)
			(get-nested-value game player-key enemy-key value-if-false)
		)
	)
)

;; This is some fun! You can supply an effect with the following:
;; :effected = :you/:other
;; :resource = (:gems/:magic/:tower/:wall/etc) OR [some-function [:other/:you resource] [:other/:you resource] value-if-true value-if-false]
;; For example: :resource could be [> [:other :wall] [10] :wall :tower]
;; That effectively says (if (> enemy-wall 10) :wall :tower) which could be used in a card that's "x damage to enemy tower if wall > 10, otherwise y damage to wall"
;; Similarly, :amount = some-int OR [some-function [:other/:you resource] [:other/:you resource] value-if-true value-if-false]
;; Example. [< [:you :wall] [:enemy :wall] [:enemy :wall] [:you :wall]] which will set your wall to enemy wall if it's smaller.
(defn get-effect [{effected :effected resource-param :resource amt-param :amount set :set}]
	(fn [game]
		(let [
			player-key (if (= (game :turn) 0) :player1 :player2)
			enemy-key (if (= (game :turn) 1) :player1 :player2)
			amt (if (int? amt-param) amt-param (get-value game player-key enemy-key amt-param))
			resource (if (keyword? resource-param) resource-param (get-value game player-key enemy-key resource-param))
			effected-key (if (= effected :you) player-key enemy-key)
			new-amt (if (or (nil? set) (not set)) (+ (get-in game [effected-key resource]) amt) amt)
			new-game (assoc-in game [effected-key resource] new-amt)
			]
			new-game
		)
	)
)

(defn load-cards [] 
	(->> 
		(csv/load-file "resources/Cards.csv")
		(map #(update % :description (fn [a] (str/replace a "\"" ""))))
	)
)

(def cards [
	{:name "Brick Shortage"     
	:discardable true 
	:play-again false 
	:description "All players lose 8 bricks"              
	:cost {:type :bricks :amount 0}   
	:effects [
	  (get-effect {:effected :you :resource :bricks :amount -8})
	  (get-effect {:effected :other :resource :bricks :amount -8})
	  ]
	}
	{:name "Lucky Cache"        
	:discardable true 
	:play-again true 
	:description "+2 Bricks, +2 Gems, Play again"         
	:cost {:type :bricks :amount 0}   
	:effects [
		(get-effect {:effected :you :resource :bricks :amount 2})
		(get-effect {:effected :you :resource :gems :amount 2})
		]
	}
	{:name "Friendly Terrain"   
	:discardable true 
	:play-again true 
	:description "+1 Wall, Play again"                    
	:cost {:type :bricks :amount 8}   
	:effects [
		(get-effect {:effected :you :resource :wall :amount 1})
		]
	}
	{:name "Miners"             
	:discardable true 
	:play-again false 
	:description "+1 Quarry" 
	:cost {:type :bricks :amount 3}   
	:effects [
		(get-effect {:effected :you :resource :quarry :amount 1})
	]
	}
	{:name "Mother load"             
	:discardable true 
	:play-again false 
	:description "If quarry is less than enemy's quarry, +2 quarry. Otherwise, +1 quarry" 
	:cost {:type :bricks :amount 4}   
	:effects [
		(get-effect {:effected :you :resource :quarry :amount [< [:you :quarry] [:other :quarry] 2 1]})
	]
	}
	{:name "Dwarven Miners"             
	:discardable true 
	:play-again false 
	:description "+4 Wall, +1 Quarry" 
	:cost {:type :bricks :amount 7}   
	:effects [
		(get-effect {:effected :you :resource :quarry :amount 1})
		(get-effect {:effected :you :resource :wall :amount 4})
	]
	}
	{:name "Work Overtime"             
	:discardable true 
	:play-again false 
	:description "+5 Wall, Lose 6 Gems" 
	:cost {:type :bricks :amount 2}   
	:effects [
		(get-effect {:effected :you :resource :wall :amount 5})
		(get-effect {:effected :you :resource :gems :amount -6})
	]
	}
	{:name "Copying the tech"             
	:discardable true 
	:play-again false 
	:description "If you have less quarry than the enemy, set your quarry to theirs." 
	:cost {:type :bricks :amount 5}   
	:effects [
		(fn [game] 
			(let [
				player-key (if (game :turn 0) :player1 :player2)
				enemy-key (if (game :turn 1) :player1 :player2)
				player (get game player-key)
				enemy (get game enemy-key)
				quarry (if (< (player :quarry) (enemy :quarry)) (enemy :quarry) (player :quarry))
				]
				(assoc game player-key (assoc player :quarry quarry))
			)
		)
	]
	}
	{:name "Basic Wall"             
	:discardable true 
	:play-again false 
	:description "+3 Wall" 
	:cost {:type :bricks :amount 2}   
	:effects [
		(get-effect {:effected :you :resource :wall :amount 3})
	]
	}
	{:name "Sturdy Wall"             
	:discardable true 
	:play-again false 
	:description "+4 Wall" 
	:cost {:type :bricks :amount 3}   
	:effects [
		(get-effect {:effected :you :resource :wall :amount 4})
	]
	}
	{:name "Innovations"             
	:discardable true 
	:play-again false 
	:description "+1 To everyone's quarry. +4 Gems for you." 
	:cost {:type :bricks :amount 3}   
	:effects [
		(get-effect {:effected :you :resource :gems :amount 4})
		(get-effect {:effected :you :resource :quarry :amount 1})
		(get-effect {:effected :other :resource :quarry :amount 1})
	]
	}
  ])