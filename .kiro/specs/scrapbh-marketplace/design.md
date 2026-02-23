# Design Document: ScrapBH Marketplace

## Overview

ScrapBH is a web-based marketplace platform for buying and selling scrap auto parts in Bahrain. The system addresses the informal scrap parts market by providing a centralized, searchable platform with real-time chat and secure online payments with Stripe-powered escrow protection. The platform supports dual user roles (Buyer/Seller), post management with car compatibility metadata, and an escrow system that holds funds until buyer approval. Built with Java Spring Boot backend, React frontend, and Supabase PostgreSQL database with Supabase Auth integration, the system must handle search queries within 2 seconds and support responsive web access across major browsers.

## Architecture

```mermaid
graph TD
    A[React Frontend] --> B[API Gateway/Load Balancer]
    B --> C[Java Spring Boot Backend]
    C --> D[Supabase PostgreSQL]
    C --> E[Supabase Realtime]
    C --> F[Stripe Payment Gateway]
    C --> G[Supabase Storage]
    C --> H[Supabase Auth]
    E --> A
    I[WebSocket Connection] --> E
    A --> I
```

### Architecture Layers

1. **Presentation Layer**: React SPA with responsive design
2. **API Layer**: RESTful endpoints + WebSocket for real-time chat
3. **Business Logic Layer**: Java Spring Boot services
4. **Data Layer**: Supabase PostgreSQL with Realtime subscriptions
5. **Storage Layer**: Supabase Storage for post images
6. **Payment Layer**: Stripe payment gateway with escrow support
7. **Authentication Layer**: Supabase Auth for user management


## Main Workflows

### User Registration and Authentication Flow

```mermaid
sequenceDiagram
    participant U as User
    participant F as React Frontend
    participant A as Auth Service
    participant SA as Supabase Auth
    participant D as Supabase DB
    
    U->>F: Register (username, password, full_name, role)
    F->>A: POST /api/auth/register
    A->>SA: Create auth user
    SA-->>A: Auth user created (UUID)
    A->>D: Insert public.users record
    D-->>A: User profile created
    A->>SA: Get session token
    SA-->>A: JWT token
    A-->>F: Return token + user profile
    F->>F: Store token in localStorage
    F-->>U: Redirect to dashboard
```

### Post Creation Flow

```mermaid
sequenceDiagram
    participant S as Seller
    participant F as React Frontend
    participant P as Post Service
    participant ST as Supabase Storage
    participant D as Supabase DB
    
    S->>F: Create post form
    F->>F: Select car make/model/year
    F->>F: Upload images (multiple)
    F->>ST: Upload images
    ST-->>F: Image URLs array
    F->>P: POST /api/posts
    P->>P: Validate required fields
    P->>D: Insert post record with images array
    D-->>P: Post created
    P-->>F: Return post ID
    F-->>S: Show success message
```

### Negotiation and Escrow Flow

```mermaid
sequenceDiagram
    participant B as Buyer
    participant F as React Frontend
    participant C as Conversation Service
    participant E as Escrow Service
    participant S as Stripe
    participant D as Supabase DB
    participant Seller as Seller
    
    B->>F: Contact seller about post
    F->>C: Create/get conversation
    C->>D: Create conversation record
    B->>F: Agree on price via chat
    B->>F: Initiate escrow payment
    F->>E: POST /api/escrow/initiate
    E->>S: Create payment intent
    S-->>E: Payment intent ID
    E->>D: Create escrow_transaction (on_hold)
    D-->>E: Transaction created
    E-->>F: Return payment intent
    F->>S: Complete payment
    S->>E: Webhook: Payment successful
    E->>D: Update escrow status to on_hold
    E->>Seller: Notify funds held in escrow
```


### Escrow Release Flow

```mermaid
sequenceDiagram
    participant B as Buyer
    participant F as React Frontend
    participant E as Escrow Service
    participant S as Stripe
    participant D as Supabase DB
    participant Seller as Seller
    
    Note over B,Seller: Buyer receives part
    B->>F: Approve transaction
    F->>E: POST /api/escrow/{id}/approve
    E->>D: Update buyer_approved_at timestamp
    E->>D: Update status to released
    E->>S: Transfer funds to seller
    S-->>E: Transfer confirmed
    E->>D: Update completed_at timestamp
    E->>Seller: Notify payment released
    E-->>F: Transaction completed
    
    Note over B,Seller: Alternative: Dispute
    B->>F: Raise dispute
    F->>E: POST /api/escrow/{id}/dispute
    E->>D: Update status to disputed
    E->>D: Store dispute_reason
    E->>Seller: Notify of dispute
    E-->>F: Dispute initiated
```

### Real-Time Chat Flow

```mermaid
sequenceDiagram
    participant B as Buyer
    participant F as React Frontend
    participant WS as WebSocket Handler
    participant R as Supabase Realtime
    participant D as Supabase DB
    participant S as Seller
    
    B->>F: Open chat with seller
    F->>WS: Connect WebSocket
    WS->>R: Subscribe to conversation channel
    R-->>WS: Connected
    B->>F: Send message (text or image)
    F->>WS: Emit message event
    WS->>D: Insert message record (message_type: text/image)
    D->>R: Broadcast to channel
    R->>S: Deliver message
    S->>F: Display message
    S->>F: Send reply
    F->>WS: Emit reply event
    WS->>D: Insert message record
    D->>R: Broadcast to channel
    R->>B: Deliver reply
```


## Components and Interfaces

### Backend Components (Java Spring Boot)

#### Component 1: AuthenticationService

**Purpose**: Handles user registration, login, and integration with Supabase Auth

**Interface**:
```java
public interface AuthenticationService {
    AuthResponse register(RegisterRequest request) throws UserAlreadyExistsException;
    AuthResponse login(LoginRequest request) throws InvalidCredentialsException;
    UserProfile getCurrentUser(String token) throws UnauthorizedException;
    void logout(String token);
    boolean validateToken(String token);
    User syncUserProfile(String authUserId) throws NotFoundException;
}
```

**Responsibilities**:
- Integrate with Supabase Auth for user authentication
- Sync auth.users with public.users table
- Generate and validate JWT tokens from Supabase Auth
- Manage user sessions
- Handle username uniqueness validation

#### Component 2: PostService

**Purpose**: Manages CRUD operations for auto part posts

**Interface**:
```java
public interface PostService {
    Post createPost(CreatePostRequest request, String userId) throws ValidationException;
    Post updatePost(UUID postId, UpdatePostRequest request, String userId) throws UnauthorizedException;
    void deletePost(UUID postId, String userId) throws UnauthorizedException;
    Post getPostById(UUID postId) throws NotFoundException;
    Page<Post> searchPosts(SearchCriteria criteria, Pageable pageable);
    List<Post> getSellerPosts(String userId);
    void updatePostStatus(UUID postId, PostStatus newStatus) throws ValidationException;
}
```

**Responsibilities**:
- Validate post data (car make/model/year, part_name required)
- Manage post lifecycle (active, sold, archived via status enum)
- Handle image uploads (stored as text array in images column)
- Support post_type enum for different listing types
- Enforce business rules

#### Component 7: NotificationService

**Purpose**: Manages user notifications

**Interface**:
```java
public interface NotificationService {
    void createNotification(String userId, String type, String title, String body, JsonObject data);
    List<Notification> getUserNotifications(String userId, boolean unreadOnly);
    void markAsRead(UUID notificationId, String userId);
    void markAllAsRead(String userId);
}
```

**Responsibilities**:
- Create notifications with type, title, body, and JSON data
- Track read/unread status
- Retrieve user notifications
- Support various notification types


#### Component 3: ConversationService

**Purpose**: Manages real-time messaging between buyers and sellers

**Interface**:
```java
public interface ConversationService {
    Conversation createOrGetConversation(UUID postId, String buyerId, String sellerId);
    Message sendMessage(SendMessageRequest request, String senderId) throws UnauthorizedException;
    List<Message> getConversationHistory(UUID conversationId, String userId, Pageable pageable);
    List<Conversation> getUserConversations(String userId);
    void markMessagesAsRead(UUID conversationId, String userId);
    void updateLastMessageTimestamp(UUID conversationId);
}
```

**Responsibilities**:
- Create conversations for buyer-seller pairs per post
- Store message history with message_type support (text/image)
- Integrate with Supabase Realtime for live updates
- Track last_message_at timestamp
- Support both text messages (body field) and image messages (image_url field)

#### Component 4: EscrowService

**Purpose**: Handles Stripe payment processing and escrow management

**Interface**:
```java
public interface EscrowService {
    EscrowTransaction initiateEscrow(UUID postId, String buyerId, String sellerId, BigDecimal amount) throws ValidationException;
    void handleStripeWebhook(StripeWebhookEvent event);
    void approveBuyerRelease(UUID transactionId, String buyerId) throws UnauthorizedException;
    void raiseDispute(UUID transactionId, String buyerId, String reason) throws UnauthorizedException;
    EscrowTransaction getTransactionById(UUID transactionId);
    List<EscrowTransaction> getUserTransactions(String userId);
    void releaseToSeller(UUID transactionId) throws InvalidStateException;
}
```

