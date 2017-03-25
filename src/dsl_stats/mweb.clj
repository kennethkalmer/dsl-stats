(ns dsl-stats.mweb
  (:require [clj-http.client :as client]
            [net.cgrand.enlive-html :as html]
            [clojure.string :as str]
            [clj-time.core :as t]
            [dsl-stats.config :refer [conf]]))

(def ^:dynamic *username* (get-in conf [:mweb :username]))
(def ^:dynamic *password* (get-in conf [:mweb :password]))
(def ^:dynamic *base-url* "https://myaccount.mweb.co.za/mwebcore/myaccount")

(def cs (clj-http.cookies/cookie-store))

(defn url [path]
  (str *base-url* path))

(defn login
  ([]
   (login *username* *password*))
  ([username password]
   (let [response (client/post (url "/login/login.jsp")
                               {:form-params {:userIdentifier username
                                              :password password
                                              :hp ""
                                              :LoginUser "Sign In"}
                                :cookie-store cs})]
     (if (= 302 (:status response))
       true
       false))))

(defn open-capped-adsl-view []
  (let [response (client/get (url "/modules/services/adsl/view/cappedview.jsp")
                             {:cookie-store cs})]
    (html/html-snippet (:body response))))

(defn extract-usage [node]
  (let [attrs (:attrs node)
        percentage (Float/parseFloat (:data-percent attrs))
        [used total] (map #(Float/parseFloat %) (re-seq #"[\d\.]+" (:data-info attrs)))]
    {:used       used
     :total      total
     :percentage percentage}))

(def ^:dynamic *standard-usage-selector* [:#circlifulStandardUsage])
(def ^:dynamic *nightly-usage-selector* [:#circlifulNightUsage])

(defn get-standard-usage [dom]
  (extract-usage (first (html/select dom *standard-usage-selector*))))

(defn get-nightly-usage [dom]
  (extract-usage (first (html/select dom *nightly-usage-selector*))))

(defn get-month-progress []
  (let [today (t/now)
        month-end (t/number-of-days-in-the-month today)
        progress (double (* 100 (/ (t/day today) month-end)))]
    ;; (str "Month progress: " (format "%.1f" progress) "%")
    progress))

(defn run []
  (login)
  (let [dom (open-capped-adsl-view)
        n   (get-nightly-usage dom)
        s   (get-standard-usage dom)
        m   (get-month-progress)]
    {:standard s
     :nightly  n
     :month    m
     }))
