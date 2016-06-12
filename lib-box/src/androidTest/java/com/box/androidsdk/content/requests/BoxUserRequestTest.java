package com.box.androidsdk.content.requests;

import com.box.androidsdk.content.BoxApiUser;
import com.box.androidsdk.content.models.BoxUser;

import junit.framework.Assert;
import junit.framework.TestCase;

import java.io.UnsupportedEncodingException;
import java.text.ParseException;

public class BoxUserRequestTest extends TestCase {

    public void testCreateEnterpriseUserRequestProperties() throws ParseException {
        String login = "tester@gmail.com";
        String name = "tester";
        String address = "4440 El Camino Real";
        String jobTitle = "Tester";
        String phone = "123-456-7890";
        double space = 1000;
        String timezone = "Asia/Hong_Kong";

        BoxApiUser userApi = new BoxApiUser(null);
        BoxRequestsUser.CreateEnterpriseUser request = userApi.getCreateEnterpriseUserRequest(login, name)
                .setAddress(address)
                .setJobTitle(jobTitle)
                .setPhone(phone)
                .setSpaceAmount(space)
                .setRole(BoxUser.Role.COADMIN)
                .setStatus(BoxUser.Status.ACTIVE)
//                .setTrackingCodes()
                .setTimezone(timezone)
                .setCanSeeManagedUsers(true)
                .setIsExemptFromDeviceLimits(true)
                .setIsExemptFromLoginVerification(true)
                .setIsSyncEnabled(true);

        Assert.assertEquals(login, request.getLogin());
        Assert.assertEquals(name, request.getName());
        Assert.assertEquals(address, request.getAddress());
        Assert.assertEquals(jobTitle, request.getJobTitle());
        Assert.assertEquals(phone, request.getPhone());
        Assert.assertEquals(space, request.getSpaceAmount());
        Assert.assertEquals(timezone, request.getTimezone());
        Assert.assertEquals(BoxUser.Status.ACTIVE, request.getStatus());
        Assert.assertEquals(BoxUser.Role.COADMIN, request.getRole());
        Assert.assertTrue(request.getCanSeeManagedUsers());
        Assert.assertTrue(request.getIsExemptFromDeviceLimits());
        Assert.assertTrue(request.getIsExemptFromLoginVerification());
        Assert.assertTrue(request.getCanSeeManagedUsers());
        Assert.assertTrue(request.getIsSyncEnabled());
    }

    public void testCreateEnterpriseUserRequest() throws UnsupportedEncodingException {
        String expected = "{\"login\":\"tester@gmail.com\",\"name\":\"tester\",\"address\":\"4440 El Camino Real\",\"job_title\":\"Tester\",\"phone\":\"123-456-7890\",\"space_amount\":\"1000.0\",\"role\":\"coadmin\",\"status\":\"active\",\"timezone\":\"Asia/Hong_Kong\",\"can_see_managed_users\":\"true\",\"is_exempt_from_device_limits\":\"true\",\"is_exempt_from_login_verification\":\"true\",\"is_sync_enabled\":\"true\"}";

        String login = "tester@gmail.com";
        String name = "tester";
        String address = "4440 El Camino Real";
        String jobTitle = "Tester";
        String phone = "123-456-7890";
        double space = 1000;
        String timezone = "Asia/Hong_Kong";

        BoxApiUser userApi = new BoxApiUser(null);
        BoxRequestsUser.CreateEnterpriseUser request = userApi.getCreateEnterpriseUserRequest(login, name)
                .setAddress(address)
                .setJobTitle(jobTitle)
                .setPhone(phone)
                .setSpaceAmount(space)
                .setRole(BoxUser.Role.COADMIN)
                .setStatus(BoxUser.Status.ACTIVE)
//                .setTrackingCodes()
                .setTimezone(timezone)
                .setCanSeeManagedUsers(true)
                .setIsExemptFromDeviceLimits(true)
                .setIsExemptFromLoginVerification(true)
                .setIsSyncEnabled(true);

        String json = request.getStringBody();
        Assert.assertEquals(expected, json);
    }
}
