# EC2 Instance Configuration for the Collaborative Whiteboard Backend
# This file provisions a t2.micro EC2 instance to host the Spring Boot
# application with a systemd service for process management.

# Security group to control inbound and outbound traffic for the EC2 instance
# Acts as a virtual firewall - only the ports listed below are reachable
resource "aws_security_group" "backend_sg" {
  name        = "whiteboard-backend-sg"
  description = "Security group for the whiteboard backend EC2 instance"
  vpc_id      = aws_vpc.whiteboard_vpc.id

  # Allow SSH access on port 22 for deployment via GitHub Actions and manual ops
  # In a production hardened environment this should be restricted to specific CIDRs
  ingress {
    description = "SSH access for deployment"
    from_port   = 22
    to_port     = 22
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  # Allow HTTP traffic on port 8080 where Spring Boot listens by default
  # This is the port used by the REST API, WebSocket endpoint, and Swagger UI
  ingress {
    description = "Spring Boot application port"
    from_port   = 8080
    to_port     = 8080
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  # Allow all outbound traffic so the instance can download packages,
  # send logs to CloudWatch, and communicate with RDS and S3
  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Name = "whiteboard-backend-sg"
  }
}

# EC2 instance that hosts the Spring Boot backend application
# The user_data script runs once on first boot to install Java and register
# the application as a systemd service so it survives reboots automatically
resource "aws_instance" "backend_server" {
  ami                    = var.ami_id
  instance_type          = var.instance_type
  subnet_id              = aws_subnet.public_subnet.id
  vpc_security_group_ids = [aws_security_group.backend_sg.id]

  # The key pair must already exist in the target AWS account; it is used
  # by the GitHub Actions CD pipeline to authenticate the SCP and SSH steps
  key_name = var.key_pair_name

  # Bootstrap script executed by cloud-init on the very first instance boot
  # It installs the JDK and creates a systemd unit so the JAR deployed by CI/CD
  # is automatically managed as a Linux service
  user_data = <<-EOF
              #!/bin/bash
              # Update all installed system packages to their latest versions
              sudo apt-get update -y
              sudo apt-get upgrade -y
              # Install OpenJDK 17 (headless variant - no GUI libs needed)
              sudo apt-get install -y openjdk-17-jdk-headless
              # Create the directory where the CI/CD pipeline will place the JAR
              sudo mkdir -p /opt/whiteboard-backend
              # Write the systemd service unit file for the Spring Boot application
              cat <<SYSTEMD | sudo tee /etc/systemd/system/whiteboard-backend.service
              [Unit]
              Description=Collaborative Whiteboard Backend Spring Boot Application
              After=network.target

              [Service]
              Type=simple
              User=ubuntu
              ExecStart=/usr/bin/java -jar /opt/whiteboard-backend/app.jar --spring.profiles.active=prod
              Restart=on-failure
              RestartSec=10
              StandardOutput=journal
              StandardError=journal

              [Install]
              WantedBy=multi-user.target
              SYSTEMD
              # Reload systemd so it picks up the newly created unit file
              sudo systemctl daemon-reload
              # Enable the service so it starts automatically after every reboot
              sudo systemctl enable whiteboard-backend
              EOF

  tags = {
    Name    = "whiteboard-backend-server"
    Project = "collaborative-whiteboard"
    Student = "SrinidhiVutkoori-X25173243"
  }
}

# Elastic IP for the backend EC2 instance
# Provides a static public IP address that persists across instance stop/start cycles
resource "aws_eip" "backend_eip" {
  instance = aws_instance.backend_server.id
  domain   = "vpc"

  tags = {
    Name    = "whiteboard-backend-eip"
    Project = "collaborative-whiteboard"
  }

  depends_on = [aws_internet_gateway.whiteboard_igw]
}
