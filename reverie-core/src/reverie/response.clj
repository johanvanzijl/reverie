(ns reverie.response
  (:refer-clojure :exclude [get])
  (:require [clojure.core.match :refer [match]]
            [slingshot.slingshot :refer [throw+]]))


(defprotocol ResponseProtocol
  (get [response] [response location-or-body] [response headers body]))

(extend-protocol ResponseProtocol
  clojure.lang.PersistentArrayMap
  (get
    ([this] this)
    ([this _] (get 501))
    ([this _ _] (get 501)))
  java.lang.Long
  (get
    ([this] (get this nil nil))
    ([this location-or-body] (get this location-or-body nil))
    ([this headers body]
       (match [this (string? headers) (nil? body)]
              [301 true _] {:status this :headers {"Location" headers} :body ""}
              [302 true _] {:status this :headers {"Location" headers} :body ""}
              [307 true _] {:status this :headers {"Location" headers} :body ""}
              [308 true _] {:status this :headers {"Location" headers} :body ""}
              [_ true true] {:status this :headers {} :body headers}
              [_ _ _] {:status this
                       :headers (or headers {})
                       :body (or body
                                 (case this
                                   400 "400, Bad Request"
                                   401 "401, Unauthorized"
                                   402 "402, Payment Required"
                                   403 "403, Forbidden"
                                   404 "404, Page Not Found"
                                   405 "405, Method Not Allowed"
                                   406 "406, Not Acceptable"
                                   500 "500, Internal Server Error"
                                   501 "501, Not Implemented"
                                   ""))} )))
  nil
  (get
    ([this] nil)
    ([this _] nil)
    ([this _ _] nil)))

(defn response [status & args]
  (throw+ {:type :response :status status :args args}))