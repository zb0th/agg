(ns ad-view-counter
  (require [agg.core :refer :all]
           [clojure.core.async :refer [chan close!]]
           [clojure.java.jdbc :as jdbc]
           [clj-time.core :as t]
           [clj-time.coerce :as c]))

(def db {:subprotocol "mysql"
         :subname "//192.168.59.103:3306/agg"
         :user "agg"
         :password "agg"})

(defn flush-state [partition-id counter-type {:keys [delta offset] :as state}]
  (let [now (c/to-date (t/now))]
    (jdbc/with-db-transaction [conn db :isolation :read-committed]
        (doseq [[ad-id value] delta] ;; one per transaction for now, support batching later
          (jdbc/execute! conn [(str "INSERT INTO ad_counter(ad_id, type, value, modification_date, creation_date)"
                                    " VALUES (?,?,?,?,?)"
                                    " ON DUPLICATE KEY UPDATE value = ?, modification_date = ?")
                               ad-id counter-type value now now value now]))
        (jdbc/execute! conn [(str "INSERT INTO ad_counter_offset(partition_id, offset) VALUES (?, ?)"
                                  " ON DUPLICATE KEY UPDATE offset = ?")
                             partition-id offset offset]))))

(def counter (atom 0))

(defn increment-counter [[result delta] ad-id]
  (swap! counter dec)
  (let [new-result (update-in result [ad-id] #(if % (inc %) 1))
        new-delta (assoc delta ad-id (get new-result ad-id))]
    [new-result new-delta]))


(defn fetch-event [partition-id offset]
  (let [n 100000]
    (swap! counter inc)
    {:value (+ (* partition-id n) (rand-int n)) :offset (inc offset)}))

;;(defn agg [in f init out n msecs]

(defn start-counting [counter-type partition-id]
  (let [in (chan 16)
        out (chan 16)]
    (agg in increment-counter {} out 128 (* 5 1000))
    (sample-from-offset (partial fetch-event partition-id) 0 10 in)
    (subscribe out (partial flush-state partition-id counter-type))
    in)
  )


(def c1 (start-counting "view" 1))
(def c2 (start-counting "view" 2))
(def c3 (start-counting "view" 3))
(def c4 (start-counting "view" 4))
(def c5 (start-counting "view" 5))
(def c6 (start-counting "view" 6))
(def c7 (start-counting "view" 7))
(def c8 (start-counting "view" 8))
(def c9 (start-counting "view" 9))
(def c10 (start-counting "view" 10))
(def c11 (start-counting "view" 11))
(def c12 (start-counting "view" 12))
(def c13 (start-counting "view" 13))
(def c14 (start-counting "view" 14))
(def c15 (start-counting "view" 15))

(close! c13)

@counter



