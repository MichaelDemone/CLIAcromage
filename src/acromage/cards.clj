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
		(if ((resolve func) eval-param1 eval-param2)
			(get-nested-value game player-key enemy-key value-if-true)
			(get-nested-value game player-key enemy-key value-if-false)
		)
	)
)

;; This is some fun! You can supply an effect with the following:
;; :effected = :you/:other
;; :resource = (:gems/:magic/:tower/:wall/etc) OR [some-function [:other/:you resource] [:other/:you resource] value-if-true value-if-false]
;; For example: :resource could be [> [:other :wall] 10 :wall :tower]
;; That effectively says (if (> enemy-wall 10) :wall :tower) which could be used in a card that's "x damage to enemy tower if wall > 10, otherwise y damage to wall"
;; Similarly, :amount = some-int OR [some-function [:other/:you resource] [:other/:you resource] value-if-true value-if-false]
;; Example. [< [:you :wall] [:enemy :wall] [:enemy :wall] [:you :wall]] which will set your wall to enemy wall if it's smaller.
(defn get-effect [{effected :effected resource-param :resource amt-param :amount set :set}]
	(fn [game]
		(let [
			player-key (:turn game)
			enemy-key (if (= (game :turn) :player1) :player2 :player1)
			amt (if (int? amt-param) amt-param (get-value game player-key enemy-key amt-param))
			resource (if (keyword? resource-param) resource-param (get-value game player-key enemy-key resource-param))
			effected-key (if (= effected :you) player-key enemy-key)
			new-amt (if (or (nil? set) (not set)) (+ (get-in game [effected-key resource]) amt) amt)
			new-amt (max 0 new-amt)
			new-game (assoc-in game [effected-key resource] new-amt)
			]
			new-game
		)
	)
)

(defn load-cards [] 
	(->> 
		(csv/load-csv-file "resources/Cards.csv")
		(map #(update % :description (fn [a] (str/replace a "\"" ""))))
		(map #(utils/parse-map % [:discardable :play-again :type :cost :effects]))
	)
)