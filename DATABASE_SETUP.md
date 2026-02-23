# Database Setup Guide

This guide explains how to set up the ScrapBH Marketplace database on Supabase.

## Prerequisites

- A Supabase account (sign up at https://supabase.com)
- Access to your Supabase project dashboard

## Step 1: Create a Supabase Project

1. Log in to your Supabase account
2. Click "New Project"
3. Fill in the project details:
   - **Name**: ScrapBH Marketplace
   - **Database Password**: Choose a strong password (save this!)
   - **Region**: Choose the closest region to Bahrain
4. Click "Create new project"
5. Wait for the project to be provisioned (takes 1-2 minutes)

## Step 2: Execute the Database Schema

1. In your Supabase dashboard, navigate to the **SQL Editor**
2. Click "New Query"
3. Copy the entire contents of `src/main/resources/db/schema.sql`
4. Paste it into the SQL Editor
5. Click "Run" to execute the schema

This will create:
- **5 enum types**: user_role, post_type, post_status, message_type, escrow_status
- **7 tables**: users, posts, conversations, messages, escrow_transactions, bookmarks, notifications
- **20+ indexes** for optimal query performance
- **Foreign key constraints** for data integrity

## Step 3: Verify the Schema

After running the schema, verify that all tables were created:

1. Navigate to **Table Editor** in the Supabase dashboard
2. You should see all 7 tables listed:
   - users
   - posts
   - conversations
   - messages
   - escrow_transactions
   - bookmarks
   - notifications

## Step 4: Get Your Connection Details

You'll need the following information for your application:

### Database Connection String

1. Go to **Settings** → **Database**
2. Find the **Connection string** section
3. Select **URI** tab
4. Copy the connection string (it looks like):
   ```
   postgresql://postgres:[YOUR-PASSWORD]@db.xxxxx.supabase.co:5432/postgres
   ```
5. Replace `[YOUR-PASSWORD]` with your actual database password

### API Keys

1. Go to **Settings** → **API**
2. Copy the following:
   - **Project URL**: `https://xxxxx.supabase.co`
   - **anon public key**: Used for client-side operations
   - **service_role key**: Used for server-side operations (keep secret!)

## Step 5: Configure Your Application

Update your `.env` file with the values from Step 4:

```env
# Supabase Configuration
SUPABASE_URL=https://xxxxx.supabase.co
SUPABASE_KEY=your-anon-key-here
SUPABASE_SERVICE_KEY=your-service-role-key-here
SUPABASE_STORAGE_BUCKET=post-images

# Database Configuration
DATABASE_URL=postgresql://postgres:[password]@db.xxxxx.supabase.co:5432/postgres
DATABASE_USERNAME=postgres
DATABASE_PASSWORD=your-database-password
```

## Step 6: Set Up Storage Bucket (Optional)

For image uploads, you'll need to create a storage bucket:

1. Navigate to **Storage** in the Supabase dashboard
2. Click "Create a new bucket"
3. Name it: `post-images`
4. Set it to **Public** (so images can be accessed via URL)
5. Click "Create bucket"

## Database Schema Overview

### Tables and Relationships

```
auth.users (Supabase Auth)
    ↓ (1:1)
public.users
    ↓ (1:N)
    ├── posts
    │   ↓ (1:N)
    │   ├── conversations
    │   │   ↓ (1:N)
    │   │   └── messages
    │   ├── escrow_transactions
    │   └── bookmarks
    └── notifications
```

### Key Features

1. **Foreign Key Constraints**: All relationships are enforced at the database level
2. **Cascade Deletes**: When a user is deleted, all related data is automatically removed
3. **Unique Constraints**: 
   - Username must be unique
   - User can only bookmark a post once
   - Only one conversation per post-buyer-seller combination
4. **Check Constraints**: Escrow amount must be positive
5. **Default Values**: 
   - Post status defaults to 'active'
   - Message type defaults to 'text'
   - Escrow status defaults to 'on_hold'
   - Timestamps are auto-generated

### Performance Indexes

The schema includes 20+ indexes for optimal performance:

- **Posts**: Indexed on status, user_id, car_make, car_model, car_year, price, created_at
- **Full-text search**: GIN index on post titles and content
- **Foreign keys**: All foreign key columns are indexed
- **Frequently queried fields**: is_read, status, etc.

## Troubleshooting

### Error: "type user_role does not exist"

This means the enum types weren't created. Make sure you run the entire schema.sql file, not just parts of it.

### Error: "relation auth.users does not exist"

Supabase Auth tables are created automatically. If you see this error, your Supabase project may not be fully initialized. Wait a few minutes and try again.

### Error: "permission denied for schema public"

Make sure you're using the service_role key for database operations that require elevated permissions.

### Connection Timeout

If you're experiencing connection timeouts:
1. Check that your IP is allowed in Supabase (Settings → Database → Connection Pooling)
2. Verify your connection string is correct
3. Ensure your database password doesn't contain special characters that need URL encoding

## Next Steps

After setting up the database:

1. Test the connection by running the Spring Boot application
2. Verify that HikariCP connection pool is working (check logs)
3. Test basic CRUD operations on the tables
4. Set up Supabase Auth for user authentication
5. Configure Stripe for payment processing

## Security Considerations

1. **Never commit** your `.env` file to version control
2. **Keep your service_role key secret** - it has full database access
3. **Use the anon key** for client-side operations only
4. **Enable Row Level Security (RLS)** in production for additional security
5. **Rotate your keys** periodically

## Support

For issues with:
- **Supabase**: Check the [Supabase documentation](https://supabase.com/docs)
- **PostgreSQL**: Check the [PostgreSQL documentation](https://www.postgresql.org/docs/)
- **This application**: Open an issue in the project repository
