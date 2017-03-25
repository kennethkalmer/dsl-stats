(ns dsl-stats.core
  (:require [dsl-stats.config :refer [conf]]
            [dsl-stats.mweb :as mweb]
            [cheshire.core :refer [generate-string]]
            [clj-http.client :as http]
            [clj-time.core :as t]
            [clj-time.coerce :as c]
            [clj-time.periodic :refer [periodic-seq]]
            [chime :refer [chime-at]]
            [clojure.tools.cli :refer [parse-opts]])
  (:import [org.joda.time DateTimeZone])
  (:gen-class))

(def schedule (->> (periodic-seq (.. (t/now)
                                     (withZone (DateTimeZone/forID "Africa/Johannesburg"))
                                     (withTime 8 0 0 0))
                                 (-> 1 t/days))))

(def cli-options
  [["-s" "--scheduled" (str "Run in foreground performing a check daily at " (take 1 schedule))]])

(defn usage-color [usage]
  (let [standard (:standard usage)
        nightly  (:nightly usage)
        check    (:month usage)]
    (cond
      (and (> check (:percentage standard)) (> check (:percentage nightly)))
      "good"
      (or (< check (:percentage standard)) (< check (:percentage nightly)))
      "warning"
      :else
      "danger"
      )))

(defn usage-payload [usage]
  (let [standard (:standard usage)
        nightly (:nightly usage)]
    {:username "adsl-usage"
     :icon_emoji ":grumpycat:"
     :fallback (str "ADSL usage: " (:used standard) "/" (:used nightly))
     :attachments
     [{
       :text "ADSL Usage Update"
       :color (usage-color usage)
       :fields
       [
        {:title "Standard usage"
         :value (str (:used standard) "GB of " (:total standard) "GB (" (:percentage standard) "%)")}
        {:title "Nightly usage"
         :value (str (:used nightly) "GB of " (:total nightly) "GB (" (:percentage nightly) "%)")}
        {:title "Month progress"
         :value (str (format "%.1f" (:month usage)) "%")}]
       }]}))

(defn post-adsl-usage []
  (let [usage (mweb/run)
        payload (usage-payload usage)
        webhook (:slack-webhook conf)]
    (println payload)
    (println (generate-string payload))
    (http/post webhook {:body (generate-string payload) :content-type :json :accept :json})))

;; Loopy constructs to keep it up forever
(def counter (atom 0))

(defn infinite-loop [fn]
  (fn)
  (future (infinite-loop fn))
  nil)

(defn -main [& args]
  (let [{:keys [options]} (parse-opts args cli-options)]
    (cond
      (:scheduled options)
      (do
        (println "Starting in scheduled mode")
        (chime-at schedule (fn [_] (post-adsl-usage)))
        (infinite-loop
         #(do
            (Thread/sleep 30000)
            (swap! counter inc))))
      :else
      (post-adsl-usage))))
