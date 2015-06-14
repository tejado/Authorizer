package com.microsoft.onedriveaccess;

import com.microsoft.onedriveaccess.model.Drive;
import com.microsoft.onedriveaccess.model.Item;
import com.microsoft.onedriveaccess.model.UploadSession;

import java.util.Map;

import retrofit.Callback;
import retrofit.client.Response;
import retrofit.http.Body;
import retrofit.http.DELETE;
import retrofit.http.GET;
import retrofit.http.Headers;
import retrofit.http.PATCH;
import retrofit.http.POST;
import retrofit.http.PUT;
import retrofit.http.Path;
import retrofit.http.QueryMap;
import retrofit.mime.TypedByteArray;

/**
 * Service interface that will connect to OneDrive
 */
public interface IOneDriveService {

    /**
     * The ID of the root folder for a OneDrive
     */
    String ROOT_FOLDER_ID = "root";

    /**
     * Gets the default drive
     * @param driveCallback The callback when the drive has been retrieved
     */
    @GET("/v1.0/drive")
    @Headers("Accept: application/json")
    void getDrive(final Callback<Drive> driveCallback);

    /**
     * Gets the default drive synchronously
     */
    @GET("/v1.0/drive")
    @Headers("Accept: application/json")
    Drive getDrive();

    /**
     * Gets the specified drive
     * @param driveId the id of the drive to be retrieved
     * @param driveCallback The callback when the drive has been retrieved
     */
    @GET("/v1.0/drives/{drive-id}")
    @Headers("Accept: application/json")
    void getDrive(@Path("drive-id") final String driveId, final Callback<Drive> driveCallback);

    /**
     * Gets the root of the default drive
     * @param rootCallback The callback when the root has been retrieved
     */
    @GET("/v1.0/drives/root")
    @Headers("Accept: application/json")
    void getMyRoot(final Callback<Item> rootCallback);

    /**
     * Gets an item
     * @param itemId the item id
     * @param itemCallback The callback when the item has been retrieved
     */
    @GET("/v1.0/drive/items/{item-id}/")
    @Headers("Accept: application/json")
    void getItemId(@Path("item-id") final String itemId, final Callback<Item> itemCallback);

    /**
     * Gets an item with options
     * @param itemId the item id
     * @param options parameter options for this request
     * @param itemCallback The callback when the item has been retrieved
     */
    @GET("/v1.0/drive/items/{item-id}/")
    @Headers("Accept: application/json")
    void getItemId(@Path("item-id") final String itemId,
                   @QueryMap Map<String, String> options,
                   final Callback<Item> itemCallback);

    /**
     * Deletes an item
     * @param itemId the item id
     * @param callback The callback when the delete has been finished
     */
    @DELETE("/v1.0/drive/items/{item-id}/")
    void deleteItemId(@Path("item-id") final String itemId, final Callback<Response> callback);

    /**
     * Updates an item
     * @param itemId the item id
     * @param updatedItem the updated item
     * @param itemCallback The callback when the item has been retrieved
     */
    @PATCH("/v1.0/drive/items/{item-id}/")
    @Headers("Accept: application/json")
    void updateItemId(@Path("item-id") final String itemId, @Body Item updatedItem, final Callback<Item> itemCallback);

    /**
     * Creates a Folder on OneDrive
     * @param itemId the item id
     * @param newItem The item to create
     * @param itemCallback The callback when the item has been retrieved
     */
    @POST("/v1.0/drive/items/{item-id}/children")
    @Headers("Accept: application/json")
    void createItemId(@Path("item-id") final String itemId,
                      @Body Item newItem,
                      final Callback<Item> itemCallback);

    /**
     * Creates a file on OneDrive
     * @param itemId the item id
     * @param fileName The name of the file that is being created
     * @param fileBody the contents of the file
     * @param itemCallback The callback when the item has been retrieved
     */
    @PUT("/v1.0/drive/items/{item-id}/children/{file-name}/content")
    void createItemId(@Path("item-id") final String itemId,
                      @Path("file-name") final String fileName,
                      @Body TypedByteArray fileBody,
                      final Callback<Item> itemCallback);

    /**
     * Creates a large file upload session
     * @param itemId The item id
     * @param fileName The name of the file that is being created
     * @param sessionCallback the callback when the session has been created
     */
    @POST("/v1.0/drive/items/{item-id}:/{file-name}:/upload.createSession")
    void createUploadSession(@Path("item-id") final String itemId,
                       @Path("file-name") final String fileName,
                       final Callback<UploadSession> sessionCallback);
}
