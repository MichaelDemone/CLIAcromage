(ns achromage.cards
	(:use achromage.utils))

(defn get-effect [{players :players resource :resource amt :amount}]
	(if (empty? players)
		(fn [game] game)
		(fn [game]
			(let [
				player-key (if (= (game :turn) 0) :player1 :player2)
				enemy-key (if (= (game :turn) 1) :player1 :player2)
				key-to-use (if (in? players :you) player-key enemy-key)
				player-to-use (if (in? players :you) :you :other)
				new-amt (+ (get-in game [key-to-use resource]) amt)
				new-game (assoc-in game [key-to-use resource] new-amt)
				new-function (get-effect {:players (remove #(= % player-to-use) players) :resource resource :amount amt})
				]
				(new-function new-game)
			)
		)
	)
)

(def cards [
	{:name "Brick Shortage"     
	:discardable true 
	:play-again false 
	:description "All players lose 8 bricks"              
	:cost {:type :bricks :amount 0}   
	:effects [
	  (get-effect {:players [:you :other] :resource :bricks :amount -8})
	  ]
	}
	{:name "Lucky Cache"        
	:discardable true 
	:play-again true 
	:description "+2 Bricks, +2 Gems, Play again"         
	:cost {:type :bricks :amount 0}   
	:effects [
		(get-effect {:players [:you] :resource :bricks :amount 2})
		(get-effect {:players [:you] :resource :gems :amount 2})
		]
	}
	{:name "Friendly Terrain"   
	:discardable true 
	:play-again true 
	:description "+1 Wall, Play again"                    
	:cost {:type :bricks :amount 8}   
	:effects [
		(get-effect {:players [:you] :resource :wall :amount 1})
		]
	}
	{:name "Miners"             
	:discardable true 
	:play-again false 
	:description "+1 Quarry" 
	:cost {:type :bricks :amount 3}   
	:effects [
		(get-effect {:players [:you] :resource :quarry :amount 1})
	]
	}
	{:name "Mother load"             
	:discardable true 
	:play-again false 
	:description "If quarry is less than enemy's quarry, +2 quarry. Otherwise, +1 quarry" 
	:cost {:type :bricks :amount 4}   
	:effects [
		(fn [game] 
			(let [
				player-key (if (game :turn 0) :player1 :player2)
				enemy-key (if (game :turn 1) :player1 :player2)
				player (get game player-key)
				enemy (get game enemy-key)
				quarry-gain (if (< (player :quarry) (enemy :quarry)) 2 1)
				]
				(assoc game player-key (assoc player :quarry (+ (player :quarry) quarry-gain)))
			)
		)
	]
	}
	{:name "Dwarven Miners"             
	:discardable true 
	:play-again false 
	:description "+4 Wall, +1 Quarry" 
	:cost {:type :bricks :amount 7}   
	:effects [
		(get-effect {:players [:you] :resource :quarry :amount 1})
		(get-effect {:players [:you] :resource :wall :amount 4})
	]
	}
	{:name "Work Overtime"             
	:discardable true 
	:play-again false 
	:description "+5 Wall, Lose 6 Gems" 
	:cost {:type :bricks :amount 2}   
	:effects [
		(get-effect {:players [:you] :resource :wall :amount 5})
		(get-effect {:players [:you] :resource :gems :amount -6})
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
		(get-effect {:players [:you] :resource :wall :amount 3})
	]
	}
	{:name "Sturdy Wall"             
	:discardable true 
	:play-again false 
	:description "+4 Wall" 
	:cost {:type :bricks :amount 3}   
	:effects [
		(get-effect {:players [:you] :resource :wall :amount 4})
	]
	}
	{:name "Innovations"             
	:discardable true 
	:play-again false 
	:description "+1 To everyone's quarry. +4 Gems for you." 
	:cost {:type :bricks :amount 3}   
	:effects [
		(get-effect {:players [:you] :resource :gems :amount 4})
		(get-effect {:players [:you :other] :resource :quarry :amount 1})
	]
	}
	{:name "Foundations"             
	:discardable true 
	:play-again false 
	:description "+1 To everyone's quarry. +4 Gems for you." 
	:cost {:type :bricks :amount 3}   
	:effects [
		(get-effect {:players [:you] :resource :gems :amount 4})
		(get-effect {:players [:you :other] :resource :quarry :amount 1})
	]
	}
  ])