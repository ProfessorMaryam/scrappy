# Requirements Document: ScrapBH Marketplace

## Introduction

ScrapBH is a web-based marketplace platform for buying and selling scrap auto parts in Bahrain. The system provides a centralized, searchable platform with real-time chat communication and secure online payments with Stripe-powered escrow protection. The platform supports dual user roles (Buyer/Seller), post management with car compatibility metadata, and an escrow system that holds funds until buyer approval. The system is built with Java Spring Boot backend, React frontend, and Supabase PostgreSQL database with Supabase Auth integration.

## Glossary

- **System**: The ScrapBH marketplace platform
- **User**: A registered person using the platform (can be Buyer or Seller)
- **Buyer**: A user with buyer role who purchases auto parts
- **Seller**: A user with seller role who lists auto parts for sale
- **Post**: A listing for an auto part (sale or wanted)
- **Conversation**: A chat session between a buyer and seller
- **Message**: A text or image communication within a conversation
- **Escrow_Transaction**: A payment held by Stripe until buyer approval
- **Bookmark**: A saved post for later reference
- **Notification**: A system-generated alert for users
- **Supabase_Auth**: The authentication service managing user credentials
- **Public_Users**: The application user profile table
- **Auth_Users**: The Supabase authentication user table
- **Stripe**: The payment gateway service
- **Payment_Intent**: A Stripe object representing a payment
- **Post_Status**: Enum values: active, sold, archived
- **Escrow_Status**: Enum values: on_hold, released, disputed, completed
- **Message_Type**: Enum values: text, image
- **Post_Type**: Enum values: sale, wanted
- **User_Role**: Enum values defined in database

## Requirements

### Requirement 1: User Registration and Authentication

**User Story:** As a new user, I want to register with username and password, so that I can access the marketplace platform.

#### Acceptance Criteria

1. WHEN a user submits registration with username, password, full_name, and role, THE System SHALL create an auth user in Supabase_Auth
2. WHEN Supabase_Auth creates an auth user, THE System SHALL create a corresponding record in Public_Users with matching UUID
3. WHEN a user attempts to register with an existing username, THE System SHALL reject the registration and return a conflict error
4. WHEN a user submits valid login credentials, THE System SHALL return a JWT token from Supabase_Auth
5. WHEN a user submits invalid login credentials, THE System SHALL reject the login and return an unauthorized error
6. THE System SHALL validate that every Public_Users record has a corresponding Auth_Users record with matching ID

### Requirement 2: User Profile Management

**User Story:** As a registered user, I want to manage my profile information, so that other users can identify me.

#### Acceptance Criteria

1. WHEN a user updates their profile, THE System SHALL validate that the user is authenticated
2. WHEN a user updates their username, THE System SHALL ensure the new username is unique
3. THE System SHALL store user avatar_url as an optional field
4. THE System SHALL store full_name as a required field
5. THE System SHALL prevent users from modifying other users' profiles

### Requirement 3: Post Creation and Management

**User Story:** As a seller, I want to create posts for auto parts, so that buyers can find and purchase them.

#### Acceptance Criteria

1. WHEN a user creates a post, THE System SHALL require title and user_id fields
2. WHEN a user creates a post, THE System SHALL store images as a PostgreSQL text array
3. WHEN a user creates a post, THE System SHALL set status to active by default
4. WHEN a user creates a post, THE System SHALL validate post_type is either sale or wanted
5. WHEN a user uploads images, THE System SHALL validate each image is under 5MB
6. WHEN a user uploads images, THE System SHALL validate each image format is JPEG, PNG, or WebP
7. THE System SHALL store car_make, car_model, car_year, part_name, and price as optional fields
8. WHEN a user updates a post, THE System SHALL verify the user owns the post
9. WHEN a user deletes a post, THE System SHALL verify the user owns the post
10. WHEN a post status is sold, THE System SHALL prevent further modifications to the post


### Requirement 4: Post Search and Discovery

**User Story:** As a buyer, I want to search for auto parts using keywords and filters, so that I can find parts compatible with my vehicle.

#### Acceptance Criteria

