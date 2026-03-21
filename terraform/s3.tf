# S3 Configuration for the Collaborative Whiteboard Backend
# This file creates an S3 bucket for storing application artifacts,
# backups, and exported whiteboard images.

# S3 bucket for deployment artifacts, database backups, and exported canvases
# The bucket name is suffixed with the region to keep it globally unique
resource "aws_s3_bucket" "backend_artifacts" {
  bucket = "whiteboard-backend-artifacts-${random_id.bucket_suffix.hex}"

  tags = {
    Name    = "whiteboard-backend-artifacts"
    Project = "collaborative-whiteboard"
    Student = "SrinidhiVutkoori-X25173243"
  }
}

# Enable versioning so every uploaded artifact is retained and recoverable
# This means overwriting a JAR or backup file does not destroy the previous copy
resource "aws_s3_bucket_versioning" "backend_artifacts_versioning" {
  bucket = aws_s3_bucket.backend_artifacts.id

  versioning_configuration {
    # Enabled - all object versions are stored; Suspended would stop new versions
    status = "Enabled"
  }
}

# Block every form of public access to the bucket
# Application artifacts and backups must never be publicly readable or writable
resource "aws_s3_bucket_public_access_block" "backend_artifacts_block" {
  bucket = aws_s3_bucket.backend_artifacts.id

  # Prevent S3 from applying ACLs that grant public read or write access
  block_public_acls = true

  # Prevent bucket policies that allow public access from taking effect
  block_public_policy = true

  # Ignore any pre-existing public ACLs that may have been set outside Terraform
  ignore_public_acls = true

  # Reject any requests that would grant public access through bucket policies
  restrict_public_buckets = true
}

# Enable server-side encryption so all objects are encrypted at rest
# AES-256 with AWS-managed keys (SSE-S3) requires no additional KMS cost
resource "aws_s3_bucket_server_side_encryption_configuration" "backend_artifacts_encryption" {
  bucket = aws_s3_bucket.backend_artifacts.id

  rule {
    apply_server_side_encryption_by_default {
      # SSE-S3 applies AES-256 encryption automatically to every stored object
      sse_algorithm = "AES256"
    }
  }
}