**Responsibilities**:
- Create Stripe payment intents
- Handle Stripe webhooks
- Manage escrow status (on_hold, released, disputed, completed)
- Track buyer_approved_at timestamp
- Store dispute_reason for disputed transactions
- Release funds to seller after buyer approval
- Handle completed_at timestamp

#### Component 5: SearchService

**Purpose**: Provides fast search and filtering capabilities

**Interface**:
```java
public interface SearchService {
    SearchResult search(SearchQuery query);
    List<Post> filterByCarCompatibility(String make, String model, Integer year);
    List<Post> getRecentPosts(int limit);
    List<String> getCarMakes();
    List<String> getCarModels(String make);
    List<Integer> getCarYears();
}
```

**Responsibilities**:
- Execute keyword searches across post titles and content
- Apply filters (car make/model/year, price range, post_type)
- Sort results (newest, price low-to-high, price high-to-low)
- Return results within 2 seconds (performance requirement)
- Provide car data for Bahrain market


#### Component 6: BookmarkService

**Purpose**: Manages buyer bookmarks for tracking posts

**Interface**:
```java
public interface BookmarkService {
    void addToBookmarks(String buyerId, UUID postId) throws NotFoundException;
    void removeFromBookmarks(String buyerId, UUID postId);
    List<Post> getBookmarks(String buyerId);
    boolean isBookmarked(String buyerId, UUID postId);
}
```

**Responsibilities**:
- Add/remove posts from buyer bookmarks
- Retrieve bookmarked items
- Notify buyers of bookmarked post updates

### Frontend Components (React)

#### Component 1: AuthContext

**Purpose**: Global authentication state management

**Interface**:
```typescript
interface AuthContextType {
  user: UserProfile | null;
  token: string | null;
  login: (email: string, password: string) => Promise<void>;
  register: (data: RegisterData) => Promise<void>;
  logout: () => void;
  isAuthenticated: boolean;
  isSeller: boolean;
  isBuyer: boolean;
}
```

#### Component 2: PostForm

**Purpose**: Create and edit post forms

**Interface**:
```typescript
interface PostFormProps {
  initialData?: Post;
  onSubmit: (data: PostFormData) => Promise<void>;
  onCancel: () => void;
}

interface PostFormData {
  title: string;
  content: string;
  carMake: string;
  carModel: string;
  carYear: number;
  partName: string;
  postType: 'sale' | 'wanted';
  price?: number;
  images: File[];
}
```

#### Component 3: SearchBar

**Purpose**: Search and filter interface

**Interface**:
```typescript
interface SearchBarProps {
  onSearch: (query: SearchQuery) => void;
  initialQuery?: SearchQuery;
}

interface SearchQuery {
  keyword?: string;
  carMake?: string;
  carModel?: string;
  carYear?: number;
  minPrice?: number;
  maxPrice?: number;
  postType?: 'sale' | 'wanted';
  sortBy?: 'NEWEST' | 'PRICE_LOW' | 'PRICE_HIGH';
}
```


#### Component 4: EscrowPanel

**Purpose**: Display and manage escrow transactions

**Interface**:
```typescript
interface EscrowPanelProps {
  post: Post;
  transaction: EscrowTransaction;
  currentUser: UserProfile;
  onApproveRelease: () => Promise<void>;
  onRaiseDispute: (reason: string) => Promise<void>;
}
```

#### Component 5: ConversationWindow

**Purpose**: Real-time chat interface

**Interface**:
```typescript
interface ConversationWindowProps {
  conversationId: string;
  currentUserId: string;
  onSendMessage: (content: string, messageType: 'text' | 'image') => void;
}

interface ChatMessage {
  id: string;
  senderId: string;
  senderName: string;
  messageType: 'text' | 'image';
  body?: string;
  imageUrl?: string;
  timestamp: Date;
  isRead: boolean;
}
```

#### Component 6: StripePaymentModal

**Purpose**: Stripe payment processing interface

**Interface**:
```typescript
interface StripePaymentModalProps {
  post: Post;
  amount: number;
  onPaymentComplete: () => void;
  onCancel: () => void;
}
```

## Data Models

### User

```java
@Entity
@Table(name = "users", schema = "public")
public class User {
    @Id
    private UUID id; // References auth.users(id)
    
    @Column(nullable = false)
    private String fullName;
    
    @Column(unique = true, nullable = false)
    private String username;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role; // USER-DEFINED enum
    
    private String avatarUrl;
    
    @Column(nullable = false)
    private LocalDateTime createdAt;
}
```

**Validation Rules**:
- ID must match Supabase Auth user ID (foreign key to auth.users)
- Username must be unique
- Full name is required
- Role must be defined in database enum type
- Avatar URL is optional
- Created timestamp is auto-generated


### Post

```java
@Entity
@Table(name = "posts")
public class Post {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(nullable = false)
    private UUID userId;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PostType postType; // USER-DEFINED enum
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PostStatus status; // USER-DEFINED enum, default 'active'
    
    @Column(nullable = false)
    private String title;
    
    @Column(columnDefinition = "TEXT")
    private String content;
    
    @Column(columnDefinition = "text[]")
    private String[] images; // Array of image URLs
    
    private String carMake;
    
    private String carModel;
    
    private Integer carYear;
    
    private String partName;
    
    @Column(precision = 10, scale = 2)
    private BigDecimal price;
    
    @Column(nullable = false)
    private LocalDateTime createdAt;
}
```

**Validation Rules**:
- Title is required
- User ID must reference valid user
- Post type is required (enum)
- Status defaults to 'active'
- Images stored as PostgreSQL text array (default empty array)
- Car make, model, year are optional
- Part name is optional
- Price is optional (numeric type)
- Content is optional text field


### Conversation

```java
@Entity
@Table(name = "conversations")
public class Conversation {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    private UUID postId;
    
    @Column(nullable = false)
    private UUID buyerId;
    
    @Column(nullable = false)
    private UUID sellerId;
    
    private LocalDateTime lastMessageAt;
    
    @Column(nullable = false)
    private LocalDateTime createdAt;
}
```

**Validation Rules**:
- Buyer and seller must exist
- Post ID is optional (nullable)
- Last message timestamp updated on each message
- Created timestamp is auto-generated


### Message

```java
@Entity
@Table(name = "messages")
public class Message {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(nullable = false)
    private UUID conversationId;
    
    @Column(nullable = false)
    private UUID senderId;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MessageType messageType; // USER-DEFINED enum, default 'text'
    
    @Column(columnDefinition = "TEXT")
    private String body;
    
    private String imageUrl;
    
    @Column(nullable = false)
    private boolean isRead = false;
    
    @Column(nullable = false)
    private LocalDateTime createdAt;
}
```

**Validation Rules**:
- Conversation ID must reference valid conversation
- Sender must be either buyer or seller in the conversation
- Message type defaults to 'text'
- Body is optional (for text messages)
- Image URL is optional (for image messages)
- Is read defaults to false
- Cannot edit or delete messages after sending

### EscrowTransaction

```java
@Entity
@Table(name = "escrow_transactions")
public class EscrowTransaction {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(nullable = false)
    private UUID postId;
    
    @Column(nullable = false)
    private UUID buyerId;
    
    @Column(nullable = false)
    private UUID sellerId;
    
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EscrowStatus status; // USER-DEFINED enum, default 'on_hold'
    
    private String stripePaymentIntentId;
    
    private LocalDateTime buyerApprovedAt;
    
    @Column(columnDefinition = "TEXT")
    private String disputeReason;
    
    private LocalDateTime completedAt;
    
    @Column(nullable = false)
    private LocalDateTime createdAt;
}
```

**Validation Rules**:
- Post, buyer, and seller must reference valid records
- Amount is required
- Status defaults to 'on_hold'
- Stripe payment intent ID stored for payment tracking
- Buyer approved timestamp set when buyer approves release
- Dispute reason stored if status becomes 'disputed'
- Completed timestamp set when funds released to seller


### Bookmark

```java
@Entity
@Table(name = "bookmarks")
public class Bookmark {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(nullable = false)
    private UUID userId;
    
    @Column(nullable = false)
    private UUID postId;
    
    @Column(nullable = false)
    private LocalDateTime createdAt;
}
```

**Validation Rules**:
- Unique constraint on (userId, postId)
- User cannot bookmark own posts
- Post must exist

### Notification

```java
@Entity
@Table(name = "notifications")
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(nullable = false)
    private UUID userId;
    
    @Column(nullable = false)
    private String type;
    
    @Column(nullable = false)
    private String title;
    
    @Column(columnDefinition = "TEXT")
    private String body;
    
    @Column(columnDefinition = "jsonb")
    private JsonObject data; // JSONB type, default '{}'
    
    @Column(nullable = false)
    private boolean isRead = false;
    
    @Column(nullable = false)
    private LocalDateTime createdAt;
}
```

**Validation Rules**:
- User ID must reference valid user
- Type, title are required
- Body is optional
- Data stored as JSONB for flexible notification metadata
- Is read defaults to false

## Algorithmic Pseudocode

