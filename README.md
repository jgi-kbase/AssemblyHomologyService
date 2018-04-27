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
* The implementation used to create the sketch database (e.g. Mash, Sourmash)
  * The parameters used to create the sketch database (kmer size and sketch size or scaling factor)
  
Note that searches against a namespace **may not be reproducible over time**. 

### API

## TODO