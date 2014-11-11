package com.smartbear.threescalesupport;

import com.eviware.soapui.SoapUI;
import com.eviware.soapui.impl.rest.RestService;
import com.eviware.soapui.impl.wsdl.WsdlProject;
import com.eviware.soapui.support.StringUtils;

import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.stream.JsonParsingException;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.json.JsonValue;
import com.smartbear.swagger.*;

public class ThreeScale{

    public static class ApiDefLink{
        public String name;
        public String systemName;
        public String description;
        public String path;
    }

    public static List<ApiDefLink> getApiDefLinks(URL developerPortalUrl) throws IOException{
        URL url = new URL(developerPortalUrl, "api_docs/services.json");
        Reader reader;
        try {
            reader = new InputStreamReader(url.openStream());
        }
        catch(FileNotFoundException e){
            throw new FileNotFoundException("API specification list has not been found at the expected URL");
        }
        final javax.json.JsonReader jsonReader = javax.json.Json.createReader(reader);
        JsonObject jsonObject;
        try {
            jsonObject = jsonReader.readObject();
        }
        catch(JsonParsingException e){
            throw new RuntimeException("The server response has not JSON format which is expected for API specification list.", e);
        }
        JsonValue apisValue = jsonObject.get("apis");
        if(apisValue == null || !(apisValue instanceof JsonArray)) throw new RuntimeException("API specification list has incorrect format: no \"apis\" entry has been found.");
        JsonArray apis = (JsonArray)apisValue;
        ArrayList<ApiDefLink> result = new ArrayList<ApiDefLink>();
        for(javax.json.JsonValue it: apis){
            if(it instanceof JsonObject){
                JsonObject item = (JsonObject) it;
                ApiDefLink apiLink = new ApiDefLink();
                apiLink.name = item.getString("name", null);
                apiLink.systemName = item.getString("system_name", null);
                apiLink.description = item.getString("description", null);
                apiLink.path = item.getString("path", null);
                if(StringUtils.hasContent(apiLink.name) && StringUtils.hasContent(apiLink.path)){
                    apiLink.path = new URL(developerPortalUrl, apiLink.path).toString();
                    result.add(apiLink);
                }
            }
        }
        return result;
    }


    public static RestService importAPItoProject(ApiDefLink apiLink,  WsdlProject project){
        SwaggerImporter importer = SwaggerUtils.createSwaggerImporter(apiLink.path, project);
        SoapUI.log("Importing Swagger from [" + apiLink.path + "]");
        return importer.importApiDeclaration(apiLink.path);
    }
}