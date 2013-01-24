package com.okcupidlabs.neo4j.server.plugins;

import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.*;

import org.codehaus.jackson.map.ObjectMapper;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.neo4j.graphdb.*;
import org.neo4j.graphdb.index.Index;
import org.neo4j.kernel.impl.transaction.xaframework.ForceMode;
import org.neo4j.server.ServerTestUtils;
import org.neo4j.server.database.Database;
import org.neo4j.test.ImpermanentGraphDatabase;
import org.neo4j.test.server.EntityOutputFormat;
import org.neo4j.server.rest.domain.GraphDbHelper;
import org.neo4j.server.rest.web.DatabaseActions;
import org.neo4j.server.rest.paging.FakeClock;
import org.neo4j.server.rest.paging.LeaseManager;
import org.neo4j.server.rest.repr.formats.JsonFormat;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.net.URISyntaxException;



import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class AtomicCreateUpdateTest {

    private static final String BASE_URI = "http://neo4j.org/";
    private AtomicCreateUpdate service;
    private ObjectMapper objectMapper = new ObjectMapper();
    private static final RelationshipType KNOWS = DynamicRelationshipType.withName("KNOWS");
    private static Database database;
    private static GraphDbHelper helper;
    private static EntityOutputFormat output;
    private static LeaseManager leaseManager;
    private static final ForceMode FORCE = ForceMode.forced;

    @Before
    public void setUp() {
        database = new Database( ServerTestUtils.EPHEMERAL_GRAPH_DATABASE_FACTORY, null );
        helper = new GraphDbHelper( database );
        output = new EntityOutputFormat( new JsonFormat(), URI.create( BASE_URI ), null );
        leaseManager = new LeaseManager( new FakeClock() );
        service = new AtomicCreateUpdate( uriInfo(), new JsonFormat(), output,
                new DatabaseActions(database, leaseManager, ForceMode.forced, true), database.getGraph());
        populateDb(database.getGraph());
    }

    private static UriInfo uriInfo()
    {
        UriInfo mockUriInfo = mock( UriInfo.class );
        try
        {
            when( mockUriInfo.getBaseUri() ).thenReturn( new URI( BASE_URI ) );
        }
        catch ( URISyntaxException e )
        {
            throw new RuntimeException( e );
        }

        return mockUriInfo;
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

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowIfIndexMissing() {
        service.upsertNode(FORCE, "things", "foo", "bar", "bangin body");
    }

    @Test()
    public void shouldCreateNewNode() {
       final Response response = service.upsertNode(FORCE, "people", "name", "E", "{ \"name\": \"E\" }");
       assertEquals(response.getStatus(), 200);
       Node created = this.graphdb().index().forNodes("people").get("name", "E").getSingle();
       assertNotNull(created);
       assertEquals(created.getProperty("name"), "E");
    }

    @Test()
    public void shouldUpdateExistingNode() {
        final Response response = service.upsertNode(FORCE, "people", "name", "A", "{ \"foo\": \"bar\" }");
        assertEquals(response.getStatus(), 200);
        Node updated = this.graphdb().index().forNodes("people").get("name", "A").getSingle();
        assertEquals(updated.getProperty("name"), "A");
        assertEquals(updated.getProperty("foo"), "bar");
    }

    @After
    public void tearDown() throws Exception {
        try {
            database.shutdown();
        } catch (Throwable e) {
            // I don't care.
        }
    }

    @Test
    public void shouldQueryDbForFriends() throws IOException {
        //Response response = service.getFriends("B", db);
        //List list = objectMapper.readValue((String) response.getEntity(), List.class);
        //assertEquals(new HashSet<String>(Arrays.asList("A", "C")), new HashSet<String>(list));
    }

    public GraphDatabaseService graphdb() {
        return database.getGraph();
    }
}