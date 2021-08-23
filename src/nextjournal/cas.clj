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

(defn upload! [config file]
  (let [sha (base58-sha file)
        target-path (str (:bucket config) "/data/" sha)
        result @(process [(:exec-path config) "cp" file (str "gs://" target-path)]
                         {:out :string})]

    (if (zero? (:exit result))
      (str "https://storage.googleapis.com/" target-path)
      (throw (ex-info "error uploading to CAS"
                      {:file file
                       :config config
                       :sha sha})))))

(comment
  (upload! config "examples/nextjournal.png")
  (digest/sha2-512 (io/input-stream  "examples/nextjournal.png"))
  (base58-sha "examples/nextjournal.png"))
