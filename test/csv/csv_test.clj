(ns csv.csv-test
	(:require 
		[clojure.test :refer :all] 
		[csv.csv :as csv]
	)
)

(deftest simple-csv-test
	(testing "Parsing super small csv"
		(let [
			csv-maps (csv/load-string "Name,Value\nTestName,TestValue\nTestName2,TestValue2")
			]
			(is (= ((first csv-maps) :Name) "TestName"))
			(is (= ((first csv-maps) :Value) "TestValue"))
			(is (= ((second csv-maps) :Name) "TestName2"))
			(is (= ((second csv-maps) :Value) "TestValue2"))
		)
	)
)