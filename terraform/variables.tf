# Input Variables for the Collaborative Whiteboard Backend Infrastructure
# These variables allow customization of the deployment without modifying
# the main configuration files. Override defaults by supplying a
# terraform.tfvars file or by setting TF_VAR_* environment variables.

variable "aws_region" {
  description = "The AWS region where all resources will be provisioned"
  type        = string
  # eu-west-1 (Ireland) is used as the default for EU data residency
  default = "eu-west-1"
}

variable "instance_type" {
  description = "The EC2 instance type for the backend server"
  type        = string
  # t2.micro qualifies for the AWS Free Tier; upgrade for production workloads
  default = "t3.micro"
}

variable "ami_id" {
  description = "The Amazon Machine Image ID for the EC2 instance (Amazon Linux 2023 in eu-west-1)"
  type        = string
  # This AMI ID is region-specific; update when deploying to a different region
  default = "ami-0c38b837cd80f13bb"
}

variable "key_pair_name" {
  description = "The name of the SSH key pair used for EC2 access during deployments"
  type        = string
  # The key pair must be created in the AWS Console and the private key stored
  # in GitHub Actions secrets as EC2_SSH_KEY before running the pipeline
  default = "whiteboard-backend-key"
}

variable "db_instance_class" {
  description = "The RDS instance class for the PostgreSQL database"
  type        = string
  # db.t3.micro is the smallest RDS size; suitable for low-traffic environments
  default = "db.t3.micro"
}

variable "db_name" {
  description = "The name of the PostgreSQL database created on the RDS instance"
  type        = string
  default     = "whiteboard_db"
}

variable "db_username" {
  description = "The master username for the PostgreSQL database"
  type        = string
  # Marked sensitive so Terraform redacts this value from plan and apply output
  default   = "wbadmin"
  sensitive = true
}

variable "db_password" {
  description = "The master password for the PostgreSQL database"
  type        = string
  # Marked sensitive so Terraform redacts this value from plan and apply output.
  # In production, provide this via an environment variable (TF_VAR_db_password)
  # or a secrets manager integration rather than relying on the default
  default   = "Whiteboard2024!"
  sensitive = true
}