### Algorithm 1: Search Posts with Filters

```java
public SearchResult searchPosts(SearchQuery query) {
    // INPUT: query containing keyword, filters, sort, pagination
    // OUTPUT: SearchResult with matching posts and metadata
    
    // PRECONDITIONS:
    // - query is not null
    // - pagination parameters are valid (page >= 0, size > 0, size <= 100)
    
    CriteriaBuilder cb = entityManager.getCriteriaBuilder();
    CriteriaQuery<Post> cq = cb.createQuery(Post.class);
    Root<Post> post = cq.from(Post.class);
    List<Predicate> predicates = new ArrayList<>();
    
    // Filter by status (only active posts)
    predicates.add(cb.equal(post.get("status"), PostStatus.ACTIVE));
    
    // Keyword search (title and content)
    if (query.getKeyword() != null && !query.getKeyword().isEmpty()) {
        String keyword = "%" + query.getKeyword().toLowerCase() + "%";
        Predicate titleMatch = cb.like(cb.lower(post.get("title")), keyword);
        Predicate contentMatch = cb.like(cb.lower(post.get("content")), keyword);
        predicates.add(cb.or(titleMatch, contentMatch));
    }
    
    // Filter by car make
    if (query.getCarMake() != null) {
        predicates.add(cb.equal(post.get("carMake"), query.getCarMake()));
    }
    
    // Filter by car model
    if (query.getCarModel() != null) {
        predicates.add(cb.equal(post.get("carModel"), query.getCarModel()));
    }
    
    // Filter by car year
    if (query.getCarYear() != null) {
        predicates.add(cb.equal(post.get("carYear"), query.getCarYear()));
    }
    
    // Filter by price range
    if (query.getMinPrice() != null) {
        predicates.add(cb.greaterThanOrEqualTo(post.get("price"), query.getMinPrice()));
    }
    if (query.getMaxPrice() != null) {
        predicates.add(cb.lessThanOrEqualTo(post.get("price"), query.getMaxPrice()));
    }
    
    // Filter by post type
    if (query.getPostType() != null) {
        predicates.add(cb.equal(post.get("postType"), query.getPostType()));
    }
    
    // Apply all predicates
    cq.where(predicates.toArray(new Predicate[0]));
    
    // Apply sorting
    if (query.getSortBy() == SortBy.NEWEST) {
        cq.orderBy(cb.desc(post.get("createdAt")));
    } else if (query.getSortBy() == SortBy.PRICE_LOW) {
        cq.orderBy(cb.asc(post.get("price")));
    } else if (query.getSortBy() == SortBy.PRICE_HIGH) {
        cq.orderBy(cb.desc(post.get("price")));
    }
    
    // Execute query with pagination
    TypedQuery<Post> typedQuery = entityManager.createQuery(cq);
    typedQuery.setFirstResult(query.getPage() * query.getSize());
    typedQuery.setMaxResults(query.getSize());
    
    List<Post> results = typedQuery.getResultList();
    long totalCount = countSearchResults(predicates);
    
    // POSTCONDITIONS:
    // - Results contain only active posts
    // - Results match all specified filters
    // - Results are sorted according to sortBy parameter
    // - Query execution time < 2 seconds
    
    return new SearchResult(results, totalCount, query.getPage(), query.getSize());
}
```

**Preconditions**:
- query object is not null
- Pagination parameters are valid (page >= 0, size > 0, size <= 100)
- Database connection is available

**Postconditions**:
- Returns SearchResult with posts matching all filters
- Only active posts are included
- Results are sorted according to sortBy parameter
- Total count reflects total matching records (not just current page)
- Query execution completes within 2 seconds (performance requirement)

**Loop Invariants**: N/A (no explicit loops, using JPA Criteria API)


### Algorithm 2: Initiate Escrow Transaction

```java
@Transactional
public EscrowTransaction initiateEscrow(UUID postId, String buyerId, String sellerId, BigDecimal amount) 
    throws ValidationException {
    // INPUT: postId, buyerId, sellerId, agreed amount
    // OUTPUT: Created EscrowTransaction with Stripe payment intent
    
    // PRECONDITIONS:
    // - postId exists in database
    // - buyerId and sellerId are valid users
    // - amount is positive
    // - Post status is active
    
    // Step 1: Fetch and validate post
    Post post = postRepository.findById(postId)
        .orElseThrow(() -> new NotFoundException("Post not found"));
    
    // Step 2: Verify post is available
    if (post.getStatus() != PostStatus.ACTIVE) {
        throw new InvalidStateException("Post is not available");
    }
    
    // Step 3: Verify buyer and seller exist
    User buyer = userRepository.findById(UUID.fromString(buyerId))
        .orElseThrow(() -> new NotFoundException("Buyer not found"));
    User seller = userRepository.findById(UUID.fromString(sellerId))
        .orElseThrow(() -> new NotFoundException("Seller not found"));
    
    // Step 4: Create Stripe payment intent
    PaymentIntent paymentIntent = stripeService.createPaymentIntent(
        amount,
        "usd", // or appropriate currency
        Map.of(
            "postId", postId.toString(),
            "buyerId", buyerId,
            "sellerId", sellerId
        )
    );
    
    // Step 5: Create escrow transaction record
    EscrowTransaction transaction = new EscrowTransaction();
    transaction.setPostId(postId);
    transaction.setBuyerId(UUID.fromString(buyerId));
    transaction.setSellerId(UUID.fromString(sellerId));
    transaction.setAmount(amount);
    transaction.setStatus(EscrowStatus.ON_HOLD);
    transaction.setStripePaymentIntentId(paymentIntent.getId());
    transaction.setCreatedAt(LocalDateTime.now());
    
    escrowRepository.save(transaction);
    
    // Step 6: Notify seller that funds are in escrow
    notificationService.createNotification(
        sellerId,
        "ESCROW_CREATED",
        "Payment Received",
        "Buyer has placed funds in escrow for your part",
        JsonObject.of("transactionId", transaction.getId().toString())
    );
    
    // POSTCONDITIONS:
    // - EscrowTransaction created with status ON_HOLD
    // - Stripe payment intent created and ID stored
    // - Seller receives notification
    // - Transaction is atomic
    
    return transaction;
}
```

**Preconditions**:
- postId exists in database
- buyerId and sellerId are valid users
- amount is positive
- Post status is active

**Postconditions**:
- EscrowTransaction created with status ON_HOLD
- Stripe payment intent created and stored
- Seller receives notification
- All database changes are atomic

**Loop Invariants**: N/A (no loops)


### Algorithm 3: Process Stripe Webhook and Update Escrow

```java
@Transactional
public void handleStripeWebhook(StripeWebhookEvent event) {
    // INPUT: StripeWebhookEvent from Stripe
    // OUTPUT: Updated escrow transaction record
    
    // PRECONDITIONS:
    // - event is valid and authenticated from Stripe
    // - event.paymentIntentId exists in database
    
    // Step 1: Verify webhook signature
    if (!stripeService.verifyWebhookSignature(event)) {
        throw new SecurityException("Invalid webhook signature");
    }
    
    // Step 2: Fetch escrow transaction
    EscrowTransaction transaction = escrowRepository
        .findByStripePaymentIntentId(event.getPaymentIntentId())
        .orElseThrow(() -> new NotFoundException("Transaction not found"));
    
    // Step 3: Check for duplicate processing
    if (transaction.getStatus() != EscrowStatus.ON_HOLD) {
        logger.warn("Transaction already processed: {}", transaction.getId());
        return; // Idempotent handling
    }
    
    // Step 4: Process based on payment status
    if (event.getStatus() == StripePaymentStatus.SUCCEEDED) {
        // Payment successful - funds held in escrow
        transaction.setStatus(EscrowStatus.ON_HOLD);
        escrowRepository.save(transaction);
        
        // Notify both parties
        notificationService.createNotification(
            transaction.getBuyerId().toString(),
            "PAYMENT_SUCCESS",
            "Payment Successful",
            "Your payment is held in escrow until you approve release",
            JsonObject.of("transactionId", transaction.getId().toString())
        );
        
        notificationService.createNotification(
            transaction.getSellerId().toString(),
            "ESCROW_FUNDED",
            "Escrow Funded",
            "Buyer's payment is held in escrow. Ship the part.",
            JsonObject.of("transactionId", transaction.getId().toString())
        );
        
    } else if (event.getStatus() == StripePaymentStatus.FAILED) {
        // Payment failed
        transaction.setStatus(EscrowStatus.ON_HOLD); // Keep as on_hold or create failed status
        escrowRepository.save(transaction);
        
        // Notify buyer of failure
        notificationService.createNotification(
            transaction.getBuyerId().toString(),
            "PAYMENT_FAILED",
            "Payment Failed",
            event.getFailureReason(),
            JsonObject.of("transactionId", transaction.getId().toString())
        );
    }
    
    // POSTCONDITIONS (SUCCESS case):
    // - Transaction status remains ON_HOLD
    // - Buyer and seller receive notifications
    // - Funds are held by Stripe
    
    // POSTCONDITIONS (FAILURE case):
    // - Buyer receives failure notification
    // - Buyer can retry payment
}
```

