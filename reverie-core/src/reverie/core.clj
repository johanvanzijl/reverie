(ns reverie.core
  (:require [clojure.string :as str]
            [reverie.area :as a]
            [reverie.i18n :as i18n]
            [reverie.module :as module]
            [reverie.module.entity :as entity]
            [reverie.render :as render]
            [reverie.route :as route]
            [reverie.site :as site]
            [reverie.system :as sys]
            [reverie.template :as template]
            [reverie.util :as util]
            reverie.AreaException)
  (:import [reverie AreaException]))

(defmacro area
  ([name]
     (let [name (if (symbol? name) (keyword name) name)
           params (keys &env)]
       (cond
        (some #(= name %) [:body :headers :status])
        (throw (AreaException. "areas can't be named body, headers or status"))
        (and (some #(= 'request %) params)
             (some #(= 'page %) params))
        `(render/render (a/area (keyword ~name)) ~'request ~'page)
        :else (throw (AreaException. "area assumes variables 'request' and 'page' to be present. If you wish to use other named variables send them after the name of the area like this -> (area :a req p)")))))
  ([name display]
     (let [name (if (symbol? name) (keyword name) name)
           display (if (symbol? display) (keyword display) display)
           params (keys &env)]
       (cond
        (some #(= name %) [:body :headers :status])
        (throw (AreaException. "areas can't be named body, headers or status"))
        (and (some #(= 'request %) params)
             (some #(= 'page %) params))
        `(render/render (a/area ~(keyword name) ~(keyword display)) ~'request ~'page)
        :else (throw (AreaException. "area assumes variables 'request' and 'page' to be present. If you wish to use other named variables send them after the name of the area like this -> (area :a req p)")))))
  ([name request page]
     (let [name (if (symbol? name) (keyword name) name)]
       `(render/render (a/area (keyword ~name)) ~request ~page)))
  ([name display request page]
     (let [name (if (symbol? name) (keyword name) name)
           display (if (symbol? display) (keyword display) display)]
       `(render/render (a/area (keyword ~name) (keyword ~display)) ~request ~page))))

(defmacro deftemplate [name function]
  (let [name (keyword name)]
    `(swap! sys/storage assoc-in [:templates ~name]
            (template/template ~function))))

(defmacro defapp [name options routes]
  (let [name (keyword name)
        migration (:migration options)]
    `(do
       (i18n/load-from-options! ~options)
       (when ~migration
         (swap! sys/storage assoc-in [:migrations ~name] ~migration))
       (swap! sys/storage assoc-in [:apps ~name]
              {:app-routes (map route/route ~routes)
               :options ~options}))))

(defmacro defpage [path options routes]
  (let [properties {:name path :type :raw}
        migration (:migration options)]
    `(do
       (i18n/load-from-options! ~options)
       (when ~migration
         (swap! sys/storage assoc-in [:migrations ~name] ~migration))
       (swap! site/routes assoc ~path [(route/route [~path]) ~properties])
       (swap! sys/storage assoc-in [:raw-pages ~path]
              {:routes (map route/route ~routes)
               :options ~options}))))

(defmacro defmodule [name options & [routes]]
  (let [name (keyword name)
        interface? (:interface? options)
        migration (:migration options)
        path (str "/admin/frame/module/" (clojure.core/name name))]
    `(do
       (i18n/load-from-options! ~options)
       (when ~migration
         (swap! sys/storage assoc-in [:migrations ~name] ~migration))
       (swap! site/routes assoc ~path
              [(route/route [~path]) {:name ~name
                                      :path ~path
                                      :type :module}])
       (swap! sys/storage assoc-in [:modules ~name]
              {:options ~options
               :name ~name
               :module (module/module
                        ~name
                        (map entity/module-entity (:entities ~options))
                        ~options
                        (map route/route (if ~interface?
                                           (vec
                                            (concat
                                             ~routes
                                             (:module-default-routes @sys/storage)))
                                           ~routes)))}))))

(defmacro defobject [name options methods]
  (let [name (keyword name)
        migration (:migration options)]
    `(do
       (i18n/load-from-options! ~options)
       (when ~migration
         (swap! sys/storage assoc-in [:migrations ~name] ~migration))
       (swap! sys/storage assoc-in [:objects ~name]
              {:options ~options
               :methods ~methods
               :table (keyword
                       (or (get ~options :table)
                           (str/replace ~name #"/|\." "_")))}))))
