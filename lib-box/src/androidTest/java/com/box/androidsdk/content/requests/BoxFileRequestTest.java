package com.box.androidsdk.content.requests;

import com.box.androidsdk.content.BoxApiFile;
import com.box.androidsdk.content.BoxException;
import com.box.androidsdk.content.models.BoxSharedLink;
import com.box.androidsdk.content.utils.BoxDateFormat;

import junit.framework.Assert;
import junit.framework.TestCase;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

public class BoxFileRequestTest extends TestCase {

    public void testFileUpdateRequest() throws NoSuchMethodException, BoxException, InvocationTargetException, IllegalAccessException, UnsupportedEncodingException, ParseException {
        String expected = "{\"name\":\"NewName\",\"description\":\"NewDescription\",\"parent\":{\"id\":\"0\"},\"shared_link\":{\"access\":\"collaborators\",\"unshared_at\":\"2015-01-01T00:00:00-08:00\",\"permissions\":{\"can_download\":true}},\"tags\":[\"tag1\",\"tag2\"]}";

        TimeZone.setDefault(TimeZone.getTimeZone("GMT-8"));
        Date unshared = BoxDateFormat.parse("2015-01-01T00:00:00-08:00");
        List<String> tags = new ArrayList<String>();
        tags.add("tag1");
        tags.add("tag2");

        BoxApiFile fileApi = new BoxApiFile(null);
        BoxRequestsFile.UpdatedSharedFile updateReq = fileApi.getUpdateRequest("1")
                .setName("NewName")
                .setDescription("NewDescription")
                .setParentId("0")
                .updateSharedLink()
                .setAccess(BoxSharedLink
                        .Access.COLLABORATORS)
                .setUnsharedAt(unshared)
                .setCanDownload(true)
                .setTags(tags);

        String actual = updateReq.getStringBody();
        Assert.assertEquals(expected, actual);
    }
}