**Preconditions**:
- Webhook event is authenticated and valid
- Payment intent ID exists in database
- Transaction status is appropriate for update

**Postconditions (Success)**:
- Transaction status is ON_HOLD
- Buyer receives payment confirmation
- Seller receives escrow funded notification
- Funds held by Stripe

**Postconditions (Failure)**:
- Buyer receives failure notification
- Buyer can retry payment

**Loop Invariants**: N/A (no loops)
### Algorithm 4: Approve Buyer Release and Complete Escrow

```java
@Transactional
public void approveBuyerRelease(UUID transactionId, String buyerId) 
    throws UnauthorizedException, InvalidStateException {
    // INPUT: transactionId, buyerId approving release
    // OUTPUT: Updated escrow transaction with funds released to seller
    
    // PRECONDITIONS:
    // - transactionId exists in database
    // - buyerId matches the transaction buyer
    // - Transaction status is ON_HOLD
    
    // Step 1: Fetch and validate transaction
    EscrowTransaction transaction = escrowRepository.findById(transactionId)
        .orElseThrow(() -> new NotFoundException("Transaction not found"));
    
    // Step 2: Verify buyer authorization
    if (!transaction.getBuyerId().toString().equals(buyerId)) {
        throw new UnauthorizedException("Only buyer can approve release");
    }
    
    // Step 3: Validate transaction status
    if (transaction.getStatus() != EscrowStatus.ON_HOLD) {
        throw new InvalidStateException("Transaction is not in escrow");
    }
    
    // Step 4: Update buyer approval timestamp
    transaction.setBuyerApprovedAt(LocalDateTime.now());
    transaction.setStatus(EscrowStatus.RELEASED);
    escrowRepository.save(transaction);
    
    // Step 5: Transfer funds to seller via Stripe
    stripeService.transferToSeller(
        transaction.getStripePaymentIntentId(),
        transaction.getSellerId().toString(),
        transaction.getAmount()
    );
    
    // Step 6: Mark transaction as completed
    transaction.setCompletedAt(LocalDateTime.now());
    escrowRepository.save(transaction);
    
    // Step 7: Update post status to sold
    Post post = postRepository.findById(transaction.getPostId())
        .orElseThrow(() -> new NotFoundException("Post not found"));
    post.setStatus(PostStatus.SOLD);
    postRepository.save(post);
    
    // Step 8: Notify seller
    notificationService.createNotification(
        transaction.getSellerId().toString(),
        "FUNDS_RELEASED",
        "Payment Released",
        "Buyer approved release. Funds transferred to your account.",
        JsonObject.of("transactionId", transactionId.toString())
    );
    
    // POSTCONDITIONS:
    // - Transaction status is RELEASED then COMPLETED
    // - buyer_approved_at timestamp is set
    // - completed_at timestamp is set
    // - Funds transferred to seller
    // - Post status is SOLD
    // - Seller receives notification
    
}
```

**Preconditions**:
- transactionId exists in database
- buyerId matches the transaction buyer
- Transaction status is ON_HOLD
- Stripe payment intent is valid

**Postconditions**:
- Transaction status is RELEASED then COMPLETED
- buyer_approved_at timestamp is set
- completed_at timestamp is set
- Funds transferred to seller via Stripe
- Post status updated to SOLD
- Seller receives notification
- All database changes are atomic

**Loop Invariants**: N/A (no loops)


### Algorithm 5: Raise Dispute on Escrow Transaction

```java
@Transactional
public void raiseDispute(UUID transactionId, String buyerId, String reason) 
    throws UnauthorizedException, InvalidStateException {
    // INPUT: transactionId, buyerId raising dispute, dispute reason
    // OUTPUT: Updated escrow transaction with disputed status
    
    // PRECONDITIONS:
    // - transactionId exists in database
    // - buyerId matches the transaction buyer
    // - Transaction status is ON_HOLD
    // - reason is not empty
    
    // Step 1: Fetch and validate transaction
    EscrowTransaction transaction = escrowRepository.findById(transactionId)
        .orElseThrow(() -> new NotFoundException("Transaction not found"));
    
    // Step 2: Verify buyer authorization
    if (!transaction.getBuyerId().toString().equals(buyerId)) {
        throw new UnauthorizedException("Only buyer can raise dispute");
    }
    
    // Step 3: Validate transaction status
    if (transaction.getStatus() != EscrowStatus.ON_HOLD) {
        throw new InvalidStateException("Can only dispute transactions in escrow");
    }
    
    // Step 4: Update transaction with dispute
    transaction.setStatus(EscrowStatus.DISPUTED);
    transaction.setDisputeReason(reason);
    escrowRepository.save(transaction);
    
    // Step 5: Notify seller of dispute
    notificationService.createNotification(
        transaction.getSellerId().toString(),
        "DISPUTE_RAISED",
        "Dispute Raised",
        "Buyer has raised a dispute on the transaction",
        JsonObject.of(
            "transactionId", transactionId.toString(),
            "reason", reason
        )
    );
    
    // Step 6: Notify admin/support (future enhancement)
    // adminNotificationService.notifyDisputeRaised(transaction, reason);
    
    // POSTCONDITIONS:
    // - Transaction status is DISPUTED
    // - dispute_reason is stored
    // - Seller receives notification
    // - Funds remain held in escrow pending resolution
}
```

**Preconditions**:
- transactionId exists in database
- buyerId matches the transaction buyer
- Transaction status is ON_HOLD
- Dispute reason is provided

**Postconditions**:
- Transaction status is DISPUTED
- dispute_reason field is populated
- Seller receives dispute notification
- Funds remain held in Stripe escrow
- Admin/support can review dispute (future)

**Loop Invariants**: N/A (no loops)

## Key Functions with Formal Specifications

### Function 1: validatePostData()

```java
public void validatePostData(CreatePostRequest request) throws ValidationException
```

**Preconditions**:
- request is not null
- request contains all required fields

**Postconditions**:
- If validation passes: no exception thrown, method returns normally
- If validation fails: ValidationException thrown with descriptive message
- No side effects on request object

**Validation Logic**:
- Title: non-empty, reasonable length
- Car make, model, year: optional but if provided must be valid
- Part name: optional
- Images: array of URLs (can be empty)
- Post type: must be valid enum value
- Price: optional, if provided must be positive


### Function 2: validateEscrowAmount()

```java
public void validateEscrowAmount(UUID postId, BigDecimal amount, String buyerId) 
    throws ValidationException
```

**Preconditions**:
- postId exists in database
- amount is not null
- buyerId is not null

**Postconditions**:
- If validation passes: no exception thrown
- If validation fails: ValidationException thrown with specific reason
- No database modifications

**Validation Logic**:
- Post must exist and be active
- Buyer cannot create escrow for own posts
- Amount must be positive
- Amount must have at most 2 decimal places

### Function 3: canRaiseDispute()

```java
public boolean canRaiseDispute(UUID transactionId, String buyerId) throws UnauthorizedException
```

**Preconditions**:
- transactionId exists in database
- buyerId is not null

**Postconditions**:
- Returns true if buyer can raise dispute
- Returns false if dispute cannot be raised
- Throws UnauthorizedException if buyerId doesn't match transaction buyer
- No database modifications

**Business Rules**:
- Buyer must own the transaction
- Transaction status must be ON_HOLD (cannot dispute released or completed transactions)
- Returns true only if both conditions are met

### Function 4: syncSupabaseAuthUser()

```java
public User syncSupabaseAuthUser(String authUserId, String username, String fullName, UserRole role)
```

**Preconditions**:
- authUserId is valid Supabase Auth user ID
- username is not null and unique
- fullName is not null
- role is valid enum value

**Postconditions**:
- Returns User record from public.users table
- User ID matches auth.users ID
- Username is unique in public.users
- No side effects if user already exists

**Sync Process**:
- Check if user exists in public.users with given auth ID
- If not exists, create new record
- If exists, return existing record
- Ensure foreign key constraint to auth.users is satisfied


### Function 5: uploadPostImages()

```java
public String[] uploadPostImages(List<MultipartFile> images) throws ValidationException
```

**Preconditions**:
- images list is not null
- Each image file size <= 5MB
- Each image is valid image format (JPEG, PNG, WebP)

**Postconditions**:
- Returns array of public URLs for uploaded images
- Images are stored in Supabase Storage
- Image URLs are accessible via HTTPS
- If any upload fails: throws ValidationException and cleans up partial uploads
- Original files are not modified

**Upload Process**:
1. Validate each image size and format
2. Generate unique filename for each image
3. Upload to Supabase Storage bucket
4. Retrieve public URL for each uploaded image
5. Return array of URLs

## Example Usage

### Example 1: User Registration with Supabase Auth

