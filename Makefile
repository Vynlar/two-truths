uberjar:
	npm install
	clojure -A:dev:shadow-cljs release app
	clojure -X:uberjar