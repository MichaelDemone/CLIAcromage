# Acromage

A Clojure CLI version of Acromage from the Might and Magic series! I am using this project to wrap my head around functional programming, to learn Clojure, and to get some AI experience by programming enemies.

Acromage is a card based strategy game developed by 3DO (RIP) where your goal is to destroy the other player's tower, reach a certain amount of resources, or get a big enough tower. Each player has resources that allow them to use cards, namely bricks, gems, and beasts. Cards are the key to victory and allow you to do all sorts of things from getting more resources by strip mining, to reducing the enemy's resources through magic, to destroying the enemies tower and your own tower with a goblin mob.

## Rules
There are 3 resources: bricks, gems, and beasts which are gained each turn based on your quarry, magic, and zoos, respectively. Each allow you to use different types of cards.

Each player has a tower and wall. The main goal is to destroy the other players tower which can only be done once the wall is destroyed (unless otherwise stated by a card). 

Each player always has 5 cards in their hand.

The winning conditions are configurable, but are defaulted to destroying the enemy tower, getting your tower to 100, or getting a resource to 100.

## Implementation Features
### Terminal
CLIAcromage creates a terminal using [lanterna](https://github.com/MultiMUD/clojure-lanterna) to display the game state and get user input!
![Photo of terminal](https://github.com/MichaelDemone/CLIAcromage/blob/master/doc/Terminal.png)

### Config Files
Currently all of the cards are defined in resources/Cards.csv and parsed with my own parser. You can load the CSV file into google sheets/excel and add or remove cards to your heart's content. For the most part, configurations are straight-forward:

`name`, `description`, `discardable`, and `cost` are all self-explanatory. 

`play-again` is whether or not the card lets you play again. 

`type` is the type of the card (i.e. what it costs). This is `:gems` `:bricks` or `:beasts`. 

`effects` is a bit more flexible at the cost of complexity - it is allowed to have simple logic. [Here is a discussion of effects.](https://github.com/MichaelDemone/CLIAcromage/blob/master/doc/effects.md)
