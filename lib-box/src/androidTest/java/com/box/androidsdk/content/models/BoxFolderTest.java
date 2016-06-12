package com.box.androidsdk.content.models;

import junit.framework.TestCase;

import java.util.TimeZone;

public class BoxFolderTest extends TestCase {

    protected void setUp() {

    }

    public void testParseJson() {
        String folderJson = "{\"type\":\"folder\",\"id\":\"11446498\",\"sequence_id\":\"1\",\"etag\":\"1\",\"name\":\"Pictures\",\"created_at\":\"2012-12-12T10:53:43-08:00\",\"modified_at\":\"2012-12-12T11:15:04-08:00\",\"description\":\"Some pictures I took\",\"size\":629644,\"path_collection\":{\"entries\":[{\"type\":\"folder\",\"id\":\"0\",\"name\":\"All Files\"}],\"total_count\":1},\"created_by\":{\"type\":\"user\",\"id\":\"17738362\",\"name\":\"sean rose\",\"login\":\"sean@box.com\"},\"modified_by\":{\"type\":\"user\",\"id\":\"17738362\",\"name\":\"sean rose\",\"login\":\"sean@box.com\"},\"owned_by\":{\"type\":\"user\",\"id\":\"17738362\",\"name\":\"sean rose\",\"login\":\"sean@box.com\"},\"shared_link\":{\"url\":\"https://www.box.com/s/vspke7y05sb214wjokpk\",\"is_password_enabled\":false,\"download_count\":0,\"preview_count\":0,\"access\":\"open\",\"permissions\":{\"can_download\":true,\"can_preview\":true}},\"folder_upload_email\":{\"access\":\"open\",\"email\":\"upload.Picture.k13sdz1@u.box.com\"},\"parent\":{\"type\":\"folder\",\"id\":\"0\",\"name\":\"All Files\"},\"item_status\":\"active\",\"item_collection\":{\"entries\":[{\"type\":\"file\",\"id\":\"5000948880\",\"sequence_id\":\"3\",\"etag\":\"3\",\"sha1\":\"134b65991ed521fcfe4724b7d814ab8ded5185dc\",\"name\":\"tigers.jpeg\"}],\"total_count\":1,\"offset\":0,\"limit\":100}}";
        String apiFolderJson = "{\"type\":\"folder\",\"id\":\"3163144889\",\"etag\":\"3\",\"sequence_id\":\"3\",\"name\":\"FolderInfo\",\"created_at\":\"2015-02-23T19:47:10-08:00\",\"modified_at\":\"2015-02-23T20:40:08-08:00\",\"description\":\"FolderInfo Description\",\"size\":0,\"path_collection\":{\"total_count\":3,\"entries\":[{\"type\":\"folder\",\"id\":\"0\",\"sequence_id\":null,\"etag\":null,\"name\":\"All Files\"},{\"type\":\"folder\",\"id\":\"3163143991\",\"sequence_id\":\"0\",\"etag\":\"0\",\"name\":\"Integration Tests (DO NOT MODIFY)\"},{\"type\":\"folder\",\"id\":\"3167574657\",\"sequence_id\":\"0\",\"etag\":\"0\",\"name\":\"Folder\"}]},\"created_by\":{\"type\":\"user\",\"id\":\"230400369\",\"name\":\"Enterprise Test\",\"login\":\"enterprisetestboxer@gmail.com\"},\"modified_by\":{\"type\":\"user\",\"id\":\"230400369\",\"name\":\"Enterprise Test\",\"login\":\"enterprisetestboxer@gmail.com\"},\"trashed_at\":null,\"purged_at\":null,\"content_created_at\":\"2015-02-23T19:47:10-08:00\",\"content_modified_at\":\"2015-02-23T20:40:08-08:00\",\"owned_by\":{\"type\":\"user\",\"id\":\"230400369\",\"name\":\"Enterprise Test\",\"login\":\"enterprisetestboxer@gmail.com\"},\"shared_link\":{\"url\":\"https:\\/\\/app.box.com\\/s\\/44s213jfeooxfjal7nddrczmhl9hftpj\",\"download_url\":null,\"vanity_url\":null,\"effective_access\":\"open\",\"is_password_enabled\":false,\"unshared_at\":null,\"download_count\":0,\"preview_count\":0,\"access\":\"open\",\"permissions\":{\"can_download\":true,\"can_preview\":true}},\"folder_upload_email\":null,\"parent\":{\"type\":\"folder\",\"id\":\"3167574657\",\"sequence_id\":\"0\",\"etag\":\"0\",\"name\":\"Folder\"},\"item_status\":\"active\",\"sync_state\":\"not_synced\",\"has_collaborations\":false,\"permissions\":{\"can_download\":true,\"can_upload\":true,\"can_rename\":true,\"can_delete\":true,\"can_share\":true,\"can_invite_collaborator\":true,\"can_set_share_access\":true},\"tags\":[],\"can_non_owners_invite\":true,\"is_externally_owned\":false,\"allowed_shared_link_access_levels\":[\"collaborators\",\"open\"],\"allowed_invitee_roles\":[\"viewer\",\"editor\",\"previewer\",\"uploader\",\"previewer uploader\",\"co-owner\",\"viewer uploader\"],\"item_collection\":{\"total_count\":0,\"entries\":[],\"offset\":0,\"limit\":100,\"order\":[{\"by\":\"type\",\"direction\":\"ASC\"},{\"by\":\"name\",\"direction\":\"ASC\"}]}}";

        TimeZone.setDefault(TimeZone.getTimeZone("GMT-8"));
        BoxFolder folder = new BoxFolder();
        folder.createFromJson(folderJson);
        String json = folder.toJson();
        assertEquals(folderJson, json);
    }
}