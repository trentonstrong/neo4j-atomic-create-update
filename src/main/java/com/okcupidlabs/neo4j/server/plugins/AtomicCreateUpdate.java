package com.okcupidlabs.neo4j.server.plugins;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.index.IndexHits;
import org.neo4j.graphdb.index.IndexManager;
import org.neo4j.graphdb.index.Index;
import org.neo4j.server.plugins.Description;
import org.neo4j.server.plugins.Parameter;
import org.neo4j.server.plugins.PluginTarget;
import org.neo4j.server.plugins.ServerPlugin;
import org.neo4j.server.plugins.Source;
import org.neo4j.server.plugins.Name;

@Description("An extensions to the Neo4j Server for atomically creating or updating nodes/edges if they already exist.")
public class AtomicCreateUpdate extends ServerPlugin {

    @Name("upsert_node")
    @Description("Creates or patches a node given a unique index key and value for the node.")
    @PluginTarget(GraphDatabaseService.class)
    public Node upsertNode(
            @Source GraphDatabaseService service,
            @Description("The index name to use")
                @Parameter(name = "index_name") String indexName,
            @Description("The index key to use")
                @Parameter(name = "index_key") String indexKey,
            @Description("The index value to lookup")
                @Parameter(name = "index_value") String indexValue)
    {
        IndexManager indexManager = service.index();
        if (!indexManager.existsForNodes(indexName)) {
            throw new IllegalArgumentException("Index with index_name: " + indexKey + " does not exist.");
        }
        Index<Node> index = indexManager.forNodes(indexName);
        IndexHits<Node> hits = index.get(indexKey, indexValue);

        // invariant for a unique index: we should never have multiple hits
        // this allows us to be promiscuous with the hits iterator down below ;)
        boolean indexUniqueInvariant = hits.size() <= 1;
        assert indexUniqueInvariant : hits.size();
        boolean nodeFound = hits.size() == 1;

        service.
        Node upsertedNode = null;
        if (nodeFound) {
            upsertedNode = hits.next();


        } else {

        }
        return null;
    }

}
