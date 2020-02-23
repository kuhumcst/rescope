rescope
=======

TODO: rewrite description below

A facsimile viewer implemented as a [reagent](https://github.com/reagent-project/reagent) component utilising [custom HTML components](https://developer.mozilla.org/en-US/docs/Web/Web_Components/Using_custom_elements) to embed XML directly into an HTML page.

The XML is converted into Clojure's ubiquitous hiccup format and each element patched to become a valid custom HTML element. The content is then placed inside a [shadow DOM](https://developer.mozilla.org/en-US/docs/Web/Web_Components/Using_shadow_DOM) which allows for CSS to apply to it in a scoped manner. The CSS is automatically patched to eccho the renaming that allows the XML to moonlight as HTML custom elements. This means regular CSS can be written directly for the XML even though it's embedded in an HTML page.

The main purpose is to be able to display [TEI-flavoured XML](https://tei-c.org/) next to a PDF/image viewer, but in fact any valid XML can be successfully embedded in an HTML page, provided a minimal CSS file is supplied with it to provide visual structure. This has - so far - mostly only been possible to do in browsers when serving an entire page's worth of content, although there is some prior art in the form of [CETEIcean](https://github.com/TEIC/CETEIcean). This project, however, seeks to be more flexible and less buggy in its implementation, while also integrating into a reactive UI and the rich [ClojureScript](https://clojurescript.org/) ecosystem.

Development prerequisites
-------------------------
The development workflow is built around the [Clojure CLI](https://clojure.org/reference/deps_and_cli) for managing dependencies and [shadow-cljs](https://github.com/thheller/shadow-cljs) for compiling ClojureScript code and providing a live-reloading development environment.

In this project, the dependency management feature of shadow-cljs is not used directly. Rather, we leverage the built-in support in shadow-cljs for the Clojure CLI/deps.edn to download dependencies and build a classpath.

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
clj -A:shadow-cljs -Spath
```

I have also set up some aliases in my personal [~/.clojure/deps.edn](https://github.com/simongray/dotfiles/blob/master/dot/clojure/deps.edn) file to perform certain common tasks such as listing/updating outdated packages:

```
clj -A:outdated
clj -A:update
```
