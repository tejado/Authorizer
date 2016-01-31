package com.box.androidsdk.content.models;

import junit.framework.TestCase;

import java.util.TimeZone;

public class BoxCommentTest extends TestCase {

    protected void setUp() {

    }

    public void testParseJson() {
        String testJson = "{\"type\":\"comment\",\"id\":\"191969\",\"is_reply_comment\":false,\"message\":\"These tigers are cool!\",\"created_by\":{\"type\":\"user\",\"id\":\"17738362\",\"name\":\"sean rose\",\"login\":\"sean@box.com\"},\"created_at\":\"2012-12-12T11:25:01-08:00\",\"item\":{\"id\":\"5000948880\",\"type\":\"file\"},\"modified_at\":\"2012-12-12T11:25:01-08:00\"}";

        TimeZone.setDefault(TimeZone.getTimeZone("GMT-8"));
        BoxComment test = new BoxComment();
        test.createFromJson(testJson);
        String json = test.toJson();
        assertEquals(testJson, json);
    }
}
