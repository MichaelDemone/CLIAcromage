(ns utils.general)

(def debug false)
(defn dprint [s]
  (if debug (println s) nil)
)

(defn format-keyword [word]
  (clojure.string/capitalize (clojure.string/replace (str word) #":" "")))

(defn in? 
	"true if collection contains element"
	[element collection]
	(some #(= element %) collection)
)

(defn parse-map [my-map keys-to-parse]
	(into {} 
		(map 
			(fn [[key value]] 
				(if (in? key keys-to-parse) 
					[key (clojure.edn/read-string value)]
					[key value] 
				)
			)
			my-map
		)
	)
)

(defn get-input [inputs]
  (dprint "Get input called")
  (let [userinput (read-line)]

	(if (in? userinput inputs) 
		userinput
		(do 
		(println "Please put in one of: " inputs)
		(recur inputs)
		)
	)
  )
)

(defn my-get [m key & args]
  (if (empty? args)
	(get m key)
	(apply my-get (get m key) args)))

(defn array-string [[first & rest-of-list]] 
	(if (empty? rest-of-list) 
		(str first)
		(str first (array-string rest-of-list))))

(defn contains-value? [val col]
	(if (some #(= (second %) val) col) true false)
)