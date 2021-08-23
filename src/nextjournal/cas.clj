(ns nextjournal.cas
  (:require [babashka.process :refer [process]]
            [clojure.java.io :as io]
            [multihash.core :as multihash]
            [multihash.digest :as digest]))

(def config
  {:bucket    "nextjournal-cas-eu"
   :exec-path "/Users/kommen/bin/google-cloud-sdk/bin/gsutil"})

(defn base58-sha [file]
  (with-open [is (io/input-stream file)]
    (multihash/base58 (digest/sha2-512 is))))

(defn upload!
  ([config file]
   (upload! config file nil))
  ([config file content-type]
   (let [sha         (base58-sha file)
         target-path (str (:bucket config) "/data/" sha)
         args        (flatten [(:exec-path config)
                               (when content-type
                                 ["-h" (str "Content-Type:" content-type)])
                               "cp" file (str "gs://" target-path)])
         result      @(process args
                               {:out :string})]

     (if (zero? (:exit result))
       (str "https://storage.googleapis.com/" target-path)
       (throw (ex-info "error uploading to CAS"
                       {:file   file
                        :args   args
                        :config config
                        :sha    sha}))))))

(comment
  (upload! config "examples/nextjournal.png")
  (upload! config "examples/foo.edn" "application/edn")
  (digest/sha2-512 (io/input-stream  "examples/nextjournal.png"))
  (base58-sha "examples/nextjournal.png"))
