# ScrapBH Marketplace

A web-based marketplace platform for buying and selling scrap auto parts in Bahrain.

## Technology Stack

- **Backend**: Java 17, Spring Boot 3.2.0
- **Database**: PostgreSQL (Supabase)
- **Authentication**: Supabase Auth
- **Storage**: Supabase Storage
- **Payment**: Stripe
- **Real-time**: Supabase Realtime
- **Connection Pool**: HikariCP

## Project Structure

```
scrapbh-marketplace/
├── src/
│   ├── main/
│   │   ├── java/com/scrapbh/marketplace/
│   │   │   ├── config/          # Configuration classes
│   │   │   ├── entity/          # JPA entities
│   │   │   ├── enums/           # Enum types
│   │   │   └── MarketplaceApplication.java
│   │   └── resources/
│   │       ├── db/
│   │       │   └── schema.sql   # Database schema
│   │       └── application.properties
│   └── test/
├── pom.xml
├── .env.example
└── README.md
```

## Database Schema

The application uses the following database tables:

### Tables
- **users**: User profiles (linked to Supabase Auth)
- **posts**: Auto part listings (sale or wanted)
- **conversations**: Chat conversations between buyers and sellers
- **messages**: Messages within conversations
- **escrow_transactions**: Stripe-powered escrow transactions
- **bookmarks**: User bookmarks for posts
- **notifications**: System notifications

### Enums
- **user_role**: buyer, seller
- **post_type**: sale, wanted
- **post_status**: active, sold, archived
- **message_type**: text, image
- **escrow_status**: on_hold, released, disputed, completed

## Setup Instructions

### Prerequisites

- Java 17 or higher
- Maven 3.6+
- PostgreSQL (via Supabase)
- Supabase account
- Stripe account

### 1. Clone the Repository

```bash
git clone <repository-url>
cd scrapbh-marketplace
```

### 2. Set Up Supabase Database

1. Create a Supabase project at https://supabase.com
2. Navigate to the SQL Editor in your Supabase dashboard
3. Execute the schema from `src/main/resources/db/schema.sql`

This will create:
- All enum types
- All tables with proper constraints
- All indexes for performance optimization

### 3. Configure Environment Variables

Copy `.env.example` to `.env` and fill in your credentials:

```bash
cp .env.example .env
```

Update the following variables:
- `SUPABASE_URL`: Your Supabase project URL
- `SUPABASE_KEY`: Your Supabase anon key
- `SUPABASE_SERVICE_KEY`: Your Supabase service role key
- `DATABASE_URL`: Your Supabase PostgreSQL connection string
- `DATABASE_PASSWORD`: Your database password
- `STRIPE_SECRET_KEY`: Your Stripe secret key
- `STRIPE_WEBHOOK_SECRET`: Your Stripe webhook secret

### 4. Build the Project

```bash
mvn clean install
```

### 5. Run the Application

```bash
mvn spring-boot:run
```

The application will start on `http://localhost:8080`

## Database Connection Pooling

The application uses HikariCP with the following configuration:

- **Maximum Pool Size**: 20 connections
- **Minimum Idle**: 5 connections
- **Connection Timeout**: 30 seconds
- **Idle Timeout**: 10 minutes
- **Max Lifetime**: 30 minutes

These settings are optimized for performance and can be adjusted in `application.properties`.

## Performance Optimizations

### Indexes

The database schema includes comprehensive indexes for:
- Post search queries (status, car make/model/year, price, created date)
- Full-text search on post titles and content
- Foreign key relationships
- Frequently queried fields

### Connection Pooling

HikariCP is configured to maintain optimal database connections, reducing connection overhead and improving response times.

## API Endpoints

The application will expose RESTful endpoints for:
- Authentication (via Supabase Auth)
- Post management
- Conversations and messaging
- Escrow transactions
- Bookmarks
- Notifications

(Detailed API documentation will be added in subsequent tasks)

## Testing

Run tests with:

```bash
mvn test
```

## Requirements Mapping

This setup addresses the following requirements:

- **14.1-14.10**: Database indexes for performance
- **19.2**: User identity consistency with Supabase Auth
- **27.1**: Foreign key constraints for data integrity

## Next Steps

1. Implement authentication service with Supabase Auth integration
2. Implement post management service
3. Implement conversation and messaging service
4. Implement escrow payment service with Stripe
5. Implement bookmark and notification services
6. Add comprehensive tests

## License

[Add your license here]
