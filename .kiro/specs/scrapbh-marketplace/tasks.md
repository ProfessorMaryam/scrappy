# Implementation Plan: ScrapBH Marketplace

## Overview

This implementation plan breaks down the ScrapBH marketplace into discrete coding tasks. The system uses Java Spring Boot backend, React TypeScript frontend, Supabase PostgreSQL database with Auth integration, Stripe for payments, and Supabase Realtime for chat. Tasks are organized to build incrementally, with testing sub-tasks marked as optional.

## Tasks

- [x] 1. Set up project structure and database schema
  - Create Spring Boot project with Maven/Gradle
  - Configure Supabase connection and environment variables
  - Create database schema with all tables (users, posts, conversations, messages, escrow_transactions, bookmarks, notifications)
  - Create database enums (user_role, post_type, post_status, message_type, escrow_status)
  - Create all database indexes for performance
  - Set up HikariCP connection pooling
  - _Requirements: 14.1-14.10, 19.2, 27.1_

- [ ] 2. Implement authentication service with Supabase Auth
  - [x] 2.1 Create User entity and repository
    - Create User JPA entity with UUID, full_name, username, role, avatar_url, created_at
    - Create UserRepository interface extending JpaRepository
    - _Requirements: 1.2, 19.1, 19.4_

  - [x] 2.2 Implement AuthenticationService
    - Implement register() method integrating with Supabase Auth
    - Implement syncSupabaseAuthUser() to sync auth.users with public.users
    - Implement login() method returning JWT token from Supabase Auth
    - Implement validateToken() method for JWT validation
    - Implement getCurrentUser() method
    - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5, 1.6, 19.1, 19.3, 19.4_

  - [-] 2.3 Create AuthController REST endpoints
    - Create POST /api/auth/register endpoint
    - Create POST /api/auth/login endpoint
    - Create GET /api/auth/me endpoint
    - Create POST /api/auth/logout endpoint
    - _Requirements: 1.1, 1.4, 26.1_

  - [ ]* 2.4 Write unit tests for AuthenticationService
    - Test valid registration creates user and returns token
    - Test duplicate username registration throws conflict error
    - Test valid login returns token
    - Test invalid credentials throw unauthorized error
    - _Requirements: 1.3, 1.4, 1.5, 16.1, 16.2_

  - [ ]* 2.5 Write property test for user identity consistency
    - **Property 11: User Identity Consistency**
    - **Validates: Requirements 1.6, 19.1, 19.2**

- [ ] 3. Implement post management service
  - [~] 3.1 Create Post entity and repository
    - Create Post JPA entity with all fields (id, user_id, post_type, status, title, content, images array, car fields, price, created_at)
    - Create PostRepository interface with custom query methods
    - _Requirements: 3.1, 3.2, 3.3, 3.7_

  - [~] 3.2 Implement image upload to Supabase Storage
    - Create uploadPostImages() method
    - Validate image size (max 5MB) and format (JPEG, PNG, WebP)
    - Generate unique filenames
    - Upload to Supabase Storage bucket
    - Return array of public HTTPS URLs
    - Implement cleanup for failed uploads
    - _Requirements: 3.5, 3.6, 11.1, 11.2, 11.3, 11.4, 11.5, 11.6, 15.7, 15.8, 16.9, 28.1, 28.2, 28.4_

  - [~] 3.3 Implement PostService CRUD operations
    - Implement createPost() with validation
    - Implement updatePost() with owner verification
    - Implement deletePost() with owner verification
    - Implement getPostById() method
    - Implement getSellerPosts() method
    - Implement updatePostStatus() method
    - _Requirements: 3.1, 3.2, 3.3, 3.4, 3.8, 3.9, 3.10, 10.1, 10.2, 12.1, 12.2, 12.3, 12.4_

  - [~] 3.4 Create PostController REST endpoints
    - Create POST /api/posts endpoint
    - Create PUT /api/posts/{id} endpoint
    - Create DELETE /api/posts/{id} endpoint
    - Create GET /api/posts/{id} endpoint
    - Create GET /api/posts/user/{id} endpoint
    - Create POST /api/posts/images endpoint
    - _Requirements: 3.1, 3.8, 3.9, 25.1, 25.2, 25.3_

  - [ ]* 3.5 Write unit tests for PostService
    - Test create post with valid data succeeds
    - Test update post by owner succeeds
    - Test update post by non-owner throws UnauthorizedException
    - Test delete post removes from database
    - Test image validation (size and format)
    - _Requirements: 3.5, 3.6, 3.8, 3.9, 10.1, 10.2, 16.3_

  - [ ]* 3.6 Write property test for post status transitions
    - **Property 3: Post Status Invariant**
    - **Validates: Requirements 12.1, 12.2, 12.3, 12.4, 12.5**

