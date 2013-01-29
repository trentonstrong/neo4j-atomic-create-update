package com.okcupidlabs.neo4j.server.plugins;

import java.io.IOException;
import java.util.HashMap;

import com.sun.jersey.api.client.Client;
import org.codehaus.jackson.map.ObjectMapper;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.neo4j.graphdb.*;
import org.neo4j.graphdb.index.Index;
import org.neo4j.server.NeoServer;
import org.neo4j.server.helpers.ServerBuilder;
import org.neo4j.server.rest.JaxRsResponse;
import org.neo4j.server.rest.RestRequest;

import static org.junit.Assert.assertEquals;

public class AtomicCreateUpdateFunctionalTest {

    public static final Client CLIENT = Client.create();
    public static final String MOUNT_POINT = "/ext";
    private ObjectMapper objectMapper = new ObjectMapper();
    private NeoServer server;

    private static final RelationshipType KNOWS = DynamicRelationshipType.withName("KNOWS");

    @Before
    public void setUp() throws IOException {
        this.server = ServerBuilder.server()
                .withThirdPartyJaxRsPackage("com.okcupidlabs.neo4j.server.plugins", MOUNT_POINT)
                .build();
        this.server.start();
        populateDb(this.server.getDatabase().getGraph());
    }

    @Test
    public void shouldReturn400IfIndexDoesNotExist() throws IOException {
        RestRequest restRequest = new RestRequest(server.baseUri().resolve(MOUNT_POINT), CLIENT);
        JaxRsResponse response = restRequest.post("upsert", AtomicCreateUpdateTestFixtures.THINGS_FOO_BAR_FIXTURE);
        assertEquals(400, response.getStatus());
    }

    @Test
    public void shouldCreateNode() throws IOException {
        RestRequest restRequest = new RestRequest(server.baseUri().resolve(MOUNT_POINT), CLIENT);
        JaxRsResponse response = restRequest.post("upsert", AtomicCreateUpdateTestFixtures.PEOPLE_NAME_E_FIXTURE);
        assertEquals(response.getStatus(), 200);
        System.out.println(response.getEntity());
        HashMap responseMap = objectMapper.readValue(response.getEntity(), java.util.HashMap.class);
        HashMap dataMap = (HashMap)responseMap.get("data");
        assertEquals(dataMap.get("name"), "E");
    }


    private void populateDb(GraphDatabaseService db) {
        Transaction tx = db.beginTx();
        try
        {
            Node personA = createPerson(db, "A");
            Node personB = createPerson(db, "B");
            Node personC = createPerson(db, "C");
            Node personD = createPerson(db, "D");
            personA.createRelationshipTo(personB, KNOWS);
            personB.createRelationshipTo(personC, KNOWS);
            personC.createRelationshipTo(personD, KNOWS);
            tx.success();
        }
        finally
        {
            tx.finish();
        }
    }

    private Node createPerson(GraphDatabaseService db, String name) {
        Index<Node> people = db.index().forNodes("people");
        Node node = db.createNode();
        node.setProperty("name", name);
        people.add(node, "name", name);
        return node;
    }

    @After
    public void tearDown() {
        this.server.stop();
    }

}
