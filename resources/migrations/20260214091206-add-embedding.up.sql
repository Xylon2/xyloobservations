-- SigLIP embeddings are 1152-dimensional
alter table image
add embedding float[];