1. WHEN a user searches with a keyword, THE System SHALL return posts where title or content contains the keyword
2. WHEN a user applies car_make filter, THE System SHALL return only posts matching that make
3. WHEN a user applies car_model filter, THE System SHALL return only posts matching that model
4. WHEN a user applies car_year filter, THE System SHALL return only posts matching that year
5. WHEN a user applies price range filters, THE System SHALL return only posts within the specified range
6. WHEN a user applies post_type filter, THE System SHALL return only posts matching that type
7. WHEN a user sorts by newest, THE System SHALL order results by created_at descending
8. WHEN a user sorts by price low-to-high, THE System SHALL order results by price ascending
9. WHEN a user sorts by price high-to-low, THE System SHALL order results by price descending
10. THE System SHALL return search results within 2 seconds
11. THE System SHALL return only posts with status active in search results
12. THE System SHALL support pagination with configurable page size

### Requirement 5: Real-Time Messaging

**User Story:** As a buyer or seller, I want to communicate in real-time via chat, so that I can negotiate prices and arrange transactions.

#### Acceptance Criteria

1. WHEN a buyer contacts a seller about a post, THE System SHALL create or retrieve a conversation for that buyer-seller-post combination
2. WHEN a user sends a text message, THE System SHALL store the message with message_type text and populate the body field
3. WHEN a user sends an image message, THE System SHALL store the message with message_type image and populate the image_url field
4. WHEN a message is created, THE System SHALL broadcast it via Supabase Realtime to all conversation participants
5. WHEN a message is created, THE System SHALL update the conversation last_message_at timestamp
6. WHEN a user views messages, THE System SHALL mark them as read by setting is_read to true
7. THE System SHALL validate that message sender is either the buyer or seller in the conversation
8. THE System SHALL support conversations with optional post_id (nullable)
9. THE System SHALL prevent editing or deleting messages after they are sent

### Requirement 6: Escrow Payment System

**User Story:** As a buyer, I want to pay securely with funds held in escrow, so that I am protected until I receive the part.

#### Acceptance Criteria

1. WHEN a buyer initiates escrow, THE System SHALL create a Stripe Payment_Intent
2. WHEN a buyer initiates escrow, THE System SHALL create an Escrow_Transaction with status on_hold
3. WHEN a buyer initiates escrow, THE System SHALL store the Stripe payment_intent_id in the transaction
4. WHEN Stripe confirms payment success, THE System SHALL keep transaction status as on_hold
5. WHEN Stripe confirms payment failure, THE System SHALL notify the buyer with the failure reason
6. WHEN a buyer approves release, THE System SHALL verify the buyer owns the transaction
7. WHEN a buyer approves release, THE System SHALL set buyer_approved_at timestamp
8. WHEN a buyer approves release, THE System SHALL update transaction status to released
9. WHEN a buyer approves release, THE System SHALL transfer funds to the seller via Stripe
10. WHEN funds are transferred, THE System SHALL set completed_at timestamp
11. WHEN funds are transferred, THE System SHALL update the post status to sold
12. WHEN a buyer raises a dispute, THE System SHALL update transaction status to disputed
13. WHEN a buyer raises a dispute, THE System SHALL store the dispute_reason
14. WHEN a buyer raises a dispute, THE System SHALL keep funds held in Stripe escrow
15. THE System SHALL validate that escrow amount is positive
16. THE System SHALL validate that escrow amount has at most 2 decimal places

### Requirement 7: Stripe Webhook Processing

**User Story:** As the system, I want to process Stripe webhooks reliably, so that payment status is accurately reflected.

#### Acceptance Criteria

1. WHEN a Stripe webhook is received, THE System SHALL verify the webhook signature
2. WHEN a webhook signature is invalid, THE System SHALL reject the webhook with a security error
3. WHEN a webhook is processed multiple times, THE System SHALL produce the same result (idempotent)
4. WHEN a payment succeeds, THE System SHALL notify both buyer and seller
5. WHEN a payment fails, THE System SHALL notify the buyer
6. THE System SHALL prevent duplicate Escrow_Transaction records for the same payment_intent_id

### Requirement 8: Bookmark Management

**User Story:** As a buyer, I want to bookmark posts, so that I can track items of interest.

#### Acceptance Criteria

1. WHEN a user bookmarks a post, THE System SHALL create a bookmark record
2. WHEN a user bookmarks the same post twice, THE System SHALL enforce uniqueness constraint on (user_id, post_id)
3. WHEN a user removes a bookmark, THE System SHALL delete the bookmark record
4. WHEN a user retrieves bookmarks, THE System SHALL return all bookmarked posts
5. THE System SHALL prevent users from bookmarking their own posts

### Requirement 9: Notification System

