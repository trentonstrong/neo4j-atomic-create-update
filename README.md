# Neo4j Atomic Create / Update Endpoints

## Overview

This Neo4j server API extensions supports the concepts of 'upsertion' and 'upconnection', both of which are atomic
operations that create an entity if it does not exist and otherwise merge the supplied properties with those the entity
already contains.  This process combines the insertion/update processes into a single transactional operation,
hence the name 'upsert'.

The motivation for implementing these as a server-side extensions has to do with limitations of the Neo4j REST batch API
which prevent this type of operation from being implemented client-side.

## Installation

Installation is quite simple, but in order to build the extension you will need a working Java environment with Maven2
already installed.

First, clone the Git repository locally, then change to the repository directory and issue:

```
mvn package
```

Once the build/test phases complete successfully you should have output like the following:

```
Results :

Tests run: 8, Failures: 0, Errors: 0, Skipped: 0

[INFO]
[INFO] --- maven-jar-plugin:2.3.1:jar (default-jar) @ neo4j-atomic-create-update ---
[INFO] Building jar: /Users/trentstrong/Development/neo4j-atomic-create-update/target/neo4j-atomic-create-update-0.1.jar
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time: 6.124s
[INFO] Finished at: Fri Jan 25 11:12:46 PST 2013
[INFO] Final Memory: 14M/81M
[INFO] ------------------------------------------------------------------------
```

The important line is:

```
[INFO] Building jar: /Users/trentstrong/Development/neo4j-atomic-create-update/target/neo4j-atomic-create-update-0.1.jar
```

which points you to the resulting JAR.

Installation of the JAR to Neo4j requires copying the JAR into the Neo4j server's library path and editing a config file
to inform the Neo4j server of a new package containing API endpoints.

Copy the JAR to your server's plugins directory (replace $NEO4J_PATH with your Neo4j install directory)

```
cp neo4j-atomic-create-update-0.1.jar $NEO4J_PATH/plugins/
```

then edit $NEO4J_PATH/conf/neo4j-server.properties and find the line

```
#org.neo4j.server.thirdparty_jaxrs_classes=org.neo4j.examples.server.unmanaged=/examples/unmanaged
```

uncomment it edit it to look like:

```
org.neo4j.server.thirdparty_jaxrs_classes=com.okcupidlabs.neo4j.server.plugins=/
```

This informs the Neo4j server to mount our extension API endpoints anchored from the server root /.  By changing "/" to
any valid URL path fragment you can mount the extension URLs anywhere you would like.
