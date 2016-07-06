package com.box.androidsdk.content.models;

import junit.framework.TestCase;

import java.util.TimeZone;

public class BoxFileTest extends TestCase {

    protected void setUp() {

    }

    public void testParseJson() {
        String fileJson = "{\"type\":\"file\",\"id\":\"5000948880\",\"sequence_id\":\"3\",\"etag\":\"3\",\"sha1\":\"134b65991ed521fcfe4724b7d814ab8ded5185dc\",\"name\":\"tigers.jpeg\",\"description\":\"a picture of tigers\",\"size\":629644,\"path_collection\":{\"entries\":[{\"type\":\"folder\",\"id\":\"0\",\"sequence_id\":\"1\",\"etag\":\"1\",\"name\":\"All Files\"},{\"type\":\"folder\",\"id\":\"11446498\",\"sequence_id\":\"1\",\"etag\":\"1\",\"name\":\"Pictures\"}],\"total_count\":2},\"created_at\":\"2012-12-12T10:55:30-08:00\",\"modified_at\":\"2012-12-12T11:04:26-08:00\",\"created_by\":{\"type\":\"user\",\"id\":\"17738362\",\"name\":\"sean rose\",\"login\":\"sean@box.com\"},\"modified_by\":{\"type\":\"user\",\"id\":\"17738362\",\"name\":\"sean rose\",\"login\":\"sean@box.com\"},\"owned_by\":{\"type\":\"user\",\"id\":\"17738362\",\"name\":\"sean rose\",\"login\":\"sean@box.com\"},\"shared_link\":{\"url\":\"https://www.box.com/s/rh935iit6ewrmw0unyul\",\"download_url\":\"https://www.box.com/shared/static/rh935iit6ewrmw0unyul.jpeg\",\"vanity_url\":\"https://app.box.com/vanity\",\"is_password_enabled\":false,\"unshared_at\":\"2012-12-12T11:04:26-08:00\",\"download_count\":0,\"preview_count\":0,\"access\":\"open\",\"permissions\":{\"can_download\":true,\"can_preview\":true}},\"parent\":{\"type\":\"folder\",\"id\":\"11446498\",\"sequence_id\":\"1\",\"etag\":\"1\",\"name\":\"Pictures\"},\"item_status\":\"active\"}";

        TimeZone.setDefault(TimeZone.getTimeZone("GMT-8"));
        BoxFile file = new BoxFile();
        file.createFromJson(fileJson);
        String json = file.toJson();
        assertEquals(fileJson, json);
    }
}