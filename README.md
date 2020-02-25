rescope
=======
With `rescope`, modern browser features like the [shadow DOM](https://developer.mozilla.org/en-US/docs/Web/Web_Components/Using_shadow_DOM) and [custom elements](https://developer.mozilla.org/en-US/docs/Web/Web_Components/Using_custom_elements) let you embed  most documents directly in a [re-frame](https://github.com/day8/re-frame) or [reagent](https://github.com/reagent-project/reagent) application. Rescoping requires very little code, mostly just CSS. At the same time, escape hatches are provided for when you need to restructure or add interactivity to your documents.

Using [rescope.formats.xml](https://github.com/kuhumcst/rescope/tree/master/src/kuhumcst/rescope/formats), you can embed XML directly in your web page as if it were completely valid HTML. XML is no longer just for data exchange, but can be viewed as a visualisation of a document, a widget, or maybe even a little application.

CSS is automatically patched to reflect the tag & attribute renaming that allows the XML to moonlight as HTML. This means regular CSS can be written directly for the XML. And since both CSS and XML are scoped inside a shadow DOM, there is no chance of clashing with any other CSS on the page.

However, `rescope` can embed, style, and patch _any_ [hiccup-like](https://github.com/weavejester/hiccup) structure -- quite handy in the ClojureScript world! For example, the default output of [instaparse](https://github.com/Engelberg/instaparse) is a hiccup-like structure. The implication is clear: anything that can be parsed can potentially be rescoped as an interactive reagent component.

Quickstart
----------
TODO: write a small introduction with some examples.

Origin
------
The original purpose was to be able to display [TEI-flavoured XML](https://tei-c.org/) next to a PDF/image viewer while preserving the XML structure entirely. This has -- so far -- mostly only been possible to do in browsers when serving an entire page's worth of relatively static XML content, although there is some prior art in the form of [CETEIcean](https://github.com/TEIC/CETEIcean) which also uses custom elements. This project, however, seeks to be more flexible in its implementation, while integrating into a reactive UI paradigm and the rich [ClojureScript](https://clojurescript.org/) ecosystem.

Development prerequisites
-------------------------
_NOTE: this part is only relevant if you're developing the rescope library itself._

The development workflow of the project itself is built around the [Clojure CLI](https://clojure.org/reference/deps_and_cli) for managing dependencies and [shadow-cljs](https://github.com/thheller/shadow-cljs) for compiling ClojureScript code and providing a live-reloading development environment.

In this project, the dependency management feature of shadow-cljs is not used directly. Rather, I leverage the built-in support in shadow-cljs for the Clojure CLI/deps.edn to download dependencies and build a classpath.

I personally use IntelliJ with the Cursive plugin which [integrates quite well with the Clojure CLI](https://cursive-ide.com/userguide/deps.html).

### macOS setup
(assuming [homebrew](https://brew.sh/) has already been installed)


I'm not sure which JDK version you need, but anything 8+ is probably fine! I personally just use the latest from AdoptOpenJDK (currently JDK 13):

```
brew cask install adoptopenjdk
```

The following will get you the Clojure CLI and shadow-cljs, along with NodeJS:

```
brew install clojure
brew install node
npm install -g shadow-cljs
```

Workflow
--------
Development of the component is done using the live-reloading capabilities of shadow-cljs:

```
shadow-cljs watch dev
```

This will start a basic web server at `localhost:8000` serving the `:dev` build as specified in the `shadow-cljs.edn` file.

It's possible to execute unit tests while developing by also specifying the `:test` build:

```
shadow-cljs watch dev test
```

This will make test output available at `localhost:8001`. It's quite convenient to keep a separate browser tab open just for this. The favicon will be coloured green or red depending on the state of the assertions.

Personally, I use the Clojure CLI integration in Cursive to calculate a classpath and download dependencies. Something like this command is being executed behind the scenes:

```
clj -A:dev:test -Spath
```

I have also set up some aliases in my personal [~/.clojure/deps.edn](https://github.com/simongray/dotfiles/blob/master/dot/clojure/deps.edn) file to perform certain common tasks such as listing/updating outdated packages:

```
clj -A:outdated
clj -A:update
```
