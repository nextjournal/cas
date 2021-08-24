(ns nextjournal.cas
  (:gen-class)
  (:require [babashka.process :refer [process]]
            [clojure.tools.cli :refer [parse-opts]]
            [clojure.java.io :as io]
            [multihash.core :as multihash]
            [multihash.digest :as digest]))

(def config
  {:bucket    "nextjournal-cas-eu"
   :exec-path "gsutil"})


(defn base58-sha [file]
  (with-open [is (io/input-stream file)]
    (multihash/base58 (digest/sha2-512 is))))

(defn upload!
  "Uploads a `file` content addressed to cloud storage

  For supported options in config see `nextjournal.cas/config`

  For now assumes `gsutil` as the exec, readily set up and authenticated

  If content-type is set, uses it as the Content-Type header. If it is not set,
  let the underlying tooling figure out a content-type. This works for common types
  like jpeg, png, etc."
  ([config file]
   (upload! config file nil))
  ([config file content-type]
   (assert (.exists (io/file file)) "File must exist")
   (let [sha         (base58-sha file)
         target-path (str (:bucket config) "/data/" sha)
         args        (filter some?
                             (flatten [(:exec-path config)
                                       (when content-type
                                         ["-h" (str "Content-Type:" content-type)])
                                       "cp"
                                       "-n" ;; No-clobber, don't upload an existing file
                                       file (str "gs://" target-path)]))
         result      @(process args
                               {:out :string
                                :err :string})]

     (if (zero? (:exit result))
       {:url (str "https://storage.googleapis.com/" target-path)
        :out (:out result)
        :err (:err result)}
       (throw (ex-info "error uploading to CAS"
                       {:file   file
                        :args   args
                        :config config
                        :sha    sha
                        :err    (:err result)}))))))

(comment
  (upload! config "examples/nextjournal.png")
  (upload! config "examples/foo.edn" "application/edn")
  (digest/sha2-512 (io/input-stream  "examples/nextjournal.png"))
  (base58-sha "examples/nextjournal.png"))


(def cli-options
  [["-c" "--content-type CONTENT_TYPE" "the Content-Type header to use for the uploaded file"]
   ["-h" "--help"]])


(defn -main [& args]
  (let [opts (parse-opts args cli-options)]
    (if (:help opts)
      (println :summary opts)
      (let [result (upload! config
                            (first (:arguments opts))
                            (:content-type (:options opts)))]
        (println (:err result))
        (println (:url result))
        (shutdown-agents)))))
