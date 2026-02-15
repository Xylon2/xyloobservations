-- Enable pgvector extension
create extension if not exists vector;

--;;

-- SigLIP embeddings are 1152-dimensional
alter table image
add embedding vector(1152);