```java
// Registration via Supabase Auth
RegisterRequest registerReq = new RegisterRequest();
registerReq.setUsername("ahmed_bh");
registerReq.setPassword("SecurePass123");
registerReq.setFullName("Ahmed Al-Khalifa");
registerReq.setRole(UserRole.BUYER);

// Create auth user in Supabase Auth
AuthResponse authResponse = authService.register(registerReq);
String token = authResponse.getToken();
UUID authUserId = authResponse.getUserId();

// Sync to public.users table
User user = authService.syncSupabaseAuthUser(
    authUserId.toString(),
    registerReq.getUsername(),
    registerReq.getFullName(),
    registerReq.getRole()
);

// Login
LoginRequest loginReq = new LoginRequest();
loginReq.setUsername("ahmed_bh");
loginReq.setPassword("SecurePass123");

AuthResponse loginResponse = authService.login(loginReq);
String loginToken = loginResponse.getToken();
```

### Example 2: Create Post with Images

```java
// Upload images
List<MultipartFile> images = Arrays.asList(image1, image2, image3);
String[] imageUrls = postService.uploadPostImages(images);

// Create post
CreatePostRequest request = new CreatePostRequest();
request.setTitle("Toyota Camry 2015 Front Bumper - Excellent Condition");
request.setContent("Original front bumper from Toyota Camry 2015. No cracks or damage.");
request.setCarMake("Toyota");
request.setCarModel("Camry");
request.setCarYear(2015);
request.setPartName("Front Bumper");
request.setPostType(PostType.SALE);
request.setPrice(new BigDecimal("50.00"));
request.setImages(imageUrls);

Post post = postService.createPost(request, userId);
```

### Example 3: Search and Filter Posts

```java
// Search for Toyota parts
SearchQuery query = new SearchQuery();
query.setKeyword("bumper");
query.setCarMake("Toyota");
query.setCarYear(2015);
query.setSortBy(SortBy.NEWEST);
query.setPage(0);
query.setSize(20);

SearchResult result = searchService.search(query);
List<Post> posts = result.getPosts();
long totalCount = result.getTotalCount();
```


### Example 4: Negotiate and Create Escrow

```java
// Buyer contacts seller via conversation
Conversation conversation = conversationService.createOrGetConversation(
    postId, 
    buyerId, 
    sellerId
);

// Send message
SendMessageRequest msgRequest = new SendMessageRequest();
msgRequest.setConversationId(conversation.getId());
msgRequest.setMessageType(MessageType.TEXT);
msgRequest.setBody("Is this part still available? Can you do 45 BD?");

Message message = conversationService.sendMessage(msgRequest, buyerId);

// After agreeing on price, initiate escrow
EscrowTransaction transaction = escrowService.initiateEscrow(
    postId,
    buyerId,
    sellerId,
    new BigDecimal("45.00")
);

// Get Stripe payment intent client secret
String clientSecret = transaction.getStripePaymentIntentId();

// Frontend completes payment with Stripe
// ... Stripe webhook fires ...

// Escrow is now ON_HOLD
```

### Example 5: Complete Escrow Transaction

```java
// Buyer receives part and approves release
escrowService.approveBuyerRelease(transactionId, buyerId);

// Transaction is now RELEASED and COMPLETED
// Funds transferred to seller
// Post status updated to SOLD

EscrowTransaction completedTransaction = escrowService.getTransactionById(transactionId);
assert completedTransaction.getStatus() == EscrowStatus.COMPLETED;
assert completedTransaction.getBuyerApprovedAt() != null;
assert completedTransaction.getCompletedAt() != null;

// Alternative: Buyer raises dispute
escrowService.raiseDispute(
    transactionId, 
    buyerId, 
    "Part does not match description"
);

// Transaction is now DISPUTED
// Funds remain in escrow pending resolution
```

### Example 6: Real-Time Chat (React Frontend)

```typescript
// Connect to conversation
const conversation = await conversationService.createOrGetConversation(
  postId, 
  buyerId, 
  sellerId
);

// Subscribe to Supabase Realtime
const subscription = supabase
  .channel(`conversation:${conversation.id}`)
  .on('postgres_changes', 
    { 
      event: 'INSERT', 
      schema: 'public', 
      table: 'messages',
      filter: `conversation_id=eq.${conversation.id}`
    },
    (payload) => {
      const newMessage = payload.new as Message;
      setMessages(prev => [...prev, newMessage]);
    }
  )
  .subscribe();

// Send text message
const sendTextMessage = async (content: string) => {
  await conversationService.sendMessage({
    conversationId: conversation.id,
    messageType: 'text',
    body: content
  });
};

// Send image message
const sendImageMessage = async (imageUrl: string) => {
  await conversationService.sendMessage({
    conversationId: conversation.id,
    messageType: 'image',
    imageUrl: imageUrl
  });
};
```

## Correctness Properties

### Property 1: Escrow Transaction Atomicity
**Universal Quantification**: ∀ transaction: When an escrow transaction is created, a Stripe payment intent MUST be created atomically, and both records must reference each other.

**Formal Statement**:
```
∀ transaction ∈ EscrowTransactions:
  transaction.status = ON_HOLD ⟹
    (∃ paymentIntent ∈ StripePaymentIntents:
      paymentIntent.id = transaction.stripePaymentIntentId ∧
      paymentIntent.metadata.transactionId = transaction.id)
```


### Property 2: Buyer Approval Guarantees
**Universal Quantification**: ∀ transaction: A buyer-approved escrow release MUST result in funds transferred to seller, post marked as sold, and completion timestamp set.

**Formal Statement**:
```
∀ transaction ∈ EscrowTransactions:
  (transaction.buyerApprovedAt ≠ null ∧ transaction.status = RELEASED) ⟹
    (transaction.completedAt ≠ null ∧
     ∃ post ∈ Posts: post.id = transaction.postId ∧ post.status = SOLD ∧
     fundsTransferred(transaction.sellerId, transaction.amount))
```

### Property 3: Post Status Invariant
**Universal Quantification**: ∀ post: A post can only transition through valid status sequences, and sold posts cannot be modified.

**Formal Statement**:
```
∀ post ∈ Posts:
  (post.status = ACTIVE ⟹ 
    nextStatus(post) ∈ {SOLD, ARCHIVED}) ∧
  (post.status = SOLD ⟹ 
    nextStatus(post) = SOLD) ∧
  (post.status = ARCHIVED ⟹ 
    nextStatus(post) = ARCHIVED)
```

### Property 4: Escrow Status Validity
**Universal Quantification**: ∀ transaction: An escrow transaction must follow valid status transitions and disputed transactions cannot be released without resolution.

**Formal Statement**:
```
∀ transaction ∈ EscrowTransactions:
  (transaction.status = ON_HOLD ⟹
    nextStatus(transaction) ∈ {RELEASED, DISPUTED}) ∧
  (transaction.status = DISPUTED ⟹
    transaction.disputeReason ≠ null ∧
    nextStatus(transaction) ∈ {ON_HOLD, RELEASED}) ∧
  (transaction.status = RELEASED ⟹
    transaction.buyerApprovedAt ≠ null)
```

### Property 5: Search Performance Guarantee
**Universal Quantification**: ∀ search query: Search results must be returned within 2 seconds regardless of filter complexity.

**Formal Statement**:
```
∀ query ∈ SearchQueries:
  executionTime(searchPosts(query)) < 2000ms
```

### Property 6: Authorization Invariant
**Universal Quantification**: ∀ operation: Only authorized users can perform operations on resources they own or have permission to access.

**Formal Statement**:
```
∀ user ∈ Users, post ∈ Posts:
  (updatePost(post.id, user.id) succeeds) ⟹ 
    (post.userId = user.id) ∧
  (deletePost(post.id, user.id) succeeds) ⟹ 
    (post.userId = user.id) ∧
  (approveBuyerRelease(transaction.id, user.id) succeeds) ⟹ 
    (∃ transaction ∈ EscrowTransactions: 
      transaction.id = transaction.id ∧ transaction.buyerId = user.id)
```

### Property 7: Image Storage Constraints
**Universal Quantification**: ∀ post: A post can have zero or more images stored as a PostgreSQL array.

**Formal Statement**:
```
∀ post ∈ Posts:
  (post.images IS ARRAY) ∧
  ∀ imageUrl ∈ post.images:
    (fileSize(imageUrl) ≤ 5MB ∧
     format(imageUrl) ∈ {JPEG, PNG, WebP})
```


### Property 8: Conversation Uniqueness
**Universal Quantification**: ∀ post, buyer, seller: Only one conversation can exist for a given combination of post, buyer, and seller (though post is optional).

**Formal Statement**:
```
∀ conv1, conv2 ∈ Conversations:
  (conv1.postId = conv2.postId ∧
   conv1.buyerId = conv2.buyerId ∧
   conv1.sellerId = conv2.sellerId) ⟹
  conv1.id = conv2.id
```

### Property 9: Message Type Consistency
**Universal Quantification**: ∀ message: A message must have either body (for text) or imageUrl (for image) based on messageType.

**Formal Statement**:
```
∀ message ∈ Messages:
  (message.messageType = TEXT ⟹ message.body ≠ null ∧ message.imageUrl = null) ∧
  (message.messageType = IMAGE ⟹ message.imageUrl ≠ null ∧ message.body = null)
```

### Property 10: Stripe Webhook Idempotency
**Universal Quantification**: ∀ transaction: Processing the same Stripe webhook multiple times must not create duplicate transactions or inconsistent state.

