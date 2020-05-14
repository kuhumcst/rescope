Cuphic
======
Cuphic is a macro-free, declarative DSL for performing [Hiccup](https://github.com/weavejester/hiccup) data transformations in Clojure/ClojureScript. It is designed to be easy to use, but also simple to understand. It strongly resembles Hiccup.

The name is pronounced *CUP*hic, not *QUEUE*fig.

What about \<OTHER LIBRARY\>?
-----------------------------
After researching various alternatives, I started using [Meander](https://github.com/noprompt/meander) for doing `hiccup->hiccup` data transformations in my other project, [rescope](https://github.com/kuhumcst/rescope).

While Meander is quite capable, its universal DSL didn't _seem_ easier to read or write (to me) than normal Clojure code. The main reason to prefer a declarative DSL is because it makes things clearer. I think the issue is that Meander has to accommodate completely heterogeneous data, so its DSL can't rely on any implicit assumptions about the shape of the data.

Cuphic is only for Hiccup
-------------------------
Cuphic **only** handles Hiccup and is designed to look pretty much like it (with some logic variables sprinkled on). It respects the set of assumptions that come with looking like Hiccup.
 
It's not dogmatic about being declarative, either. If you ever need to veer into algorithm territory, you can just leave the declarative DSL and substitute either of the two Cuphic templates with an equivalent function. The _from_ template can be replaced with a `hiccup->bindings` function; the _to_ template with a `bindings->hiccup` function.

In most common cases, you should be able to rely on just Cuphic templates.

Example usage
-------------
TODO!!!