- [~] 4. Checkpoint - Ensure all tests pass
  - Ensure all tests pass, ask the user if questions arise.

- [ ] 5. Implement search and discovery service
  - [~] 5.1 Create SearchService with filtering logic
    - Implement searchPosts() using JPA Criteria API
    - Support keyword search on title and content (case-insensitive)
    - Support filters: car_make, car_model, car_year, price range, post_type
    - Support sorting: newest, price low-to-high, price high-to-low
    - Implement pagination with configurable page size
    - Filter to show only active posts in search results
    - Optimize query to complete within 2 seconds
    - _Requirements: 4.1, 4.2, 4.3, 4.4, 4.5, 4.6, 4.7, 4.8, 4.9, 4.10, 4.11, 4.12, 23.1, 23.2, 23.3, 23.4, 29.1, 29.2, 29.3, 29.4_

  - [~] 5.2 Implement car data helper methods
    - Implement getCarMakes() method
    - Implement getCarModels(make) method
    - Implement getCarYears() method
    - Implement filterByCarCompatibility() method
    - _Requirements: 4.2, 4.3, 4.4_

  - [~] 5.3 Create SearchController REST endpoints
    - Create GET /api/posts/search endpoint with query parameters
    - Create GET /api/posts endpoint for recent posts
    - Return paginated results with total count, page number, page size
    - _Requirements: 4.12, 25.4, 25.5_

  - [ ]* 5.4 Write unit tests for SearchService
    - Test keyword search returns matching posts
    - Test filters correctly narrow results
    - Test sorting works correctly
    - Test only active posts appear in results
    - Test pagination works correctly
    - _Requirements: 4.1, 4.2, 4.11, 23.1_

  - [ ]* 5.5 Write property test for search performance
    - **Property 5: Search Performance Guarantee**
    - **Validates: Requirements 4.10**

  - [ ]* 5.6 Write property test for search idempotency
    - **Property from design: Search results are idempotent**
    - **Validates: Requirements 4.1, 29.1**

- [ ] 6. Implement conversation and messaging service
  - [~] 6.1 Create Conversation and Message entities
    - Create Conversation JPA entity with post_id (nullable), buyer_id, seller_id, last_message_at, created_at
    - Create Message JPA entity with conversation_id, sender_id, message_type, body, image_url, is_read, created_at
    - Create ConversationRepository and MessageRepository
    - Add unique constraint on (post_id, buyer_id, seller_id) for conversations
    - _Requirements: 5.1, 5.2, 5.3, 5.8, 20.1, 20.2_

  - [~] 6.2 Implement ConversationService
    - Implement createOrGetConversation() ensuring uniqueness
    - Implement getUserConversations() ordered by last_message_at descending
    - Implement getConversationHistory() with pagination (50 messages per page)
    - Implement markMessagesAsRead() method
    - Implement updateLastMessageTimestamp() method
    - _Requirements: 5.1, 5.6, 5.8, 18.4, 20.1, 20.3, 20.4_

  - [~] 6.3 Implement message sending with Supabase Realtime
    - Implement sendMessage() for text messages (populate body field)
    - Implement sendMessage() for image messages (populate image_url field)
    - Validate sender is participant in conversation
    - Update conversation last_message_at timestamp
    - Integrate with Supabase Realtime to broadcast messages
    - _Requirements: 5.2, 5.3, 5.4, 5.5, 5.7, 5.9_

  - [~] 6.4 Create ConversationController REST endpoints
    - Create POST /api/conversations endpoint
    - Create GET /api/conversations endpoint
    - Create GET /api/conversations/{id}/messages endpoint
    - Create POST /api/messages endpoint
    - Create PUT /api/conversations/{id}/read endpoint
    - _Requirements: 5.1, 5.6, 25.1, 25.2_

  - [ ]* 6.5 Write unit tests for ConversationService
    - Test create conversation for buyer-seller pair succeeds
    - Test send text message stores body field
    - Test send image message stores image_url field
    - Test mark messages as read updates is_read flag
    - Test update last_message_at timestamp
    - _Requirements: 5.1, 5.2, 5.3, 5.5, 5.6_

  - [ ]* 6.6 Write property test for conversation uniqueness
    - **Property 8: Conversation Uniqueness**
    - **Validates: Requirements 5.1, 20.1**

  - [ ]* 6.7 Write property test for message type consistency
    - **Property 9: Message Type Consistency**
    - **Validates: Requirements 5.2, 5.3**

