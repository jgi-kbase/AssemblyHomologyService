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
This repo (git clone https://github.com/jgi-kbase/AssemblyHomologyService)  
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

The namespace YAML file contains up to 5 keys in a top level map:

```
id: mynamespace
datasource: KBase
sourcedatabase: CI Refdata
description: some reference data
filterid: kbaseprod
```

`id` is the id of the namespace. This is an arbitrary string consisting of ASCII alphanumeric
characters and the underscore, with a maximum length of 256 Unicode code points.

`datasource` is an identifier for the source of the data, like KBase or JGI.

`sourcedatabase` (optional) is an identifier for the database within the `datasource` from
which the sketch database was generated. If `sourcedatabase` is omitted the value `default`
is used.

`description` (optional) is a free text description of the namespace.

`filterid` (optional) is the ID of the filter to associate with the namespace. The nature of
the ID depends on the filter implementation. For more information, see Filters below.

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

## Filters

A filter can be optionally attached to a namespace on load by specifying the filter ID in the
namespace YAML file. The filter ID depends on the filter implementation - consult the documentation
of the filter to determine the appropriate ID. The filter must be configured and enabled in
the `deploy.cfg` file used by the loader and the service.

A filter receives Minhash distances from the Minhash implementation and chooses, based on the
filter implementation, whether to pass them on to the rest of the system for further processing and
eventual presentation to the user.

If a user provides an authentication token to the service when requesting a Minhash search
(see API below), the token is passed to any filters attached to namespaces involved in that
search. Filters that make use of the token are expected to provide a name for their
authentication source. For a single search, all the filters activated in the search must share
the same authentication source. It is permissible to mix filters that have no authentication
source with filters with an authentication source. If a filter requires a token and no token
is provided, it will throw an error.

If a filter is specified for a namespace, on load the filter also validates that the sequence IDs
in the sketch database are acceptable. This validation is again implementation specific.

### KBase Authenticated Filter

The KBase authenticated filter accepts a KBase authentication token and filters out any distances
for sequences for which the user does not have access. If no token is supplied, it filters out all
distances except those for public sequences.

The filter expects the sketch database IDs to have the format `W_O_V`, where `W`is the integer
workspace ID, `O` is the integer object ID, and `V` is the version. The filter checks `W` is
contained in the set of workspace IDs to which the user has read access (either via public
workspaces or specific read grants), and if not, does not pass on the distance for that sequence.
Note that on load, the filter does not contact the workspace to validate the IDs; it validates
the format only.

The filter ID for the filter is either `kbaseprod`, `kbaseappdev`, `kbasenext`, or `kbaseci`
depending on how the filter is configured in the `deploy.cfg` file. The authentication source
for each filter is identical to the filter ID.

Only one filter per KBase environment (prod, appdev, next, or ci) can be configured.

The `deploy.cfg.example` file contains an example configuration for the KBase
filter, but the filter is not enabled by default.

### Implementing new filters

To add a new filter to the system:

* Implement `us.kbase.assemblyhomology.core.MinHashDistanceFilterFactory`.
  * See `us.kbase.assemblyhomology.filters.KBaseAuthenticatedFilterFactory` for an
    example.
* The factory must have a constructor that accepts a `Map<String, String>` as its only
  argument. The configuration supplied in the `deploy.cfg` file will be provided to the filter
  in this map.
* Be careful when specifying the authsource name. If filters already exist for the
  authentication source, follow their conventions. Using an incorrect name means that either
  * Users will encounter errors as their tokens are sent to the wrong authentication
    source or
  * Users will be unable to search namespaces from the same authentication source
    at the same time as their authsource names are different.
* Provide documentation regarding how to configure the filter, the ID of the filter, and
  the authsource of the filter.

Note that filters may buffer Minhash distances for batched processing if desired. If so the
`MinHashDistanceFilter.flush()` method must be implemented and must complete all pending
processing and either discard or pass on all distances to the collector.

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

```
HEADER (optional):
Authorization: <token>

POST /namespace/<namespace id,namespace id,...>/search[?notstrict&max=<integer>]
```

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

Some namespaces may allow, or require, an authorization token if the filter they're associated
with allows or requires one. If a token is allowed the `authsource` field in the namespace
listing will be populated with the name of the authentication source from which a token is
expected (for example, kbaseprod, kbaseci, jgi, etc.). In this case the user can provide the
token in the `Authorization` header. To determine whether the token is required or merely
allowed consult the server administrator or this documentation for filters provided with the
core system.  
TODO: add a field noting this in the namespace data structure?

Authorization sources may not be mixed together in a single search, but namespaces without an
authorization source specified may be searched at the same time as namespaces with one.

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
`AuthorizationException` and subclasses - 401  
`NoDataException` and subclasses - 404  

`JsonMappingException` (from [Jackson](https://github.com/FasterXML/jackson)) - 400  

Anything else is mapped to 500.

## TODO

* Search namespaces (no free text search)
* HTTP2 support
* (Semi-?) realtime data updates
  * May be scheduled batch updates rather than near instantaneous
* Other implementations (SourMash, FastANI)
* Return implementation specific statistics (e.g. mash p-values etc)
* Collapsing closely related sequences