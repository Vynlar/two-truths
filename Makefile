uberjar:
	npm install
	npx shadow-cljs release app
	clojure -X:uberjar