- [ ] 7. Implement escrow payment service with Stripe
  - [~] 7.1 Create EscrowTransaction entity and repository
    - Create EscrowTransaction JPA entity with all fields
    - Create EscrowTransactionRepository with custom queries
    - Add index on stripe_payment_intent_id
    - _Requirements: 6.2, 6.3, 7.6, 14.10_

  - [~] 7.2 Integrate Stripe SDK and configure webhooks
    - Add Stripe Java SDK dependency
    - Configure Stripe API keys from environment variables
    - Implement webhook signature verification
    - _Requirements: 7.1, 7.2, 17.6, 17.7_

  - [~] 7.3 Implement EscrowService core methods
    - Implement initiateEscrow() creating Stripe payment intent and transaction record atomically
    - Implement validateEscrowAmount() checking positive value and 2 decimal places
    - Implement getTransactionById() method
    - Implement getUserTransactions() for buyer and seller
    - _Requirements: 6.1, 6.2, 6.3, 6.15, 6.16, 21.1, 21.2, 21.3, 21.4, 21.5, 30.1, 30.2, 30.3, 30.4_

  - [~] 7.4 Implement Stripe webhook handler
    - Implement handleStripeWebhook() with signature verification
    - Handle payment success: keep status as on_hold, notify buyer and seller
    - Handle payment failure: notify buyer with failure reason
    - Implement idempotent processing to prevent duplicate updates
    - _Requirements: 6.4, 6.5, 7.1, 7.2, 7.3, 7.4, 7.5, 7.6_

  - [~] 7.5 Implement buyer approval and dispute methods
    - Implement approveBuyerRelease() with buyer verification
    - Set buyer_approved_at timestamp
    - Update status to released then completed
    - Transfer funds to seller via Stripe
    - Set completed_at timestamp
    - Update post status to sold
    - Implement raiseDispute() with buyer verification
    - Store dispute_reason and update status to disputed
    - Keep funds in Stripe escrow when disputed
    - _Requirements: 6.6, 6.7, 6.8, 6.9, 6.10, 6.11, 6.12, 6.13, 6.14, 10.3, 10.4, 12.5, 13.1, 13.2, 13.3, 13.4, 13.5, 13.6, 22.1, 22.2, 22.3, 22.4, 22.5_

  - [~] 7.6 Create EscrowController REST endpoints
    - Create POST /api/escrow/initiate endpoint
    - Create POST /api/escrow/webhook endpoint (Stripe webhook)
    - Create PUT /api/escrow/{id}/approve endpoint
    - Create PUT /api/escrow/{id}/dispute endpoint
    - Create GET /api/escrow/transactions endpoint
    - Create GET /api/escrow/{id} endpoint
    - _Requirements: 6.1, 6.6, 6.12, 25.1, 25.2_

  - [ ]* 7.7 Write unit tests for EscrowService
    - Test initiate escrow creates Stripe payment intent
    - Test approve buyer release transfers funds and marks post sold
    - Test raise dispute updates status and stores reason
    - Test unauthorized escrow operations throw exceptions
    - _Requirements: 6.1, 6.6, 6.12, 10.3, 10.4_

  - [ ]* 7.8 Write property test for escrow transaction atomicity
    - **Property 1: Escrow Transaction Atomicity**
    - **Validates: Requirements 6.1, 6.2, 6.3, 30.1, 30.2, 30.3, 30.4**

  - [ ]* 7.9 Write property test for buyer approval guarantees
    - **Property 2: Buyer Approval Guarantees**
    - **Validates: Requirements 6.6, 6.7, 6.8, 6.9, 6.10, 6.11**

  - [ ]* 7.10 Write property test for escrow status validity
    - **Property 4: Escrow Status Validity**
    - **Validates: Requirements 13.1, 13.2, 13.3, 13.4, 13.5, 13.6**

  - [ ]* 7.11 Write property test for Stripe webhook idempotency
    - **Property 10: Stripe Webhook Idempotency**
    - **Validates: Requirements 7.3, 7.6**

