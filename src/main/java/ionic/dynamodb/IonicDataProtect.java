/*
* (c) 2018 Ionic Security Inc.
* By using this code, I agree to the Terms & Conditions (https://www.ionic.com/terms-of-use/)
* and the Privacy Policy (https://www.ionic.com/privacy-notice/).
*/

package ionic.dataprotect;

import java.util.UUID;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.List;

import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.AmazonServiceException;

import com.amazonaws.services.dynamodbv2.model.AttributeDefinition;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.KeySchemaElement;
import com.amazonaws.services.dynamodbv2.model.KeyType;
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughput;
import com.amazonaws.services.dynamodbv2.model.ScalarAttributeType;
import com.amazonaws.services.dynamodbv2.model.ResourceNotFoundException;

import com.amazonaws.services.dynamodbv2.model.CreateTableRequest;
import com.amazonaws.services.dynamodbv2.model.CreateTableResult;

import com.amazonaws.services.dynamodbv2.model.ScanRequest;
import com.amazonaws.services.dynamodbv2.model.ScanResult;

import com.amazonaws.services.dynamodbv2.model.GetItemRequest;
import com.amazonaws.services.dynamodbv2.model.GetItemResult;

import com.amazonaws.services.dynamodbv2.model.ListTablesRequest;
import com.amazonaws.services.dynamodbv2.model.ListTablesResult;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import org.apache.commons.lang3.StringUtils;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.io.IOException;

import org.json.JSONObject;

//TODO: ADD IONIC IMPORTS HERE
import com.ionic.sdk.agent.Agent;
import com.ionic.sdk.device.profile.persistor.DeviceProfilePersistorBase;
import com.ionic.sdk.device.profile.persistor.DeviceProfilePersistorPlainText;
import com.ionic.sdk.error.SdkException;
import com.ionic.sdk.agent.cipher.chunk.ChunkCipherV2;
import com.ionic.sdk.agent.cipher.chunk.data.ChunkCryptoEncryptAttributes;
import com.ionic.sdk.agent.key.KeyAttributesMap;
import com.ionic.sdk.agent.cipher.chunk.data.ChunkCryptoChunkInfo;
import com.ionic.sdk.agent.request.getkey.GetKeysResponse;
import com.ionic.sdk.agent.request.getkey.GetKeysResponse.Key;
import com.ionic.sdk.agent.request.updatekey.UpdateKeysResponse;
//

public class IonicDataProtect
{
    private static boolean localDynamo = true; 

    public static void main(String[] args)
    {
        if(args.length == 0) {
            help();
        }

        Options options = new Options();
        
        options.addOption("p", "project", true, "");
        options.addOption("q", "profile", true, "");
        options.addOption("i", "id", true, "");
        options.addOption("f", "file", true, "");
        options.addOption("c", "classification", true, "");
        options.addOption("u", "user", true, "");
        options.addOption("h", "help", false, "");

        parse(args, options);
    }

    private static void parse(String[] args, Options options) {
        CommandLineParser parser = new BasicParser();
        CommandLine cmd = null;
        try {
            cmd = parser.parse(options, args);
        } catch(ParseException e) {
            System.out.println(e);
            help();
        }

        if(args[0].equals("post-create")) {
            String project_name = cmd.getOptionValue("p");
            String path_to_post = cmd.getOptionValue("f");
            String profile_path = cmd.getOptionValue("q");
            String user = cmd.getOptionValue("u");
            String classification = cmd.getOptionValue("c");

            createPost(project_name, path_to_post, profile_path, user, classification);
        }
        else if(args[0].equals("post-list")) {
            String project_name = cmd.getOptionValue("p");

            listPosts(project_name);
        }
        else if(args[0].equals("post-get")) {
            String project_name = cmd.getOptionValue("p");
            String post_id = cmd.getOptionValue("i");
            String profile_path = cmd.getOptionValue("q");

            getPost(project_name, post_id, profile_path);
        }
        else if(args[0].equals("project-list")) {
            listProjects();
        }
        else if(args[0].equals("post-list-access")) {
            String project_name = cmd.getOptionValue("p");
            String post_id = cmd.getOptionValue("i");
            String profile_path = cmd.getOptionValue("q");

            listAccess(project_name, post_id, profile_path);
        }
        else if(args[0].equals("post-update-classification")) {
            String project_name = cmd.getOptionValue("p");
            String post_id = cmd.getOptionValue("i");
            String profile_path = cmd.getOptionValue("q");
            String classification = cmd.getOptionValue("c");

            updateClassification(project_name, post_id, classification, profile_path);
        }
        else if(args[0].equals("post-add-user")) {
            String project_name = cmd.getOptionValue("p");
            String post_id = cmd.getOptionValue("i");
            String profile_path = cmd.getOptionValue("q");
            String user = cmd.getOptionValue("u");

            addUser(project_name, post_id, user, profile_path);
        }
        else {
            help();
        }
    }

