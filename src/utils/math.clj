(ns utils.math)

(defn clamp [min max num]
	(if (< num min) 
		min 
		(if (> num max) 
			max 
			num
		)
	)
)