- [~] 8. Checkpoint - Ensure all tests pass
  - Ensure all tests pass, ask the user if questions arise.

- [ ] 9. Implement bookmark service
  - [~] 9.1 Create Bookmark entity and repository
    - Create Bookmark JPA entity with user_id, post_id, created_at
    - Add unique constraint on (user_id, post_id)
    - Create BookmarkRepository
    - _Requirements: 8.2_

  - [~] 9.2 Implement BookmarkService
    - Implement addToBookmarks() with duplicate prevention
    - Implement removeFromBookmarks() method
    - Implement getBookmarks() returning bookmarked posts
    - Implement isBookmarked() check method
    - Prevent users from bookmarking their own posts
    - _Requirements: 8.1, 8.2, 8.3, 8.4, 8.5_

  - [~] 9.3 Create BookmarkController REST endpoints
    - Create POST /api/bookmarks endpoint
    - Create DELETE /api/bookmarks/{postId} endpoint
    - Create GET /api/bookmarks endpoint
    - _Requirements: 8.1, 8.3_

  - [ ]* 9.4 Write unit tests for BookmarkService
    - Test bookmark post succeeds
    - Test duplicate bookmark enforces uniqueness
    - Test remove bookmark deletes record
    - Test cannot bookmark own posts
    - _Requirements: 8.1, 8.2, 8.5_

  - [ ]* 9.5 Write property test for bookmark uniqueness
    - **Property 12: Bookmark Uniqueness**
    - **Validates: Requirements 8.2**

- [ ] 10. Implement notification service
  - [~] 10.1 Create Notification entity and repository
    - Create Notification JPA entity with user_id, type, title, body, data (JSONB), is_read, created_at
    - Create NotificationRepository
    - Add indexes on user_id and is_read
    - _Requirements: 9.5, 9.6, 9.9, 14.10_

  - [~] 10.2 Implement NotificationService
    - Implement createNotification() with type, title, body, and JSONB data
    - Implement getUserNotifications() with unreadOnly filter
    - Implement markAsRead() for single notification
    - Implement markAllAsRead() for user
    - Order notifications by created_at descending
    - _Requirements: 9.1, 9.2, 9.3, 9.4, 9.5, 9.6, 9.7, 9.8, 9.9, 24.1, 24.2, 24.3, 24.4_

  - [~] 10.3 Integrate notifications into escrow flow
    - Notify seller when escrow is created
    - Notify buyer and seller when payment succeeds
    - Notify seller when funds are released
    - Notify seller when dispute is raised
    - _Requirements: 9.1, 9.2, 9.3, 9.4_

  - [~] 10.4 Create NotificationController REST endpoints
    - Create GET /api/notifications endpoint
    - Create PUT /api/notifications/{id}/read endpoint
    - Create PUT /api/notifications/read-all endpoint
    - _Requirements: 9.7, 9.8_

  - [ ]* 10.5 Write unit tests for NotificationService
    - Test create notification stores all fields correctly
    - Test get notifications filters by is_read
    - Test mark as read updates is_read flag
    - Test mark all as read updates all user notifications
    - _Requirements: 9.5, 9.6, 9.7, 9.8_

- [ ] 11. Implement security and authorization
  - [~] 11.1 Configure Spring Security
    - Create SecurityConfig with JWT filter
    - Configure CORS with allowed origins
    - Set session management to stateless
    - Configure public endpoints (auth, search, post details)
    - Configure protected endpoints (require authentication)
    - _Requirements: 10.6, 10.7, 17.9_

  - [~] 11.2 Implement JWT authentication filter
    - Create JwtAuthenticationFilter
    - Validate JWT token on each request
    - Extract user from token and set SecurityContext
    - Handle expired tokens with 401 Unauthorized
    - _Requirements: 17.3, 17.4, 17.5, 26.4, 26.5_

  - [~] 11.3 Implement input validation
    - Add Bean Validation annotations to DTOs (@NotNull, @Size, @Email, etc.)
    - Validate required fields are not null
    - Validate username uniqueness
    - Validate password minimum 8 characters
    - Validate email format
    - Validate price is positive with max 2 decimal places
    - Sanitize user-generated content to prevent XSS
    - _Requirements: 15.1, 15.2, 15.3, 15.4, 15.5, 15.6, 15.9_

  - [~] 11.4 Implement authorization checks
    - Verify user owns post before update/delete
    - Verify user is buyer before approving escrow release
    - Verify user is buyer before raising dispute
    - Verify user is conversation participant before sending message
    - _Requirements: 10.1, 10.2, 10.3, 10.4, 10.5_

  - [~] 11.5 Add security headers and request limits
    - Add X-Content-Type-Options header
    - Add X-Frame-Options header
    - Validate Content-Type headers on requests
    - Implement request size limits
    - _Requirements: 17.10, 17.11, 17.12_

  - [ ]* 11.6 Write property test for authorization invariant
    - **Property 6: Authorization Invariant**
    - **Validates: Requirements 10.1, 10.2, 10.3, 10.4**

