-- :name create-user!* :! :n
-- :doc creates a new user with the provided login and hashed password
insert into users
(login, password)
values (:login, :password)

-- :name get-user-for-auth* :? :1
-- :doc selects a user for authentication
select * from users
where login = :login

-- :name add-tag! :! :n
-- :doc add a tag
insert into tag
(tag_name, description)
values (:tagname, :description)

-- :name upload-image! :! :n
-- :doc upload the image
insert into image
(imagedata)
values (:imagedata)
