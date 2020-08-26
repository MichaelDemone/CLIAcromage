(ns csv.csv
	(:require 
		[clojure.string :as str]
		[clojure.data.csv :as csv2]
		[clojure.java.io :as io]
	)
	(:use utils.general)
)

(defn zipmap-all [keys values-list]
	(map (partial zipmap keys) values-list)
)

;; Load a comma separated string and return a list of maps with column name as key and value as cell value.
(defn load-string [csv-string]
	(let [
		lines (str/split-lines csv-string) ;; Split on new lines (note, can't have new lines in descriptions.)
		split-lines (map #(str/split % #",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)") lines) ; Split on commas not in quotes. Regex thanks to https://stackoverflow.com/questions/1757065/java-splitting-a-comma-separated-string-but-ignoring-commas-in-quotes
		column-names (map keyword (first split-lines))
		vals (zipmap-all column-names (rest split-lines))
		]
		vals
	)
)

(defn load-file [file-name]
	(load-string (slurp file-name))
)

(defn csv-data->maps [csv-data]
  (map zipmap
       (->> (first csv-data) ;; First row is the header
            (map keyword)
            repeat)
		(rest csv-data)
	)
)

(defn load-file-better [file-name]
	(with-open [reader (io/reader file-name)]
		(doall
			(->> (csv2/read-csv reader)
						csv-data->maps
			)
		)
	)
)

