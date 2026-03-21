# RDS Configuration for the Collaborative Whiteboard Backend
# This file provisions a PostgreSQL RDS instance in a private subnet
# for secure database storage of whiteboard data.

# DB subnet group tells RDS which subnets it may use for the instance and
# any standby replicas. AWS requires at least two subnets in different AZs
# even when multi_az is disabled, because it reserves the capacity
resource "aws_db_subnet_group" "whiteboard_db_subnet" {
  name       = "whiteboard-db-subnet-group"
  subnet_ids = [aws_subnet.private_subnet_a.id, aws_subnet.private_subnet_b.id]

  tags = {
    Name = "whiteboard-db-subnet-group"
  }
}

# Security group for the RDS instance
# PostgreSQL port 5432 is opened exclusively to the backend EC2 security group
# so the database is never directly reachable from the internet
resource "aws_security_group" "rds_sg" {
  name        = "whiteboard-rds-sg"
  description = "Security group for the whiteboard PostgreSQL RDS instance"
  vpc_id      = aws_vpc.whiteboard_vpc.id

  # Allow inbound PostgreSQL traffic only from instances belonging to the
  # backend security group - no direct internet or developer access
  ingress {
    description     = "PostgreSQL access from backend"
    from_port       = 5432
    to_port         = 5432
    protocol        = "tcp"
    security_groups = [aws_security_group.backend_sg.id]
  }

  # Allow all outbound traffic for RDS to communicate with AWS services
  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Name = "whiteboard-rds-sg"
  }
}

# PostgreSQL RDS instance for persisting all whiteboard application data
# Placed in the private subnet and encrypted at rest for data security
resource "aws_db_instance" "whiteboard_db" {
  identifier = "whiteboard-db"

  # PostgreSQL engine matching the version used in local development
  engine         = "postgres"
  engine_version = "16.9"

  # db.t3.micro is the smallest billable RDS size, suitable for development
  # and low-traffic production; upgrade via var.db_instance_class for scale
  instance_class    = var.db_instance_class
  allocated_storage = 20

  # Autoscaling upper limit prevents runaway storage growth from filling disk
  max_allocated_storage = 50

  # Database name and credentials come from variables so secrets are not
  # hard-coded in source control; in production supply these via a secrets
  # manager rather than variable defaults
  db_name  = var.db_name
  username = var.db_username
  password = var.db_password

  # Place the database in the private subnets defined above
  db_subnet_group_name   = aws_db_subnet_group.whiteboard_db_subnet.name
  vpc_security_group_ids = [aws_security_group.rds_sg.id]

  # Skip the final snapshot on destroy to speed up tear-downs in dev/test;
  # set to false in production to retain a backup before deleting
  skip_final_snapshot = true

  # Never expose the database endpoint on the public internet
  publicly_accessible = false

  # Single-AZ deployment is sufficient for this project scope;
  # set multi_az = true for production high-availability requirements
  multi_az = false

  # Encrypt all data at rest using AES-256 managed by AWS KMS
  storage_encrypted = true

  tags = {
    Name    = "whiteboard-postgresql-db"
    Project = "collaborative-whiteboard"
    Student = "SrinidhiVutkoori-X25173243"
  }
}
