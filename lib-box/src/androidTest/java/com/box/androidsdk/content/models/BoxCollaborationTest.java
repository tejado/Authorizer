package com.box.androidsdk.content.models;

import junit.framework.TestCase;

import java.util.TimeZone;

public class BoxCollaborationTest extends TestCase {

    protected void setUp() {

    }

    public void testParseJson() {
        String testJson = "{\"type\":\"collaboration\",\"id\":\"791293\",\"created_by\":{\"type\":\"user\",\"id\":\"17738362\",\"name\":\"sean rose\",\"login\":\"sean@box.com\"},\"created_at\":\"2012-12-12T10:54:37-08:00\",\"modified_at\":\"2012-12-12T11:30:43-08:00\",\"expires_at\":\"2015-12-12T11:30:43-08:00\",\"status\":\"accepted\",\"accessible_by\":{\"type\":\"user\",\"id\":\"18203124\",\"name\":\"sean\",\"login\":\"sean+test@box.com\"},\"role\":\"editor\",\"acknowledged_at\":\"2012-12-12T11:30:43-08:00\",\"item\":{\"type\":\"folder\",\"id\":\"11446500\",\"sequence_id\":\"0\",\"etag\":\"0\",\"name\":\"Shared Pictures\"}}";

        TimeZone.setDefault(TimeZone.getTimeZone("GMT-8"));
        BoxCollaboration test = new BoxCollaboration();
        test.createFromJson(testJson);
        String json = test.toJson();
        assertEquals(testJson, json);
    }
}
