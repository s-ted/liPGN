Lichess game downloader
------------------------

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
java -jar liPGN.jar -u Fins -p classical
```

### Options
```
  -q, --quiet                                            Don't output messages on console
  -C, --casual                                           Also handle casual games
  -v, --variant <variant>    standard                    Handle games for the given variant (standard/chess960/kinfOfTheHill/threeCheck)
  -p, --speed <speed>                                    Handle games for the given speed (bullet/blitz/classical/unlimited)
  -U, --url <URL>            http://en.lichess.org/api/  URL of the API
  -s, --store <store>        plocal:db                   The store to use for keeping the data (use 'memory:tmp' for a transient run)
  -c, --color <color>                                    Handle games for the given color (white/black)
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

### Credits

<a rel="license" href="http://creativecommons.org/licenses/by-nc-sa/4.0/"><img alt="Creative Commons License" style="border-width:0" src="https://i.creativecommons.org/l/by-nc-sa/4.0/88x31.png" /></a><br /><span xmlns:dct="http://purl.org/dc/terms/" property="dct:title">liPGN</span> by <a xmlns:cc="http://creativecommons.org/ns#" href="https://github.com/s-ted/liPGN" property="cc:attributionName" rel="cc:attributionURL">s-ted</a> is licensed under a <a rel="license" href="http://creativecommons.org/licenses/by-nc-sa/4.0/">Creative Commons Attribution-NonCommercial-ShareAlike 4.0 International License</a>.<br />Based on a work at <a xmlns:dct="http://purl.org/dc/terms/" href="https://github.com/s-ted/liPGN" rel="dct:source">https://github.com/s-ted/liPGN</a>.