- [ ] 12. Implement error handling and validation
  - [~] 12.1 Create global exception handler
    - Handle InvalidCredentialsException → 401 with message
    - Handle UserAlreadyExistsException → 409 with message
    - Handle UnauthorizedException → 403 with message
    - Handle NotFoundException → 404 with message
    - Handle ValidationException → 400 with validation errors
    - Handle Stripe errors → 502 with failure reason
    - Handle database errors → 503 Service Unavailable
    - _Requirements: 16.1, 16.2, 16.3, 16.4, 16.5, 16.6, 16.7, 16.8, 16.9_

  - [~] 12.2 Create custom exception classes
    - Create InvalidCredentialsException
    - Create UserAlreadyExistsException
    - Create UnauthorizedException
    - Create NotFoundException
    - Create ValidationException
    - Create InvalidStateException
    - _Requirements: 16.1, 16.2, 16.3, 16.4, 16.5, 16.6_

  - [~] 12.3 Implement consistent API response format
    - Return appropriate 2xx status codes for success
    - Return appropriate 4xx/5xx status codes for errors
    - Include descriptive error messages in error responses
    - Include total count, page number, page size in paginated responses
    - Set appropriate Content-Type headers
    - _Requirements: 25.1, 25.2, 25.3, 25.4, 25.5_

  - [ ]* 12.4 Write integration tests for error handling
    - Test invalid credentials return 401
    - Test duplicate registration returns 409
    - Test unauthorized access returns 403
    - Test not found returns 404
    - Test validation errors return 400
    - _Requirements: 16.1, 16.2, 16.3, 16.4, 16.5, 16.6_

- [~] 13. Checkpoint - Ensure all tests pass
  - Ensure all tests pass, ask the user if questions arise.

- [~] 14. Set up React frontend project
  - Create React project with TypeScript using Vite
  - Install dependencies: React Router, React Query, Tailwind CSS, Supabase client, Axios
  - Configure Tailwind CSS
  - Set up environment variables for API base URL, Supabase URL/key, Stripe publishable key
  - Create project structure (components, pages, services, contexts, hooks)
  - _Requirements: 25.1_

- [ ] 15. Implement frontend authentication
  - [~] 15.1 Create AuthContext for global state
    - Create AuthContext with user, token, login, register, logout, isAuthenticated
    - Implement login() calling backend API
    - Implement register() calling backend API
    - Implement logout() clearing token from localStorage
    - Store JWT token in localStorage
    - Provide isSeller and isBuyer computed properties
    - _Requirements: 1.1, 1.4, 26.1, 26.2, 26.3_

  - [~] 15.2 Create authentication pages
    - Create LoginPage with email and password fields
    - Create RegisterPage with username, password, full_name, role fields
    - Display error messages for invalid credentials or duplicate username
    - Redirect to dashboard after successful login/register
    - _Requirements: 1.1, 1.3, 1.4, 16.1, 16.2_

  - [~] 15.3 Create ProtectedRoute component
    - Check if user is authenticated
    - Redirect to login if not authenticated
    - Render protected content if authenticated
    - _Requirements: 10.7, 26.4_

  - [ ]* 15.4 Write unit tests for AuthContext
    - Test login stores token in localStorage
    - Test logout clears token
    - Test isAuthenticated returns correct value
    - _Requirements: 26.1, 26.3_

