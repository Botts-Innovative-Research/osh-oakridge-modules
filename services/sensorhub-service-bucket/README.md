# Bucket Service

## Purpose
The purpose of this service is to store and serve objects/files in a bucket architecture.
Each bucket can have its own set of permissions to enable fine-grained control over access/uploads.

### Configuration
Please specify a root directory to store buckets. 
In the future, this will change to have a selectable bucket store configuration, 
but for now the default will use the filesystem.