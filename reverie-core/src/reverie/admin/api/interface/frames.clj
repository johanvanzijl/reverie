(ns reverie.admin.api.interface.frames
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.walk :as walk]
            [cheshire.core :refer [encode]]
            [hiccup.page :refer [html5]]
            [reverie.admin.looknfeel.common :as common]
            [reverie.admin.looknfeel.form :as form]
            [reverie.auth :as auth]
            [reverie.database :as db]
            [reverie.module.entity :as e]
            [reverie.object :as object]
            [reverie.page :as page]))



(defn edit-object [request page {:keys [object-id]}]
  (let [db (get-in request [:reverie :database])
        user (get-in request [:reverie :user])
        object (db/get-object db object-id)
        data {:id object-id
              :form-params
              (merge (object/initial-fields (object/name object))
                     (object/properties object)
                     (walk/keywordize-keys (:form-params request)))}]
    (if (auth/authorize? (object/page object) user db "edit")
      (html5
       (common/head "Object editing")
       [:body
        (form/get-object-form object data)
        (when (= (:request-method request) :post)
          [:script {:type "text/javascript"}
           "opener.dom.reload_main();"
           "window.close();"])])

      (html5
       [:head
        [:title "Object editing"]]
       [:body "You are not allowed to edit this object"]))))


(defn handle-object [request page {:keys [object-id] :as params}]
  (let [db (get-in request [:reverie :database])
        user (get-in request [:reverie :user])
        object (db/get-object db object-id)]
    (if (auth/authorize? (object/page object) user db "edit")
      (do (db/update-object! db object-id params)
          (edit-object request page params))
      (html5
       [:head
        [:title "Object editing"]]
       [:body "You are not allowed to edit this object"]))))



(defn richtext [request page {:keys [field object-id]}]
  (let [db (get-in request [:reverie :database])
        user (get-in request [:reverie :user])
        object (db/get-object db object-id)
        format (:format (e/field object (keyword field)))
        init-tinymce-js (slurp (io/resource "public/static/admin/js/init-tinymce.js"))]
    (if (auth/authorize? (object/page object) user db "edit")
      (html5
       (common/head "Object: Richtext")
       [:body
        [:textarea {:style "width: 400px; height: 600px;"}
         (get (object/properties object) (keyword field))]
        [:div.buttons
         [:button.btn.btn-primary {:id :save} "Save"]
         [:button.btn.btn-warning {:id :cancel} "Cancel"]]
        (common/footer {:filter-by #{:base :richtext}})
        (str "<script type='text/javascript'>"
             (str/replace init-tinymce-js #"\|\|extra-formats\|\|"
                          (if format
                            (str ", " (encode {:title "Custom", :items format}))
                            ""))
             "</script>")
        (when (= (:request-method request) :post)
          [:script {:type "text/javascript"}
           "opener.dom.reload_main();"
           "window.close();"])])

      (html5
       [:head
        [:title "Object editing"]]
       [:body "You are not allowed to edit this object"]))))


(defn url-picker [request page params]
  (let [db (get-in request [:reverie :database])]
    (html5
     (common/head "URL:s")
     [:body
      "url picker"])))


(defn file-picker [request page params]
  (let [db (get-in request [:reverie :database])]
    (html5
     (common/head "Files")
     [:body
      "file picker"])))