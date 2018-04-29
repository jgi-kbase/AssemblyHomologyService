# Joint KBase / JGI Assembly Homology Service

This repo contains the KBase / JGI Assembly Homology Service. The service provides sequence
assembly matching based on implementations of the
[MinHash algorithm](https://ieeexplore.ieee.org/abstract/document/666900/?reload=tru). Currently
the service supports
[Mash](https://genomebiology.biomedcentral.com/articles/10.1186/s13059-016-0997-x).

## Usage

MinHash sketch databases are organized by namespaces, where there is a 1:1 relationship between
a sketch database and a namespace. A namespace has the following properties:
* A unique string ID set by the creator of the namespace
* A string uniquely identifying the source of the data (e.g. JGI, KBase, etc)
* A string uniquely identifying the source database within the data source
* An optional free text description
* The implementation used to create the sketch database (e.g. Mash, Sourmash)
  * The parameters used to create the sketch database (kmer size and sketch size or scaling factor)
  
Note that searches against a namespace **may not be reproducible over time**. 

Most input strings do not allow empty strings and have a maximum size of 256 unicode code points.

### API

## TODO

* Reaper thread that finds sequence metadata with a namespace or load id that does not exist,
  is older than some time period, and deletes it. E.g. cleans up unfinished loads and reloads
  that overwrite some, but not all, of the prior load's data.
* Search namespaces (no free text search)
* Update source ID, source DB ID, and description fields of namespace