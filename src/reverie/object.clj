(ns reverie.object
  (:refer-clojure :exclude [get meta])
  (:require [clojure.string :as s]
            [clojure.zip :as zip]
            [clout.core :as clout]
            [korma.core :as k]
            [reverie.util :as util])
  (:use reverie.atoms
        reverie.entity))

(defn- get-last-order [{:keys [object-id area page-id]}]
  (let [page-id (or
                 page-id
                 (-> object (k/select (k/where {:id object-id})) first :page_id))]
    (+ 1
       (or
        (-> object (k/select (k/aggregate (max :order) :order)
                             (k/where {:page_id page-id :area (util/kw->str area)})) first :order)
        0))))

(defn- get-serial-object []
  (let [serial (-> object (k/select (k/aggregate (max :serial) :serial)) first :serial)]
    (if serial
      (+ 1 serial)
      1)))

(defn- beginning?
  "Used for move!"
  [loc]
  (= (zip/node loc) (zip/node (zip/leftmost loc))))
(defn- end?
  "Used for move!"
  [loc]
  (= (zip/node loc) (zip/node (zip/rightmost loc))))


(defn get-attributes [name]
  (let [name (keyword name)]
    (-> @objects name :options :attributes)))

(defn get-attributes-order
  "Get order in which the attributes are supposed to be in. Will look for attributes-order in options. If nothing is found it will send back a sorted key-list of the attributes."
  [name]
  (let [name (keyword name)]
    (or
     (-> @objects name :options :attributes-order)
     (sort (keys (get-attributes name))))))

(defn get* [w]
  (k/select object (k/where w) (k/order :order)))

(defn get [object-id & [cmd]]
  (let [obj (-> object (k/select (k/where {:id object-id})) first)
        data (-> obj
                 :name
                 (get-object-entity)
                 (k/select (k/where {:object_id (:id obj)}))
                 first)]
    (case cmd
      :name-object [data (keyword (:name obj))]
      :data-object [data obj]
      data)))

