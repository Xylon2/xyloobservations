(ns xyloobservations.queuefunctions
  (:gen-class)
  (:require [langohr.core      :as rmq]
            [langohr.channel   :as lch]
            [langohr.queue     :as lq]
            [langohr.consumers :as lc]
            [langohr.basic     :as lb]))

(def ^{:const true}
  default-exchange-name "")

(defn message-handler
  [ch {:keys [content-type delivery-tag type] :as meta} ^bytes payload]
  (spit "/home/joseph/cljdebug.txt" (str "starting delay" (format "[consumer] Received a message: %s, delivery tag: %d, content type: %s, type: %s"
                                                                  (String. payload "UTF-8") delivery-tag content-type type)))
  (Thread/sleep 30000)
  (spit "/home/joseph/cljdebug.txt" (str "ending delay" (format "[consumer] Received a message: %s, delivery tag: %d, content type: %s, type: %s"
                                                                  (String. payload "UTF-8") delivery-tag content-type type))))

(def conn  (rmq/connect))
(def ch    (lch/open conn))
(def qname "langohr.examples.hello-world")

(lq/declare ch qname {:exclusive false :auto-delete true})
(lc/subscribe ch qname message-handler {:auto-ack true})

(defn add [thing]
  (lb/publish ch default-exchange-name qname thing {:content-type "text/plain" :type "greetings.hi"}))
