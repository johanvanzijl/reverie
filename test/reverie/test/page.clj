(ns reverie.test.page
  (:require [korma.core :as korma]
            [reverie.entity :as entity]
            [reverie.page :as page])
  (:use midje.sweet
        reverie.test.init
        ring.mock.request))




(fact
 "page/add!"
 (let [p (page/add! {:tx-data {:updated (korma/sqlfn now)
                               :name "Test" :title "Test"
                               :template :main :uri "/test"
                               :parent 0 :order 0 :version 0}})]
   (> (:page-id p) 0)) => true)

(fact
 "page/update!"
 (page/update! {:serial 1 :version 0 :tx-data {:name "Updated!"}})
 (:name (page/get {:serial 1 :version 0 })) => "Updated!")

(fact
 "page/edit!"
 (let [r (request :get "/test")]
  [(page/edit! r)
   (page/mode? r :edit)]) => [true true])

(fact
 "page/view!"
 (let [r (request :get "/test")]
   [(page/view! r)
    (page/mode? r :view)]) => [true true])
