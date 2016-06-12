package com.box.androidsdk.content.models;

import junit.framework.TestCase;

import java.util.TimeZone;

public class BoxUserTest extends TestCase {

    protected void setUp() {

    }

    public void testParseJson() {
        String testJson = "{\"type\":\"user\",\"id\":\"181216415\",\"name\":\"sean rose\",\"login\":\"sean+awesome@box.com\",\"created_at\":\"2012-05-03T21:39:11-08:00\",\"modified_at\":\"2012-11-14T11:21:32-08:00\",\"role\":\"admin\",\"language\":\"en\",\"timezone\":\"Africa/Bujumbura\",\"space_amount\":11345156112,\"space_used\":1237009912,\"max_upload_size\":2147483648,\"tracking_codes\":[],\"can_see_managed_users\":true,\"is_sync_enabled\":true,\"status\":\"active\",\"job_title\":\"\",\"phone\":\"6509241374\",\"address\":\"\",\"avatar_url\":\"https://www.box.com/api/avatar/large/181216415\",\"is_exempt_from_device_limits\":false,\"is_exempt_from_login_verification\":false,\"enterprise\":{\"type\":\"enterprise\",\"id\":\"17077211\",\"name\":\"seanrose enterprise\"},\"my_tags\":[\"important\",\"needs review\"]}";

        TimeZone.setDefault(TimeZone.getTimeZone("GMT-8"));
        BoxUser test = new BoxUser();
        test.createFromJson(testJson);
        String json = test.toJson();
        assertEquals(testJson, json);
    }
}
