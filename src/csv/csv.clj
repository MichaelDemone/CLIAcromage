(ns csv.csv
	(:require [clojure.string :as str])
)

(defn zipmap-all [keys values-list]
	(map (partial zipmap keys) values-list)
)

;; Load a comma separated string and return a list of maps with column name as key and value as cell value.
(defn load-string [csv-string]
	(let [
		lines (str/split-lines csv-string)
		split-lines (map #(str/split % #",") lines)
		column-names (map keyword (first split-lines))
		vals (zipmap-all column-names (rest split-lines))
	]
		vals
	)
)