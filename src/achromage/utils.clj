(ns achromage.utils)

(def debug false)
(defn dprint [s]
  (if debug (println s) nil)
)

(defn in? 
	"true if collection contains element"
	[collection element]
	(some #(= element %) collection)
)

(defn get-input [inputs]
  (dprint "Get input called")
  (let [userinput (read-line)]

	(if (in? inputs userinput) 
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