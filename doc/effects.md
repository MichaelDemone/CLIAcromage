# Effects

This section describes how to define effects in the Cards.csv file to achieve simple behaviours.

`effects` is a list of dictionaries going `[{dictionary1} {dictionary2} ... {dictionaryn}]` where each dictionary requires the following format

```{:effected (:you or :other) :resource (any resource) :amount (amount or logical statement) :set true/false}```


`:effected` is who this effect affects (`:you` for the current player, `:other` for the other player)

`:resource` is the resource that is effected, this can be `:gems` `:magic` `:beasts` `:zoo` `:bricks` `:quarry` `:tower` `:wall` or `:damage`

`:amount` is the amount to be added to the resource (or for the resource to be set to if the `:set` flag is set to true).

`:set` is an optional parameter that forces the resource that is effected to be set to the amount rather than being added. The usefulness of this becomes apparent in the examples.

## Simple Logic
Both `:resource` and `:amount` can have "simple logic" in them with the format:

```[some-function parameter1 parameter2 result-if-true result-if-false]```

`some-function` can be `>` `<` `<=` `>=` or just about any function that you have access to at runtime (not very secure, but I'm not too concerned!).

Every other parameter can either be an integer value or a "smart value" which I will describe later.

This can be read as `if (parameter1 some-function parameter2) then result-if-true otherwise result-if-false` so if some-function is <, parameter1 is 1, parameter2 is 2, result-if-true is 3, and result-if-false is 4, then you end up with `if (1 < 2) then 3 otherwise 4` and since 1 is less than 2, this expression returns 3.

### Smart Values
Each of the parameters in the simple logic expressions can be a "smart value" that has some context about the game. They have the following format:

`[:you/:other some-resource]`

`some-resource` is the resource to get from a player. This can be any of the values of `:resource` except `:damage`.

`:you/:other` is the person to get the resource from. `:you` for the current player and `:other` for the other player.

## Effect Examples
This level of flexibility is at the cost of complexity, so here is some examples to make it seem a bit less daunting.

### Simple Example
This example aims to be as simple as possible and examines the "Miners" card which has the description:

> +1 Quarry

and its effect is 

```[{:effected :you :resource :quarry :amount 1}]```

`:effected` is `:you` because it affects the current player

`:resource` is `:quarry` because it affects the quarry

`:amount` is `1` because it adds 1 to the player's total quarry.

### Simple Logic Example
This examples aims to be slightly more complex than the last.

The "Foundations" card has the description

> If you have no wall, +6 Wall, otherwise +3 Wall

and its effect is

```[{:effected :you :resource :wall :amount [= [:you :wall] 0 6 3]}]```

`:effected` is `:you` because it affects the current player.

`:resource` is `:wall` because it affects the wall

`:amount` is where the fun begins! it has the value `[= [:you :wall] 0 6 3]`

Breaking this down into the "simple logic" format discussed above we have

`some-function` is `=`

`parameter1` is `[:you :wall]` which can be interpreted as "current player's wall"

`parameter2` is `0`

`return-if-true` is `6`

`return-if-false` is `3`


This can be read as `if (current player's wall = 0) then 6 otherwise 3` which almost perfectly matches the description of the card.

### Slightly More Complex Logic Example
This will cover 2 "Smart values" in an effect.

The "Mother Lode" card has the description

> If your quarry is less than the enemies, +2 quarry. +1 otherwise.

and its effect is

```[{:effected :you :resource :quarry :amount [< [:you :quarry] [:other :quarry] 2 1]}]```

The part of concern is `:amount` which is `[< [:you :quarry] [:other :quarry] 2 1]`

Breaking this down again:

`some-function` is `<`

`parameter1` is `[:you :quarry]` which can be interpreted as "current player's quarry"

`parameter2` is `[:other :quarry]` which can be interpreted as "other player's quarry"

`return-if-true` is `2`

`return-if-false` is `1`


This can be read as `if (current player's wall = other player's wall) then 2 otherwise 1`. This roughly reflects the cards description.


### Complex Set Example
This example covers when set is useful. The "Parity" card is being examined and it has the defintion

> All player's magic equals the highest player's magic

with the effects

`[{:set true :effected :you :resource :magic :amount [< [:you :magic] [:other :magic] [:other :magic] [:you :magic]]} {:set true :effected :other :resource :magic :amount [< [:you :magic] [:other :magic] [:other :magic] [:you :magic]]}]`

So there is 2 effects going on here, 1 to set the current player's magic to the highest player's magic, and 1 to set the enemies magic to the highest player's magic. We will only examine one because doing both feels redundant.

`{:set true :effected :you :resource :magic :amount [< [:you :magic] [:other :magic] [:other :magic] [:you :magic]]}`

`:effected` is `:you` because this is dealing with setting your magic to the highest player's magic.

`:resource` is `:magic` because it's dealing with magic.

`:amount` is `[< [:you :magic] [:other :magic] [:other :magic] [:you :magic]]`

which broken down can be

`some-function` is `<` or "is less than"

`parameter1` is `[:you :magic]` or "current player's magic"

`parameter2` is `[:other :magic]` or "other player's magic"

`result-if-true` is `[:other :magic]` or "other player's magic"

`result-if-false` is `[:you :magic]` or "current player's magic"

putting it all together this becomes `if (current player's magic is less than other player's magic) then other player's magic otherwise current player's magic` and the `:set true` flag says to `SET the current player's magic to that amount` rather than add that amount.

## Summary
Hopefully with these examples you can create new cards! Remember, both `:resource` and `:amount` can contain logic, and each parameter can be be a Smart Value. Good luck :D!
