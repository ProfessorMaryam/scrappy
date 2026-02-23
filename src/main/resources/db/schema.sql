-- ScrapBH Marketplace Database Schema
-- This schema should be executed on Supabase PostgreSQL database

-- ============================================
-- ENUMS
-- ============================================

-- User role enum
CREATE TYPE user_role AS ENUM ('buyer', 'seller');

-- Post type enum
CREATE TYPE post_type AS ENUM ('sale', 'wanted');

-- Post status enum
CREATE TYPE post_status AS ENUM ('active', 'sold', 'archived');

-- Message type enum
CREATE TYPE message_type AS ENUM ('text', 'image');

-- Escrow status enum
CREATE TYPE escrow_status AS ENUM ('on_hold', 'released', 'disputed', 'completed');

-- ============================================
-- TABLES
-- ============================================

-- Users table (references Supabase Auth)
CREATE TABLE public.users (
    id UUID NOT NULL,
    full_name TEXT NOT NULL,
    username TEXT NOT NULL UNIQUE,
    role user_role NOT NULL,
    avatar_url TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT users_pkey PRIMARY KEY (id),
    CONSTRAINT users_id_fkey FOREIGN KEY (id) REFERENCES auth.users(id) ON DELETE CASCADE
);

-- Posts table
CREATE TABLE public.posts (
    id UUID NOT NULL DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    post_type post_type NOT NULL,
    status post_status NOT NULL DEFAULT 'active',
    title TEXT NOT NULL,
    content TEXT,
    images TEXT[] DEFAULT '{}'::TEXT[],
    car_make TEXT,
    car_model TEXT,
    car_year INTEGER,
    part_name TEXT,
    price NUMERIC(10, 2),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT posts_pkey PRIMARY KEY (id),
    CONSTRAINT posts_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE
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
    CONSTRAINT conversations_post_id_fkey FOREIGN KEY (post_id) REFERENCES public.posts(id) ON DELETE SET NULL,
    CONSTRAINT conversations_buyer_id_fkey FOREIGN KEY (buyer_id) REFERENCES public.users(id) ON DELETE CASCADE,
    CONSTRAINT conversations_seller_id_fkey FOREIGN KEY (seller_id) REFERENCES public.users(id) ON DELETE CASCADE,
    CONSTRAINT conversations_unique UNIQUE (post_id, buyer_id, seller_id)
);

-- Messages table
CREATE TABLE public.messages (
    id UUID NOT NULL DEFAULT gen_random_uuid(),
    conversation_id UUID NOT NULL,
    sender_id UUID NOT NULL,
    message_type message_type NOT NULL DEFAULT 'text',
    body TEXT,
    image_url TEXT,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT messages_pkey PRIMARY KEY (id),
    CONSTRAINT messages_conversation_id_fkey FOREIGN KEY (conversation_id) REFERENCES public.conversations(id) ON DELETE CASCADE,
    CONSTRAINT messages_sender_id_fkey FOREIGN KEY (sender_id) REFERENCES public.users(id) ON DELETE CASCADE
);

-- Escrow transactions table
CREATE TABLE public.escrow_transactions (
    id UUID NOT NULL DEFAULT gen_random_uuid(),
    post_id UUID NOT NULL,
    buyer_id UUID NOT NULL,
    seller_id UUID NOT NULL,
    amount NUMERIC(10, 2) NOT NULL,
    status escrow_status NOT NULL DEFAULT 'on_hold',
    stripe_payment_intent_id TEXT,
    buyer_approved_at TIMESTAMP WITH TIME ZONE,
    dispute_reason TEXT,
    completed_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT escrow_transactions_pkey PRIMARY KEY (id),
    CONSTRAINT escrow_transactions_buyer_id_fkey FOREIGN KEY (buyer_id) REFERENCES public.users(id) ON DELETE CASCADE,
    CONSTRAINT escrow_transactions_seller_id_fkey FOREIGN KEY (seller_id) REFERENCES public.users(id) ON DELETE CASCADE,
    CONSTRAINT escrow_transactions_post_id_fkey FOREIGN KEY (post_id) REFERENCES public.posts(id) ON DELETE CASCADE,
    CONSTRAINT escrow_transactions_amount_positive CHECK (amount > 0)
);

-- Bookmarks table
CREATE TABLE public.bookmarks (
    id UUID NOT NULL DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    post_id UUID NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT bookmarks_pkey PRIMARY KEY (id),
    CONSTRAINT bookmarks_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE,
    CONSTRAINT bookmarks_post_id_fkey FOREIGN KEY (post_id) REFERENCES public.posts(id) ON DELETE CASCADE,
    CONSTRAINT bookmarks_unique UNIQUE (user_id, post_id)
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
    CONSTRAINT notifications_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE
);

-- ============================================
-- INDEXES FOR PERFORMANCE
-- ============================================

-- Posts indexes
CREATE INDEX idx_posts_status ON public.posts(status);
CREATE INDEX idx_posts_user_id ON public.posts(user_id);
CREATE INDEX idx_posts_car_make ON public.posts(car_make);
CREATE INDEX idx_posts_car_model ON public.posts(car_model);
CREATE INDEX idx_posts_car_year ON public.posts(car_year);
CREATE INDEX idx_posts_created_at ON public.posts(created_at DESC);
CREATE INDEX idx_posts_price ON public.posts(price);
CREATE INDEX idx_posts_search ON public.posts USING GIN(to_tsvector('english', title || ' ' || COALESCE(content, '')));

-- Conversations indexes
CREATE INDEX idx_conversations_buyer_id ON public.conversations(buyer_id);
CREATE INDEX idx_conversations_seller_id ON public.conversations(seller_id);
CREATE INDEX idx_conversations_post_id ON public.conversations(post_id);

-- Messages indexes
CREATE INDEX idx_messages_conversation_id ON public.messages(conversation_id);
CREATE INDEX idx_messages_created_at ON public.messages(created_at DESC);

-- Escrow transactions indexes
CREATE INDEX idx_escrow_buyer_id ON public.escrow_transactions(buyer_id);
CREATE INDEX idx_escrow_seller_id ON public.escrow_transactions(seller_id);
CREATE INDEX idx_escrow_status ON public.escrow_transactions(status);
CREATE INDEX idx_escrow_stripe_payment_intent_id ON public.escrow_transactions(stripe_payment_intent_id);

-- Bookmarks indexes
CREATE INDEX idx_bookmarks_user_id ON public.bookmarks(user_id);
CREATE INDEX idx_bookmarks_post_id ON public.bookmarks(post_id);

-- Notifications indexes
CREATE INDEX idx_notifications_user_id ON public.notifications(user_id);
CREATE INDEX idx_notifications_is_read ON public.notifications(is_read);

-- ============================================
-- COMMENTS
-- ============================================

COMMENT ON TABLE public.users IS 'User profiles linked to Supabase Auth';
COMMENT ON TABLE public.posts IS 'Auto part listings (sale or wanted)';
COMMENT ON TABLE public.conversations IS 'Chat conversations between buyers and sellers';
COMMENT ON TABLE public.messages IS 'Messages within conversations';
COMMENT ON TABLE public.escrow_transactions IS 'Stripe-powered escrow transactions';
COMMENT ON TABLE public.bookmarks IS 'User bookmarks for posts';
COMMENT ON TABLE public.notifications IS 'System notifications for users';