- [ ] 16. Implement frontend post management
  - [~] 16.1 Create PostForm component
    - Create form with title, content, car_make, car_model, car_year, part_name, post_type, price fields
    - Implement multiple image upload with preview
    - Validate image size (max 5MB) and format before upload
    - Call POST /api/posts/images to upload images
    - Call POST /api/posts to create post with image URLs
    - Display success message after post creation
    - _Requirements: 3.1, 3.2, 3.5, 3.6, 11.1, 11.2, 11.3, 11.4, 11.5, 11.6_

  - [~] 16.2 Create PostList and PostCard components
    - Create PostCard displaying post title, price, car info, images
    - Create PostList rendering array of PostCard components
    - Display post status badge (active, sold, archived)
    - Add click handler to navigate to post details
    - _Requirements: 3.1, 3.3, 23.1_

  - [~] 16.3 Create PostDetailPage
    - Display full post details with all images
    - Show seller information
    - Add "Contact Seller" button for buyers
    - Add "Edit" and "Delete" buttons for post owner
    - Display post status
    - _Requirements: 3.1, 10.1, 10.2, 23.2_

  - [~] 16.4 Create MyPostsPage for sellers
    - Fetch and display user's posts
    - Show post status for each post
    - Add edit and delete actions
    - _Requirements: 3.1, 10.1, 10.2_

  - [ ]* 16.5 Write component tests for PostForm
    - Test form validation
    - Test image upload preview
    - Test successful post creation
    - _Requirements: 3.1, 3.5, 3.6_

- [ ] 17. Implement frontend search and discovery
  - [~] 17.1 Create SearchBar component
    - Create search input for keyword
    - Create filter dropdowns for car_make, car_model, car_year, post_type
    - Create price range inputs (min and max)
    - Create sort dropdown (newest, price low-to-high, price high-to-low)
    - Call GET /api/posts/search with query parameters
    - _Requirements: 4.1, 4.2, 4.3, 4.4, 4.5, 4.6, 4.7, 4.8, 4.9_

  - [~] 17.2 Create SearchResultsPage
    - Display SearchBar component
    - Display search results using PostList component
    - Implement pagination controls
    - Display total count and current page
    - Show "No results found" message when appropriate
    - _Requirements: 4.11, 4.12, 25.4_

  - [~] 17.3 Create HomePage with recent posts
    - Fetch and display recent posts
    - Add link to search page
    - Display featured or popular posts
    - _Requirements: 4.11_

  - [ ]* 17.4 Write integration tests for search flow
    - Test keyword search returns results
    - Test filters narrow results
    - Test pagination works correctly
    - _Requirements: 4.1, 4.2, 4.12_

- [ ] 18. Implement frontend real-time chat
  - [~] 18.1 Create ConversationWindow component
    - Display conversation history with message bubbles
    - Show sender name and timestamp for each message
    - Display text messages in body field
    - Display image messages with image preview
    - Implement scroll to bottom on new message
    - Mark messages as read when viewed
    - _Requirements: 5.2, 5.3, 5.6_

  - [~] 18.2 Implement Supabase Realtime integration
    - Connect to Supabase Realtime on conversation open
    - Subscribe to conversation channel
    - Listen for new message events
    - Update UI when new message arrives
    - Implement automatic reconnection on connection drop
    - Fetch missed messages on reconnection
    - _Requirements: 5.4, 18.1, 18.2, 18.3_

  - [~] 18.3 Create message input component
    - Create text input for message body
    - Create image upload button for image messages
    - Call POST /api/messages to send text message
    - Call POST /api/messages to send image message
    - Disable send button while sending
    - _Requirements: 5.2, 5.3_

  - [~] 18.4 Create ConversationListPage
    - Fetch and display user's conversations
    - Show last message and timestamp for each conversation
    - Order by last_message_at descending
    - Add click handler to open conversation
    - Show unread indicator if messages are unread
    - _Requirements: 5.6, 20.3_

  - [~] 18.5 Implement "Contact Seller" flow
    - Add "Contact Seller" button on PostDetailPage
    - Call POST /api/conversations to create or get conversation
    - Navigate to ConversationWindow
    - _Requirements: 5.1_

  - [ ]* 18.6 Write component tests for ConversationWindow
    - Test messages display correctly
    - Test send message updates UI
    - Test image messages display correctly
    - _Requirements: 5.2, 5.3_