    private static void help() {
        String usage =
        "USAGE: java -jar target/ionic-dataprotect-cloud-1.0.jar [COMMAND] [OPTIONS]" + "\n" +
        "\n";

        String options =
        "project-list" + "\n" +
        "\n" +
        "post-list -p ${PROJECT_NAME}" + "\n" +  
            "\t-p\t--project\tName of the project to list posts" + "\n" +  
            "\n" +
        "post-create -p ${PROJECT_NAME} -f ${FILE_PATH}" + "\n" + 
            "\t-p\t--project" + "\n" +
            "\t-f\t--file" + "\n" +
            "\t-u\t--user\t\t\t(optional)" + "\n" + 
            "\t-c\t--classification\t(optional)" + "\n" +  
            "\t-q\t--profile\t\t(optional)" + "\n" +    
        "\n" +
        "post-get -p ${PROJECT_NAME} -i ${POST_ID}" + "\n" +
            "\t-p\t--project" + "\n" +  
            "\t-i\t--id" + "\n" +  
        "\n" +
        "post-list-access -p ${PROJECT_NAME} -i ${POST_ID} -q ${PROFILE_PATH}" + "\n" +  
            "\t-p\t--project" + "\n" +  
            "\t-i\t--id" + "\n" +    
            "\t-q\t--profile" + "\n" +    
        "\n" + 
        "post-add-user -p ${PROJECT_NAME} -i ${POST_ID} -q ${PROFILE_PATH} -u ${USER_EMAIL}" + "\n" +    
            "\t-p\t--project" + "\n" +    
            "\t-i\t--id" + "\n" +    
            "\t-u\t--user" + "\n" +    
            "\t-q\t--profile" + "\n" +    
        "\n" +
        "post-update-classification -p ${PROJECT_NAME} -i ${POST_ID} -c ${CLASSIFICATION}" + "\n" +    
            "\t-p\t--project" + "\n" +    
            "\t-i\t--id" + "\n" +    
            "\t-c\t--classification" + "\n" +    
            "\t-q\t--profile" + "\n" +    
        "\n";
        System.out.println(usage + options);
        System.exit(1);
    }

    private static void displayPost(String project_name, String post_id, String body, String title, String author) {

        System.out.print("\n");
        System.out.format("%s%-50s\n", "PROJECT: ", project_name);
        System.out.format("%s\n", StringUtils.repeat("-", 80));
        System.out.format("%-5s%-10s%-60s\n", "|", "ID: ", post_id);
        System.out.format("%-5s%-10s%-60s\n", "|", "AUTHOR: ", author);
        System.out.format("%s%79s\n", "|", StringUtils.repeat("-", 79));
        System.out.format("%-5s%-10s%-60s\n", "|", "TITLE: ", title);
        System.out.format("%s%20s\n", "|", StringUtils.repeat("-", 20));
        System.out.format("%-5s%-10s%-60s\n", "|", "BODY: ", body);
        System.out.format("%s\n\n", StringUtils.repeat("-", 80));
    }

    private static void listProjects() {
        AmazonDynamoDB ddb = null;
        if(localDynamo) {
            ddb = AmazonDynamoDBClientBuilder.standard().withEndpointConfiguration(
                new AwsClientBuilder.EndpointConfiguration("http://localhost:8000", "us-east-1"))
                .build();
        } else {
            ddb = AmazonDynamoDBClientBuilder.defaultClient();
        }

        ListTablesResult result = null;
        try {
            ListTablesRequest request = new ListTablesRequest();
            result = ddb.listTables(request); 
        } catch (AmazonServiceException e) {
            System.out.println(e);
            help();
        }

        List<String> project_names = result.getTableNames();

        if(project_names.size() > 0) {
            for(String project_name : project_names) {
                System.out.format("\t* %s\n", project_name);
            }
        }
    }

