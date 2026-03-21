# Output Values for the Collaborative Whiteboard Backend Infrastructure
# These outputs are printed after a successful terraform apply and can also
# be queried at any time with: terraform output <output_name>
# They provide the connection details needed to configure the frontend,
# the CI/CD pipeline, and any external monitoring tools.

# Public IP of the backend server - used by the CI/CD pipeline smoke test
# and to configure the frontend API base URL
output "backend_server_public_ip" {
  description = "The public IP address of the backend EC2 server"
  value       = aws_eip.backend_eip.public_ip
}

# Public DNS hostname assigned by AWS - an alternative to the raw IP address
# that remains stable across stop/start cycles when using Elastic IPs
output "backend_server_public_dns" {
  description = "The public DNS name of the backend EC2 server"
  value       = aws_instance.backend_server.public_dns
}

# Fully formed URL for the REST API - copy this into the frontend .env file
# or GitHub Actions secret (EC2_HOST) after provisioning
output "backend_api_url" {
  description = "The base URL to access the backend REST API"
  value       = "http://${aws_eip.backend_eip.public_ip}:8080"
}

# Direct link to the Swagger UI - useful for manual API exploration and testing
# without needing a separate API client
output "swagger_ui_url" {
  description = "The URL to access Swagger UI API documentation"
  value       = "http://${aws_eip.backend_eip.public_ip}:8080/swagger-ui.html"
}

# RDS connection endpoint in host:port format (e.g. whiteboard-db.xxxx.eu-west-1.rds.amazonaws.com:5432)
# Set this as the SPRING_DATASOURCE_URL host when configuring the production profile
output "rds_endpoint" {
  description = "The connection endpoint for the PostgreSQL RDS instance"
  value       = aws_db_instance.whiteboard_db.endpoint
}

# The database name to include in the JDBC connection string
output "rds_database_name" {
  description = "The name of the PostgreSQL database"
  value       = aws_db_instance.whiteboard_db.db_name
}

# S3 bucket name - store this in the application's production properties
# if the backend uploads whiteboard exports or database backups to S3
output "s3_artifacts_bucket" {
  description = "The name of the S3 bucket for backend artifacts and backups"
  value       = aws_s3_bucket.backend_artifacts.bucket
}
