(ns reverie.util
  (:require [clojure.string :as str]))

(defn shorten-uri
  "shortens the uri by removing the unwanted part. Used for defpage and defapp"
  [uri to-remove]
  (if (= to-remove "/")
    uri
    (let [uri (str/replace
               uri (re-pattern (str/replace to-remove #"/$" "")) "")]
      (if (str/blank? uri)
        "/"
        uri))))


(defn slugify [name]
  (-> name
      str
      (str/replace #"[åÅäÄĀāĀāÀÁÂÃÆàáâãæ]" "a")
      (str/replace #"[ČčÇç]" "c")
      (str/replace #"[Ðð]" "d")
      (str/replace #"[ĒēĒēËëÈÉÊËèéêë]" "e")
      (str/replace #"[Ğğ]" "g")
      (str/replace #"[ĪīĪīÏïİıìíîïÌÍÎÏ]" "i")
      (str/replace #"[Ĳĳ]" "ij")
      (str/replace #"[Ññ]" "n")
      (str/replace #"[öÖŐőŌōŌōŒœŒœòóôõöøÒÓÔÕÖØ]" "o")
      (str/replace #"[Þþ]" "p")
      (str/replace #"[Řř]" "r")
      (str/replace #"[ŠšŠšŠŞşŠš]" "s")
      (str/replace #"[ß]" "ss")
      (str/replace #"[ŰűŪūŪūÜüÙÚÛÜùúûü]" "u")
      (str/replace #"[ẀẁẂẃŴŵ]" "w")
      (str/replace #"[ŶŷŸýÝÿŸ]" "y")
      (str/replace #"[ŽžŽžŽžžŽ]" "z")
      (str/replace #"\s" "-")
      (str/replace #"\&" "-")
      (str/replace #"\." "-")
      (str/replace #":" "-")
      (str/replace #"[^a-zA-Z0-9\-\_\.]" "")
      (str/replace #"^-" "")
      (str/replace #"-$" "")
      (str/replace #"-{2,}" "-")
      str/lower-case))


(defn kw->str [x]
  (if (keyword? x)
    (str/replace (str x) #":" "")
    x))

(defn str->kw [x]
  (if (string? x)
    (keyword x)
    x))

(defn qsize [qs]
  (str/join "&" (map (fn [[k v]] (str (name k) "=" v)) qs)))