    private static void createTable(String table_name) {
        AmazonDynamoDB ddb = null;
        if(localDynamo) {
            ddb = AmazonDynamoDBClientBuilder.standard().withEndpointConfiguration(
                new AwsClientBuilder.EndpointConfiguration("http://localhost:8000", "us-east-1"))
                .build();
        } else {
            ddb = AmazonDynamoDBClientBuilder.defaultClient();
        }

        ProvisionedThroughput provThru = new ProvisionedThroughput(new Long(10), new Long(10));
        AttributeDefinition attrDef = new AttributeDefinition("id", ScalarAttributeType.S);
        KeySchemaElement keySchema = new KeySchemaElement("id", KeyType.HASH);

        CreateTableRequest request = new CreateTableRequest()
            .withAttributeDefinitions(attrDef)
            .withKeySchema(keySchema)
            .withProvisionedThroughput(provThru)
            .withTableName(table_name);

        try {
            CreateTableResult result = ddb.createTable(request);
        } catch (AmazonServiceException e) {
            System.err.println(e.getErrorMessage());
            System.exit(1);
        }
    }

    private static void createPost(String project_name, String post_file_path, String profile_path, String user, String classification) {
        AmazonDynamoDB ddb = null;
        if(localDynamo) {
            ddb = AmazonDynamoDBClientBuilder.standard().withEndpointConfiguration(
                new AwsClientBuilder.EndpointConfiguration("http://localhost:8000", "us-east-1"))
                .build();
        } else {
            ddb = AmazonDynamoDBClientBuilder.defaultClient();
        }

        String post_string = null;
        try {
            post_string = new String(Files.readAllBytes(Paths.get(post_file_path)));
        } catch(IOException e) {
        }

        JSONObject post_json = new JSONObject(post_string);

        String title = post_json.getString("title");
        String author = post_json.getString("author");
        String body = post_json.getString("body");
        String post_id = UUID.randomUUID().toString();

//TODO: ADD CODE HERE
        if(profile_path != null) {
            body = encryptPost(body, profile_path, user, classification);
        }
//

        HashMap<String,AttributeValue> item_values = new HashMap<String,AttributeValue>();

        item_values.put("id", new AttributeValue(post_id));
        item_values.put("title", new AttributeValue(title));
        item_values.put("author", new AttributeValue(author));
        item_values.put("body", new AttributeValue(body));

        try {
            ddb.putItem(project_name, item_values);
        } catch(ResourceNotFoundException e) {
            //If the project is not found, create a new table for the project and re-attempt to add the post
            createTable(project_name);
            ddb.putItem(project_name, item_values);
        } catch(AmazonServiceException e) {
            System.out.println(e);
            help();
        }

        displayPost(project_name, post_id, body, title, author);
    }

    private static void listPosts(String project_name) {
        AmazonDynamoDB ddb = null;
        if(localDynamo) {
            ddb = AmazonDynamoDBClientBuilder.standard().withEndpointConfiguration(
                new AwsClientBuilder.EndpointConfiguration("http://localhost:8000", "us-east-1"))
                .build();
        } else {
            ddb = AmazonDynamoDBClientBuilder.defaultClient();
        }

        ScanResult result = null;
        try {
            ScanRequest req = new ScanRequest()
                .withTableName(project_name);
            
            result = ddb.scan(req);
        } catch(AmazonServiceException e) {
            System.out.println(e);
            help();
        }

        System.out.format("%20s%-19s%s%20s%-20s\n", "", "Title", "|", "", "ID");
        System.out.format("%s\n", StringUtils.repeat("-", 80));
        for(Map<String,AttributeValue> item : result.getItems()) {
            System.out.format("%-40s%-40s\n", item.get("title").getS(), item.get("id").getS());
        }
    }

    private static Map<String,AttributeValue> getPost(String project_name, String post_id, String profile_path) {
        AmazonDynamoDB ddb = null;
        if(localDynamo) {
            ddb = AmazonDynamoDBClientBuilder.standard().withEndpointConfiguration(
                new AwsClientBuilder.EndpointConfiguration("http://localhost:8000", "us-east-1"))
                .build();
        } else {
            ddb = AmazonDynamoDBClientBuilder.defaultClient();
        }

        String author = null;
        String title = null;
        String body = null;
        Map<String,AttributeValue> result = null;
        try {
            HashMap<String,AttributeValue> key_to_get = new HashMap<String,AttributeValue>();
            key_to_get.put("id", new AttributeValue(post_id));

            GetItemRequest req = new GetItemRequest()
                .withKey(key_to_get)
                .withTableName(project_name);
 
            result = ddb.getItem(req).getItem();
            author = result.get("author").getS();
            title = result.get("title").getS();
            body = result.get("body").getS();

//TODO: ADD CODE HERE          
            if(profile_path != null) {
                body = decryptPost(body, profile_path);
            }
//

        } catch(AmazonServiceException e) {
            System.out.println(e);
            help();
        }

        displayPost(project_name, post_id, body, title, author);

        return result;
    }