**Formal Statement**:
```
∀ transaction ∈ EscrowTransactions, event ∈ StripeWebhookEvents:
  (handleStripeWebhook(event) called n times where n ≥ 1) ⟹
    (∃! transaction: transaction.stripePaymentIntentId = event.paymentIntentId ∧
     transaction.status ∈ {ON_HOLD, RELEASED, DISPUTED, COMPLETED})
```

### Property 11: User Identity Consistency
**Universal Quantification**: ∀ user: A user in public.users must have a corresponding auth.users record with matching ID.

**Formal Statement**:
```
∀ user ∈ public.users:
  ∃ authUser ∈ auth.users:
    authUser.id = user.id
```

### Property 12: Bookmark Uniqueness
**Universal Quantification**: ∀ user, post: A user can bookmark a post at most once.

**Formal Statement**:
```
∀ bookmark1, bookmark2 ∈ Bookmarks:
  (bookmark1.userId = bookmark2.userId ∧
   bookmark1.postId = bookmark2.postId) ⟹
  bookmark1.id = bookmark2.id
```

## Error Handling

### Error Scenario 1: Invalid Credentials During Login

**Condition**: User provides incorrect email or password
**Response**: Return 401 Unauthorized with message "Invalid email or password"
**Recovery**: User can retry with correct credentials; account not locked after failed attempts (for MVP)

### Error Scenario 2: Duplicate Email Registration

**Condition**: User attempts to register with an email that already exists
**Response**: Return 409 Conflict with message "Email already registered"
**Recovery**: User can login with existing account or use different email

### Error Scenario 3: Unauthorized Listing Modification

**Condition**: User attempts to edit/delete a listing they don't own
**Response**: Return 403 Forbidden with message "You don't have permission to modify this listing"
**Recovery**: User can only modify their own listings

### Error Scenario 4: Escrow Dispute Raised

**Condition**: Buyer raises dispute on escrow transaction
**Response**: Return 200 OK with dispute recorded
**Recovery**: 
- Transaction status set to DISPUTED
- dispute_reason stored
- Seller notified
- Funds remain in Stripe escrow
- Admin/support can review (future enhancement)


### Error Scenario 5: Stripe Payment Failure

**Condition**: Stripe payment intent fails or times out
**Response**: Return 502 Bad Gateway with message "Payment processing failed. Please try again."
**Recovery**: 
- Escrow transaction remains in initial state
- Buyer can retry payment
- Stripe payment intent can be reused or new one created

### Error Scenario 6: Image Upload Exceeds Limit

**Condition**: User attempts to upload images exceeding 5MB each
**Response**: Return 400 Bad Request with message "Each image must be under 5MB"
**Recovery**: User can compress images before retrying

### Error Scenario 7: Invalid Username

**Condition**: User attempts to register with username that already exists
**Response**: Return 409 Conflict with message "Username already taken"
**Recovery**: User must choose different username

### Error Scenario 8: Unauthorized Escrow Approval

**Condition**: User attempts to approve escrow release for transaction they don't own
**Response**: Return 403 Forbidden with message "Only buyer can approve release"
**Recovery**: Only the buyer associated with the transaction can approve

### Error Scenario 9: Database Connection Failure

**Condition**: Supabase database becomes unavailable
**Response**: Return 503 Service Unavailable with message "Service temporarily unavailable. Please try again later."
**Recovery**: 
- Implement retry logic with exponential backoff
- Display user-friendly error message
- Log error for monitoring

### Error Scenario 10: WebSocket Connection Dropped

**Condition**: Real-time chat connection is lost
**Response**: Display "Connection lost. Reconnecting..." message in chat UI
**Recovery**:
- Automatically attempt to reconnect
- Fetch missed messages on reconnection
- Show success message when reconnected

## Testing Strategy

### Unit Testing Approach

**Framework**: JUnit 5 for Java backend, Jest for React frontend

**Key Test Cases**:

1. **Authentication Tests**
   - Valid registration creates user and returns token
   - Duplicate email registration throws UserAlreadyExistsException
   - Valid login returns token
   - Invalid credentials throw InvalidCredentialsException
   - JWT token validation correctly identifies valid/invalid tokens

2. **Post Service Tests**
   - Create post with valid data succeeds
   - Create post stores images as array
   - Update post by owner succeeds
   - Update post by non-owner throws UnauthorizedException
   - Delete post removes from database
   - Post status transitions work correctly

3. **Escrow Service Tests**
   - Initiate escrow creates Stripe payment intent
   - Approve buyer release transfers funds and marks post sold
   - Raise dispute updates status and stores reason
   - Stripe webhook processing is idempotent
   - Invalid escrow operations throw appropriate exceptions

4. **Conversation Service Tests**
   - Create conversation for buyer-seller pair succeeds
   - Send text message stores body field
   - Send image message stores imageUrl field
   - Mark messages as read updates is_read flag
   - Update last_message_at timestamp on new message

5. **Search Service Tests**
   - Keyword search returns matching posts
   - Filters correctly narrow results
   - Sorting works correctly (newest, price)
   - Search completes within 2 seconds (performance test)
   - Only active posts appear in results

**Coverage Goal**: Minimum 80% code coverage for backend services


### Property-Based Testing Approach

**Property Test Library**: jqwik for Java backend, fast-check for TypeScript frontend

**Key Properties to Test**:

1. **Escrow Status Transition Property**
   ```java
   @Property
   void escrowStatusTransitionsAreValid(
       @ForAll("validEscrowTransitions") StatusTransition transition) {
       
       EscrowTransaction transaction = createTransactionWithStatus(transition.from);
       applyTransition(transaction, transition.to);
       
       assertThat(isValidEscrowTransition(transition.from, transition.to)).isTrue();
   }
   ```

2. **Post Status Transition Property**
   ```java
   @Property
   void postStatusTransitionsAreValid(
       @ForAll("validStatusTransitions") StatusTransition transition) {
       
       Post post = createPostWithStatus(transition.from);
       applyTransition(post, transition.to);
       
       assertThat(isValidTransition(transition.from, transition.to)).isTrue();
   }
   ```

3. **Search Idempotency Property**
   ```java
   @Property
   void searchResultsAreIdempotent(@ForAll SearchQuery query) {
       SearchResult result1 = searchService.search(query);
       SearchResult result2 = searchService.search(query);
       
       assertThat(result1.getPosts()).isEqualTo(result2.getPosts());
       assertThat(result1.getTotalCount()).isEqualTo(result2.getTotalCount());
   }
   ```

4. **Stripe Webhook Idempotency Property**
   ```java
   @Property
   void stripeWebhookIsIdempotent(
       @ForAll StripeWebhookEvent event,
       @ForAll @IntRange(min = 1, max = 5) int callCount) {
       
       EscrowTransaction initialTransaction = createInitiatedTransaction(event);
       
       for (int i = 0; i < callCount; i++) {
           escrowService.handleStripeWebhook(event);
       }
       
       EscrowTransaction finalTransaction = escrowRepository
           .findByStripePaymentIntentId(event.getPaymentIntentId())
           .orElseThrow();
       
       assertThat(finalTransaction.getStatus()).isIn(
           EscrowStatus.ON_HOLD, 
           EscrowStatus.RELEASED,
           EscrowStatus.DISPUTED
       );
       
       // Verify no duplicate transactions created
       long count = escrowRepository
           .countByStripePaymentIntentId(event.getPaymentIntentId());
       assertThat(count).isEqualTo(1);
   }
   ```

5. **Authorization Property**
   ```java
   @Property
   void onlyOwnersCanModifyPosts(
       @ForAll String ownerId,
       @ForAll String otherUserId) {
       
       assume(!ownerId.equals(otherUserId));
       
       Post post = createPostWithOwner(ownerId);
       
       // Owner can modify
       assertDoesNotThrow(() -> 
           postService.updatePost(post.getId(), updateData, ownerId)
       );
       
       // Non-owner cannot modify
       assertThrows(UnauthorizedException.class, () ->
           postService.updatePost(post.getId(), updateData, otherUserId)
       );
   }
   ```

### Integration Testing Approach

**Framework**: Spring Boot Test with Testcontainers for database

**Key Integration Tests**:

1. **End-to-End Post Creation Flow**
   - Upload images to Supabase Storage
   - Create post with image URLs array
   - Verify post appears in search results
   - Verify post is retrievable by ID

2. **Complete Escrow Flow**
   - Create post
   - Initiate escrow transaction
   - Simulate Stripe webhook callback
   - Approve buyer release
   - Verify post marked as SOLD
   - Verify escrow transaction completed

3. **Negotiation and Payment Flow**
   - Create conversation
   - Exchange messages
   - Agree on price
   - Initiate escrow
   - Complete payment via Stripe
   - Verify funds held in escrow

4. **Real-Time Chat Integration**
   - Create conversation
   - Send messages via WebSocket
   - Verify messages stored in database
   - Verify messages broadcast to subscribers
   - Test both text and image message types

5. **Authentication Flow with Supabase Auth**
   - Register new user in Supabase Auth
   - Sync to public.users table
   - Login with credentials
   - Access protected endpoints with token
   - Verify unauthorized access is blocked

