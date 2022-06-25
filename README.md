# xyloobservations

I made this app for [my personal photo gallery](https://gallery.xylon.me.uk/). I wanted elegant tag-based organization for my photos.

It's based on [Luminus](https://luminusweb.com/) and I designed it to work with [PostgreSQL](https://www.postgresql.org/).

## Running this code

To run this you will need [Leiningen](https://github.com/technomancy/leiningen) installed.

You're gonna need a PostgreSQL database, and to create a file `dev-config.edn` with credentials. An example of how that might look:
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

    lein run 

There is an admin interface which you can access at `/login`. To create credentials for this use the repl:
```
lein repl
(in-ns 'xyloobservations.auth)
 (create-user! "youruser" "yourpass")
```

For ideas on hosting it check out [the luminus docs](https://luminusweb.com/docs/deployment.html).

## License

Copyright © 2022 Joseph Graham