    private static String encryptPost(String pt_post_body, String profile_path, String user, String classification) {
//TODO: ADD CODE HERE
        String ct_post_body = null;

        try {
            Agent isAgent = new Agent();
            DeviceProfilePersistorPlainText ptPersistor = new DeviceProfilePersistorPlainText(profile_path);
            isAgent.initialize(ptPersistor);

            KeyAttributesMap mutableKeyAttrs = new KeyAttributesMap();

            if(user != null) {
                List<String> ionic_permit_user_by_email_vals = new ArrayList<String>();
                ionic_permit_user_by_email_vals.add(user);
                mutableKeyAttrs.set("ionic-permit-user-by-email", ionic_permit_user_by_email_vals);
            }

            if(classification != null) {
                List<String> classification_vals = new ArrayList<String>();
                classification_vals.add(classification);
                mutableKeyAttrs.set("classification", classification_vals);
            }

            ChunkCryptoEncryptAttributes attrs = new ChunkCryptoEncryptAttributes(new KeyAttributesMap(), mutableKeyAttrs);

            ChunkCipherV2 cipher = new ChunkCipherV2(isAgent);
            ct_post_body = cipher.encrypt(pt_post_body, attrs);
        } catch (SdkException e) {
            System.out.println(e);
            if(e.getReturnCode() == AgentErrorModuleConstants.ISAGENT_NO_DEVICE_PROFILE.value()) {
                System.out.println("Profile does not exist");
            }
            //Fail Open -- Continue with plaintext body
            ct_post_body = pt_post_body;
        }

        return ct_post_body;
//
    }
 
    public static String decryptPost(String ct_post_body, String profile_path) {
//TODO: ADD CODE HERE
         String pt_post_body = null;

         try {
             Agent isAgent = new Agent();
             DeviceProfilePersistorPlainText ptPersistor = new DeviceProfilePersistorPlainText(profile_path);
             isAgent.initialize(ptPersistor);

             ChunkCipherV2 cipher = new ChunkCipherV2(isAgent);
             pt_post_body = cipher.decrypt(ct_post_body);
         } catch (SdkException e) {
             System.out.println(e);
             if(e.getReturnCode() == AgentErrorModuleConstants.ISAGENT_NO_DEVICE_PROFILE.value()) {
                 System.out.println("Profile does not exist");
                 pt_post_body = ct_post_body;
             }
             if(e.getReturnCode() == AgentErrorModuleConstants.ISAGENT_KEY_DENIED.value()) {
                 System.out.println("Key request denied");
                 pt_post_body = "*ACCESS DENIED*";
             }
             else {
                 pt_post_body = ct_post_body;
             }
         }

         return pt_post_body;
//
    }

    public static void listAccess(String project_name, String post_id, String profile_path) {
//TODO: ADD CODE HERE

        Map<String,AttributeValue> result = getPost(project_name, post_id, profile_path);

        String ct_post_body = result.get("body").getS();

        try {
            Agent isAgent = new Agent();
            DeviceProfilePersistorPlainText ptPersistor = new DeviceProfilePersistorPlainText(profile_path);
            isAgent.initialize(ptPersistor);

            ChunkCipherV2 cipher = new ChunkCipherV2(isAgent);

            ChunkCryptoChunkInfo info = cipher.getChunkInfo(ct_post_body);

            String key_id = info.getKeyId();

            GetKeysResponse response = isAgent.getKey(key_id);

            List<GetKeysResponse.Key> keys = response.getKeys();

            if(keys.isEmpty()) {
                System.out.println("empty key response");
                System.exit(1);
            }

            System.out.format("%s%s\n", "Access for: ", key_id);
            System.out.format("%s\n", StringUtils.repeat("-", 40));
            System.out.format("\n");
            for(GetKeysResponse.Key key : keys) {
                KeyAttributesMap keyAttr = key.getMutableAttributes();

                if(keyAttr == null) {
                    System.out.format("\n");
                    continue;
                }

                System.out.format("%s\n", "classification");
                System.out.format("%s\n", StringUtils.repeat("-", 20));
                if(keyAttr.containsKey("classification")) {
                    List<String> classificatons = keyAttr.get("classification");

                    for(String classification : classificatons) {
                        System.out.println(classification);
                    }
                }
                System.out.format("\n");

                System.out.format("%s\n", "ionic-permit-user-by-email");
                System.out.format("%s\n", StringUtils.repeat("-", 20));
                if(keyAttr.containsKey("ionic-permit-user-by-email")) {
                    List<String> permitted_emails = keyAttr.get("ionic-permit-user-by-email");

                    for(String email : permitted_emails) {
                        System.out.println(email);
                    }
                }
                System.out.format("\n");
            }

        } catch (SdkException e) {
            System.out.println(e);
            if(e.getReturnCode() == AgentErrorModuleConstants.ISAGENT_NO_DEVICE_PROFILE.value()) {
                System.out.println("Profile does not exist");
            }
            else if(e.getReturnCode() == AgentErrorModuleConstants.ISAGENT_KEY_DENIED.value()) {
                System.out.println("Access request denied");
            }
            System.exit(1);
        }
//
    }

