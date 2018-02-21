This sample is used in the Ionic Data Protect for Cloud Storage tutorial found [here](https://dev.ionic.com/tutorials/dataprotect-cloud-java).

## Setup
[Set up Ionic for Java](https://dev.ionic.com/getting-started/java-jvm.html)

## Build

~~~bash
mvn package
~~~

## Run

~~~bash
java -jar target/ionic-dataprotect-cloud-1.0.jar --help
USAGE: java -jar target/ionic-dataprotect-cloud-1.0.jar [COMMAND] [OPTIONS]

project-list  

post-list -p ${PROJECT_NAME}  
    -p    --project    Name of the project to list posts  

post-create -p ${PROJECT_NAME} -f ${FILE_PATH}  
    -p    --project  -f    --file  
    -u    --user              (optional)  
    -c    --classification    (optional)  
    -q    --profile           (optional)  

post-get -p ${PROJECT_NAME} -i ${POST_ID}  
    -p    --project  
    -i    --id  

post-access-list -p ${PROJECT_NAME} -i ${POST_ID} -q ${PROFILE_PATH}  
    -p    --project  
    -i    --id  
    -q    --profile  

post-add-user -p ${PROJECT_NAME} -i ${POST_ID} -q ${PROFILE_PATH} -u ${USER_EMAIL}  
    -p    --project  
    -i    --id  
    -u    --user  
    -q    --profile  

post-classification-update -p ${PROJECT_NAME} -i ${POST_ID} -c ${CLASSIFICATION}  
    -p    --project  
    -i    --id  
    -c    --classification  
    -q    --profile
~~~


~~~bash
java -jar target/ionic-dataprotect-cloud-1.0.jar post-create -p MyProject -f test_posts/1.json

PROJECT: MyProject
--------------------------------------------------------------------------------
|    ID:        c1e269aa-d93b-4571-aeea-4462ea4a8623
|    AUTHOR:    t@ionic.com
|-------------------------------------------------------------------------------
|    TITLE:     Introduction
|-------------------
|    BODY:      Welcome to the project board!
--------------------------------------------------------------------------------
~~~
