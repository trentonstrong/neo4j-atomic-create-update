package com.okcupidlabs.neo4j.server.plugins;

import java.util.Map;
import java.net.URI;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.core.MediaType;

import org.neo4j.graphdb.*;
import org.neo4j.graphdb.index.UniqueFactory;
import org.neo4j.kernel.GraphDatabaseAPI;
import org.neo4j.kernel.impl.transaction.xaframework.ForceMode;
import org.neo4j.server.rest.domain.PropertySettingStrategy;
import org.neo4j.server.rest.repr.*;
import org.neo4j.server.rest.web.DatabaseActions;
import org.neo4j.server.rest.web.PropertyValueException;


/**
 * Class containing JAX-RS endpoints for atomically 'upserting' and 'upconnecting' nodes and edges.
 */
@Path("/")
public class AtomicCreateUpdate {

    private static final String REQUIRED_PARAMETERS = "from, to, relationship_type, properties";

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

    /**
     * Inserts or updates a new node by first determining whether that node exists by using a lookup index.
     *
     * @param force Force mode for transaction, normally used internally.
     * @param indexName Name of index to use for lookup
     * @param indexKey Index key to utilize for lookup
     * @param indexValue Index value to utilize for lookup.  Should be unique per index/key.
     * @param body JSON encoded properties to insert/merge with node properties.
     * @return JSON representation of node.
     */
    @POST
    @Path("/upsert/{index_name}/{index_key}/{index_value}")
    public Response upsertNode(
                final @HeaderParam("Transaction") ForceMode force,
                final @PathParam("index_name") String indexName,
                final @PathParam("index_key") String indexKey,
                final @PathParam("index_value") String indexValue,
                final String body)
    {
        if (!this.service.index().existsForNodes(indexName)) {
            return output.badRequest(
                    new IllegalArgumentException("Index with index_name: " + indexName + " does not exist."));
        }

        UniqueFactory<Node> nodeFactory = new UniqueFactory.UniqueNodeFactory(service, indexName)
        {
            @Override
            protected void initialize( Node created, Map<String, Object> properties )
            {
                //noop
            }
        };

        final Node upsertedNode = nodeFactory.getOrCreate(indexKey, indexValue);
        try {
            this.propertySetter.setProperties(upsertedNode, input.readMap(body));
        } catch (BadInputException e) {

            return output.badRequest(e);
        } catch (ArrayStoreException e) {

            return badJsonFormat(body);
        }

        return output.ok(new NodeRepresentation(upsertedNode));
    }

    /***
     * Connects two nodes if an edge of the given type does not already exist between them, otherwise updates the
     * edge properties.
     * @param force Force mode for transaction, normally used internally.
     * @param body JSON encoded parameters.  See docs for specific details.
     * @return JSON representation of edge.
     */
    @POST
    @Path("/upconnect")
    public Response upconnectNodes(
            final @HeaderParam("Transaction") ForceMode force,
            final String body)
    {
        final Map<String, Object> properties;
        try {
            properties = input.readMap(body);
        } catch (BadInputException e) {
            return output.badRequest(e);
        }

        if(!ensureRequiredUpconnectParameters(properties)) {
            return missingUpconnectParameters(properties);
        }

        long fromId = parseNodeIdFromURI(URI.create((String) properties.get("from")));
        long toId = parseNodeIdFromURI(URI.create((String)properties.get("to")));
        Node fromNode = this.service.getNodeById(fromId);
        Node toNode = this.service.getNodeById(toId);
        RelationshipType relationshipType = DynamicRelationshipType.withName(
                (String)properties.get("relationship_type"));
        Map<String, Object> relationshipProperties = (Map<String, Object>)properties.get("properties");

        Relationship upconnectedRelationship;
        try {
            upconnectedRelationship= createOrUpdateRelationship(fromNode, toNode, relationshipType, relationshipProperties);
        } catch (PropertyValueException e) {
            return output.badRequest(e);
        }

        return output.ok(new RelationshipRepresentation(upconnectedRelationship));
    }

    /**
     * Atomically creates or updates a an edge, utilizing Neo4j transactional locking.
     * @param fromNode Node to attach outgoing side of edge
     * @param toNode Node to attach incoming side of edge
     * @param type Edge type
     * @param properties Key/value pairs to associate with edge
     * @return The created or updated relationship
     * @throws PropertyValueException
     */
    private Relationship createOrUpdateRelationship(
            final Node fromNode,
            final Node toNode,
            final RelationshipType type,
            final Map<String, Object> properties) throws PropertyValueException
    {
        // check if relationship exists first, if it does update properties and GTFO
        Relationship relationship = fromNode.getSingleRelationship(type, Direction.OUTGOING);
        if (relationship != null) {
            this.propertySetter.setProperties(relationship, properties);
            return relationship;
        }

        // otherwise acquire write lock on from node
        Transaction tx = this.service.beginTx();
        Lock writeLock = tx.acquireWriteLock(fromNode);

        // check and see if we were beat to the lock, if so update and GTFO
        relationship = fromNode.getSingleRelationship(type, Direction.OUTGOING);
        if (relationship != null) {
            tx.finish();
            this.propertySetter.setProperties(relationship, properties);
            return relationship;
        }

        // alright, let's do the damn thing
        try {
            relationship = fromNode.createRelationshipTo(toNode, type);
            this.propertySetter.setProperties(relationship, properties);
            tx.success();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        } finally {
            tx.finish();
        }

        return relationship;
    }

    /**
     * Parses a node id from a Neo4j REST node URI.
     * @param uri URI to a given node
     * @return Node ID
     */
    private long parseNodeIdFromURI(URI uri)
    {
        String path = uri.getPath();
        String idStr = path.substring(path.lastIndexOf('/') + 1);
        return Long.parseLong(idStr);
    }

    /**
     * Validates that required parameters are supplied in upconnect property map
     * @param properties Map containing supplied parameters to upconnect endpoint
     * @return False if any required keys are missing
     */
    private boolean ensureRequiredUpconnectParameters(Map<String, Object> properties)
    {
        return properties.containsKey("from") &&
                properties.containsKey("to") &&
                properties.containsKey("relationship_type") &&
                properties.containsKey("properties");
    }


    /**
     * Helper method for generating response when required upconnect parameters are missing
     * @param properties Map containing supplied parametes to upconnect endpoint
     * @return Response representing failed conditions on upconnect endpoint
     */
    private Response missingUpconnectParameters(Map<String, Object> properties)
    {
        String[] receivedParams = properties.keySet().toArray(new String[0]);
        String receivedParamString = "";
        if (receivedParams.length > 0) {
            StringBuilder sb = new StringBuilder();
            sb.append(receivedParams[0]);

            for (int i = 1; i < receivedParams.length; i++) {
                sb.append(", ");
                sb.append(receivedParams[i]);
            }
            receivedParamString = sb.toString();
        }

        return Response.status( 400 )
                .type(MediaType.TEXT_PLAIN)
                .entity("Required parameters: " + REQUIRED_PARAMETERS + "\n"
                      + "Received parameters: " + receivedParamString)
                .build();
    }

    private Response badJsonFormat(String body) {
        return Response.status( 400 )
                .type( MediaType.TEXT_PLAIN )
                .entity( "Invalid JSON array in POST body: " + body )
                .build();
    }

}
