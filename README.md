# xyloobservations

I made this app for [my personal photo gallery](https://gallery.xylon.me.uk/). I wanted elegant tag-based organization for my photos.

It's based on [Luminus](https://luminusweb.com/) and I designed it to work with [PostgreSQL](https://www.postgresql.org/).

## Running this code

Here I explain how you may run this code on your workstation for development.

Install:
- [Leiningen](https://codeberg.org/leiningen/leiningen)
- [PostgreSQL](https://medium.com/coding-blocks/creating-user-database-and-adding-access-on-postgresql-8bfcd2f4a91e)
- [RabbitMQ](https://www.rabbitmq.com/download.html)
- [ImageMagick](https://imagemagick.org/script/download.php)

Create a PostgreSQL database and user, and create a file `dev-config.edn` with credentials. An example of how that might look:
```
{:dev true
 :port 3000
 ;; when :nrepl-port is set the application starts the nREPL server on load
 :nrepl-port 7000
 
 ; set your dev database connection URL here
 :database-url "postgresql://localhost/dbname?user=dbuser&password=dbpass"

 ;; s3 or filesystem
 :image-store "filesystem"

 ;; the url s3 or your webserver exposes the images
 :url-prefix "https://example.com/"}

 ;; if storing on filessytem
 ;; this should be a writeable directory where your webserver will serve the images
 :img-path "/var/www/html/images/"

 ;; ;; if storing in s3.
 ;; ;; you will need to setup the bucket for public access
 ;; :aws-access-key ""
 ;; :aws-secret-key ""
 ;; :aws-region "eu-west-2"
 ;; :bucket-name ""
}
```

To start a web server for the application, run:
```
lein repl
(start)
```

Now you should be able to access the app at http://localhost:3000/.

For uploading images there is an admin interface which you can access at `/login`. To create credentials for this type these in the repl (after running `(start)`):
```
(in-ns 'xyloobservations.authfunctions)
 (create-user! "youruser" "yourpass")
```

## Hosting

For general info on hosting Luminus apps check out [the luminus docs](https://luminusweb.com/docs/deployment.html). However I designed this to be hosted on Heroku.

xyloobservations can store it's images either in an S3 bucket or on the filesystem. If you want to use s3 you will need to specify the aws credentials and bucket-name.

For using s3, `dev-config.edn` will look something like this:
```
 :url-prefix "https://bucketname.s3.eu-west-2.amazonaws.com/"

 :aws-access-key ""
 :aws-secret-key ""
 :aws-region "eu-west-2"
 :bucket-name ""
```

If configuring with environment variables the settings are upper-case and use under-scores. Setting the variables for Heroku looks something like:
```
heroku config:set IMAGE_STORE=s3
heroku config:set URL_PREFIX=https://bucketname.s3.eu-west-2.amazonaws.com/
heroku config:set AWS_ACCESS_KEY=
heroku config:set AWS_SECRET_KEY=
heroku config:set AWS_REGION=eu-west-2
heroku config:set BUCKET_NAME=
heroku config:set CLOUDAMQP_APIKEY=
heroku config:set CLOUDAMQP_URL=
```

Also note for Heroku you need:
- Heroku 22 stack or newer
- [Heroku Postgres addon](https://elements.heroku.com/addons/heroku-postgresql)
- [CloudAMQP addon](https://elements.heroku.com/addons/cloudamqp)
- [Apt buildpack](https://github.com/heroku/heroku-buildpack-apt)

## License

Copyright © 2022 Joseph Graham