**Test Environment**: Use Testcontainers to spin up PostgreSQL container for isolated testing


## Performance Considerations

### Search Performance (Critical Requirement)

**Requirement**: Search results must be returned within 2 seconds

**Optimization Strategies**:

1. **Database Indexing**
   ```sql
   CREATE INDEX idx_listings_status ON listings(status);
   CREATE INDEX idx_listings_car_make ON listings(car_make);
   CREATE INDEX idx_listings_car_model ON listings(car_model);
   CREATE INDEX idx_listings_car_year ON listings(car_year);
   CREATE INDEX idx_listings_created_at ON listings(created_at DESC);
   CREATE INDEX idx_listings_fixed_price ON listings(fixed_price);
   CREATE INDEX idx_listings_search ON listings USING GIN(to_tsvector('english', title || ' ' || description));
   ```

2. **Query Optimization**
   - Use JPA Criteria API for dynamic query building
   - Fetch only required fields (projection)
   - Implement pagination to limit result set size
   - Use database-level full-text search for keyword queries

3. **Caching Strategy**
   - Cache car data (makes, models, years) in application memory
   - Cache recent searches with Redis (if needed in future)
   - Cache homepage listings for 5 minutes

4. **Connection Pooling**
   - Configure HikariCP with optimal pool size
   - Set reasonable connection timeout values

### Photo Upload Performance

**Optimization Strategies**:
- Upload photos in parallel using CompletableFuture
- Implement client-side image compression before upload
- Use Supabase Storage CDN for fast delivery
- Generate thumbnails for listing previews

### Real-Time Chat Performance

**Optimization Strategies**:
- Use Supabase Realtime for efficient WebSocket connections
- Implement message pagination (load 50 messages at a time)
- Lazy load chat history on scroll
- Debounce typing indicators

### Database Connection Management

**Configuration**:
```properties
spring.datasource.hikari.maximum-pool-size=20
spring.datasource.hikari.minimum-idle=5
spring.datasource.hikari.connection-timeout=30000
spring.datasource.hikari.idle-timeout=600000
spring.datasource.hikari.max-lifetime=1800000
```

## Security Considerations

### Authentication and Authorization

**Security Measures**:

1. **Password Security**
   - Hash passwords using BCrypt with cost factor 12
   - Never store plain text passwords
   - Implement password strength requirements (min 8 characters)

2. **JWT Token Security**
   - Sign tokens with HS256 algorithm
   - Store secret key in environment variables
   - Set token expiration to 24 hours
   - Implement token refresh mechanism (future enhancement)
   - Validate token on every protected endpoint

3. **Session Management**
   - Store JWT in httpOnly cookies (preferred) or localStorage
   - Implement logout functionality to clear tokens
   - No server-side session storage (stateless)

### Input Validation

**Validation Strategies**:
- Validate all user inputs on backend (never trust client)
- Use Bean Validation annotations (@NotNull, @Size, @Email, etc.)
- Sanitize user-generated content to prevent XSS
- Implement rate limiting on API endpoints (future enhancement)

### Payment Security

**Security Measures**:
- Never store credit card information
- Use PCI-compliant payment gateway
- Validate webhook signatures to prevent spoofing
- Use HTTPS for all payment-related communications
- Implement idempotent webhook processing

### Data Privacy

**Privacy Measures**:
- Don't expose user email addresses publicly
- Show only partial phone numbers in listings
- Implement user data deletion on account closure (GDPR compliance)
- Log access to sensitive data for audit trail


### API Security

**Security Measures**:
- Implement CORS with whitelist of allowed origins
- Add rate limiting to prevent abuse (future enhancement)
- Validate Content-Type headers
- Implement request size limits
- Add security headers (X-Content-Type-Options, X-Frame-Options, etc.)

**Spring Security Configuration**:
```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf().disable() // Using JWT, not session-based
            .cors().and()
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/api/listings/search").permitAll()
                .requestMatchers("/api/listings/{id}").permitAll()
                .anyRequest().authenticated()
            )
            .sessionManagement()
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            .and()
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
        
        return http.build();
    }
}
```

### File Upload Security

**Security Measures**:
- Validate file types (only JPEG, PNG, WebP allowed)
- Enforce file size limits (5MB per photo)
- Scan uploaded files for malware (future enhancement)
- Generate unique filenames to prevent overwriting
- Store files in isolated storage bucket

## Dependencies

### Backend Dependencies (Java Spring Boot)

**Core Framework**:
- Spring Boot 3.2.x
- Spring Web (REST API)
- Spring Data JPA (Database access)
- Spring Security (Authentication/Authorization)

**Database**:
- PostgreSQL JDBC Driver
- Supabase Java Client

**Authentication**:
- jjwt (JWT token generation and validation)
- BCrypt (password hashing)

**Validation**:
- Hibernate Validator (Bean Validation)

**Testing**:
- JUnit 5
- Mockito
- Spring Boot Test
- Testcontainers (PostgreSQL)
- jqwik (Property-based testing)

**Utilities**:
- Lombok (Reduce boilerplate)
- MapStruct (DTO mapping)

**Build Tool**: Maven or Gradle

### Frontend Dependencies (React)

**Core Framework**:
- React 18.x
- React Router (Navigation)
- TypeScript

**State Management**:
- React Context API (Authentication state)
- React Query / TanStack Query (Server state management)

**UI Components**:
- Tailwind CSS (Styling)
- Headless UI or Radix UI (Accessible components)
- React Hook Form (Form handling)

**Real-Time Communication**:
- @supabase/supabase-js (Supabase client)
- WebSocket API (Real-time chat)

**HTTP Client**:
- Axios or Fetch API

**Testing**:
- Jest (Unit testing)
- React Testing Library
- fast-check (Property-based testing)

**Build Tool**: Vite or Create React App

### External Services

**Database and Storage**:
- Supabase (PostgreSQL database)
- Supabase Auth (User authentication)
- Supabase Storage (Image storage)
- Supabase Realtime (WebSocket for chat)

**Payment Gateway**:
- Stripe (Payment processing and escrow)

**Deployment** (Future):
- Backend: Heroku, AWS, or DigitalOcean
- Frontend: Vercel, Netlify, or AWS S3 + CloudFront
- Database: Supabase hosted PostgreSQL

### Environment Variables

**Backend (.env)**:
```properties
# Supabase
SUPABASE_URL=https://jkobgqcoednzpbfktogj.supabase.co
SUPABASE_KEY=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Imprb2JncWNvZWRuenBiZmt0b2dqIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NzE4MzUzMjEsImV4cCI6MjA4NzQxMTMyMX0.3HNh-YSffFFQEDDp8RvzY9hypXZc7tmfmGhO3jlL0wY
SUPABASE_SERVICE_KEY=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Imprb2JncWNvZWRuenBiZmt0b2dqIiwicm9sZSI6InNlcnZpY2Vfcm9sZSIsImlhdCI6MTc3MTgzNTMyMSwiZXhwIjoyMDg3NDExMzIxfQ.L_Zv8WbbpGn8JAk96Hglas6jBjVCdKWCZglScPLaNe0
DATABASE_URL=postgresql://[user]:[password]@[host]:[port]/[database]

# Stripe
STRIPE_SECRET_KEY=[secret-key]
STRIPE_WEBHOOK_SECRET=[webhook-secret]

# Storage
SUPABASE_STORAGE_BUCKET=post-images
```

**Frontend (.env)**:
```properties
VITE_API_BASE_URL=http://localhost:8080/api
VITE_SUPABASE_URL=https://jkobgqcoednzpbfktogj.supabase.co
VITE_SUPABASE_ANON_KEY=[anon-key]
VITE_STRIPE_PUBLISHABLE_KEY=[publishable-key]
```

## Database Schema

### Complete Schema Definition

