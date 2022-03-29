(ns starter.reader
  (:require [clojure.string :as string]
            [clojure.pprint :refer [pprint]]))

;; Figure out how to read "\"some text\"" and preserve the quotes
(def re-patterns
  [:rygb-re             "\\s#\\.\\S+\\s"
   :keyed-re            "\\&{.+}"
   :template-literal    "#`(.+)`"
   ;; :backslash+quote    "\\\""
   ;; :stuff-in-quotes    "\"(?:\\\"|[^\"])*\""
   :px                  "[0-9]+px"
   :pct                 "[0-9]+%"
   :line-comment        ";[^\\n\\r]+?(?:[\\n\\r])"
   ])

(def global-re-pattern
  (->> re-patterns
      (map-indexed (fn [idx v] (when (odd? idx) v)))
      (remove nil?)
      (string/join "|")
      re-pattern))

;; What is going on here? talking about double quotes and/or escaped quotes within double quotes
(def pat (re-pattern "\"(?:\\\"|[^\"])*\""))
#_(println "pat find? " (re-find pat "\"\\\"\""))


(def litvar-re-string "\\$\\{([^\\{\\}]+)}")
(def litvar-re (re-pattern litvar-re-string))
(defn lit->cljstr [v]
  #_(println (str "|" v "|"))
  (let [marker "____"
        with-marked-vars (string/replace v litvar-re (fn [[_ x]] (str "${" marker x "}")))
        as-seq (string/split with-marked-vars litvar-re)
        with-quotes (map #(if (string/starts-with? % marker)
                            (string/replace % (re-pattern (str "^" marker)) "")
                            (str "\"" % "\""))
                         as-seq)
        ret (string/join " " with-quotes)]
    ret))

(defn lite-read-string [s]
  #_(println "s" s)
  (string/replace
   s
   global-re-pattern
   (fn [[m v]]
     #_(pprint {:m m :v v})
     (let [replaced (cond
                      ;; Do we want to keep around as comments? probably.
                      ;; (re-find #"^;" m) (str "(line-comment \"" (string/replace m #"^[\;\s]+|[\n\r]*$" "") "\")")
                      (re-find #"^;" m) ""
                      (re-find #"^\s#\." m) (string/replace (string/replace m #"^\s#\." "(rygb \"") #"\s$" "\")")
                      (re-find #"\&\{" m) (string/replace (string/replace m #"^\&\{" "(keyed ") #"\}$" ")")
                      (re-find #"^#`" m) (str "(str " (lit->cljstr v) ")")
                      (re-find #"px$" m) (str \" m \")
                      (re-find #"%$" m) (str \" m \")
                      (re-find #"#:[rygbv\\S]+" m) (str "(rygb \"" v "\")"))
           #_ #_ hi :bye]
       #_(println (str "\nMatch: `" m "` \n ... Replacing with: \n" replaced)
            "\nhi")
       replaced))))
                      ;; (re-find #"\"$" m) (str "\"")
