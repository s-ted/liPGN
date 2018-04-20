DEPRECATION NOTICE
==================

Since a couple of days, nothing get downloaded because of the instantly appearing `Lichess is rate-limiting us, waiting 120s ...` message (see #11).

Having check with Thibault (lichess main dev) :

> that API is still supported but it's being hammered so hard, it has hit the global rate limit :("

this means that _lichess_ has closed this path, and thus, liPGN will **never work again as-is** without quite a heavy code refactoring...

But Thibault also give you a new way of doing it, directly via the lichess API (thus without having to use liPGN) : https://lichess.org/api#operation/gamesExportUsername

This new API simply means that *liPGN is not needed anymore* ! and that anyone can use it on it's own to retrieve directly a PGN formatted file.
Though, advanced filtering and local data caching, that liPGN was providing, won't be available anymore...


I am not planning on moving liPGN to the new lichess API as it is not really suited for parsing (JSON format has been replaced by a plain flat PGN format).

> So Long and Thanks for all the Fish.



Lichess game downloader
------------------------

  [![Build Status](https://travis-ci.org/s-ted/liPGN.svg)](https://travis-ci.org/s-ted/liPGN)


This program uses the lichess REST API to retrieve all the games of a
user and transform them into a big PGN file that you can then import
into your favourite game analysis tool (ie. scid)

### Usage

```
java -jar liPGN.jar [options]
```

For instance, if I want to prepare against Thibault for my next correspondance game:

```
java -jar liPGN.jar -u thibault -p unlimited
```


And if I want to prep for my next standard game against John Bartholomew:

```
java -jar liPGN.jar -u Fins --variant standard --speed classical,blitz
```

And if I want to challenge JannLee:
```
java -jar liPGN.jar -u JannLee --variant crazyHouse --speed classical,blitz
```

### Options
```
  -q, --quiet                                            Don't output messages on console
  -C, --casual                                           Also handle casual games
  -v, --variant <v1,v2...>   standard                    Handle games for the given variant
  (standard/chess960/kinfOfTheHill/threeCheck/racingKings/horde/crazyHouse/antichess/atomic)
  -p, --speed <s1,s2...>                                 Handle games for the given speed (bullet/blitz/classical/unlimited). By default: all.
  -U, --url <URL>            http://en.lichess.org/api/  URL of the API
  -s, --store <store>        plocal:db                   The store to use for keeping the data (use 'memory:tmp' for a transient run)
  -c, --color <color>                                    Handle games for the given color (white/black)
  -R, --refresh-all                                      Refresh all games, even if some are already in the local DB (useful if you break a previous import with Ctrl-C...)
  -S, --no-sync                                          Don't sync the games with the server
  -t, --with-times                                       Decorate the PGN with the move times
  -u, --username <username>                              The username for whom to retrieve the games
  -o, --output <file>                                    The file to output, use '-' for stdout. By default, output to '<username>.pgn'
      --template-pgn <file>                              A file to use for templating the PGN (markdown format).
      --template-move-pair <file>                        A file to use for templating a move pair (markdown format).
  -h, --help                                             Print this help
```

### Build

```
lein uberjar
```

### Templating

Templating is easily overridable using Markdown format and the vars returned by the Lichess API.

#### Default PGN template
```
[Event "{{id}}"]
[Site "{{speed}}"]
[Date "{{date}}"]
[Round "{{url}}"]
[White "{{players.white.name}}"]
[WhiteElo "{{players.white.elo}}"]
[Black "{{players.black.name}}"]
[BlackElo "{{players.black.elo}}"]
[Variant "{{variant}}"]
[Result "{{result}}"]

{{moves}}{{analysis}}{{result}}

```

#### Default move-pair template
```
{{n}}. {{white}} {{black}}
```

### Notes

The standard behavior will create a ./db directory, hosting the local database (used to reduce exchanges with the lichess server).
You can delete this directory to "flush caches".

To get around this, you can use a transient DB (in RAM, won't be persistent) using `--store memory:tmp`

## License

Copyright © 2016 [Sylvain Tedoldi](https://github.com/s-ted)

Distributed under the Eclipse Public License either version 1.0 or
(at your option) any later version.
