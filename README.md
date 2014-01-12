# bltool

A command line tool for managing a Backloggery game collection, including bulk add/delete and import from Steam.

## Installation

Download from github. If building from source, run `lein uberjar` to compile (or just run it directly with `lein run`). If you downloaded an uberjar, run it with `java -jar bltool-VERSION.jar ...`.

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
