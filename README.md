# Photo Blog App

## Project Overview

The **Photo Blog App** is a serverless web application that allows users to create and manage their personal photo blogs. Users can upload, view, and delete their blog posts and images, with authentication and email notifications integrated into the system.

## Technical Requirements

- **Frontend** must be hosted using **AWS Amplify** with a secure deployment process.
- **Backend** must be fully serverless and provisioned using **AWS SAM**.
- **User authentication** handled via **Amazon Cognito** (signup/sign-in).
- **Storage** for images and blog content using **Amazon S3**.
- **Compute logic** powered by **AWS Lambda**.
- **Database** for storing metadata using **Amazon DynamoDB**.
- **Notifications** for user actions using **Amazon SNS**.
- **API endpoints** managed via **Amazon API Gateway**.
- **Decoupled tasks** managed via **Amazon SQS**
- **Domain creation** managed via **Amazon Rout353**, **Amazon ACM**
- **Deployment pipeline** using **GitHub Actions**.

## Functional Requirements

### 🏆 User Account Management

- Users can **sign up** and create their own blog space.
- Users can **log in** to:
  - Upload images 📤
  - View images 👀
  - Delete images ❌

### 📸 Image Management

- Only **watermarked images** are displayed to users
- Users can generate **time-bound shareable links** for non-registered.

### 🗑️ Recycling Bin

- Deleted images are moved to a **recycling bin** instead of permanent deletion.
- Users can **restore** or **permanently delete** images from the recycling bin.
- Images in the recycling bin **cannot be shared** but remain viewable to the owner.

## Disaster Recovery Requirements

- **RPO/RTO of 10 minutes** using a **Warm Standby Disaster Recovery Strategy**.
- **Automated deployment of backend resources** to a disaster recovery (DR) region.
- **Processed user images** must be replicated to a secondary S3 bucket in the DR region.

## Deployment Guide

### Prerequisites

- AWS CLI installed and configured
- AWS SAM installed
- Node.js and npm installed
- GitHub Actions configured for CI/CD

### Steps to Deploy

1. **Clone the repository:**
