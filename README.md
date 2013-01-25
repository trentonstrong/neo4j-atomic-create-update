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
mvn install
```

Once the build/test phases complete successfully you should have output like the following:

```
[INFO]
[INFO] --- maven-jar-plugin:2.3.1:jar (default-jar) @ neo4j-atomic-create-update ---
[INFO] Building jar: /Users/trentstrong/Development/neo4j-atomic-create-update/target/neo4j-atomic-create-update-0.1.jar
[INFO]
[INFO] --- maven-install-plugin:2.3.1:install (default-install) @ neo4j-atomic-create-update ---
[INFO] Installing /Users/trentstrong/Development/neo4j-atomic-create-update/target/neo4j-atomic-create-update-0.1.jar to /Users/trentstrong/.m2/repository/org/okcupidlabs/neo4j-atomic-create-update/0.1/neo4j-atomic-create-update-0.1.jar
[INFO] Installing /Users/trentstrong/Development/neo4j-atomic-create-update/pom.xml to /Users/trentstrong/.m2/repository/org/okcupidlabs/neo4j-atomic-create-update/0.1/neo4j-atomic-create-update-0.1.pom
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time: 5.601s
[INFO] Finished at: Fri Jan 25 10:28:25 PST 2013
[INFO] Final Memory: 6M/81M
[INFO] ------------------------------------------------------------------------
```