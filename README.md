# xyloobservations

I made this app for [my personal photo gallery](https://gallery.xylon.me.uk/). I wanted elegant tag-based organization for my photos.

It's based on [Luminus](https://luminusweb.com/) and I designed it to work with [PostgreSQL](https://www.postgresql.org/).

## Running this code

Here I explain how you may run this code on your workstation for development.

Install:
- [Leiningen](https://github.com/technomancy/leiningen)
- [PostgreSQL](https://medium.com/coding-blocks/creating-user-database-and-adding-access-on-postgresql-8bfcd2f4a91e)

Create a PostgreSQL database and user, and create a file `dev-config.edn` with credentials. An example of how that might look:
```
{:dev true
 :port 3000
 ;; when :nrepl-port is set the application starts the nREPL server on load
 :nrepl-port 7000
 
 ; set your dev database connection URL here
 :database-url "postgresql://localhost/dbname?user=dbuser&password=dbpass"

 ;; s3 or postgres
 :image-store "postgres"
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

xyloobservations can store it's images either in postgres or an S3 bucket. Postgres is easier for development but s3 is a better long-term solution. If you want to use s3 you will need to set the right variables.

In `dev-config.edn` it will look something like this:
```
 :aws-access-key ""
 :aws-secret-key ""
 :aws-region "eu-west-2"
 :bucket-name ""
 :url-prefix "https://bucketname.s3.eu-west-2.amazonaws.com/"
```

If configuring with environment variables the settings are upper-case and use under-scores. Setting the variables for Heroku looks something like:
```
heroku config:set IMAGE_STORE=s3
heroku config:set AWS_ACCESS_KEY=
heroku config:set AWS_SECRET_KEY=
heroku config:set AWS_REGION=eu-west-2
heroku config:set BUCKET_NAME=
heroku config:set URL_PREFIX=https://bucketname.s3.eu-west-2.amazonaws.com/
```

## License

Copyright © 2022 Joseph Graham
