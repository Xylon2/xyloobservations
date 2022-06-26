# xyloobservations

I made this app for [my personal photo gallery](https://gallery.xylon.me.uk/). I wanted elegant tag-based organization for my photos.

It's based on [Luminus](https://luminusweb.com/) and I designed it to work with [PostgreSQL](https://www.postgresql.org/).

## Running this code

Here I explain how you may run this code on your Linux or Mac workstation for testing.

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
}
```

To start a web server for the application, run:
```
lein repl
(start)
```

Now you should be able to access the app at http://localhost:3000/.

For uploading images there is an admin interface which you can access at `/login`. To create credentials for this type these in the repl:
```
(in-ns 'xyloobservations.authfunctions)
 (create-user! "youruser" "yourpass")
```

For ideas on hosting it check out [the luminus docs](https://luminusweb.com/docs/deployment.html).

## License

Copyright © 2022 Joseph Graham