```sql
-- Users table (references Supabase Auth)
CREATE TABLE public.users (
    id UUID NOT NULL,
    full_name TEXT NOT NULL,
    username TEXT NOT NULL UNIQUE,
    role USER-DEFINED NOT NULL,
    avatar_url TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT users_pkey PRIMARY KEY (id),
    CONSTRAINT users_id_fkey FOREIGN KEY (id) REFERENCES auth.users(id)
);

-- Posts table
CREATE TABLE public.posts (
    id UUID NOT NULL DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    post_type USER-DEFINED NOT NULL,
    status USER-DEFINED NOT NULL DEFAULT 'active'::post_status,
    title TEXT NOT NULL,
    content TEXT,
    images TEXT[] DEFAULT '{}'::TEXT[],
    car_make TEXT,
    car_model TEXT,
    car_year INTEGER,
    part_name TEXT,
    price NUMERIC,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT posts_pkey PRIMARY KEY (id),
    CONSTRAINT posts_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id)
);

-- Conversations table
CREATE TABLE public.conversations (
    id UUID NOT NULL DEFAULT gen_random_uuid(),
    post_id UUID,
    buyer_id UUID NOT NULL,
    seller_id UUID NOT NULL,
    last_message_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT conversations_pkey PRIMARY KEY (id),
    CONSTRAINT conversations_post_id_fkey FOREIGN KEY (post_id) REFERENCES public.posts(id),
    CONSTRAINT conversations_buyer_id_fkey FOREIGN KEY (buyer_id) REFERENCES public.users(id),
    CONSTRAINT conversations_seller_id_fkey FOREIGN KEY (seller_id) REFERENCES public.users(id)
);

-- Messages table
CREATE TABLE public.messages (
    id UUID NOT NULL DEFAULT gen_random_uuid(),
    conversation_id UUID NOT NULL,
    sender_id UUID NOT NULL,
    message_type USER-DEFINED NOT NULL DEFAULT 'text'::message_type,
    body TEXT,
    image_url TEXT,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT messages_pkey PRIMARY KEY (id),
    CONSTRAINT messages_conversation_id_fkey FOREIGN KEY (conversation_id) REFERENCES public.conversations(id),
    CONSTRAINT messages_sender_id_fkey FOREIGN KEY (sender_id) REFERENCES public.users(id)
);

-- Escrow transactions table
CREATE TABLE public.escrow_transactions (
    id UUID NOT NULL DEFAULT gen_random_uuid(),
    post_id UUID NOT NULL,
    buyer_id UUID NOT NULL,
    seller_id UUID NOT NULL,
    amount NUMERIC NOT NULL,
    status USER-DEFINED NOT NULL DEFAULT 'on_hold'::escrow_status,
    stripe_payment_intent_id TEXT,
    buyer_approved_at TIMESTAMP WITH TIME ZONE,
    dispute_reason TEXT,
    completed_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT escrow_transactions_pkey PRIMARY KEY (id),
    CONSTRAINT escrow_transactions_buyer_id_fkey FOREIGN KEY (buyer_id) REFERENCES public.users(id),
    CONSTRAINT escrow_transactions_seller_id_fkey FOREIGN KEY (seller_id) REFERENCES public.users(id),
    CONSTRAINT escrow_transactions_post_id_fkey FOREIGN KEY (post_id) REFERENCES public.posts(id)
);

-- Bookmarks table
CREATE TABLE public.bookmarks (
    id UUID NOT NULL DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    post_id UUID NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT bookmarks_pkey PRIMARY KEY (id),
    CONSTRAINT bookmarks_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id),
    CONSTRAINT bookmarks_post_id_fkey FOREIGN KEY (post_id) REFERENCES public.posts(id)
);

-- Notifications table
CREATE TABLE public.notifications (
    id UUID NOT NULL DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    type TEXT NOT NULL,
    title TEXT NOT NULL,
    body TEXT,
    data JSONB DEFAULT '{}'::JSONB,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT notifications_pkey PRIMARY KEY (id),
    CONSTRAINT notifications_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id)
);

-- Indexes for performance
CREATE INDEX idx_posts_status ON public.posts(status);
CREATE INDEX idx_posts_user ON public.posts(user_id);
CREATE INDEX idx_posts_car_make ON public.posts(car_make);
CREATE INDEX idx_posts_car_model ON public.posts(car_model);
CREATE INDEX idx_posts_car_year ON public.posts(car_year);
CREATE INDEX idx_posts_created_at ON public.posts(created_at DESC);
CREATE INDEX idx_posts_price ON public.posts(price);
CREATE INDEX idx_posts_search ON public.posts USING GIN(to_tsvector('english', title || ' ' || COALESCE(content, '')));

CREATE INDEX idx_conversations_buyer ON public.conversations(buyer_id);
CREATE INDEX idx_conversations_seller ON public.conversations(seller_id);
CREATE INDEX idx_conversations_post ON public.conversations(post_id);

CREATE INDEX idx_messages_conversation ON public.messages(conversation_id);
CREATE INDEX idx_messages_created_at ON public.messages(created_at DESC);

CREATE INDEX idx_escrow_buyer ON public.escrow_transactions(buyer_id);
CREATE INDEX idx_escrow_seller ON public.escrow_transactions(seller_id);
CREATE INDEX idx_escrow_status ON public.escrow_transactions(status);
CREATE INDEX idx_escrow_stripe ON public.escrow_transactions(stripe_payment_intent_id);

CREATE INDEX idx_bookmarks_user ON public.bookmarks(user_id);
CREATE INDEX idx_bookmarks_post ON public.bookmarks(post_id);

CREATE INDEX idx_notifications_user ON public.notifications(user_id);
CREATE INDEX idx_notifications_read ON public.notifications(is_read);
```

## API Endpoints Reference

### Authentication Endpoints

```
POST   /api/auth/register          - Register new user (Supabase Auth)
POST   /api/auth/login             - Login user (Supabase Auth)
POST   /api/auth/logout            - Logout user
GET    /api/auth/me                - Get current user profile
PUT    /api/auth/profile           - Update user profile
```

### Post Endpoints

```
POST   /api/posts                  - Create new post
GET    /api/posts                  - Get all posts (with pagination)
GET    /api/posts/search           - Search posts with filters
GET    /api/posts/{id}             - Get post by ID
PUT    /api/posts/{id}             - Update post
DELETE /api/posts/{id}             - Delete post
GET    /api/posts/user/{id}        - Get user's posts
POST   /api/posts/images           - Upload post images
```

### Escrow Endpoints

```
POST   /api/escrow/initiate        - Initiate escrow transaction
POST   /api/escrow/webhook         - Stripe webhook handler
PUT    /api/escrow/{id}/approve    - Approve buyer release
PUT    /api/escrow/{id}/dispute    - Raise dispute
GET    /api/escrow/transactions    - Get user transactions
GET    /api/escrow/{id}            - Get transaction details
```

### Conversation Endpoints

```
POST   /api/conversations          - Create or get conversation
GET    /api/conversations          - Get user's conversations
GET    /api/conversations/{id}/messages - Get conversation history
POST   /api/messages               - Send message
PUT    /api/conversations/{id}/read - Mark messages as read
```

### Bookmark Endpoints

```
POST   /api/bookmarks              - Add to bookmarks
DELETE /api/bookmarks/{postId}     - Remove from bookmarks
GET    /api/bookmarks              - Get user's bookmarks
```

### Notification Endpoints

```
GET    /api/notifications          - Get user notifications
PUT    /api/notifications/{id}/read - Mark notification as read
PUT    /api/notifications/read-all - Mark all as read
```

---

## Implementation Notes

### Development Timeline (One Week)

**Day 1-2**: Backend setup and core services
- Set up Spring Boot project structure
- Configure Supabase connection
- Implement authentication service
- Create database schema and migrations

**Day 3-4**: Backend business logic
- Implement listing service
- Implement bid service
- Implement payment service
- Implement chat service

**Day 5-6**: Frontend development
- Set up React project with TypeScript
- Implement authentication UI
- Implement listing creation and search
- Implement bidding interface
- Implement chat interface

**Day 7**: Integration and testing
- End-to-end testing
- Bug fixes
- Deployment preparation

### Future Enhancements (Out of Scope for MVP)

- VIN-based part search
- Delivery service integration
- Seller analytics dashboard
- AI-powered part recommendations
- Mobile native apps (iOS/Android)
- Advanced fraud detection
- Seller ratings and reviews
- Multi-language support (Arabic/English)
- Push notifications
- Email notifications
- Advanced search filters (location, condition)

---

## Notes on Schema Differences from Original Requirements

The actual Supabase schema differs from the original requirements in several ways:

### Removed Features (Not Implemented in Current Schema)

1. **Bidding System**: No `bids` table exists in the actual schema. The original design included a bidding system where buyers could place bids on listings and sellers could accept/decline them. The current implementation appears to use direct negotiation via chat instead.

2. **Car Data Table**: No `car_data` table for pre-loaded Bahrain car makes/models/years. Car information (make, model, year) is stored directly in posts table as optional text fields without validation against a reference table.

3. **Auto-Archive Feature**: No scheduled job to archive posts after 90 days. The original design included automatic archiving of unsold listings after 90 days.

### Terminology Changes

- `listings` → `posts`
- `chat_rooms` → `conversations`
- `watchlist` → `bookmarks`
- `transactions` → `escrow_transactions`
- `listing_photos` table → `images` array column in posts table

### Schema Simplifications

1. **User Authentication**: Uses Supabase Auth (`auth.users`) with foreign key from `public.users`, eliminating need for password hashing in application code.

2. **Images Storage**: Posts use a PostgreSQL `text[]` array for image URLs instead of a separate `listing_photos` table with display order.

3. **Conversations**: Post ID is optional (nullable) in conversations table, allowing conversations without specific posts.

4. **Message Types**: Messages support both text (`body` field) and images (`image_url` field) with `message_type` enum.

5. **Escrow Status**: Uses database enum types for status fields (post_status, escrow_status, message_type, etc.) instead of application-level enums.

### Implementation Recommendations

If bidding functionality is required, consider:
- Adding a `bids` table with the structure from the original design
- Implementing bid acceptance workflow
- Adding bid-related notifications

If car data validation is required, consider:
- Creating `car_data` table with Bahrain market data
- Adding foreign key constraints or validation logic
- Implementing dropdown population from car_data table
