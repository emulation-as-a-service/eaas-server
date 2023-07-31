package de.bwl.bwfla.emil.utils;

import de.bwl.bwfla.blobstore.api.BlobDescription;
import de.bwl.bwfla.blobstore.api.BlobHandle;
import de.bwl.bwfla.blobstore.client.BlobStoreClient;
import de.bwl.bwfla.common.exceptions.BWFLAException;
import de.bwl.bwfla.common.utils.InputStreamDataSource;
import de.bwl.bwfla.emil.datatypes.rest.UploadResponse;
import de.bwl.bwfla.common.services.security.Role;
import de.bwl.bwfla.common.services.security.Secured;
import org.apache.tamaya.inject.api.Config;
import org.jboss.resteasy.plugins.providers.multipart.InputPart;
import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataInput;

import javax.activation.DataHandler;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Path("/upload")
@ApplicationScoped
public class Upload  {

    @Inject
    @Config(value = "rest.blobstore")
    private String blobStoreRestAddress;

    @Inject
    @Config(value = "ws.blobstore")
    private String blobStoreWsAddress;

    private static final String HTTP_FORM_HEADER_FILE = "file";

    @Secured(roles = {Role.PUBLIC})
    @POST
    @Path("/")
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    @Produces(MediaType.APPLICATION_JSON)
    public UploadResponse upload(InputStream inputStream, @QueryParam("filename") String filename)
    {
        UploadResponse response = new UploadResponse();

        try {
            final BlobDescription blob = new BlobDescription()
                    .setNamespace("user-upload")
                    .setData(new DataHandler(new InputStreamDataSource(inputStream)))
                    .setName(UUID.randomUUID().toString());

            if (filename == null)
                filename = blob.getName();

            blob.setDescription("user-uploaded file " + filename);

            BlobHandle handle = BlobStoreClient.get()
                    .getBlobStorePort(blobStoreWsAddress)
                    .put(blob);

            UploadResponse.UploadedItem item = new UploadResponse.UploadedItem(new URL(handle.toRestUrl(blobStoreRestAddress)), filename);
            response.getUploadedItemList().add(item);
            response.getUploads().add(handle.toRestUrl(blobStoreRestAddress));
        } catch (IOException | BWFLAException  e) {
            return new UploadResponse(new BWFLAException(e));
        }

        System.out.println(response);
        return response;
    }

    @Deprecated
    @Secured(roles = {Role.PUBLIC})
    @POST
    @Path("/")
    @Consumes("multipart/form-data")
    @Produces(MediaType.APPLICATION_JSON)
    public UploadResponse upload(MultipartFormDataInput input)
    {
        InputStream inputFile =  null;

        Map<String, List<InputPart>> uploadForm = input.getFormDataMap();
        List<InputPart> inputPartsFiles = uploadForm.get(HTTP_FORM_HEADER_FILE);

        if(inputPartsFiles == null)
            return new UploadResponse(new BWFLAException("invalid form data"));

        UploadResponse response = new UploadResponse();

        for (InputPart inputPart : inputPartsFiles) {
            try {
                MultivaluedMap<String, String> header = inputPart.getHeaders();
                String fileName = null;
                String[] contentDispositionHeader = header.getFirst("Content-Disposition").split(";");
                for (String name : contentDispositionHeader) {
                    if ((name.trim().startsWith("filename"))) {
                        String[] tmp = name.split("=");
                        fileName = tmp[1].trim().replaceAll("\"", "");
                    }
                }
                inputFile = inputPart.getBody(InputStream.class,null);

                final BlobDescription blob = new BlobDescription()
                        .setDescription("upload")
                        .setNamespace("user-upload")
                        .setData(new DataHandler(new InputStreamDataSource(inputFile)))
                        .setName(UUID.randomUUID().toString());

                BlobHandle handle = BlobStoreClient.get()
                        .getBlobStorePort(blobStoreWsAddress)
                        .put(blob);

                UploadResponse.UploadedItem item = new UploadResponse.UploadedItem(new URL(handle.toRestUrl(blobStoreRestAddress)), fileName);
                response.getUploadedItemList().add(item);
                response.getUploads().add(handle.toRestUrl(blobStoreRestAddress));
            } catch (IOException | BWFLAException  e) {
                return new UploadResponse(new BWFLAException(e));
            }
        }
        System.out.println(response.toString());
        return response;
    }
}