**User Story:** As a user, I want to receive notifications about important events, so that I stay informed about my transactions.

#### Acceptance Criteria

1. WHEN an escrow is created, THE System SHALL notify the seller
2. WHEN a payment succeeds, THE System SHALL notify both buyer and seller
3. WHEN funds are released, THE System SHALL notify the seller
4. WHEN a dispute is raised, THE System SHALL notify the seller
5. WHEN a notification is created, THE System SHALL store type, title, body, and data fields
6. WHEN a notification is created, THE System SHALL set is_read to false by default
7. WHEN a user marks a notification as read, THE System SHALL update is_read to true
8. WHEN a user marks all notifications as read, THE System SHALL update all their notifications
9. THE System SHALL store notification data as JSONB with default empty object

### Requirement 10: Authorization and Access Control

**User Story:** As the system, I want to enforce authorization rules, so that users can only access resources they own or have permission to view.

#### Acceptance Criteria

1. WHEN a user updates a post, THE System SHALL verify the user is the post owner
2. WHEN a user deletes a post, THE System SHALL verify the user is the post owner
3. WHEN a user approves escrow release, THE System SHALL verify the user is the transaction buyer
4. WHEN a user raises a dispute, THE System SHALL verify the user is the transaction buyer
5. WHEN a user sends a message, THE System SHALL verify the user is a participant in the conversation
6. THE System SHALL allow unauthenticated access to post search and post details
7. THE System SHALL require authentication for all other endpoints

### Requirement 11: Image Upload and Storage

**User Story:** As a seller, I want to upload multiple images for my posts, so that buyers can see the condition of parts.

#### Acceptance Criteria

1. WHEN a user uploads images, THE System SHALL upload them to Supabase Storage
2. WHEN images are uploaded, THE System SHALL generate unique filenames
3. WHEN images are uploaded, THE System SHALL return an array of public HTTPS URLs
4. WHEN any upload fails, THE System SHALL clean up partial uploads
5. THE System SHALL store image URLs as a PostgreSQL text array in the posts table
6. THE System SHALL default to an empty array when no images are provided

### Requirement 12: Post Status Transitions

**User Story:** As the system, I want to enforce valid post status transitions, so that post lifecycle is consistent.

#### Acceptance Criteria

1. WHEN a post is created, THE System SHALL set status to active
2. WHEN a post status is active, THE System SHALL allow transition to sold or archived
3. WHEN a post status is sold, THE System SHALL prevent any status changes
4. WHEN a post status is archived, THE System SHALL prevent any status changes
5. WHEN an escrow transaction completes, THE System SHALL update the associated post status to sold

### Requirement 13: Escrow Status Transitions

**User Story:** As the system, I want to enforce valid escrow status transitions, so that payment flow is consistent.

#### Acceptance Criteria

1. WHEN an escrow is created, THE System SHALL set status to on_hold
2. WHEN escrow status is on_hold, THE System SHALL allow transition to released or disputed
3. WHEN escrow status is disputed, THE System SHALL allow transition to on_hold or released
4. WHEN escrow status is released, THE System SHALL require buyer_approved_at to be set
5. WHEN escrow status is released, THE System SHALL transition to completed after funds transfer
6. THE System SHALL prevent releasing funds from a disputed transaction without resolution

### Requirement 14: Database Performance

**User Story:** As the system, I want to maintain fast query performance, so that users have a responsive experience.

#### Acceptance Criteria

1. THE System SHALL create an index on posts(status)
2. THE System SHALL create an index on posts(user_id)
3. THE System SHALL create an index on posts(car_make)
4. THE System SHALL create an index on posts(car_model)
5. THE System SHALL create an index on posts(car_year)
6. THE System SHALL create an index on posts(created_at) with descending order
7. THE System SHALL create an index on posts(price)
8. THE System SHALL create a GIN index on posts for full-text search
9. THE System SHALL create indexes on foreign key columns in all tables
10. THE System SHALL configure database connection pooling with HikariCP

### Requirement 15: Input Validation

**User Story:** As the system, I want to validate all user inputs, so that data integrity is maintained.

#### Acceptance Criteria

