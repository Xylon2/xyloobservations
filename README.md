# xyloobservations

I made this app for [my personal photo gallery](https://gallery.xylon.me.uk/). I wanted elegant tag-based organization for my photos.

It's based on [Luminus](https://luminusweb.com/) and I designed it to work with [PostgreSQL](https://www.postgresql.org/).

## Running this code

Here I explain briefly how you may run this code on your workstation for development.

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

 ;; webp, avif or jpeg
 :img-format "webp"
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

If you are willing to comply with the requirements of the Affero GPL you may use this code for your own photo gallery. In addition to applying appropriate config for your environment you will need to modify some of the templates to customise it for your site. In-particular `base.html` and `about.html`.

For general info on hosting Luminus apps check out [the luminus docs](https://luminusweb.com/docs/deployment.html).

xyloobservations can store it's images either in an S3 bucket or on the filesystem. If you want to use s3 you will need to specify the aws credentials and bucket-name.

For using s3, `dev-config.edn` will look something like this:
```
 :url-prefix "https://bucketname.s3.eu-west-2.amazonaws.com/"

 :aws-access-key ""
 :aws-secret-key ""
 :aws-region "eu-west-2"
 :bucket-name ""
```

If configuring with environment variables the settings are upper-case and use under-scores. Example:
```
IMAGE_STORE=s3
URL_PREFIX=https://bucketname.s3.eu-west-2.amazonaws.com/
AWS_ACCESS_KEY=
AWS_SECRET_KEY=
AWS_REGION=eu-west-2
BUCKET_NAME=
CLOUDAMQP_APIKEY=
CLOUDAMQP_URL=
DATABASE_URL=postgresql://localhost/dbname?user=dbuser&password=dbpass
IMG_FORMAT=avif
```

Note if your RabbitMQ is running on localhost you may ommit the `CLOUDAMQP_APIKEY` and `CLOUDAMQP_URL`.

I no-longer recommend hosting on Heroku due to:
- their pricing-structure is no longer affordable for small hobby sites
- message-size restrictions on their managed CloudAMQP
- unreasonably small memory allowance on the dynos is problematic for resizing large images

However if you do want to host it on Heroku or similar, note that it requires imagemagick to be available for commands `convert` and `identify`.

To create an admin login, the .jar can be called with an argument "add-user". Here is an example:
```
set -o allexport
source /var/gallery/env
/usr/bin/java -jar /var/gallery/xyloobservations.jar add-user
```

It will prompt you for a username and password.

## Special upgrades

### url_prefix

If you upgrade from an old version of the gallery and ___ you likely need to run
"special-migrate". This populates the new "url_prefix" column of the user table.
```
set -o allexport
source /var/gallery/env
/usr/bin/java -jar /var/gallery/xyloobservations.jar special-migrate
```

### recompress images

If you want to re-compress the images, either because you are changing the compression
format or because you want to migrate them to a new storage back-end, there is a
command for this:
```
set -o allexport
source /var/gallery/env
/usr/bin/java -jar /var/gallery/xyloobservations.jar recompress-all
```

It will run in the background, re-compressing your images one at-a-time.

## License

Copyright © 2022 Joseph Graham

This program is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License along with this program. If not, see <https://www.gnu.org/licenses/>.
