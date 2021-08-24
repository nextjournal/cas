(ns nextjournal.cas
  (:require [babashka.process :refer [process]]
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

  For now assumes `gsutil` as the exec, readilty set up and authenticated

  If content-type is set, uses it as the Content-Type header. If it is not set,
  let the underlying tooling figure out a content-type. This works for common like
  jpeg, png, etc."
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
