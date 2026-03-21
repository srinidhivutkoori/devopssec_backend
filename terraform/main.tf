# Main Terraform configuration for the Collaborative Whiteboard Backend
# This file defines the AWS provider, VPC, subnets, and internet gateway
# for hosting the Spring Boot backend application on AWS infrastructure.

terraform {
  # Minimum Terraform version required to use the syntax and features below
  required_version = ">= 1.0.0"

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
    random = {
      source  = "hashicorp/random"
      version = "~> 3.0"
    }
  }
}

# Configure the AWS provider with the region specified in variables.tf
# Credentials are read from the environment (AWS_ACCESS_KEY_ID / AWS_SECRET_ACCESS_KEY)
# or from the ~/.aws/credentials file - never hard-code credentials here
provider "aws" {
  region = var.aws_region
}

# Create a Virtual Private Cloud (VPC) for network isolation
# All application resources are confined to this VPC to prevent unintended exposure
resource "aws_vpc" "whiteboard_vpc" {
  cidr_block = "10.0.0.0/16"

  # Required for EC2 instances and RDS to receive resolvable DNS hostnames
  enable_dns_hostnames = true
  enable_dns_support   = true

  tags = {
    Name    = "whiteboard-vpc"
    Project = "collaborative-whiteboard"
    Student = "SrinidhiVutkoori-X25173243"
  }
}

# Create a public subnet for the EC2 instance
# Instances launched here receive a public IP so the API is reachable from the internet
resource "aws_subnet" "public_subnet" {
  vpc_id                  = aws_vpc.whiteboard_vpc.id
  cidr_block              = "10.0.1.0/24"
  availability_zone       = "${var.aws_region}a"
  map_public_ip_on_launch = true

  tags = {
    Name = "whiteboard-public-subnet"
  }
}

# Create a private subnet for the RDS database in the first availability zone
# The database is intentionally not publicly accessible - only the backend EC2
# instance can reach it via the security group rules defined in rds.tf
resource "aws_subnet" "private_subnet_a" {
  vpc_id            = aws_vpc.whiteboard_vpc.id
  cidr_block        = "10.0.2.0/24"
  availability_zone = "${var.aws_region}a"

  tags = {
    Name = "whiteboard-private-subnet-a"
  }
}

# Create a second private subnet in a different availability zone
# AWS RDS subnet groups require subnets in at least two AZs for high availability
resource "aws_subnet" "private_subnet_b" {
  vpc_id            = aws_vpc.whiteboard_vpc.id
  cidr_block        = "10.0.3.0/24"
  availability_zone = "${var.aws_region}b"

  tags = {
    Name = "whiteboard-private-subnet-b"
  }
}

# Create an Internet Gateway and attach it to the VPC
# This is the entry and exit point for all internet-bound traffic from the public subnet
resource "aws_internet_gateway" "whiteboard_igw" {
  vpc_id = aws_vpc.whiteboard_vpc.id

  tags = {
    Name = "whiteboard-igw"
  }
}

# Create a route table for the public subnet
# The default route (0.0.0.0/0) sends all non-local traffic to the internet gateway
resource "aws_route_table" "public_rt" {
  vpc_id = aws_vpc.whiteboard_vpc.id

  route {
    cidr_block = "0.0.0.0/0"
    gateway_id = aws_internet_gateway.whiteboard_igw.id
  }

  tags = {
    Name = "whiteboard-public-rt"
  }
}

# Associate the public subnet with the public route table
# Without this association the subnet would use the default (local-only) route table
# and instances would not be reachable from the internet
resource "aws_route_table_association" "public_rta" {
  subnet_id      = aws_subnet.public_subnet.id
  route_table_id = aws_route_table.public_rt.id
}

# Generate a random suffix for globally unique S3 bucket names
resource "random_id" "bucket_suffix" {
  byte_length = 4
}