- [ ] 19. Implement frontend escrow payment
  - [~] 19.1 Create EscrowPanel component
    - Display transaction status (on_hold, released, disputed, completed)
    - Show amount and post details
    - Add "Approve Release" button for buyer (when status is on_hold)
    - Add "Raise Dispute" button for buyer (when status is on_hold)
    - Display buyer_approved_at and completed_at timestamps
    - Display dispute_reason if status is disputed
    - _Requirements: 6.2, 6.6, 6.7, 6.8, 6.12, 6.13_

  - [~] 19.2 Create StripePaymentModal component
    - Integrate Stripe Elements for payment form
    - Call POST /api/escrow/initiate to create payment intent
    - Use Stripe client secret to complete payment
    - Display payment success or failure message
    - Handle payment errors gracefully
    - _Requirements: 6.1, 6.4, 6.5, 16.7_

  - [~] 19.3 Implement "Initiate Escrow" flow
    - Add "Buy with Escrow" button in chat or post detail
    - Open StripePaymentModal with agreed amount
    - Create escrow transaction on successful payment
    - Display escrow status in conversation
    - _Requirements: 6.1, 6.2, 6.3_

  - [~] 19.4 Implement buyer approval flow
    - Call PUT /api/escrow/{id}/approve when buyer clicks "Approve Release"
    - Update UI to show released/completed status
    - Display success message
    - _Requirements: 6.6, 6.7, 6.8, 6.9, 6.10, 6.11_

  - [~] 19.5 Implement dispute flow
    - Show dispute reason input when buyer clicks "Raise Dispute"
    - Call PUT /api/escrow/{id}/dispute with reason
    - Update UI to show disputed status
    - Display dispute reason
    - _Requirements: 6.12, 6.13, 6.14_

  - [~] 19.6 Create TransactionHistoryPage
    - Fetch and display user's transactions (as buyer or seller)
    - Show transaction status, amount, post details
    - Order by created_at descending
    - Add link to view post details
    - _Requirements: 21.1, 21.2, 21.3, 21.4, 21.5_

  - [ ]* 19.7 Write integration tests for escrow flow
    - Test initiate escrow creates transaction
    - Test approve release updates status
    - Test raise dispute updates status
    - _Requirements: 6.1, 6.6, 6.12_

- [~] 20. Checkpoint - Ensure all tests pass
  - Ensure all tests pass, ask the user if questions arise.

- [ ] 21. Implement frontend bookmarks and notifications
  - [~] 21.1 Create bookmark functionality
    - Add bookmark icon to PostCard and PostDetailPage
    - Call POST /api/bookmarks to add bookmark
    - Call DELETE /api/bookmarks/{postId} to remove bookmark
    - Update icon state based on bookmark status
    - _Requirements: 8.1, 8.3_

  - [~] 21.2 Create BookmarksPage
    - Fetch and display user's bookmarked posts
    - Use PostList component to display bookmarks
    - Add remove bookmark action
    - _Requirements: 8.3, 8.4_

  - [~] 21.3 Create NotificationBell component
    - Display notification icon in header
    - Show unread count badge
    - Open dropdown with recent notifications on click
    - Display notification title and body
    - Mark notification as read on click
    - _Requirements: 9.6, 9.7, 24.2, 24.3_

  - [~] 21.4 Create NotificationsPage
    - Fetch and display all user notifications
    - Show notification type, title, body, timestamp
    - Highlight unread notifications
    - Add "Mark all as read" button
    - Order by created_at descending
    - _Requirements: 9.6, 9.7, 9.8, 24.2_

  - [ ]* 21.5 Write component tests for bookmarks
    - Test add bookmark updates UI
    - Test remove bookmark updates UI
    - _Requirements: 8.1, 8.3_

- [ ] 22. Implement responsive design and accessibility
  - [~] 22.1 Make all pages responsive
    - Use Tailwind responsive classes for mobile, tablet, desktop
    - Test on different screen sizes
    - Ensure navigation works on mobile
    - Make forms usable on mobile devices
    - _Requirements: Design requirement for responsive web access_

  - [~] 22.2 Add accessibility features
    - Add proper ARIA labels to interactive elements
    - Ensure keyboard navigation works
    - Add alt text to images
    - Ensure sufficient color contrast
    - Add focus indicators
    - _Requirements: Accessibility compliance_

  - [~] 22.3 Optimize image loading
    - Implement lazy loading for images
    - Add loading placeholders
    - Use responsive image sizes
    - _Requirements: 28.1, 28.3_

  - [ ]* 22.4 Write accessibility tests
    - Test keyboard navigation
    - Test screen reader compatibility
    - Test color contrast
    - _Requirements: Accessibility compliance_