(defn attributes? [data attributes]
  (if (and (string? (:app_paths attributes))
           (string? (:app_paths data)))
    (let [attr1 (s/split (:app_paths attributes) #",")
          attr2 (s/split (:app_paths data) #",")]
      (and
       (= (dissoc attributes :app_paths) (select-keys data (keys (dissoc attributes :app_paths))))
       (= attr1 (filter (fn [a2] (some #(= a2 %) attr1)) attr2))))
    (= attributes (select-keys data (keys attributes)))))

(defn add! [{:keys [page-id name area]} obj]
  (let [name (clojure.core/name name)
        page-obj (k/insert object
                           (k/values {:page_id page-id :updated (k/sqlfn now)
                                      :name name
                                      :area (util/kw->str area)
                                      :serial (get-serial-object)
                                      :order (get-last-order {:page-id page-id
                                                              :area (util/kw->str area)})}))
        
        real-obj (k/insert (get-object-entity name)
                           (k/values (assoc obj :object_id (:id page-obj))))]
    page-obj))

(defn copy! [object-id]
  (let [[obj {:keys [name area page_id]}] (get object-id :data-object)]
    (add! {:page-id page_id :name name :area area} (dissoc obj :object_id :id))))

(defn update! [object-id obj-data]
  (let [table (-> object (k/select (k/where {:id object-id})) first :name get-object-entity)]
    (k/update table
              (k/set-fields obj-data)
              (k/where {:object_id object-id}))))

(defn render [request]
  (let [[obj obj-name] (get (get-in request [:reverie :object-id]) :name-object)]
    (if-let [f (or
                (get-in @objects [obj-name (:request-method request)])
                (get-in @objects [obj-name :any]))]
      (f request obj (:params request)))))

(defn move! [{:keys [object-id hit-mode anchor page-serial after-object-id]}]
  (let [{page-id :page_id area :area} (-> object (k/select (k/where {:id object-id})) first)
        objs (vec (map :id (k/select object
                                     (k/where {:page_id page-id
                                               :area area}))))]
    (case hit-mode
      "object-paste" (let [{page-id :page_id} (first (k/select object
                                                               (k/where {:id after-object-id})))
                           objs (vec (map :id (k/select object
                                                        (k/where {:page_id page-id
                                                                  :area (util/kw->str anchor)}))))
                           new-order (loop [loc (zip/vector-zip objs)]
                                       (let [next-loc (zip/next loc)]
                                         (if (zip/end? next-loc)
                                           (zip/root (zip/append-child (zip/vector-zip objs) object-id))
                                           (let [node-value (zip/node next-loc)]
                                             (if (= node-value after-object-id)
                                               (zip/root (zip/insert-right next-loc object-id))
                                               (recur next-loc))))))]
                       (k/update object
                               (k/set-fields {:area (util/kw->str anchor)
                                              :page_id page-id})
                               (k/where {:id object-id}))
                       (doseq [[obj-id order] (map vector new-order (range 1 (+ 1 (count new-order))))]
                         (k/update object
                                   (k/set-fields {:order order})
                                   (k/where {:id obj-id})))
                       true)
      "area-paste" (let [{page-id :id} (first (k/select page
                                                        (k/where {:serial page-serial
                                                                  :version 0})))]
                     (k/update object
                               (k/set-fields {:order (get-last-order {:object-id object-id
                                                                      :area (util/kw->str anchor)})
                                              :area (util/kw->str anchor)
                                              :page_id page-id})
                               (k/where {:id object-id}))
                     true)
      "area" (do
               (k/update object
                         (k/set-fields {:order (get-last-order {:object-id object-id
                                                                :area (util/kw->str anchor)})
                                        :area (util/kw->str anchor)})
                         (k/where {:id object-id}))
               true)
      "up" (let [new-order (loop [loc (zip/vector-zip objs)]
                             (let [next-loc (zip/next loc)]
                               (if (zip/end? next-loc)
                                 (zip/root loc)
                                 (let [node-value (zip/node next-loc)]
                                   (if (= object-id node-value)
                                     (if (beginning? next-loc)
                                       (zip/root next-loc)
                                       (-> next-loc
                                           zip/remove
                                           (zip/insert-left node-value)
                                           zip/root))
                                     (recur next-loc))))))]
             (doseq [[obj-id order] (map vector new-order (range 1 (+ 1 (count new-order))))]
               (k/update object
                         (k/set-fields {:order order})
                         (k/where {:id obj-id})))
             true)
      "down" (let [new-order (loop [loc (zip/vector-zip objs)]
                               (let [next-loc (zip/next loc)]
                                 (if (zip/end? next-loc)
                                   (zip/root loc)
                                   (let [node-value (zip/node next-loc)]
                                     (if (= object-id node-value)
                                       (if (end? next-loc)
                                         (zip/root next-loc)
                                         (-> next-loc
                                             zip/remove
                                             zip/next
                                             (zip/insert-right node-value)
                                             zip/root))
                                       (recur next-loc))))))]
               (doseq [[obj-id order] (map vector new-order (range 1 (+ 1 (count new-order))))]
                 (k/update object
                           (k/set-fields {:order order})
                           (k/where {:id obj-id})))
               true)
      "top" (let [new-order (loop [loc (zip/vector-zip objs)]
                              (let [next-loc (zip/next loc)]
                                (if (zip/end? next-loc)
                                  (zip/root loc)
                                  (let [node-value (zip/node next-loc)]
                                    (if (= object-id node-value)
                                      (if (beginning? next-loc)
                                        (zip/root next-loc)
                                        (-> next-loc
                                            zip/remove
                                            zip/leftmost
                                            (zip/insert-left node-value)
                                            zip/root))
                                      (recur next-loc))))))]
              (doseq [[obj-id order] (map vector new-order (range 1 (+ 1 (count new-order))))]
                (k/update object
                          (k/set-fields {:order order})
                          (k/where {:id obj-id})))
              true)
      "bottom" (let [new-order (loop [loc (zip/vector-zip objs)]
                                 (let [next-loc (zip/next loc)]
                                   (if (zip/end? next-loc)
                                     (zip/root loc)
                                     (let [node-value (zip/node next-loc)]
                                       (if (= object-id node-value)
                                         (do
                                           (if (end? next-loc)
                                             (zip/root next-loc)
                                             (-> next-loc
                                                 zip/remove
                                                 zip/next
                                                 zip/rightmost
                                                 (zip/insert-right node-value)
                                                 zip/root)))
                                         (recur next-loc))))))]
                 (doseq [[obj-id order] (map vector new-order (range 1 (+ 1 (count new-order))))]
                   (k/update object
                             (k/set-fields {:order order})
                             (k/where {:id obj-id})))
                 true)
      false)))
