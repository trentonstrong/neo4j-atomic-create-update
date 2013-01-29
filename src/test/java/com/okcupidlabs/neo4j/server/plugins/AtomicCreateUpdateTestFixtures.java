package com.okcupidlabs.neo4j.server.plugins;

/**
 * Created with IntelliJ IDEA.
 * User: trentstrong
 * Date: 1/29/13
 * Time: 11:04 AM
 * To change this template use File | Settings | File Templates.
 */
public class AtomicCreateUpdateTestFixtures {
    public static final String THINGS_FOO_BAR_FIXTURE = "{" +
            "\"index_name\": \"things\"," +
            "\"index_key\": \"foo\"," +
            "\"index_value\": \"bar\"," +
            "\"body\": \"dat bangin' beetle body\"" +
            "}";

    public static final String PEOPLE_NAME_E_FIXTURE = "{" +
            "\"index_name\": \"people\"," +
            "\"index_key\": \"name\"," +
            "\"index_value\": \"E\"," +
            "\"properties\": {\"name\": \"E\"}" +
            "}";

    public static final String PEOPLE_NAME_A_FOO_BAR_FIXTURE = "{" +
            "\"index_name\": \"people\"," +
            "\"index_key\": \"name\"," +
            "\"index_value\": \"A\"," +
            "\"properties\": {\"foo\": \"bar\"}" +
            "}";
}
