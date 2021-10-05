# Nextjournal CAS

Library for storing content-addressed blobs to cloud buckets.

It currently only supports uploading to Google Cloud Storage.

## Setup

- Install `gsutil` and have it's `bin` directory on your `PATH`
- use `gcloud auth login` and log in with a Google Cloud account which has write
access to the `nextjournal-cas-eu` bucket

## Use from the command line

```
$ bb upload examples/foo.edn -c application/edn
$ bb upload examples/nextjournal.png
```

Looks like this:

```
$ bb upload examples/nextjournal.png           
Copying file://examples/nextjournal.png [Content-Type=image/png]...
/ [1 files][ 10.8 KiB/ 10.8 KiB]                                                
Operation completed over 1 objects/10.8 KiB.                                     

https://storage.googleapis.com/nextjournal-cas-eu/data/8Vsz72Uv7KB5DzU4UDFMmC1hU85JNgss6BkDS7zd3zyPiwZzJbNmZZHo3fe7VnhyD6B8GKLGwJrBaSVSPxwr7Wj4rH
```

## TODO

- [x] add a nice way to call it from the command line
- [ ] ability to override config via command line
- [ ] abstraction for other cloud services
