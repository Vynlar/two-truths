uberjar:
	npm install
	clojure -A:dev:frontend release app
	clojure -X:uberjar