# Assembly Homology Service release notes

## 0.1.4

* Switched the build system from Ant to Gradle. As such, all build artifacts, including the
  `assembly_homology` script, are now found in the `build` directory.

## 0.1.3

* Update the Java Mongo client to 3.10.1. This supports the latest Mongo version, 4.0.

## 0.1.2

* Increases the maximum number of returned distances to 1000 from 100.

## 0.1.1

* Adds an extensible Minhash distance filtering system. Filters can be associated with
  namespaces on load, are interposed between the Minhash implementation producing distances
  and delivery of the final results, and can filter the distances as desired. A filter
  implementation is provided that filters out sequences to which a KBase user does not have access.
* Adds a data reaper that, once per day, deletes data that is at least a week old and either has
  no existing namespace record in the Mongo database (e.g. a load didn't complete) or has no
  existing namespace / load ID combination (e.g. a load was superseded by a new load within the
  same namespace).
* Added a `lastmod` field to the namespace data structure in the API that provides the date
  the namespace was last modified.
* Made the timeout for MinHash processes configurable.

## 0.1.0

* Initial release