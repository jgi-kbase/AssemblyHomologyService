# Assembly Homology Service release notes

## 0.1.1

* Adds a data reaper that, once per day, deletes data that is at least a week old and either has
  no existing namespace record in the Mongo database (e.g. a load didn't complete) or has no
  existing namespace / load ID combination (e.g. a load was superseded by a new load within the
  same namespace).
* Added a `lastmod` field to the namespace data structure in the API that provides the date
  the namespace was last modified.
* Made the timeout for MinHash processes configurable.

## 0.1.0

* Initial release