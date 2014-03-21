# bltool

A command line tool for managing a Backloggery game collection, including bulk add/delete and import from Steam.

## Installation

Download the latest release from https://github.com/ToxicFrog/bltool/releases and unzip it. At this point you can import your games from Steam by editing `steam2backloggery.bat` (windows) or `steam2backloggery.sh` (Linux/OSX) to have the correct login information, then running it; if you need more contronl, you can run it directly from the command line with `java -jar bltool.jar <arguments>`.

If you want to build from source, it uses Leiningen, the standard Clojure build tool; use `lein uberjar` to build or `lein run` to run. To create a release zip, use `./release.sh <version>`.

## Usage

    Usage: bltool <command> [<args>]

     Switches                       Default  Desc                                                                                               
     --------                       -------  ----                                                                                               
     --from                                  What type of data source to read the games/edits from. Use '--help formats' for a list of formats. 
     --to                                    What type of destination to write the changes to. Use '--help formats' for a list of formats.      
     --steam-name                            Steam Community name                                                                               
     --steam-platform               PC       Default platform to use for Steam games (recommended: PC, PCDL, or Steam)                          
     --bl-name                               backloggery username                                                                               
     --bl-pass                               backloggery password                                                                               
     --no-bl-stealth, --bl-stealth  true     use 'stealth add' and 'stealth edit' when updating backloggery                                     
     --help                                  Show detailed help. Try 'bltool --help (formats|usage)'                                            
     --no-filter, --filter          false    Filter out games already on Backloggery. Requires loading the game list from Backloggery.          
     --input                        -        For file-based formats, read input from this file. '-' means stdin.                                
     --output                       -        For file-based formats, write output to this file. '-' means stdout.                               
     --name                                  Include only games where the name contains this string.                                            

You will need to at least specify `--from` and `--to` to specify input and output formats; some formats may require additional arguments. See the next session for details on how to use them.

## Supported data sources/formats

    This tool can read and write a variety of sources. The --from and --to
    options control what formats it reads and write; the --input and --output
    options control what file it writes to or reads from, for formats that are
    stored in files.
   
    Format       RW  Desc
    ------       --  ----
    backloggery  RW  Backloggery game library. When writing, equivalent to bl-add.
    bl-add        W  Add new games to Backloggery.
    bl-edit*     RW  Edit existing games, overwriting current information.
                     In read mode, gets all game info, not just basic info (slow!)
    bl-delete     W  Delete all listed games; all properties except ID are ignored.
    steam        R   Game list from Steam Community
    html*         W  HTML file that can submit changes to Backloggery
    text         RW  User-editable plain text
    edn          RW  Machine-readable EDN
   
    * Not yet implemented

### Instructions for specific formats

* `backloggery`, `bl-add`, `bl-edit`, `bl-delete`

  These all read or modify your backloggery game list. You'll need to specify `--bl-name` and `--bl-pass` so that it can log in to backloggery as you. If adding games, you might want to also use `--filter`, which will exclude games already on your backloggery. If you want bulk adds/deletes to show up on your multitap, use `--no-bl-stealth` as well.

* `steam`

  This reads your Steam game list from `http://steamcommunity.com/id/<name>`. This means that your Steam profile has to be public, and you need to use `--steam-name` to tell it what name to use. This needs to be the name that appears in your Steam Community URL, *not* your Steam login name or display name. The default platform it uses for games from Steam is "PC"; you can use `--steam-platform` to override this.

* `text`

  This is a tab-separated format, one line per game, with the fields `ID`, `platform`, `status`, and `name`. `ID` is the Backloggery game ID (or 0 for games that weren't imported from Backloggery). `platform` is the game's platform; it defaults to PC but can be any platform Backloggery supports. `status` is one of "unplayed", "unfinished", "beaten", "completed", "mastered", or "null". Name can contain anything at all (including tabs and other whitespace) and is thus the last field.

  The platform needs to be the platform ID that backloggery uses; for example, "PCDL", not "PC Downloads". At the moment the only way to get a complete list is to "view source" on backloggery's add-a-game page.

  This is a file oriented format, so you probably want `--input <file>` (when using `--from text`) or `--output <file>` (when using `--to text`). Otherwise it will read from and write to the terminal.

* `edn`

  This saves or loads the game list in [Extensible Data Notation](https://github.com/edn-format/edn) format. It is primarily useful if you want the game list in a machine-readable format for use with other tools.

  Like text, it is file-oriented and should be combined with `--input` or `--output`.

## Examples

Download your game list from backloggery for a look:

    bltool --from backloggery --to text --output games.txt --bl-name MyName --bl-pass TopSecret

Import your game list from Steam to Backloggery, editing the game list first. Note that you need the backloggery login options for --filter to work, since this also downloads your backloggery game list and excludes any games you already have there.

    bltool --from steam --to text --output games.txt --steam-name MyName --bl-name MyName --bl-pass TopSecret --filter
    edit games.txt
    bltool --from text --to backloggery --input games.txt --bl-name MyName --bl-pass TopSecret

Import your game list from Steam in one step with no filtering:

    bltool --from steam --to backloggery --steam-name MyName --bl-name MyName --bl-pass TopSecret --filter

Filter out duplicates from your Backloggery game list (in one step - you probably want to double-check this first, in reality!):

    bltool --from backloggery --bl-name MyName --bl-pass TopSecret --output text \
    | sort | uniq -d -f3 \
    | bltool --from text --to bl-delete --bl-name MyName --bl-pass TopSecret

Remove all of your DLC from backloggery, using "stealth delete":

    bltool --from backloggery --to bl-delete --name DLC --bl-stealth --bl-pass TopSecret --output text

### Bugs

Two features (html output and backloggery edit support) are not yet implemented.

Apart from that, bug reports are welcome! Post them on github and I'll email me.

## License

Copyright © 2013 Ben "ToxicFrog" Kelly, Google Inc.

Distributed under the Apache License v2; see the file LICENSE for details.