1. WHEN a user submits data, THE System SHALL validate required fields are not null
2. WHEN a user submits a username, THE System SHALL validate it is unique
3. WHEN a user submits a password, THE System SHALL validate it is at least 8 characters
4. WHEN a user submits an email, THE System SHALL validate it is a valid email format
5. WHEN a user submits a price, THE System SHALL validate it is positive
6. WHEN a user submits a price, THE System SHALL validate it has at most 2 decimal places
7. WHEN a user submits an image, THE System SHALL validate the file size is under 5MB
8. WHEN a user submits an image, THE System SHALL validate the format is JPEG, PNG, or WebP
9. THE System SHALL sanitize user-generated content to prevent XSS attacks

### Requirement 16: Error Handling

**User Story:** As a user, I want to receive clear error messages, so that I understand what went wrong and how to fix it.

#### Acceptance Criteria

1. WHEN invalid credentials are provided, THE System SHALL return 401 Unauthorized with message "Invalid email or password"
2. WHEN a duplicate username is registered, THE System SHALL return 409 Conflict with message "Username already taken"
3. WHEN a duplicate email is registered, THE System SHALL return 409 Conflict with message "Email already registered"
4. WHEN unauthorized access is attempted, THE System SHALL return 403 Forbidden with descriptive message
5. WHEN a resource is not found, THE System SHALL return 404 Not Found with descriptive message
6. WHEN validation fails, THE System SHALL return 400 Bad Request with specific validation errors
7. WHEN Stripe payment fails, THE System SHALL return 502 Bad Gateway with failure reason
8. WHEN database connection fails, THE System SHALL return 503 Service Unavailable
9. WHEN an image exceeds size limit, THE System SHALL return 400 Bad Request with message "Each image must be under 5MB"

### Requirement 17: Security

**User Story:** As the system, I want to implement security best practices, so that user data and payments are protected.

#### Acceptance Criteria

1. THE System SHALL hash passwords using BCrypt with cost factor 12
2. THE System SHALL never store plain text passwords
3. THE System SHALL sign JWT tokens with HS256 algorithm
4. THE System SHALL set JWT token expiration to 24 hours
5. THE System SHALL validate JWT tokens on every protected endpoint
6. THE System SHALL verify Stripe webhook signatures to prevent spoofing
7. THE System SHALL never store credit card information
8. THE System SHALL use HTTPS for all payment-related communications
9. THE System SHALL implement CORS with whitelist of allowed origins
10. THE System SHALL add security headers (X-Content-Type-Options, X-Frame-Options)
11. THE System SHALL validate Content-Type headers on requests
12. THE System SHALL implement request size limits

### Requirement 18: Real-Time Communication

**User Story:** As a user, I want messages to appear instantly, so that I can have fluid conversations.

#### Acceptance Criteria

1. WHEN a message is sent, THE System SHALL broadcast it via Supabase Realtime within 1 second
2. WHEN a WebSocket connection drops, THE System SHALL attempt to reconnect automatically
3. WHEN reconnecting, THE System SHALL fetch any missed messages
4. THE System SHALL implement message pagination with 50 messages per page
5. THE System SHALL lazy load chat history on scroll

### Requirement 19: User Identity Consistency

**User Story:** As the system, I want to maintain consistency between auth and profile data, so that user identity is reliable.

#### Acceptance Criteria

1. WHEN a user is created in Supabase_Auth, THE System SHALL create a corresponding Public_Users record
2. THE System SHALL enforce foreign key constraint from Public_Users.id to Auth_Users.id
3. WHEN syncing a user, THE System SHALL check if the user already exists before creating
4. THE System SHALL use the same UUID for both Auth_Users and Public_Users records

### Requirement 20: Conversation Management

**User Story:** As a user, I want conversations to be organized by post and participants, so that I can track multiple negotiations.

#### Acceptance Criteria

1. WHEN creating a conversation, THE System SHALL ensure uniqueness for (post_id, buyer_id, seller_id) combination
2. WHEN creating a conversation, THE System SHALL allow post_id to be null
3. WHEN retrieving conversations, THE System SHALL order by last_message_at descending
4. THE System SHALL update last_message_at whenever a new message is added

### Requirement 21: Transaction History

**User Story:** As a user, I want to view my transaction history, so that I can track my purchases and sales.

#### Acceptance Criteria

1. WHEN a buyer requests transactions, THE System SHALL return all transactions where they are the buyer
2. WHEN a seller requests transactions, THE System SHALL return all transactions where they are the seller
3. WHEN retrieving transactions, THE System SHALL include post details
4. WHEN retrieving transactions, THE System SHALL include buyer and seller information
5. THE System SHALL order transactions by created_at descending

