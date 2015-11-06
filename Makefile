figwheel:
	rlwrap lein run -m clojure.main script/figwheel.clj

uberjar:
	lein uberjar uberjar
