import boto3
import os
import json

# Get environment variables
secondaryRegion = os.getenv("DRRegion")
primaryRegion = os.getenv("PrimaryRegion")
userPoolId = os.getenv("SecondaryUserPoolId")
topicArnPrimary = os.getenv("SNSNotificationTopicPrimary")
topicArnSecondary = os.getenv("SNSNotificationTopicSecondary")
dRCognitoAuthEndpoint = os.getenv("DRCognitoAuthEndpoint")
dRCallBackUrlEndpoint = os.getenv("DRCallBackUrlEndpoint")
dRClientId = os.getenv("DRClientId")
dRClientSecret = os.getenv("DRClientSecret")
dRWebsocketEndpoint = os.getenv("DRWebsocketEndpoint")






# AWS Clients
secondary_sns_client = boto3.client("sns", region_name=secondaryRegion)
primary_sns_client = boto3.client("sns", region_name=primaryRegion)
cognito_client = boto3.client("cognito-idp", region_name=secondaryRegion)
ssm_client = boto3.client("ssm", region_name=primaryRegion)

def snsNotificationSubscription(email, topicArn):
    """Subscribe an email to an SNS topic."""
    filter_policy = json.dumps({"endpointEmail": [email]})  # Correct JSON format

    secondary_sns_client.subscribe(
      TopicArn=topicArn,
      Protocol="email",  
      Endpoint=email,
      Attributes={
      "FilterPolicy": filter_policy})

    print(f"Subscribed {email} to {topicArn}.")

def reset_all_users_password():
    """Loop through all users in the secondary Cognito pool and send reset notifications."""

    try:
        pagination_token = None  # Initialize pagination token

        while True:
            # Fetch users with pagination
            try:
                if pagination_token:
                    response = cognito_client.list_users(
                        UserPoolId=userPoolId,
                        PaginationToken=pagination_token
                    )
                else:
                    response = cognito_client.list_users(UserPoolId=userPoolId)

                users = response.get("Users", [])

                # If no users found, break loop
                if not users:
                    print("No users found in Cognito.")
                    break

                for user in users:
                    attributes = {attr['Name']: attr['Value'] for attr in user.get('Attributes', [])}
                    email = attributes.get('email')
                    username = user.get('Username')

                    if not email:
                        print(f"Skipping user {username}: No email found.")
                        continue

                    try:
                        # Send reset password notification
                        message_attributes = {
                            "endpointEmail": {
                                "DataType": "String",
                                "StringValue": email
                            }
                        }

                        sns_response = primary_sns_client.publish(
                            TopicArn=topicArnPrimary,
                            Message="Visit the login page and click on forgot password.",
                            Subject="Password Reset Instructions - Action Required",
                            MessageAttributes=message_attributes
                        )
                        print(f"Notification sent to {email}: {sns_response}")

                        # Subscribe user to SNS topic
                        snsNotificationSubscription(email, topicArnSecondary)

                    except Exception as sns_error:
                        print(f"Error sending SNS notification for {username}: {sns_error}")

                # Update pagination token
                pagination_token = response.get('PaginationToken')

                # If there's no next page, break the loop
                if not pagination_token:
                    break

            except Exception as cognito_error:
                print(f"Error retrieving users from Cognito: {cognito_error}")
                break  # Break the loop to avoid infinite execution

    except Exception as e:
        print(f"Unexpected error: {e}")

def update_parameter_store_data():
    """Update AWS Parameter Store with secondary region configurations"""
    
    # Update the SSM parameter
    parameters = [
      {"Name": "/photoblog/auth_endpoint", "Value": dRCallBackUrlEndpoint, "Type": "String"},
      {"Name": "/photoblog/callback_endpoint", "Value": dRCallBackUrlEndpoint, "Type": "String"},
      {"Name": "/photoblog/client_id", "Value": dRClientId, "Type": "String"},
      {"Name": "/photoblog/client_secret", "Value": dRClientSecret, "Type": "SecureString"},
      {"Name": "/photoblog/websocket_endpoint", "Value": dRWebsocketEndpoint, "Type": "String"},
    ]

    # Update each parameter in SSM
    for param in parameters:
        ssm_client.put_parameter(
            Name=param["Name"],
            Value=param["Value"],
            Type=param["Type"],
            Description="Updated parameter in primary region",
            Overwrite=True,
        )

    print(f"Updated parameters in SSM.")

def lambda_handler(event, context):
    print(f"Lambda triggered with event: {event}")
    
    # Update AWS Parameter Store
    update_parameter_store_data()

    # Trigger failover process here
    reset_all_users_password()

    return {"statusCode": 200, "body": "Failover triggered successfully!"}