### Requirement 22: Dispute Resolution

**User Story:** As a buyer, I want to raise disputes when issues occur, so that I can seek resolution before releasing funds.

#### Acceptance Criteria

1. WHEN a buyer raises a dispute, THE System SHALL validate the transaction is in on_hold status
2. WHEN a dispute is raised, THE System SHALL require a dispute_reason
3. WHEN a dispute is raised, THE System SHALL notify the seller with the reason
4. WHEN a transaction is disputed, THE System SHALL prevent buyer from approving release until resolved
5. THE System SHALL keep funds in Stripe escrow while transaction is disputed

### Requirement 23: Post Visibility

**User Story:** As a user, I want to see only relevant posts, so that I don't waste time on unavailable items.

#### Acceptance Criteria

1. WHEN browsing posts, THE System SHALL show only posts with status active
2. WHEN viewing a specific post, THE System SHALL allow viewing posts with any status
3. WHEN a post is sold, THE System SHALL exclude it from search results
4. WHEN a post is archived, THE System SHALL exclude it from search results

### Requirement 24: Notification Delivery

**User Story:** As a user, I want to receive timely notifications, so that I can respond to important events quickly.

#### Acceptance Criteria

1. WHEN a notification is created, THE System SHALL store it in the database immediately
2. WHEN retrieving notifications, THE System SHALL order by created_at descending
3. WHEN filtering notifications, THE System SHALL support filtering by is_read status
4. THE System SHALL include notification data as JSONB for flexible metadata

### Requirement 25: API Response Format

**User Story:** As a frontend developer, I want consistent API responses, so that I can handle them predictably.

#### Acceptance Criteria

1. WHEN an API request succeeds, THE System SHALL return appropriate 2xx status code
2. WHEN an API request fails, THE System SHALL return appropriate 4xx or 5xx status code
3. WHEN returning errors, THE System SHALL include a descriptive error message
4. WHEN returning paginated data, THE System SHALL include total count, page number, and page size
5. THE System SHALL return JSON responses with appropriate Content-Type header

### Requirement 26: Session Management

**User Story:** As a user, I want my session to persist, so that I don't have to log in repeatedly.

#### Acceptance Criteria

1. WHEN a user logs in, THE System SHALL return a JWT token
2. THE System SHALL implement stateless authentication (no server-side sessions)
3. WHEN a user logs out, THE System SHALL clear the token from client storage
4. THE System SHALL validate token expiration on each request
5. THE System SHALL reject expired tokens with 401 Unauthorized

### Requirement 27: Data Integrity

**User Story:** As the system, I want to maintain referential integrity, so that data remains consistent.

#### Acceptance Criteria

1. THE System SHALL enforce foreign key constraints on all relationships
2. WHEN a user is deleted, THE System SHALL handle cascading deletes appropriately
3. WHEN a post is deleted, THE System SHALL handle related conversations and bookmarks
4. THE System SHALL use database transactions for multi-step operations
5. THE System SHALL ensure atomic operations for escrow creation and payment intent creation

### Requirement 28: Image Delivery

**User Story:** As a user, I want images to load quickly, so that I can evaluate parts efficiently.

#### Acceptance Criteria

1. THE System SHALL serve images via Supabase Storage CDN
2. THE System SHALL return HTTPS URLs for all images
3. WHEN images are requested, THE System SHALL leverage CDN caching
4. THE System SHALL generate unique filenames to prevent collisions

### Requirement 29: Search Accuracy

**User Story:** As a buyer, I want search results to be relevant, so that I can find what I need quickly.

#### Acceptance Criteria

1. WHEN searching with keywords, THE System SHALL use full-text search on title and content
2. WHEN multiple filters are applied, THE System SHALL combine them with AND logic
3. WHEN no filters are applied, THE System SHALL return all active posts
4. THE System SHALL perform case-insensitive keyword matching

### Requirement 30: Escrow Transaction Atomicity

**User Story:** As the system, I want escrow creation to be atomic, so that payment state is always consistent.

#### Acceptance Criteria

1. WHEN creating an escrow transaction, THE System SHALL create both database record and Stripe Payment_Intent atomically
2. WHEN Stripe Payment_Intent creation fails, THE System SHALL rollback the database transaction
3. WHEN database insert fails, THE System SHALL not create a Stripe Payment_Intent
4. THE System SHALL ensure both records reference each other correctly

