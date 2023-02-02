(ns html
  (:require
   [clojure.edn :as edn]
   [clojure.java.io :as java.io]
   [clojure.java.shell :as java.sh]
   [clj-commons.digest :as digest]
   [shadow.cljs.devtools.api :as shadow]
   [taoensso.timbre :as timbre]
   [taoensso.timbre.appenders.3rd-party.rolling :as appenders]
   [hiccup.core :as hiccup]
   [clojure.string :as str])
  (:gen-class))

(timbre/merge-config!
 {:appenders {:spit (appenders/rolling-appender {:path "./logs/build.log" :pattern :daily})}})

(defn index-html [chunks scripts links]
  [:html
   [:head
    [:meta {:charset "utf-8"}]
    [:title "revert-v3staker"]
    (map (fn [link]
           [:link link]) links)
    (map (fn [script]
           [:script script]) scripts)]
   [:body
    [:div#root]
    (map (fn [script]
           [:script script]) chunks)]])

(def manifest "./public/js/manifest.edn")
(def index "./public/index.html")

(def files [{:path "./public/css/main.css"
             :dest "./public/css"
             :href "/css"
             :name "main"
             :ext "css"
             :params {:rel "stylesheet" :type "text/css"}
             :cdn true}
            {:path "./public/rainbow/style.css"
             :dest "./public/css"
             :href "/css"
             :name "rainbow"
             :ext "css"
             :params {:rel "stylesheet" :type "text/css"}
             :cdn true}])

(def links [{:href "/fonts/RegioMono/fonts.css" :rel "stylesheet" :type "text/css" :cdn true}
            {:href "https://fonts.googleapis.com/css2?family=Material+Symbols+Sharp:opsz,wght,FILL,GRAD@20..48,100..700,0..1,-50..200" :rel "stylesheet"}])

(def scripts [])

(def not-found
  {:status 404
   :headers {"content-type" "text/plain"}
   :body "Not found."})

(defn handler [{:keys [http-config] :as req}]
  (let [accept (get-in req [:headers "accept"])]
    (if (and accept (not (str/includes? accept "text/html")))
      not-found
      (let [headers
            (get http-config :push-state/headers {"content-type" "text/html; charset=utf-8"})

            chunks [{:src "/js/main.js"}]

            links (->> files
                       (map (fn [file]
                              (assoc (:params file) :href (str/replace (:path file) "./public" ""))))
                       (concat links))]

        {:status 200
         :headers headers
         :body (->> (index-html chunks scripts links)
                    (hiccup/html)
                    (str "<!doctype html>"))}))))

(defn release [& args]
  (let [curr-env (into {} (System/getenv))
        cdn (get curr-env "CDN_BASE" "")]
    (shadow/release :whitespace) ;; hack to make tailwindcss read the class names well
    (java.sh/sh "npm" "run" "build-css" :env (assoc curr-env "NODE_ENV" "production"))
    (java.sh/sh "rm" "-rf" "./public/js" :env curr-env)
    (shadow/release :prod)
    (let [manifest
          (try
            (with-open [r (java.io/reader manifest)]
              (edn/read (java.io.PushbackReader. r)))
            (catch java.io.IOException e
              (timbre/error "Couldn't open '%s': %s\n" manifest (.getMessage e)))
            (catch RuntimeException e
              (timbre/error "Error parsing edn file '%s': %s\n" manifest (.getMessage e))))

          chunks (for [chunk manifest :when (#{:main} (:name chunk))]
                   {:src (format "%s/js/%s" cdn (:output-name chunk))})

          scripts (->> scripts
                       (map #(assoc % :src (format "%s%s" (if (:cdn %) cdn "") (:src %))))
                       (map #(dissoc % :cdn)))

          files (->> files
                     (map (fn [file]
                            (try
                              (assoc file :hash (-> (:path file)
                                                    (java.io/file)
                                                    (digest/md5)
                                                    (str/upper-case)))
                              (catch RuntimeException _ file))))
                     (map (fn [file]
                            (let [name (format "%s.%s.%s" (:name file) (:hash file) (:ext file))]
                              (java.io/copy (java.io/file (:path file)) (java.io/file (format "%s/%s" (:dest file) name)))
                              (assoc (:params file) :href (format "%s%s/%s" (if (:cdn file) cdn "") (:href file) name)))))
                     (map #(dissoc % :cdn)))
          links (->> links
                     (map #(assoc % :href (format "%s%s" (if (:cdn %) cdn "") (:href %))))
                     (map #(dissoc % :cdn)))
          html-str (->> (index-html chunks scripts (concat files links))
                        (hiccup/html)
                        (str "<!doctype html>"))]
      (try
        (with-open [w (java.io/writer index :write true)]
          (.write w html-str))
        (catch java.io.IOException e
          (timbre/error "Couldn't open '%s': %s\n" index (.getMessage e)))
        (catch RuntimeException e
          (timbre/error "Error parsing edn file '%s': %s\n" index (.getMessage e)))))))
