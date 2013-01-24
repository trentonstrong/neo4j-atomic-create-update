package com.okcupidlabs.neo4j.server.plugins;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.core.MediaType;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.index.IndexHits;
import org.neo4j.graphdb.index.UniqueFactory;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.kernel.GraphDatabaseAPI;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.TransactionFailureException;
import org.neo4j.kernel.impl.transaction.xaframework.ForceMode;
import org.neo4j.server.rest.domain.PropertySettingStrategy;
import org.neo4j.server.rest.repr.BadInputException;
import org.neo4j.server.rest.repr.InputFormat;
import org.neo4j.server.rest.repr.NodeRepresentation;
import org.neo4j.server.rest.repr.OutputFormat;
import org.neo4j.server.rest.web.DatabaseActions;
import org.neo4j.server.rest.web.NodeNotFoundException;
import sun.jvm.hotspot.tools.FinalizerInfo;


//An extension to the Neo4j Server for atomically creating or updating nodes/edges if they already exist.
@Path("/test")
public class AtomicCreateUpdate {

    private final UriInfo uriInfo;
    private final InputFormat input;
    private final OutputFormat output;
    private final DatabaseActions actions;
    private final GraphDatabaseService service;
    private final PropertySettingStrategy propertySetter;

    public AtomicCreateUpdate(@Context UriInfo uriInfo, @Context InputFormat input,
                              @Context OutputFormat output, @Context DatabaseActions actions,
                              @Context GraphDatabaseService service)
    {
        this.uriInfo = uriInfo;
        this.input = input;
        this.output = output;
        this.actions = actions;
        this.service = service;
        // NOTE: This is ugly as hell.  I don't want to depend on this cast but I do want
        // the PropertySettingStrategy instead of re-implementing that functionality.
        // WHATCHAGONNADO.
        this.propertySetter = new PropertySettingStrategy((GraphDatabaseAPI)service);

    }

    // Creates or patches a node given a unique index key and value for the node.
    @POST
    @Path("/upsert_node/{index_name}/{index_key}/{index_value}")
    public Response upsertNode(
                final @HeaderParam("Transaction") ForceMode force,
                @PathParam("index_name") String indexName,
                @PathParam("index_key") String indexKey,
                @PathParam("index_value") String indexValue,
                String body)
    {
        if (!this.service.index().existsForNodes(indexName)) {
            throw new IllegalArgumentException("Index with index_name: " + indexKey + " does not exist.");
        }

        UniqueFactory<Node> nodeFactory = new UniqueFactory.UniqueNodeFactory(service, indexName)
        {
            @Override
            protected void initialize( Node created, Map<String, Object> properties )
            {
                //noop
            }
        };

        Node upsertedNode = nodeFactory.getOrCreate(indexKey, indexValue);
        try {
            this.propertySetter.setProperties(upsertedNode, input.readMap(body));
        } catch (BadInputException e) {

            return output.badRequest(e);
        } catch (ArrayStoreException e) {

            return badJsonFormat(body);
        }

        return output.ok(new NodeRepresentation(upsertedNode));
    }

    private Response badJsonFormat(String body) {
        return Response.status( 400 )
                .type( MediaType.TEXT_PLAIN )
                .entity( "Invalid JSON array in POST body: " + body )
                .build();
    }

}