    public static void addUser(String project_name, String post_id, String user_email, String profile_path) {
//TODO: ADD CODE HERE
        Map<String,AttributeValue> result = getPost(project_name, post_id, profile_path);

        String ct_post_body = result.get("body").getS();

        try {
            Agent isAgent = new Agent();
            DeviceProfilePersistorPlainText ptPersistor = new DeviceProfilePersistorPlainText(profile_path);
            isAgent.initialize(ptPersistor);

            ChunkCipherV2 cipher = new ChunkCipherV2(isAgent);

            ChunkCryptoChunkInfo info = cipher.getChunkInfo(ct_post_body);

            String key_id = info.getKeyId();

            GetKeysResponse response = isAgent.getKey(key_id);

            List<GetKeysResponse.Key> keys = response.getKeys();

            if(keys.isEmpty()) {
                System.out.println("empty key response");
                System.exit(1);
            }

            for(GetKeysResponse.Key key : keys) {
            KeyAttributesMap keyAttr = key.getMutableAttributes();

                if(keyAttr == null) {
                    System.out.format("\n");
                    continue;
                }

                List<String> ionic_permit_user_by_email_vals = null;
                if(keyAttr.containsKey("ionic-permit-user-by-email")) {
                    ionic_permit_user_by_email_vals = keyAttr.get("ionic-permit-user-by-email");
                }
                else {
                    ionic_permit_user_by_email_vals = new ArrayList<String>();
                }

                ionic_permit_user_by_email_vals.add(user_email);
                keyAttr.set("ionic-permit-user-by-email", ionic_permit_user_by_email_vals);

                UpdateKeysResponse update = isAgent.updateKey(key, true);
            }
        } catch (SdkException e) {
            System.out.println(e);
            if(e.getReturnCode() == AgentErrorModuleConstants.ISAGENT_NO_DEVICE_PROFILE.value()) {
                System.out.println("Profile does not exist");
            }
            else if(e.getReturnCode() == AgentErrorModuleConstants.ISAGENT_KEY_DENIED.value()) {
                System.out.println("Access request denied");
            }
            System.exit(1);
        }
//
    }

    public static void updateClassification(String project_name, String post_id, String classification, String profile_path) {
//TODO: ADD CODE HERE
        Map<String,AttributeValue> result = getPost(project_name, post_id, profile_path);

        String ct_post_body = result.get("body").getS();

        try {
            Agent isAgent = new Agent();
            DeviceProfilePersistorPlainText ptPersistor = new DeviceProfilePersistorPlainText(profile_path);
            isAgent.initialize(ptPersistor);

            ChunkCipherV2 cipher = new ChunkCipherV2(isAgent);

            ChunkCryptoChunkInfo info = cipher.getChunkInfo(ct_post_body);

            String key_id = info.getKeyId();

            GetKeysResponse response = isAgent.getKey(key_id);

            List<GetKeysResponse.Key> keys = response.getKeys();

            if(keys.isEmpty()) {
                System.out.println("empty key response");
                System.exit(1);
            }

            for(GetKeysResponse.Key key : keys) {
                KeyAttributesMap keyAttr = key.getMutableAttributes();

                if(keyAttr == null) {
                    System.out.format("\n");
                    continue;
                }

                List<String> classification_vals = new ArrayList<String>();
                classification_vals.add(classification);
                keyAttr.set("classification", classification_vals);

                UpdateKeysResponse update = isAgent.updateKey(key, true);

            }

        } catch (SdkException e) {
            System.out.println(e);
            if(e.getReturnCode() == AgentErrorModuleConstants.ISAGENT_NO_DEVICE_PROFILE.value()) {
                System.out.println("Profile does not exist");
            }
            else if(e.getReturnCode() == AgentErrorModuleConstants.ISAGENT_KEY_DENIED.value()) {
                System.out.println("Access request denied");
            }
            System.exit(1);
        }
//
    }
}