- [ ] 23. Integration and end-to-end testing
  - [ ]* 23.1 Write end-to-end test for complete post creation flow
    - Test user registration
    - Test login
    - Test image upload
    - Test post creation
    - Test post appears in search results
    - _Requirements: 1.1, 1.4, 3.1, 4.1, 11.1_

  - [ ]* 23.2 Write end-to-end test for complete escrow flow
    - Test post creation
    - Test conversation creation
    - Test escrow initiation
    - Test Stripe payment
    - Test buyer approval
    - Test post marked as sold
    - _Requirements: 3.1, 5.1, 6.1, 6.6, 12.5_

  - [ ]* 23.3 Write end-to-end test for negotiation and payment flow
    - Test buyer contacts seller
    - Test message exchange
    - Test escrow initiation
    - Test payment completion
    - Test funds held in escrow
    - _Requirements: 5.1, 5.2, 6.1, 6.4_

  - [ ]* 23.4 Write end-to-end test for real-time chat
    - Test conversation creation
    - Test send text message
    - Test send image message
    - Test message appears in real-time
    - Test mark messages as read
    - _Requirements: 5.1, 5.2, 5.3, 5.4, 5.6_

  - [ ]* 23.5 Write integration test for authentication flow with Supabase Auth
    - Test register creates user in Supabase Auth
    - Test sync to public.users table
    - Test login returns JWT token
    - Test access protected endpoints with token
    - Test unauthorized access is blocked
    - _Requirements: 1.1, 1.2, 1.4, 1.6, 10.7, 19.1_

- [ ] 24. Performance optimization and monitoring
  - [~] 24.1 Optimize database queries
    - Verify all indexes are created
    - Test search query performance (must be < 2 seconds)
    - Optimize N+1 query problems with eager loading
    - _Requirements: 4.10, 14.1-14.9_

  - [~] 24.2 Configure connection pooling
    - Set HikariCP maximum pool size to 20
    - Set minimum idle connections to 5
    - Configure connection timeout values
    - _Requirements: 14.10_

  - [~] 24.3 Implement frontend performance optimizations
    - Use React.memo for expensive components
    - Implement code splitting with React.lazy
    - Optimize bundle size
    - Use React Query caching for API calls
    - _Requirements: Performance best practices_

  - [ ]* 24.4 Write performance tests
    - Test search completes within 2 seconds
    - Test image upload performance
    - Test real-time message delivery latency
    - _Requirements: 4.10, 18.1_

- [ ] 25. Deployment preparation
  - [~] 25.1 Create production configuration
    - Set up production environment variables
    - Configure CORS for production domain
    - Set up Stripe production keys
    - Configure Supabase production instance
    - _Requirements: 17.9_

  - [~] 25.2 Create Docker configuration
    - Create Dockerfile for Spring Boot backend
    - Create Dockerfile for React frontend
    - Create docker-compose.yml for local development
    - _Requirements: Deployment preparation_

  - [~] 25.3 Set up database migrations
    - Create Flyway or Liquibase migration scripts
    - Test migrations on clean database
    - Document migration process
    - _Requirements: 27.1, 27.4_

  - [~] 25.4 Create deployment documentation
    - Document environment variables
    - Document deployment steps
    - Document database setup
    - Document Stripe webhook configuration
    - _Requirements: Deployment preparation_

  - [~] 25.5 Set up health check endpoints
    - Create GET /api/health endpoint
    - Check database connectivity
    - Check Supabase connectivity
    - Check Stripe connectivity
    - _Requirements: 16.8_

- [~] 26. Final checkpoint - Ensure all tests pass
  - Ensure all tests pass, ask the user if questions arise.

## Notes

- Tasks marked with `*` are optional and can be skipped for faster MVP
- Each task references specific requirements for traceability
- Checkpoints ensure incremental validation
- Property tests validate universal correctness properties from the design document
- Unit tests validate specific examples and edge cases
- Integration tests validate end-to-end flows
- The implementation uses Java Spring Boot for backend and React TypeScript for frontend
- Supabase provides PostgreSQL database, Auth, Storage, and Realtime services
- Stripe handles payment processing and escrow
