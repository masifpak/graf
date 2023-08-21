# GRUPI file process application

## Overview

The project is a String Batch implementation for processing GRUPI files and saving data from them to staging table(t_stg_grupi_item)

## Running project

1. From a command line, change directory to the location of the project and build the project by running the following Maven command: 

```
./mvnw clean package 
```

2. Run the application with the configurations required to process GRUPI file.
   
   To configure the `grupi-file-stager` application, use the following arguments:
   * filePath: Path to the GRUPI file to be processed;
   * grupiFileId: Identifier for StgGrupiFile;
   * spring.profiles.active: Activate Spring profile to retrieve correct application properties;
   * onefree.grupi.file-stager.chunk-size: Chunk size for processing items; `Optional`
   * onefree.grupi.file-stager.columns: Array of column names separated by `,`; `Optional`

```
java -jar target/<jar artifact> \
--spring.profiles.active=<profile> \
--onefree.grupi.file-stager.chunk-size=<chunk size> \
--onefree.grupi.file-stager.columns=<column names array> \
grupiFileId=<stg grupi file id> \
filePath=<filePath> 
```   
   
Example:

```
java -jar target/grupi-file-stager-0.0.1-SNAPSHOT.jar \
--spring.profiles.active=developer-igor \
--onefree.grupi.file-stager.chunk-size=10 \
--onefree.grupi.file-stager.columns=col_01,col_02,col_03,col_04,col_05,col_06,col_07,col_08,col_09 \
grupiFileId=10 \
filePath=grupi\tmp.csv
``` 

## Notes
* Migrations for creating staging tables implemented in OneFreeSpringApp in branch (Of-2543);
* To initialize batch tables automatically when the application starts, in the application properties, add:
```
spring.datasource.schema=classpath:/org/springframework/batch/core/schema-mysql.sql
spring.batch.initialize-schema=always
```