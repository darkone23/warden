# warden [![build status](https://travis-ci.org/eggsby/warden.png)](https://travis-ci.org/eggsby/warden)

A simple UI for managing multiple [supervisord](http://supervisord.org) nodes.

Currently a work in progress.

## Building

To build a standalone jar

    lein do cljsbuild once, ring uberjar

## Running

To start warden create your own [warden.yaml](https://github.com/eggsby/warden/blob/master/example.warden.yaml) and:

    java -jar target/warden-0.0.1-SNAPSHOT-standalone.jar

## License

warden is free software
