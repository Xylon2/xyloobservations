-- Create table for caching text embeddings
create table text_embedding_cache (
  text text primary key,
  embedding vector(1152) not null,
  created_at timestamp with time zone default now()
);

--;;

-- Index for efficient lookups (though primary key already provides this)
create index idx_text_embedding_cache_created_at on text_embedding_cache(created_at);
