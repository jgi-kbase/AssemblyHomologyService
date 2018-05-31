# Joint KBase / JGI Assembly Homology Service

This repo contains the KBase / JGI Assembly Homology Service (AHS). The service provides sequence
assembly matching based on implementations of the
[MinHash algorithm](https://ieeexplore.ieee.org/abstract/document/666900/?reload=tru).

Build status (master):
[![Build Status](https://travis-ci.org/jgi-kbase/AssemblyHomologyService.svg?branch=master)](https://travis-ci.org/jgi-kbase/AssemblyHomologyService)
[![codecov](https://codecov.io/gh/jgi-kbase/AssemblyHomologyService/branch/master/graph/badge.svg)](https://codecov.io/gh/jgi-kbase/AssemblyHomologyService)

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

The service is expected to contain <1000 namespaces, although there is no hard limit.

Most input strings do not allow empty strings and have a maximum size of 256 unicode code points.

## MinHash implementations

Currently only [Mash v2.0](https://genomebiology.biomedcentral.com/articles/10.1186/s13059-016-0997-x)
is supported. Mash is configured to never return sequences with a distance greater than 0.5.

## Requirements

Java 8 (OpenJDK OK)  
Apache Ant (http://ant.apache.org/)  
MongoDB 2.6+ (https://www.mongodb.com/)  
Jetty 9.3+ (http://www.eclipse.org/jetty/download.html)
    (see jetty-config.md for version used for testing)  
This repo (git clone https://github.com/kbaseIncubator/AssemblyHomologyService)  
The jars repo (git clone https://github.com/kbase/jars)  
The two repos above need to be in the same parent folder.

## Build

```
cd [assembly homology repo directory]
ant build
```

## Load data

These instructions assume
* MongoDB is running in a location accessible to the AHS.
* The `mash` binary is available in the system path.

Loading data is accomplished via the `assembly_homology` CLI. Get CLI help via the `-h` option:

```
$ ./assembly_homology -h
Usage: assembly_homology [options] [command] [command options]
  Options:
    -c, --config
*snip*
```

Currently only Mash sketch database uploads are supported. An upload requires 4 files:

* The assembly homology configuration file
* The sketch database
* A YAML file containing information about the namespace that will be created or updated at the
  end of the load
* A file containing, on each line, a JSON string containing metadata about each sequence in
  the sketch database.

### Assembly homology configuration file

The assembly homology configuration file contains the configuration information required for the
loader to run. Copy the `deploy.cfg.example` file to `deploy.cfg`
and fill it in appropriately.

### Namespace YAML file

The namespace YAML file contains 4 keys in a top level map:

```
id: mynamespace
datasource: KBase
sourcedatabase: CI Refdata
description: some reference data
```

`id` is the id of the namespace. This is an arbitrary string consisting of ASCII alphanumeric
characters and the underscore, with a maximum length of 256 Unicode code points.

`datasource` is an identifier for the source of the data, like KBase or JGI.

`sourcedatabase` (optional) is an identifier for the database within the `datasource` from
which the sketch database was generated. If `sourcedatabase` is omitted the value `default`
is used.

`description` (optional) is a free text description of the namespace.

### Sequence metadata file

The sequence metadata file contains multiple lines, each one corresponding to a sequence in the
sketch database. Each line is a JSON string:

```
{"sourceid": "15792/1/3", "id": "15792_1_3", "relatedids": {"NCBI": "GCF_000518705.1"}}
{"sourceid": "15792/4/3", "id": "15792_4_3", "relatedids": {"NCBI": "GCF_001735525.1"}}
```

`id` is the ID of the sequence in the sketch database. The loader will match the sequence metadata
to the sequence sketches with this ID.

`sourceid` is the ID of the sequence at the data source. This ID can be used to retrieve the
original sequence along with any other data available from the data source.

`sciname` (optional)(not shown) is the optional scientific name of the organism corresponding to
the sequence.

`relatedids` (optional) are IDs other than the source ID for the sequence, contained in a mapping
from the type or source of the ID (NCBI in this example) to the ID.

### Loading

An optional load ID may be provided to the loader. If a load ID is not provided, a random load ID
will be generated. Load IDs separate loads within a particular namespace and provide instantaneous
switches from one load to another. For example, if two data sets, one with load ID `A` and one
with load ID `B` are loaded into namespace `NS`:

* `A` is loaded and `NS` is created with load ID `A`. Users can now run queries against `NS`.
* The `B` load starts. The data in the load is kept separate from the `A` load in the database
  so queries against `NS` are not affected by the load.
* The `B` load completes and `NS`'s load ID is updated to `B`. Now any queries against `NS` will
  run against the `B` load.
* Eventually the `A` load will be reaped from the database after a period long enough to allow
  any in progress queries to complete.

Using a currently active load ID *will* affect any queries run or running against the namespace
while the load is in progress and may leave orphan data in the database (e.g. if the new sketch
database does not contain sequences in the prior load) and is generally not recommended, although
there are special cases where it may be useful, such as if a load partially completed.

Once the required data is assembled, load the data:

```
./assembly_homology load -k [path to sketch database] -n [path to namespace YAML file]
  -s [path to sequence metadata file]
```

The AHS expects that the sketch database will exist at the specified path once the load is
complete, so place the sketch database in a permanent location. The other files can be deleted
once the load is complete (although it may be advisable to retain them for reloads).

## Start service

ensure `mash` is available on the system path  
start mongodb  
cd into the assembly homology repo  
`ant build`  
copy `deploy.cfg.example` to `deploy.cfg` and fill in appropriately  
`export ASSEMBLY_HOMOLOGY_CONFIG=<path to deploy.cfg>`  
OR  
`export KB_DEPLOYMENT_CONFIG=<path to deploy.cfg>`  

`cd jettybase`  
`./jettybase$ java -jar -Djetty.http.port=<port> <path to jetty install>/start.jar`  

## API

Note that although namespace kmer sizes are returned in a list to support potential future
improvements, currently the service only supports one kmer size per namespace.

`GET /`

General server information including git commit, version, and server time.

`GET /namespace`

List all namespaces.

`GET /namespace/<namespace id>`

Returns information about a specific namespace.

`POST /namespace/<namespace id,namespace id,...>/search[?notstrict&max=<integer>]`

Performs a search with the sketch database provided in the `POST` body against the sketch
databases associated with the given namespaces. `curl -T` is useful for this:  
`curl -X POST -T kb_refseq_ci_1000_15792_446_1.msh http://localhost:20000/namespace/mynamespace/search`  
Currently the input sketch database must contain only one sequence with a single kmer size.  
Query parameters:  
* `notstrict` - if omitted, the server will return an error if the query sketch size is greater
  than any of the namespace sketch sizes. If `notstrict` is included, the server will return
  warnings instead. Any other parameter mismatches will result in an error.
* `max` - defines the maximum number of returned results. If missing, < 1, or > 100, `max` is
  set to 10.

## Developer notes

### Adding and releasing code

* Adding code
  * All code additions and updates must be made as pull requests directed at the develop branch.
    * All tests must pass and all new code must be covered by tests.
    * All new code must be documented appropriately
      * Javadoc
      * General documentation if appropriate
      * Release notes
* Releases
  * The master branch is the stable branch. Releases are made from the develop branch to the master
    branch.
  * Update the version as per the semantic version rules in `src/us/kbase/assemblyhomoloy/api/Root.java`.
  * Tag the version in git and github.

### Running tests

* Copy `test.cfg.example` to `test.cfg` and fill in the values appropriately.
  * If it works as is start buying lottery tickets immediately.
* `ant test`

### UI

Most text fields are arbitrary text entered by a data uploader. These fields should be
HTML-escaped prior to display.
  
Use common sense when displaying a field from the server regarding whether the field should be
html escaped or not.
  
### Exception mapping

In `us.kbase.assemblyhomology.core.exceptions`:  
`AssemblyHomologyException` and subclasses other than the below - 400  
`NoDataException` and subclasses - 404  

`JsonMappingException` (from [Jackson](https://github.com/FasterXML/jackson)) - 400  

Anything else is mapped to 500.

## TODO

* Reaper thread that finds sequence metadata with a namespace or load id that does not exist,
  is older than some time period, and deletes it. E.g. cleans up unfinished loads and reloads
  that overwrite some, but not all, of the prior load's data.
* Search namespaces (no free text search)
* HTTP2 support
* Private data (KBase, JGI)
* (Semi-?) realtime data updates
  * May be scheduled batch updates rather than near instantaneous
* Other implementations (SourMash, FastANI)
* Return implementation specific statistics (e.g. mash p-values etc)
* Collapsing closely related sequences