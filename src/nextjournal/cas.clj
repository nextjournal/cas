(ns nextjournal.cas
  (:gen-class)
  (:require [babashka.process :refer [process]]
            [clojure.tools.cli :refer [parse-opts]]
            [clojure.java.io :as io]
            [clojure.edn :as edn]
            [clojure.string :as str]
            [multiformats.hash :as hash]
            [multiformats.base.b58 :as b58]))

(defn read-config
  ([]
   (read-config nil))
  ([file]
   (-> (or file "nextjournal.edn")
       slurp
       edn/read-string)))

(defn base58-sha [s]
  (->> s hash/sha2-512 hash/encode b58/format-btc))

(defn mime-type [{:as _config :keys [mime-types]} file]
  (some-> (filter (fn [[k _]] (str/ends-with? file k)) mime-types)
          first
          val))

(defn upload!
  "Uploads a `file` content addressed to cloud storage

  For supported options in config see `nextjournal.cas/config`

  For now assumes `gsutil` as the exec, readily set up and authenticated

  If content-type is set, uses it as the Content-Type header. If it is not set,
  let the underlying tooling figure out a content-type. This works for common types
  like jpeg, png, etc."
  ([config src-file]
   (upload! config src-file nil))
  ([config src-file content-type]
   (assert (.exists (io/file src-file)) "File must exist")
   (let [sha         (base58-sha src-file)
         target-file (str (:target-path config) sha)
         content-type (or content-type (mime-type config src-file))
         args        (filter some?
                             (flatten [(:exec-path config)
                                       (when content-type
                                         ["-h" (str "Content-Type:" content-type)])
                                       "cp"
                                       "-n" ;; No-clobber, don't upload an existing file
                                       src-file
                                       target-file]))
         result      @(process args
                               {:out :string
                                :err :string})]

     (if (zero? (:exit result))
       {:url (str/replace-first target-file "gs://" "https://storage.googleapis.com/")
        :out (:out result)
        :err (:err result)}
       (throw (ex-info "error uploading to CAS"
                       {:src-file src-file
                        :args     args
                        :config   config
                        :sha      sha
                        :err      (:err result)}))))))

(comment
  (upload! (read-config "nextjournal.edn") "examples/nextjournal.png")
  (upload! (read-config "nextjournal.edn") "examples/foo.edn")
  (upload! (read-config "nextjournal.edn") "examples/foo.edn" "application/something-else")
  (base58-sha "examples/nextjournal.png"))


(def cli-options
  [["-c" "--content-type CONTENT_TYPE" "the Content-Type header to use for the uploaded file"]
   [nil "--config CONFIG" "the .edn configuration file to use (defaults to nextjournal.edn on the classpath)"]
   ["-h" "--help"]])


(defn -main [& args]
  (let [opts (parse-opts args cli-options)]
    (if (:help (:options opts))
      (do (println "Summary:")
          (println (:summary opts)))
      (let [config (read-config (:config (:options opts)))
            result (upload! config
                            (first (:arguments opts))
                            (:content-type (:options opts)))]
        (println (:err result))
        (println (:url result))
        (shutdown-agents)))))
