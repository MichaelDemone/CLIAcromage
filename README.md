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
This project has several config files that allow you to change how the game behaves.

#### Cards
Currently all of the cards are defined in [resources/Cards.csv](https://github.com/MichaelDemone/CLIAcromage/blob/master/resources/Cards.csv) and parsed with my own parser. You can load the CSV file into google sheets/excel and add or remove cards to your heart's content. For the most part, configurations are straight-forward:

`name`, `description`, `discardable`, and `cost` are all self-explanatory. 

`play-again` is whether or not the card lets you play again. 

`type` is the type of the card (i.e. what it costs). This is `:gems` `:bricks` or `:beasts`. 

`effects` is a bit more flexible at the cost of complexity - it is allowed to have simple logic. [Here is a discussion of effects.](https://github.com/MichaelDemone/CLIAcromage/blob/master/doc/effects.md)

#### Game Types
At the beginning of the game, the user is prompted to put in what kind of game they wish to play. This is completely configurable and is done in [resources/Games.csv](https://github.com/MichaelDemone/CLIAcromage/blob/master/resources/Games.csv).

Each game is defined with the following:

`name` is the name of the game

`starting-tower` is the amount of tower each player starts with

`starting-wall` is the amount of wall each player starts with

`start-resource` is the amount of bricks, gems, and beasts each player starts with

`start-resource-gain` is the amoutn of quarry, magic, and zoo each player starts with

`max-resource` is the amount of bricks, gems, or beasts required to win the game

`max-tower` is the amount of tower required to win the game

### Bot Training
You can battle bots by running `lein repl` and calling `evo/train-ai` where it will run a small tournament for bots and tell you how the best bot behaves using a scoring system that you can then add into main and fight that bot yourself!
