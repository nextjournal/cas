# Nextjournal CAS

Library for storing content-addressed blobs to cloud buckets.

It currently only supports uploading to Google Cloud Storage

## Setup

- Install `gsutil` and have it's `bin` directory on your `PATH`
- use `gcloud auth login` and log in with a Google Cloud account which has write
access to the `nextjournal-cas-eu` bucket

## TODO

- [] add a nice way to call it from the command line
- [] abstraction for other cloud services
